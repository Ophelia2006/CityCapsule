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

    @Test
    fun balancedPhotoRowsNeverLeaveAnOrphanGap() {
        assertEquals(listOf(2), balancedPhotoRowSizes(2))
        assertEquals(listOf(3), balancedPhotoRowSizes(3))
        assertEquals(listOf(2, 2), balancedPhotoRowSizes(4))
        assertEquals(listOf(3, 2), balancedPhotoRowSizes(5))
        assertEquals(listOf(3, 2, 2), balancedPhotoRowSizes(7))
        assertEquals(7, balancedPhotoRowSizes(7).sum())
    }
}
