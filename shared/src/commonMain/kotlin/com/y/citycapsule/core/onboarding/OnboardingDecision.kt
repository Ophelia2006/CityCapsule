package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

enum class StartupDestination {
    HOME,
    ONBOARDING
}

enum class StartupReason {
    COMPLETED,
    NOT_COMPLETED,
    OUTDATED,
    PROFILE_MISSING,
    PROFILE_INVALID,
    COMPLETION_INVALID,
    STORAGE_UNAVAILABLE
}

data class StartupRepair(
    val clearCompletionVersion: Boolean = false,
    val clearProfile: Boolean = false,
    val clearDraft: Boolean = false
) {
    val isRequired: Boolean
        get() = clearCompletionVersion || clearProfile || clearDraft
}

data class StartupDecision(
    val destination: StartupDestination,
    val reason: StartupReason,
    val profile: LocalProfile? = null,
    val draft: OnboardingDraft? = null,
    val warnings: List<StorageError> = emptyList(),
    val repair: StartupRepair = StartupRepair()
)

/** Pure startup decision table shared by both platform entry flows. */
object OnboardingStartupDecider {
    fun decide(
        completedVersion: StorageResult<Long>,
        profile: StorageResult<LocalProfile>,
        draft: StorageResult<OnboardingDraft>
    ): StartupDecision {
        val draftState = resolveDraft(draft)
        return when (completedVersion) {
            is StorageResult.Success -> {
                if (completedVersion.value >= OnboardingContract.CURRENT_COMPLETED_VERSION) {
                    decideCompleted(profile, draftState)
                } else {
                    decideIncomplete(
                        baseReason = if (completedVersion.value <= 0L) {
                            StartupReason.NOT_COMPLETED
                        } else {
                            StartupReason.OUTDATED
                        },
                        profile = profile,
                        draftState = draftState
                    )
                }
            }
            StorageResult.Missing -> decideIncomplete(
                baseReason = StartupReason.NOT_COMPLETED,
                profile = profile,
                draftState = draftState
            )
            is StorageResult.Failure -> {
                if (completedVersion.error.isCorruptValue()) {
                    decideIncomplete(
                        baseReason = StartupReason.COMPLETION_INVALID,
                        profile = profile,
                        draftState = draftState,
                        initialWarnings = listOf(completedVersion.error),
                        initialRepair = StartupRepair(clearCompletionVersion = true)
                    )
                } else {
                    StartupDecision(
                        destination = StartupDestination.ONBOARDING,
                        reason = StartupReason.STORAGE_UNAVAILABLE,
                        profile = profile.successValueOrNull(),
                        draft = draftState.value,
                        warnings = listOf(completedVersion.error) + draftState.warnings,
                        repair = draftState.repair
                    )
                }
            }
        }
    }

    private fun decideCompleted(
        profile: StorageResult<LocalProfile>,
        draftState: DraftState
    ): StartupDecision = when (profile) {
        is StorageResult.Success -> StartupDecision(
            destination = StartupDestination.HOME,
            reason = StartupReason.COMPLETED,
            profile = profile.value,
            warnings = draftState.warnings,
            repair = draftState.repair.copy(
                clearDraft = draftState.value != null || draftState.repair.clearDraft
            )
        )
        StorageResult.Missing -> StartupDecision(
            destination = StartupDestination.ONBOARDING,
            reason = StartupReason.PROFILE_MISSING,
            draft = draftState.value,
            warnings = draftState.warnings,
            repair = draftState.repair.copy(clearCompletionVersion = true)
        )
        is StorageResult.Failure -> {
            if (profile.error.isCorruptValue()) {
                StartupDecision(
                    destination = StartupDestination.ONBOARDING,
                    reason = StartupReason.PROFILE_INVALID,
                    draft = draftState.value,
                    warnings = listOf(profile.error) + draftState.warnings,
                    repair = draftState.repair.copy(
                        clearCompletionVersion = true,
                        clearProfile = true
                    )
                )
            } else {
                StartupDecision(
                    destination = StartupDestination.ONBOARDING,
                    reason = StartupReason.STORAGE_UNAVAILABLE,
                    draft = draftState.value,
                    warnings = listOf(profile.error) + draftState.warnings,
                    repair = draftState.repair
                )
            }
        }
    }

    private fun decideIncomplete(
        baseReason: StartupReason,
        profile: StorageResult<LocalProfile>,
        draftState: DraftState,
        initialWarnings: List<StorageError> = emptyList(),
        initialRepair: StartupRepair = StartupRepair()
    ): StartupDecision = when (profile) {
        is StorageResult.Success -> StartupDecision(
            destination = StartupDestination.ONBOARDING,
            reason = baseReason,
            profile = profile.value,
            draft = draftState.value,
            warnings = initialWarnings + draftState.warnings,
            repair = initialRepair.merge(draftState.repair)
        )
        StorageResult.Missing -> StartupDecision(
            destination = StartupDestination.ONBOARDING,
            reason = baseReason,
            draft = draftState.value,
            warnings = initialWarnings + draftState.warnings,
            repair = initialRepair.merge(draftState.repair)
        )
        is StorageResult.Failure -> {
            val corrupt = profile.error.isCorruptValue()
            StartupDecision(
                destination = StartupDestination.ONBOARDING,
                reason = if (corrupt) {
                    StartupReason.PROFILE_INVALID
                } else {
                    StartupReason.STORAGE_UNAVAILABLE
                },
                draft = draftState.value,
                warnings = initialWarnings + profile.error + draftState.warnings,
                repair = initialRepair.merge(draftState.repair).copy(
                    clearProfile = corrupt || initialRepair.clearProfile
                )
            )
        }
    }

    private fun resolveDraft(result: StorageResult<OnboardingDraft>): DraftState = when (result) {
        is StorageResult.Success -> DraftState(value = result.value)
        StorageResult.Missing -> DraftState()
        is StorageResult.Failure -> DraftState(
            warnings = listOf(result.error),
            repair = StartupRepair(clearDraft = result.error.isCorruptValue())
        )
    }

    private data class DraftState(
        val value: OnboardingDraft? = null,
        val warnings: List<StorageError> = emptyList(),
        val repair: StartupRepair = StartupRepair()
    )
}

private fun StorageError.isCorruptValue(): Boolean =
    code == StorageErrorCode.TYPE_MISMATCH || code == StorageErrorCode.DECODE_FAILED

private fun StorageResult<LocalProfile>.successValueOrNull(): LocalProfile? =
    (this as? StorageResult.Success)?.value

private fun StartupRepair.merge(other: StartupRepair): StartupRepair = StartupRepair(
    clearCompletionVersion = clearCompletionVersion || other.clearCompletionVersion,
    clearProfile = clearProfile || other.clearProfile,
    clearDraft = clearDraft || other.clearDraft
)
