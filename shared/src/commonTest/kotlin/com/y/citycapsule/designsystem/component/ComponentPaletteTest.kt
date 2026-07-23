package com.y.citycapsule.designsystem.component

import com.tencent.kuikly.compose.ui.graphics.Color
import com.y.citycapsule.designsystem.tokens.DarkAppColorScheme
import com.y.citycapsule.designsystem.tokens.LightAppColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentPaletteTest {
    @Test
    fun primaryButtonUsesSemanticPrimaryPair() {
        val palette = resolveAppButtonPalette(
            LightAppColorScheme,
            AppButtonVariant.PRIMARY,
            enabled = true
        )

        assertEquals(LightAppColorScheme.primary, palette.background)
        assertEquals(LightAppColorScheme.onPrimary, palette.content)
    }

    @Test
    fun disabledTextButtonStaysTransparentAndUsesDisabledContent() {
        val palette = resolveAppButtonPalette(
            DarkAppColorScheme,
            AppButtonVariant.TEXT,
            enabled = false
        )

        assertEquals(Color.Transparent, palette.background)
        assertEquals(DarkAppColorScheme.disabledContent, palette.content)
    }

    @Test
    fun statusTonesUseSemanticContainers() {
        val success = resolveAppStatusPalette(LightAppColorScheme, AppStatusTone.SUCCESS)
        val warning = resolveAppStatusPalette(DarkAppColorScheme, AppStatusTone.WARNING)
        val error = resolveAppStatusPalette(DarkAppColorScheme, AppStatusTone.ERROR)

        assertEquals(LightAppColorScheme.successContainer, success.background)
        assertEquals(DarkAppColorScheme.onWarningContainer, warning.content)
        assertEquals(DarkAppColorScheme.errorContainer, error.background)
    }
}
