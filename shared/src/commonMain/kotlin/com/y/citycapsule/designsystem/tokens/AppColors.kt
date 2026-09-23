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
    background = Color(0xFFFAFCF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F5F1),
    primary = Color(0xFF3FAF8A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDF3EA),
    onPrimaryContainer = Color(0xFF174C3C),
    textPrimary = Color(0xFF17251F),
    textSecondary = Color(0xFF6E7872),
    divider = Color(0xFFE4EAE5),
    success = Color(0xFF2E8B68),
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
    disabledSurface = Color(0xFFECEFEB),
    disabledContent = Color(0xFFA2AAA5),
    scrim = Color(0x66000000)
)

val DarkAppColorScheme = AppColorScheme(
    isDark = true,
    background = Color(0xFF111714),
    surface = Color(0xFF19211D),
    surfaceVariant = Color(0xFF222D28),
    primary = Color(0xFF6FD5B2),
    onPrimary = Color(0xFF07382A),
    primaryContainer = Color(0xFF1E4E3F),
    onPrimaryContainer = Color(0xFFB8F2DE),
    textPrimary = Color(0xFFF1F6F2),
    textSecondary = Color(0xFFADB9B2),
    divider = Color(0xFF334039),
    success = Color(0xFF75D4AC),
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
    disabledSurface = Color(0xFF29332E),
    disabledContent = Color(0xFF7F8A84),
    scrim = Color(0x99000000)
)
