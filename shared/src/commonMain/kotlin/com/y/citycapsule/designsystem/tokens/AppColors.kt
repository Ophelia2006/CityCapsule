package com.y.citycapsule.designsystem.tokens

import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * Semantic colors consumed by screens and shared components.
 * Raw color values are allowed only in token definitions and platform-native resource files.
 */
data class AppColorScheme(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val disabledSurface: Color,
    val disabledContent: Color,
    val scrim: Color
)

val LightAppColorScheme = AppColorScheme(
    isDark = false,
    background = Color(0xFFF8F6F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1EDE5),
    primary = Color(0xFFC97824),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF5E4D0),
    onPrimaryContainer = Color(0xFF70400F),
    textPrimary = Color(0xFF1D1B18),
    textSecondary = Color(0xFF6F6A62),
    divider = Color(0xFFE3DED4),
    success = Color(0xFF26703B),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE7F4EA),
    onSuccessContainer = Color(0xFF1E5C31),
    warning = Color(0xFFA15C00),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFF1D6),
    onWarningContainer = Color(0xFF6D3B00),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF8C1D18),
    disabledSurface = Color(0xFFE9E5DD),
    disabledContent = Color(0xFFA39D93),
    scrim = Color(0x66000000)
)

val DarkAppColorScheme = AppColorScheme(
    isDark = true,
    background = Color(0xFF151411),
    surface = Color(0xFF1E1C18),
    surfaceVariant = Color(0xFF292620),
    primary = Color(0xFFE2A15D),
    onPrimary = Color(0xFF3E2307),
    primaryContainer = Color(0xFF5A3512),
    onPrimaryContainer = Color(0xFFFFDDB8),
    textPrimary = Color(0xFFF4F0E8),
    textSecondary = Color(0xFFBDB5A9),
    divider = Color(0xFF3B3730),
    success = Color(0xFF7ED993),
    onSuccess = Color(0xFF0A3717),
    successContainer = Color(0xFF1D4D2B),
    onSuccessContainer = Color(0xFFB8F4C5),
    warning = Color(0xFFFFB95C),
    onWarning = Color(0xFF4B2800),
    warningContainer = Color(0xFF5C3500),
    onWarningContainer = Color(0xFFFFDDB0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    disabledSurface = Color(0xFF302D27),
    disabledContent = Color(0xFF817A70),
    scrim = Color(0x99000000)
)
