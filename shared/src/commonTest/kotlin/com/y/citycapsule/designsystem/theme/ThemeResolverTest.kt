package com.y.citycapsule.designsystem.theme

import com.y.citycapsule.core.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeResolverTest {
    @Test
    fun themeWireValuesRemainCompatibleWithStorageProtocolV1() {
        assertEquals("system", ThemeMode.SYSTEM.wireValue)
        assertEquals("light", ThemeMode.LIGHT.wireValue)
        assertEquals("dark", ThemeMode.DARK.wireValue)
        assertEquals(ThemeMode.DARK, ThemeMode.fromWireValue("dark"))
        assertNull(ThemeMode.fromWireValue("unknown"))
    }

    @Test
    fun forcedLightIgnoresDarkSystemAppearance() {
        val resolved = ThemeResolver.resolve(ThemeMode.LIGHT, systemDark = true)

        assertFalse(resolved.isDark)
        assertEquals(ThemeResolutionSource.FORCED_LIGHT, resolved.source)
    }

    @Test
    fun forcedDarkIgnoresLightSystemAppearance() {
        val resolved = ThemeResolver.resolve(ThemeMode.DARK, systemDark = false)

        assertTrue(resolved.isDark)
        assertEquals(ThemeResolutionSource.FORCED_DARK, resolved.source)
    }

    @Test
    fun systemModeFollowsAvailableSystemAppearance() {
        assertEquals(
            ThemeResolutionSource.SYSTEM_LIGHT,
            ThemeResolver.resolve(ThemeMode.SYSTEM, systemDark = false).source
        )
        assertEquals(
            ThemeResolutionSource.SYSTEM_DARK,
            ThemeResolver.resolve(ThemeMode.SYSTEM, systemDark = true).source
        )
    }

    @Test
    fun unavailableSystemAppearanceFallsBackToLight() {
        val resolved = ThemeResolver.resolve(ThemeMode.SYSTEM, systemDark = null)

        assertFalse(resolved.isDark)
        assertEquals(ThemeResolutionSource.FALLBACK_LIGHT, resolved.source)
    }
}
