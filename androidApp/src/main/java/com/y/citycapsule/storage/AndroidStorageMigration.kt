package com.y.citycapsule.storage

import android.content.Context
import com.y.citycapsule.core.storage.StorageMigrationContract
import com.y.citycapsule.core.storage.StorageMigrationState
import com.y.citycapsule.core.storage.StorageStore

internal data class AndroidLegacyThemeValue(
    val present: Boolean,
    val value: String?
)

internal interface AndroidLegacySettingsSource {
    fun readThemeMode(): AndroidLegacyThemeValue

    fun clearThemeMode()
}

internal class AndroidSharedPreferencesLegacySettingsSource(
    context: Context
) : AndroidLegacySettingsSource {
    private val preferences = context.applicationContext.getSharedPreferences(
        StorageMigrationContract.LEGACY_SETTINGS_STORE,
        Context.MODE_PRIVATE
    )

    override fun readThemeMode(): AndroidLegacyThemeValue {
        val values = preferences.all
        return AndroidLegacyThemeValue(
            present = values.containsKey(StorageMigrationContract.LEGACY_THEME_MODE),
            value = values[StorageMigrationContract.LEGACY_THEME_MODE] as? String
        )
    }

    override fun clearThemeMode() {
        preferences.edit()
            .remove(StorageMigrationContract.LEGACY_THEME_MODE)
            .apply()
    }
}

internal enum class AndroidMigrationOutcome {
    COMPLETED,
    ALREADY_COMPLETED,
    RETRY_EXHAUSTED,
    STORAGE_UNAVAILABLE,
    FAILED
}

internal data class AndroidMigrationResult(
    val outcome: AndroidMigrationOutcome,
    val attempt: Int,
    val migratedTheme: Boolean = false,
    val reason: String? = null
)

internal class AndroidStorageMigrator(
    private val provider: AndroidStorageProvider,
    private val legacySource: AndroidLegacySettingsSource
) {
    @Synchronized
    fun migrate(): AndroidMigrationResult {
        if (!provider.isReady) {
            return AndroidMigrationResult(
                AndroidMigrationOutcome.STORAGE_UNAVAILABLE,
                attempt = 0,
                reason = ERROR_STORAGE_UNAVAILABLE
            )
        }
        val meta = provider.store(StorageStore.META.wireValue)
        val preferences = provider.store(StorageStore.PREFERENCES.wireValue)
        if (meta == null || preferences == null) {
            return AndroidMigrationResult(
                AndroidMigrationOutcome.STORAGE_UNAVAILABLE,
                attempt = 0,
                reason = ERROR_STORAGE_UNAVAILABLE
            )
        }

        val schemaVersion = meta.read(StorageMigrationContract.META_SCHEMA_VERSION)?.toIntOrNull() ?: 0
        val state = StorageMigrationState.fromWireValue(
            meta.read(StorageMigrationContract.META_STATE)
        )
        val previousAttempts = meta.read(StorageMigrationContract.META_ATTEMPTS)?.toIntOrNull()
            ?.coerceAtLeast(0) ?: 0

        if (schemaVersion >= StorageMigrationContract.CURRENT_SCHEMA_VERSION &&
            state == StorageMigrationState.COMPLETED
        ) {
            return AndroidMigrationResult(
                AndroidMigrationOutcome.ALREADY_COMPLETED,
                attempt = previousAttempts
            )
        }
        if (state == StorageMigrationState.FAILED &&
            previousAttempts >= StorageMigrationContract.MAX_ATTEMPTS
        ) {
            return AndroidMigrationResult(
                AndroidMigrationOutcome.RETRY_EXHAUSTED,
                attempt = previousAttempts,
                reason = meta.read(StorageMigrationContract.META_LAST_ERROR)
            )
        }

        val attempt = previousAttempts + 1
        if (!meta.write(StorageMigrationContract.META_ATTEMPTS, attempt.toString()) ||
            !meta.write(StorageMigrationContract.META_STATE, StorageMigrationState.RUNNING.wireValue)
        ) {
            return AndroidMigrationResult(
                AndroidMigrationOutcome.FAILED,
                attempt = attempt,
                reason = ERROR_META_WRITE
            )
        }

        return try {
            var migratedTheme = false
            val targetKey = StorageMigrationContract.TARGET_THEME_MODE
            val targetTypeKey = StorageMigrationContract.TYPE_METADATA_PREFIX + targetKey
            val targetAlreadyExists = preferences.contains(targetKey)
            val legacy = legacySource.readThemeMode()

            if (targetAlreadyExists) {
                requireWrite(
                    preferences.write(targetTypeKey, StorageMigrationContract.TARGET_THEME_MODE_TYPE),
                    ERROR_TARGET_TYPE_WRITE
                )
            } else {
                val normalized = StorageMigrationContract.normalizeLegacyThemeMode(legacy.value)
                if (legacy.present && normalized != null) {
                    requireWrite(
                        preferences.write(targetTypeKey, StorageMigrationContract.TARGET_THEME_MODE_TYPE),
                        ERROR_TARGET_TYPE_WRITE
                    )
                    requireWrite(
                        preferences.write(targetKey, normalized),
                        ERROR_TARGET_WRITE
                    )
                    requireWrite(
                        preferences.read(targetKey) == normalized,
                        ERROR_TARGET_VERIFY
                    )
                    migratedTheme = true
                }
            }

            if (legacy.present) {
                runCatching(legacySource::clearThemeMode)
            }
            requireWrite(
                meta.write(
                    StorageMigrationContract.META_SCHEMA_VERSION,
                    StorageMigrationContract.CURRENT_SCHEMA_VERSION.toString()
                ),
                ERROR_META_WRITE
            )
            meta.remove(StorageMigrationContract.META_LAST_ERROR)
            requireWrite(
                meta.write(
                    StorageMigrationContract.META_STATE,
                    StorageMigrationState.COMPLETED.wireValue
                ),
                ERROR_META_WRITE
            )
            AndroidMigrationResult(
                AndroidMigrationOutcome.COMPLETED,
                attempt = attempt,
                migratedTheme = migratedTheme
            )
        } catch (error: AndroidMigrationException) {
            recordFailure(meta, error.reason)
            AndroidMigrationResult(
                AndroidMigrationOutcome.FAILED,
                attempt = attempt,
                reason = error.reason
            )
        } catch (_: Throwable) {
            recordFailure(meta, ERROR_UNEXPECTED)
            AndroidMigrationResult(
                AndroidMigrationOutcome.FAILED,
                attempt = attempt,
                reason = ERROR_UNEXPECTED
            )
        }
    }

    private fun requireWrite(success: Boolean, reason: String) {
        if (!success) {
            throw AndroidMigrationException(reason)
        }
    }

    private fun recordFailure(meta: AndroidStringStore, reason: String) {
        meta.write(StorageMigrationContract.META_LAST_ERROR, reason)
        meta.write(StorageMigrationContract.META_STATE, StorageMigrationState.FAILED.wireValue)
    }

    private companion object {
        const val ERROR_STORAGE_UNAVAILABLE = "storage_unavailable"
        const val ERROR_META_WRITE = "meta_write_failed"
        const val ERROR_TARGET_TYPE_WRITE = "target_type_write_failed"
        const val ERROR_TARGET_WRITE = "target_write_failed"
        const val ERROR_TARGET_VERIFY = "target_verify_failed"
        const val ERROR_UNEXPECTED = "unexpected_failure"
    }
}

private class AndroidMigrationException(val reason: String) : RuntimeException(reason)

