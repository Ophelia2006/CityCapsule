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
        assertEquals(Color(0xFF3FAF8A), LightAppColorScheme.primary)
        assertEquals(Color(0xFFFAFCF9), LightAppColorScheme.background)
        assertEquals(Color(0xFF17251F), LightAppColorScheme.textPrimary)
        assertEquals(Color(0xFF6E7872), LightAppColorScheme.textSecondary)
        assertEquals(Color(0xFFE4EAE5), LightAppColorScheme.divider)
        assertNotEquals(LightAppColorScheme.background, DarkAppColorScheme.background)
        assertNotEquals(LightAppColorScheme.textPrimary, DarkAppColorScheme.textPrimary)
    }

    @Test
    fun dimensionsKeepTouchAndScreenLayoutContracts() {
        assertEquals(48.dp, DefaultAppDimensions.minTouchTarget)
        assertEquals(20.dp, DefaultAppDimensions.screenHorizontalPadding)
        assertEquals(18.dp, DefaultAppDimensions.radiusMd)
        assertEquals(24.dp, DefaultAppDimensions.radiusLg)
        assertEquals(28.dp, DefaultAppDimensions.radiusXl)
        assertEquals(1.dp, DefaultAppDimensions.strokeThin)
        assertEquals(120.dp, DefaultAppDimensions.roamingPanelCollapsedHeight)
        assertEquals(420.dp, DefaultAppDimensions.roamingPanelExpandedHeight)
        assertEquals(48.dp, DefaultAppDimensions.roamingPanelHandleWidth)
        assertEquals(720.dp, DefaultAppDimensions.contentMaxWidth)
        assertEquals(640.dp, DefaultAppDimensions.readableContentMaxWidth)
        assertEquals(1200.dp, DefaultAppDimensions.adaptiveContentMaxWidth)
        assertTrue(
            DefaultAppDimensions.adaptivePrimaryPaneWidth <
                DefaultAppDimensions.adaptiveGridBreakpoint
        )
    }

    @Test
    fun typographyKeepsFrozenSemanticScale() {
        assertEquals(28.sp, DefaultAppTypography.cityTitle.fontSize)
        assertEquals(28.sp, DefaultAppTypography.pageTitle.fontSize)
        assertEquals(19.sp, DefaultAppTypography.sectionTitle.fontSize)
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
        assertEquals(3.dp, DefaultAppElevation.raised)
        assertEquals(10.dp, DefaultAppElevation.overlay)
    }
}
