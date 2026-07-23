package com.y.citycapsule.app.theme

import com.y.citycapsule.core.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeStateTest {
    @Test
    fun bootstrapHydratesPreferenceAndPlatformAppearance() {
        val state = AppThemeState()

        state.bootstrap(ThemeMode.DARK, true)

        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(true, state.systemDark)
    }

    @Test
    fun optimisticPreviewCanRollbackWithoutLosingSystemAppearance() {
        val state = AppThemeState(ThemeMode.SYSTEM, true)

        state.previewMode(ThemeMode.LIGHT)
        state.rollbackMode(ThemeMode.SYSTEM)

        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(true, state.systemDark)
    }

    @Test
    fun configurationUpdateDoesNotOverwriteForcedPreference() {
        val state = AppThemeState(ThemeMode.DARK, false)

        state.updateSystemAppearance(true)

        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(true, state.systemDark)
    }
}
