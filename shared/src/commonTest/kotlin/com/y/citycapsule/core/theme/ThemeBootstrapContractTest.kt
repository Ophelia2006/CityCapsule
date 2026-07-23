package com.y.citycapsule.core.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeBootstrapContractTest {
    @Test
    fun invalidPreferenceFallsBackToSystemAndLightWhenSystemIsUnknown() {
        val bootstrap = ThemeBootstrapFactory.create("invalid", null)

        assertEquals(ThemeMode.SYSTEM, bootstrap.themeMode)
        assertEquals(null, bootstrap.systemDark)
        assertFalse(bootstrap.resolvedDark)
    }

    @Test
    fun forcedPreferenceOverridesSystemAppearance() {
        assertFalse(ThemeBootstrapFactory.create("light", true).resolvedDark)
        assertTrue(ThemeBootstrapFactory.create("dark", false).resolvedDark)
    }

    @Test
    fun systemPreferenceTracksPlatformAppearance() {
        assertTrue(ThemeBootstrapFactory.create("system", true).resolvedDark)
        assertFalse(ThemeBootstrapFactory.create("system", false).resolvedDark)
    }
}
