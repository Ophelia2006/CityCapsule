package com.y.citycapsule.feature.profile

import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.profile.LocalProfileSource
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.feature.onboarding.FeatureNotice
import com.y.citycapsule.feature.onboarding.FeatureNoticeTone

enum class ProfileUiStatus {
    LOADING,
    READY,
    SAVING,
    CLEARING
}

data class ProfileUiState(
    val status: ProfileUiStatus = ProfileUiStatus.LOADING,
    val profile: LocalProfile = LocalProfile.DEFAULT,
    val savedProfile: LocalProfile = LocalProfile.DEFAULT,
    val editing: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val validationMessage: String? = null,
    val notice: FeatureNotice? = null
) {
    val isBusy: Boolean
        get() = status != ProfileUiStatus.READY
}

class ProfileStateHolder(
    private val profileRepository: LocalProfileRepository,
    private val onboardingRepository: OnboardingRepository,
    private val onStateChanged: (ProfileUiState) -> Unit = {}
) {
    var state: ProfileUiState = ProfileUiState()
        private set

    fun load() {
        update(state.copy(status = ProfileUiStatus.LOADING))
        profileRepository.getProfileSnapshot { snapshot ->
            val notice = when (snapshot.source) {
                LocalProfileSource.PERSISTED -> FeatureNotice(
                    "档案只保存在当前设备。",
                    FeatureNoticeTone.NEUTRAL
                )
                LocalProfileSource.DEFAULT_MISSING -> FeatureNotice(
                    "尚未保存档案，当前显示默认档案。",
                    FeatureNoticeTone.WARNING
                )
                LocalProfileSource.DEFAULT_RECOVERY -> FeatureNotice(
                    "本地存储暂不可用，当前显示默认档案。",
                    FeatureNoticeTone.WARNING
                )
            }
            update(
                ProfileUiState(
                    status = ProfileUiStatus.READY,
                    profile = snapshot.profile,
                    savedProfile = snapshot.profile,
                    notice = notice
                )
            )
        }
    }

    fun beginEditing() {
        if (!state.isBusy) {
            update(state.copy(editing = true, validationMessage = null))
        }
    }

    fun cancelEditing() {
        if (!state.isBusy) {
            update(
                state.copy(
                    profile = state.savedProfile,
                    editing = false,
                    validationMessage = null
                )
            )
        }
    }

    fun updateDisplayName(value: String) {
        updateProfile { copy(displayName = value) }
    }

    fun updateAvatar(preset: AvatarPreset) {
        updateProfile { copy(avatarPreset = preset) }
    }

    fun updateHomeCity(value: String) {
        updateProfile { copy(homeCity = value) }
    }

    fun updateBio(value: String) {
        updateProfile { copy(bio = value) }
    }

    fun save() {
        if (state.isBusy) {
            return
        }
        val normalized = LocalProfileValidator.normalizeOrNull(state.profile)
        if (normalized == null) {
            update(
                state.copy(
                    validationMessage = "请检查昵称、城市和简介的长度。"
                )
            )
            return
        }
        update(
            state.copy(
                status = ProfileUiStatus.SAVING,
                validationMessage = null,
                notice = FeatureNotice("正在保存本地档案…", FeatureNoticeTone.NEUTRAL)
            )
        )
        profileRepository.saveProfile(normalized) { result ->
            when (result) {
                is StorageResult.Success -> update(
                    state.copy(
                        status = ProfileUiStatus.READY,
                        profile = normalized,
                        savedProfile = normalized,
                        editing = false,
                        notice = FeatureNotice(
                            "本地档案已保存。",
                            FeatureNoticeTone.SUCCESS
                        )
                    )
                )
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        status = ProfileUiStatus.READY,
                        notice = FeatureNotice(
                            "保存失败，修改仍保留在当前页面，请重试。",
                            FeatureNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    fun requestClear() {
        if (!state.isBusy) {
            update(state.copy(showClearConfirmation = true))
        }
    }

    fun dismissClear() {
        if (!state.isBusy) {
            update(state.copy(showClearConfirmation = false))
        }
    }

    fun clear(onCleared: () -> Unit) {
        if (state.isBusy) {
            return
        }
        update(
            state.copy(
                status = ProfileUiStatus.CLEARING,
                showClearConfirmation = false,
                notice = FeatureNotice("正在清除本地档案…", FeatureNoticeTone.NEUTRAL)
            )
        )
        onboardingRepository.resetLocalState { result ->
            when (result) {
                is StorageResult.Success -> {
                    update(
                        ProfileUiState(
                            status = ProfileUiStatus.READY,
                            notice = FeatureNotice(
                                "本地档案已清除。",
                                FeatureNoticeTone.SUCCESS
                            )
                        )
                    )
                    onCleared()
                }
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        status = ProfileUiStatus.READY,
                        notice = FeatureNotice(
                            "未能完整清除本地档案，请重试。",
                            FeatureNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    private fun updateProfile(transform: LocalProfile.() -> LocalProfile) {
        if (!state.isBusy && state.editing) {
            update(
                state.copy(
                    profile = state.profile.transform(),
                    validationMessage = null
                )
            )
        }
    }

    private fun update(nextState: ProfileUiState) {
        state = nextState
        onStateChanged(nextState)
    }
}
