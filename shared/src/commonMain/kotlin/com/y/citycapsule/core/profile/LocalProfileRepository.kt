package com.y.citycapsule.core.profile

import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

class LocalProfileRepository(
    private val storage: KeyValueStore
) {
    /** Strict read for startup decisions, diagnostics, and tests. */
    fun getProfile(callback: StorageCallback<LocalProfile>) {
        storage.get(AppStorageKeys.Profile.LOCAL_PROFILE, callback)
    }

    /** Business read that always supplies a renderable, non-sensitive default profile. */
    fun getProfileSnapshot(callback: (LocalProfileSnapshot) -> Unit) {
        getProfile { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> LocalProfileSnapshot(
                        profile = result.value,
                        source = LocalProfileSource.PERSISTED
                    )
                    StorageResult.Missing -> LocalProfileSnapshot(
                        profile = LocalProfile.DEFAULT,
                        source = LocalProfileSource.DEFAULT_MISSING
                    )
                    is StorageResult.Failure -> LocalProfileSnapshot(
                        profile = LocalProfile.DEFAULT,
                        source = LocalProfileSource.DEFAULT_RECOVERY,
                        warning = result.error
                    )
                }
            )
        }
    }

    fun saveProfile(profile: LocalProfile, callback: StorageCallback<Unit>) {
        val normalized = LocalProfileValidator.normalizeOrNull(profile)
        if (normalized == null) {
            callback(
                StorageResult.Failure(
                    StorageError(
                        StorageErrorCode.INVALID_REQUEST,
                        "Local profile does not satisfy schema v1 validation."
                    )
                )
            )
            return
        }
        storage.put(AppStorageKeys.Profile.LOCAL_PROFILE, normalized, callback)
    }

    fun clearProfile(callback: StorageCallback<Unit>) {
        storage.remove(AppStorageKeys.Profile.LOCAL_PROFILE, callback)
    }
}

enum class LocalProfileSource {
    PERSISTED,
    DEFAULT_MISSING,
    DEFAULT_RECOVERY
}

data class LocalProfileSnapshot(
    val profile: LocalProfile,
    val source: LocalProfileSource,
    val warning: StorageError? = null
)
