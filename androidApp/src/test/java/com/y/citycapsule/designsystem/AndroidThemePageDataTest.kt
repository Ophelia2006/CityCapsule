package com.y.citycapsule.designsystem

import com.y.citycapsule.core.theme.ThemeBootstrap
import com.y.citycapsule.core.theme.ThemeBootstrapContract
import com.y.citycapsule.core.theme.ThemeHostProtocol
import com.y.citycapsule.core.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidThemePageDataTest {
    @Test
    fun pageDataContainsTheCompleteVersionOneBootstrapContract() {
        val pageData = AndroidThemePageData.create(
            ThemeBootstrap(
                themeMode = ThemeMode.DARK,
                systemDark = false,
                resolvedDark = true
            )
        )

        assertEquals(ThemeBootstrapContract.VERSION, pageData["themeProtocolVersion"])
        assertEquals("dark", pageData["themeMode"])
        assertEquals(false, pageData["systemDark"])
        assertEquals(true, pageData["resolvedDark"])
        assertEquals(false, pageData["isNightMode"])
        assertEquals(5, pageData.size)
    }

    @Test
    fun unknownSystemAppearanceUsesSafeFalseWireFallback() {
        val pageData = AndroidThemePageData.create(
            ThemeBootstrap(ThemeMode.SYSTEM, null, resolvedDark = false)
        )

        assertEquals(false, pageData[ThemeBootstrapContract.KEY_SYSTEM_DARK])
        assertEquals(false, pageData[ThemeBootstrapContract.KEY_KUIKLY_NIGHT_MODE])
    }

    @Test
    fun nativeModuleContractNamesStayFrozen() {
        assertEquals("CCThemeHostModule", ThemeHostProtocol.MODULE_NAME)
        assertEquals("applyAppearance", ThemeHostProtocol.METHOD_APPLY_APPEARANCE)
        assertEquals("isDark", ThemeHostProtocol.FIELD_IS_DARK)
    }
}
