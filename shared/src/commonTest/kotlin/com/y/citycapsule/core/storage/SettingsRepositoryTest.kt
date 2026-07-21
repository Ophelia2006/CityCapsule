package com.y.citycapsule.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
}

