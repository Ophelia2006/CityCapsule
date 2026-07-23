package com.y.citycapsule.designsystem.theme

import com.y.citycapsule.core.theme.ThemeMode

enum class ThemeResolutionSource {
    FORCED_LIGHT,
    FORCED_DARK,
    SYSTEM_LIGHT,
    SYSTEM_DARK,
    FALLBACK_LIGHT
}

data class ResolvedTheme(
    val requestedMode: ThemeMode,
    val isDark: Boolean,
    val source: ThemeResolutionSource
)

/** Pure theme resolution. Platform and persistence access must stay outside this object. */
object ThemeResolver {
    fun resolve(
        themeMode: ThemeMode,
        systemDark: Boolean?
    ): ResolvedTheme = when (themeMode) {
        ThemeMode.LIGHT -> ResolvedTheme(
            requestedMode = themeMode,
            isDark = false,
            source = ThemeResolutionSource.FORCED_LIGHT
        )
        ThemeMode.DARK -> ResolvedTheme(
            requestedMode = themeMode,
            isDark = true,
            source = ThemeResolutionSource.FORCED_DARK
        )
        ThemeMode.SYSTEM -> when (systemDark) {
            true -> ResolvedTheme(
                requestedMode = themeMode,
                isDark = true,
                source = ThemeResolutionSource.SYSTEM_DARK
            )
            false -> ResolvedTheme(
                requestedMode = themeMode,
                isDark = false,
                source = ThemeResolutionSource.SYSTEM_LIGHT
            )
            null -> ResolvedTheme(
                requestedMode = themeMode,
                isDark = false,
                source = ThemeResolutionSource.FALLBACK_LIGHT
            )
        }
    }
}
