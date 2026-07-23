package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageBatchResult
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult

class OnboardingRepository(
    private val storage: KeyValueStore
) {
    fun getStartupDecision(callback: (StartupDecision) -> Unit) {
        storage.getMany(STARTUP_KEYS) { batchResult ->
            when (batchResult) {
                is StorageResult.Success -> {
                    val decision = OnboardingStartupDecider.decide(
                        completedVersion = batchResult.value.typedResult(
                            AppStorageKeys.Onboarding.COMPLETED_VERSION
                        ),
                        profile = batchResult.value.typedResult(
                            AppStorageKeys.Profile.LOCAL_PROFILE
                        ),
                        draft = batchResult.value.typedResult(
                            AppStorageKeys.Onboarding.DRAFT
                        )
                    )
                    applyStartupRepair(decision, callback)
                }
                StorageResult.Missing -> callback(
                    unavailableDecision(
                        unexpectedMissing("Startup batch read returned missing.")
                    )
                )
                is StorageResult.Failure -> callback(unavailableDecision(batchResult.error))
            }
        }
    }

    fun saveDraft(draft: OnboardingDraft, callback: StorageCallback<Unit>) {
        val normalized = draft.normalizedOrNull()
        if (normalized == null) {
            callback(invalidRequest("Onboarding draft does not satisfy schema v1 validation."))
            return
        }
        storage.put(AppStorageKeys.Onboarding.DRAFT, normalized, callback)
    }

    /**
     * Two-phase local commit: profile first, completion marker last.
     *
     * Draft cleanup is best effort after the durable commit and cannot turn a completed
     * onboarding transaction back into a failure.
     */
    fun complete(
        profile: LocalProfile,
        callback: StorageCallback<OnboardingCompletion>
    ) {
        val normalized = LocalProfileValidator.normalizeOrNull(profile)
        if (normalized == null) {
            callback(invalidRequest("Local profile does not satisfy schema v1 validation."))
            return
        }
        storage.put(AppStorageKeys.Profile.LOCAL_PROFILE, normalized) { profileWrite ->
            when (profileWrite) {
                is StorageResult.Success -> writeCompletionMarker(normalized, callback)
                StorageResult.Missing -> callback(
                    StorageResult.Failure(
                        unexpectedMissing("Profile write returned missing.")
                    )
                )
                is StorageResult.Failure -> callback(profileWrite)
            }
        }
    }

    fun clearDraft(callback: StorageCallback<Unit>) {
        storage.remove(AppStorageKeys.Onboarding.DRAFT, callback)
    }

    /**
     * Reset order is safety-critical: remove completion first, then profile, then draft.
     * Every removal is attempted; the first failure is reported after all cleanup attempts.
     */
    fun resetLocalState(callback: StorageCallback<Unit>) {
        removeAll(
            keys = listOf(
                AppStorageKeys.Onboarding.COMPLETED_VERSION,
                AppStorageKeys.Profile.LOCAL_PROFILE,
                AppStorageKeys.Onboarding.DRAFT
            ),
            callback = callback
        )
    }

    private fun writeCompletionMarker(
        profile: LocalProfile,
        callback: StorageCallback<OnboardingCompletion>
    ) {
        storage.put(
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            OnboardingContract.CURRENT_COMPLETED_VERSION
        ) { completionWrite ->
            when (completionWrite) {
                is StorageResult.Success -> {
                    storage.remove(AppStorageKeys.Onboarding.DRAFT) { cleanup ->
                        callback(
                            StorageResult.Success(
                                OnboardingCompletion(
                                    profile = profile,
                                    completedVersion =
                                    OnboardingContract.CURRENT_COMPLETED_VERSION,
                                    cleanupWarning = (cleanup as? StorageResult.Failure)?.error
                                )
                            )
                        )
                    }
                }
                StorageResult.Missing -> callback(
                    StorageResult.Failure(
                        unexpectedMissing("Completion marker write returned missing.")
                    )
                )
                is StorageResult.Failure -> callback(completionWrite)
            }
        }
    }

    private fun applyStartupRepair(
        decision: StartupDecision,
        callback: (StartupDecision) -> Unit
    ) {
        if (!decision.repair.isRequired) {
            callback(decision)
            return
        }
        val keys = buildList<StorageKey<*>> {
            if (decision.repair.clearCompletionVersion) {
                add(AppStorageKeys.Onboarding.COMPLETED_VERSION)
            }
            if (decision.repair.clearProfile) {
                add(AppStorageKeys.Profile.LOCAL_PROFILE)
            }
            if (decision.repair.clearDraft) {
                add(AppStorageKeys.Onboarding.DRAFT)
            }
        }
        removeAllCollectingWarnings(keys) { repairWarnings ->
            callback(
                decision.copy(
                    warnings = decision.warnings + repairWarnings,
                    repair = StartupRepair()
                )
            )
        }
    }

    private fun removeAll(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<Unit>
    ) {
        removeAllCollectingWarnings(keys) { warnings ->
            callback(
                warnings.firstOrNull()?.let { StorageResult.Failure(it) }
                    ?: StorageResult.Success(Unit)
            )
        }
    }

    private fun removeAllCollectingWarnings(
        keys: List<StorageKey<*>>,
        callback: (List<StorageError>) -> Unit
    ) {
        val warnings = mutableListOf<StorageError>()

        fun removeAt(index: Int) {
            if (index >= keys.size) {
                callback(warnings)
                return
            }
            storage.remove(keys[index]) { result ->
                when (result) {
                    is StorageResult.Success -> Unit
                    StorageResult.Missing -> Unit
                    is StorageResult.Failure -> warnings += result.error
                }
                removeAt(index + 1)
            }
        }

        removeAt(0)
    }

    private companion object {
        val STARTUP_KEYS: List<StorageKey<*>> = listOf(
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            AppStorageKeys.Profile.LOCAL_PROFILE,
            AppStorageKeys.Onboarding.DRAFT
        )
    }
}

data class OnboardingCompletion(
    val profile: LocalProfile,
    val completedVersion: Long,
    val cleanupWarning: StorageError? = null
)

private inline fun <reified T> StorageBatchResult.typedResult(
    key: StorageKey<T>
): StorageResult<T> {
    val untyped = entries.firstOrNull {
        it.key.store == key.store && it.key.wireKey == key.wireKey
    }?.result ?: return StorageResult.Failure(
        StorageError(
            StorageErrorCode.NATIVE_FAILURE,
            "Startup batch response omitted '${key.wireKey}'."
        )
    )
    return when (untyped) {
        is StorageResult.Success -> {
            val value = untyped.value
            if (value is T) {
                StorageResult.Success(value)
            } else {
                StorageResult.Failure(
                    StorageError(
                        StorageErrorCode.TYPE_MISMATCH,
                        "Startup value for '${key.wireKey}' has an unexpected type."
                    )
                )
            }
        }
        StorageResult.Missing -> StorageResult.Missing
        is StorageResult.Failure -> untyped
    }
}

private fun unavailableDecision(error: StorageError): StartupDecision = StartupDecision(
    destination = StartupDestination.ONBOARDING,
    reason = StartupReason.STORAGE_UNAVAILABLE,
    warnings = listOf(error)
)

private fun unexpectedMissing(message: String): StorageError = StorageError(
    StorageErrorCode.NATIVE_FAILURE,
    message
)

private fun <T> invalidRequest(message: String): StorageResult<T> = StorageResult.Failure(
    StorageError(StorageErrorCode.INVALID_REQUEST, message)
)
