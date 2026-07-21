package com.y.citycapsule.core.storage

class SettingsRepository(
    private val storage: KeyValueStore
) {
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

    fun setThemeMode(mode: ThemeMode, callback: StorageCallback<Unit>) {
        storage.put(AppStorageKeys.Settings.THEME_MODE, mode, callback)
    }

    fun resetThemeMode(callback: StorageCallback<Unit>) {
        storage.remove(AppStorageKeys.Settings.THEME_MODE, callback)
    }
}

