package com.y.citycapsule.core.storage

class SettingsRepository(
    private val storage: KeyValueStore
) {
    /** Strict read retained for diagnostics and tests that need the original storage error. */
    fun getThemeMode(callback: StorageCallback<ThemeMode>) {
        storage.get(AppStorageKeys.Settings.THEME_MODE) { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> result
                    StorageResult.Missing -> StorageResult.Success(
                        AppStorageKeys.Settings.THEME_MODE.defaultValue
                    )
                    is StorageResult.Failure -> result
                }
            )
        }
    }

    /**
     * Business read: Settings must remain usable when storage is temporarily unavailable or
     * contains an undecodable value. The warning is preserved for UI diagnostics and logging.
     */
    fun getThemeModeSnapshot(callback: (ThemeModeSnapshot) -> Unit) {
        storage.get(AppStorageKeys.Settings.THEME_MODE) { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> ThemeModeSnapshot(
                        mode = result.value,
                        source = ThemeModeSource.PERSISTED
                    )
                    StorageResult.Missing -> ThemeModeSnapshot(
                        mode = AppStorageKeys.Settings.THEME_MODE.defaultValue,
                        source = ThemeModeSource.DEFAULT_MISSING
                    )
                    is StorageResult.Failure -> ThemeModeSnapshot(
                        mode = AppStorageKeys.Settings.THEME_MODE.defaultValue,
                        source = ThemeModeSource.DEFAULT_RECOVERY,
                        warning = result.error
                    )
                }
            )
        }
    }

    fun setThemeMode(mode: ThemeMode, callback: StorageCallback<Unit>) {
        storage.put(AppStorageKeys.Settings.THEME_MODE, mode, callback)
    }

    fun resetThemeMode(callback: StorageCallback<Unit>) {
        storage.remove(AppStorageKeys.Settings.THEME_MODE, callback)
    }
}

enum class ThemeModeSource {
    PERSISTED,
    DEFAULT_MISSING,
    DEFAULT_RECOVERY
}

data class ThemeModeSnapshot(
    val mode: ThemeMode,
    val source: ThemeModeSource,
    val warning: StorageError? = null
)
