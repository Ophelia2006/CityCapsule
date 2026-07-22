package com.y.citycapsule.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SettingsRepositoryTest {
    @Test
    fun missingThemeUsesFrozenDefault() {
        val repository = SettingsRepository(InMemoryKeyValueStore())
        var result: StorageResult<ThemeMode>? = null

        repository.getThemeMode { result = it }

        assertEquals(
            ThemeMode.SYSTEM,
            assertIs<StorageResult.Success<ThemeMode>>(result).value
        )
    }

    @Test
    fun themeRoundTripAndResetStayBehindRepository() {
        val repository = SettingsRepository(InMemoryKeyValueStore())

        repository.setThemeMode(ThemeMode.DARK) {}
        var stored: StorageResult<ThemeMode>? = null
        repository.getThemeMode { stored = it }
        assertEquals(ThemeMode.DARK, assertIs<StorageResult.Success<ThemeMode>>(stored).value)

        repository.resetThemeMode {}
        repository.getThemeMode { stored = it }
        assertEquals(ThemeMode.SYSTEM, assertIs<StorageResult.Success<ThemeMode>>(stored).value)
    }

    @Test
    fun businessReadFallsBackToDefaultAndPreservesWarning() {
        val repository = SettingsRepository(FailingKeyValueStore())
        var snapshot: ThemeModeSnapshot? = null

        repository.getThemeModeSnapshot { snapshot = it }

        assertEquals(ThemeMode.SYSTEM, assertNotNull(snapshot).mode)
        assertEquals(ThemeModeSource.DEFAULT_RECOVERY, snapshot?.source)
        assertEquals(StorageErrorCode.NOT_INITIALIZED, snapshot?.warning?.code)
    }
}

private class FailingKeyValueStore : KeyValueStore {
    private val failure = StorageResult.Failure(
        StorageError(StorageErrorCode.NOT_INITIALIZED, "Storage unavailable in test.")
    )

    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        callback(failure)
    }

    override fun <T> put(key: StorageKey<T>, value: T, callback: StorageCallback<Unit>) {
        callback(failure)
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        callback(failure)
    }

    override fun contains(key: StorageKey<*>, callback: StorageCallback<Boolean>) {
        callback(failure)
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        callback(failure)
    }
}
