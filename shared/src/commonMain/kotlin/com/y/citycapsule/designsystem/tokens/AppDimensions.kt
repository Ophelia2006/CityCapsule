package com.y.citycapsule.designsystem.tokens

import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/** Shared spacing, radius, sizing, and responsive-layout tokens. */
data class AppDimensions(
    val spacingNone: Dp,
    val spacingXxs: Dp,
    val spacingXs: Dp,
    val spacingSm: Dp,
    val spacingMd: Dp,
    val spacingLg: Dp,
    val spacingXl: Dp,
    val spacingXxl: Dp,
    val radiusSm: Dp,
    val radiusMd: Dp,
    val radiusLg: Dp,
    val radiusXl: Dp,
    val minTouchTarget: Dp,
    val screenHorizontalPadding: Dp,
    val contentMaxWidth: Dp,
    val iconSm: Dp,
    val iconMd: Dp,
    val iconLg: Dp,
    val iconXl: Dp
)

val DefaultAppDimensions = AppDimensions(
    spacingNone = 0.dp,
    spacingXxs = 4.dp,
    spacingXs = 8.dp,
    spacingSm = 12.dp,
    spacingMd = 16.dp,
    spacingLg = 20.dp,
    spacingXl = 24.dp,
    spacingXxl = 32.dp,
    radiusSm = 8.dp,
    radiusMd = 12.dp,
    radiusLg = 14.dp,
    radiusXl = 20.dp,
    minTouchTarget = 48.dp,
    screenHorizontalPadding = 24.dp,
    contentMaxWidth = 720.dp,
    iconSm = 16.dp,
    iconMd = 20.dp,
    iconLg = 24.dp,
    iconXl = 32.dp
)
