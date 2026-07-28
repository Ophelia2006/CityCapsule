package com.y.citycapsule.designsystem.tokens

import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DesignTokensTest {
    @Test
    fun colorSchemesExposeStableSemanticBrandAndSurfaceValues() {
        assertFalse(LightAppColorScheme.isDark)
        assertTrue(DarkAppColorScheme.isDark)
        assertEquals(Color(0xFFC97824), LightAppColorScheme.primary)
        assertEquals(Color(0xFFF8F6F1), LightAppColorScheme.background)
        assertEquals(Color(0xFF1D1B18), LightAppColorScheme.textPrimary)
        assertEquals(Color(0xFF6F6A62), LightAppColorScheme.textSecondary)
        assertEquals(Color(0xFFE3DED4), LightAppColorScheme.divider)
        assertNotEquals(LightAppColorScheme.background, DarkAppColorScheme.background)
        assertNotEquals(LightAppColorScheme.textPrimary, DarkAppColorScheme.textPrimary)
    }

    @Test
    fun dimensionsKeepTouchAndScreenLayoutContracts() {
        assertEquals(48.dp, DefaultAppDimensions.minTouchTarget)
        assertEquals(24.dp, DefaultAppDimensions.screenHorizontalPadding)
        assertEquals(14.dp, DefaultAppDimensions.radiusLg)
        assertEquals(1.dp, DefaultAppDimensions.strokeThin)
        assertEquals(720.dp, DefaultAppDimensions.contentMaxWidth)
    }

    @Test
    fun typographyKeepsFrozenSemanticScale() {
        assertEquals(30.sp, DefaultAppTypography.pageTitle.fontSize)
        assertEquals(18.sp, DefaultAppTypography.sectionTitle.fontSize)
        assertEquals(16.sp, DefaultAppTypography.body.fontSize)
        assertEquals(12.sp, DefaultAppTypography.caption.fontSize)
    }

    @Test
    fun motionDurationsStayOrderedByEmphasis() {
        assertTrue(
            DefaultAppMotion.feedbackDurationMillis <
                DefaultAppMotion.transitionDurationMillis
        )
        assertTrue(
            DefaultAppMotion.transitionDurationMillis <
                DefaultAppMotion.emphasizedDurationMillis
        )
    }

    @Test
    fun elevationLevelsStayOrdered() {
        assertEquals(0.dp, DefaultAppElevation.flat)
        assertTrue(DefaultAppElevation.flat < DefaultAppElevation.raised)
        assertTrue(DefaultAppElevation.raised < DefaultAppElevation.overlay)
    }
}
