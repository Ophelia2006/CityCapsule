package com.y.citycapsule.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.designsystem.tokens.AppColorScheme
import com.y.citycapsule.designsystem.tokens.AppDimensions
import com.y.citycapsule.designsystem.tokens.AppMotion
import com.y.citycapsule.designsystem.tokens.AppTypography
import com.y.citycapsule.designsystem.tokens.DarkAppColorScheme
import com.y.citycapsule.designsystem.tokens.DefaultAppDimensions
import com.y.citycapsule.designsystem.tokens.DefaultAppMotion
import com.y.citycapsule.designsystem.tokens.DefaultAppTypography
import com.y.citycapsule.designsystem.tokens.LightAppColorScheme

private val LocalAppColorScheme = staticCompositionLocalOf { LightAppColorScheme }
private val LocalAppTypography = staticCompositionLocalOf { DefaultAppTypography }
private val LocalAppDimensions = staticCompositionLocalOf { DefaultAppDimensions }
private val LocalAppMotion = staticCompositionLocalOf { DefaultAppMotion }

/** Read-only access to the active design tokens from shared composables. */
object AppTheme {
    val colors: AppColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColorScheme.current

    val typography: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dimensions: AppDimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimensions.current

    val motion: AppMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalAppMotion.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = colors.isDark
}

/**
 * Shared theme provider. Native hosts supply [systemDark]; persistence is resolved before this
 * boundary so that the design system remains independent from MMKV and bridge implementations.
 */
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    systemDark: Boolean? = null,
    content: @Composable () -> Unit
) {
    val resolvedTheme = ThemeResolver.resolve(themeMode, systemDark)
    val colorScheme = if (resolvedTheme.isDark) DarkAppColorScheme else LightAppColorScheme

    CompositionLocalProvider(
        LocalAppColorScheme provides colorScheme,
        LocalAppTypography provides DefaultAppTypography,
        LocalAppDimensions provides DefaultAppDimensions,
        LocalAppMotion provides DefaultAppMotion,
        content = content
    )
}
