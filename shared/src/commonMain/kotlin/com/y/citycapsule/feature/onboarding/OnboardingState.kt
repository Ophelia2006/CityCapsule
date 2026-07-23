package com.y.citycapsule.feature.onboarding

import com.y.citycapsule.core.onboarding.OnboardingDraft
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.onboarding.OnboardingStep
import com.y.citycapsule.core.onboarding.StartupDecision
import com.y.citycapsule.core.onboarding.StartupReason
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.storage.StorageResult

enum class OnboardingUiStatus {
    LOADING,
    READY,
    SAVING_DRAFT,
    SUBMITTING,
    COMPLETED
}

enum class FeatureNoticeTone {
    NEUTRAL,
    SUCCESS,
    WARNING,
    ERROR
}

data class FeatureNotice(
    val message: String,
    val tone: FeatureNoticeTone
)

data class OnboardingUiState(
    val status: OnboardingUiStatus = OnboardingUiStatus.LOADING,
    val draft: OnboardingDraft = OnboardingDraft.EMPTY,
    val notice: FeatureNotice? = null,
    val validationMessage: String? = null
) {
    val isBusy: Boolean
        get() = status == OnboardingUiStatus.LOADING ||
            status == OnboardingUiStatus.SAVING_DRAFT ||
            status == OnboardingUiStatus.SUBMITTING
}

