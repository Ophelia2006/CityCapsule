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
    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F2F7),
    primary = Color(0xFF5A67D8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9ECF8),
    onPrimaryContainer = Color(0xFF3F4BA8),
    textPrimary = Color(0xFF1F2430),
    textSecondary = Color(0xFF687083),
    divider = Color(0xFFDDE1EA),
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
    disabledSurface = Color(0xFFE1E4EB),
    disabledContent = Color(0xFF9AA1AF),
    scrim = Color(0x66000000)
)

val DarkAppColorScheme = AppColorScheme(
    isDark = true,
    background = Color(0xFF11131A),
    surface = Color(0xFF1A1D27),
    surfaceVariant = Color(0xFF242836),
    primary = Color(0xFFAEB5FF),
    onPrimary = Color(0xFF20275F),
    primaryContainer = Color(0xFF353D83),
    onPrimaryContainer = Color(0xFFE0E3FF),
    textPrimary = Color(0xFFF2F4FA),
    textSecondary = Color(0xFFB4BBCB),
    divider = Color(0xFF343948),
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
    disabledSurface = Color(0xFF2A2E3A),
    disabledContent = Color(0xFF767D8D),
    scrim = Color(0x99000000)
)