class OnboardingStateHolder(
    private val repository: OnboardingRepository,
    private val onStateChanged: (OnboardingUiState) -> Unit = {}
) {
    var state: OnboardingUiState = OnboardingUiState()
        private set

    fun load() {
        update(state.copy(status = OnboardingUiStatus.LOADING))
        repository.getStartupDecision { decision ->
            val restoredDraft = decision.draft
                ?: decision.profile?.toDraft()
                ?: OnboardingDraft.EMPTY
            update(
                OnboardingUiState(
                    status = OnboardingUiStatus.READY,
                    draft = restoredDraft,
                    notice = decision.toNotice()
                )
            )
        }
    }

    fun updateDisplayName(value: String) {
        updateDraft { copy(displayName = value) }
    }

    fun updateAvatar(preset: AvatarPreset) {
        updateDraft { copy(avatarPreset = preset) }
    }

    fun updateHomeCity(value: String) {
        updateDraft { copy(homeCity = value) }
    }

    fun updateBio(value: String) {
        updateDraft { copy(bio = value) }
    }

    fun next(): Boolean {
        if (state.isBusy) {
            return false
        }
        val nextStep = when (state.draft.currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.IDENTITY
            OnboardingStep.IDENTITY -> {
                if (state.draft.toLocalProfileOrNull() == null) {
                    update(
                        state.copy(
                            validationMessage = "请填写 1～20 个字符的昵称后继续。"
                        )
                    )
                    return false
                }
                OnboardingStep.DETAILS
            }
            OnboardingStep.DETAILS -> {
                if (state.draft.toLocalProfileOrNull() == null) {
                    update(
                        state.copy(
                            validationMessage = "请检查昵称、城市和简介的长度。"
                        )
                    )
                    return false
                }
                OnboardingStep.REVIEW
            }
            OnboardingStep.REVIEW -> return false
        }
        persist(state.draft.copy(currentStep = nextStep))
        return true
    }

    /** Returns false only when the caller should close the current route. */
    fun previous(): Boolean {
        if (state.isBusy) {
            return true
        }
        val previousStep = when (state.draft.currentStep) {
            OnboardingStep.WELCOME -> return false
            OnboardingStep.IDENTITY -> OnboardingStep.WELCOME
            OnboardingStep.DETAILS -> OnboardingStep.IDENTITY
            OnboardingStep.REVIEW -> OnboardingStep.DETAILS
        }
        persist(state.draft.copy(currentStep = previousStep))
        return true
    }

    fun complete(onCompleted: (LocalProfile) -> Unit) {
        val profile = state.draft.toLocalProfileOrNull()
        if (profile == null) {
            update(
                state.copy(
                    status = OnboardingUiStatus.READY,
                    validationMessage = "档案信息不完整，请返回检查后重试。"
                )
            )
            return
        }
        completeProfile(profile, onCompleted)
    }

    fun useDefaultProfile(onCompleted: (LocalProfile) -> Unit) {
        completeProfile(LocalProfile.DEFAULT, onCompleted)
    }

    private fun completeProfile(
        profile: LocalProfile,
        onCompleted: (LocalProfile) -> Unit
    ) {
        if (state.isBusy) {
            return
        }
        update(
            state.copy(
                status = OnboardingUiStatus.SUBMITTING,
                validationMessage = null,
                notice = FeatureNotice(
                    "正在保存本地档案…",
                    FeatureNoticeTone.NEUTRAL
                )
            )
        )
        repository.complete(profile) { result ->
            when (result) {
                is StorageResult.Success -> {
                    update(
                        state.copy(
                            status = OnboardingUiStatus.COMPLETED,
                            draft = result.value.profile.toDraft(OnboardingStep.REVIEW),
                            notice = FeatureNotice(
                                if (result.value.cleanupWarning == null) {
                                    "本地档案已保存。"
                                } else {
                                    "档案已保存，旧草稿将在下次启动时继续清理。"
                                },
                                if (result.value.cleanupWarning == null) {
                                    FeatureNoticeTone.SUCCESS
                                } else {
                                    FeatureNoticeTone.WARNING
                                }
                            )
                        )
                    )
                    onCompleted(result.value.profile)
                }
                StorageResult.Missing -> completeFailed()
                is StorageResult.Failure -> completeFailed()
            }
        }
    }

    private fun completeFailed() {
        update(
            state.copy(
                status = OnboardingUiStatus.READY,
                notice = FeatureNotice(
                    "暂时无法保存，输入内容仍保留在当前页面，请重试。",
                    FeatureNoticeTone.ERROR
                )
            )
        )
    }

    private fun persist(draft: OnboardingDraft) {
        update(
            state.copy(
                status = OnboardingUiStatus.SAVING_DRAFT,
                draft = draft,
                validationMessage = null
            )
        )
        repository.saveDraft(draft) { result ->
            when (result) {
                is StorageResult.Success -> update(
                    state.copy(
                        status = OnboardingUiStatus.READY,
                        notice = null
                    )
                )
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        status = OnboardingUiStatus.READY,
                        notice = FeatureNotice(
                            "当前步骤可以继续，但进度暂时无法持久化。",
                            FeatureNoticeTone.WARNING
                        )
                    )
                )
            }
        }
    }

    private fun updateDraft(transform: OnboardingDraft.() -> OnboardingDraft) {
        if (state.isBusy) {
            return
        }
        update(
            state.copy(
                draft = state.draft.transform(),
                validationMessage = null
            )
        )
    }

    private fun update(nextState: OnboardingUiState) {
        state = nextState
        onStateChanged(nextState)
    }
}

private fun LocalProfile.toDraft(
    step: OnboardingStep = OnboardingStep.WELCOME
): OnboardingDraft = OnboardingDraft(
    currentStep = step,
    displayName = displayName,
    avatarPreset = avatarPreset,
    homeCity = homeCity,
    bio = bio
)

private fun StartupDecision.toNotice(): FeatureNotice? = when (reason) {
    StartupReason.STORAGE_UNAVAILABLE -> FeatureNotice(
        "本地存储暂不可用；你仍可浏览引导，但本次进度可能无法保存。",
        FeatureNoticeTone.WARNING
    )
    StartupReason.PROFILE_INVALID,
    StartupReason.COMPLETION_INVALID -> FeatureNotice(
        "检测到不完整的本地数据，已安全恢复到引导流程。",
        FeatureNoticeTone.WARNING
    )
    StartupReason.PROFILE_MISSING -> FeatureNotice(
        "本地档案缺失，请重新完成引导。",
        FeatureNoticeTone.WARNING
    )
    StartupReason.OUTDATED -> FeatureNotice(
        "首次引导已更新，请确认你的本地档案。",
        FeatureNoticeTone.NEUTRAL
    )
    StartupReason.COMPLETED,
    StartupReason.NOT_COMPLETED -> null
}
