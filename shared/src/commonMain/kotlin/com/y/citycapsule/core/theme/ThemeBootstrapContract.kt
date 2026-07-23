package com.y.citycapsule.core.theme

/**
 * Versioned page-data contract shared by the Android host, HarmonyOS host and Kuikly pages.
 * Keep the keys mirrored by HarmonyThemeProtocol.ets.
 */
object ThemeBootstrapContract {
    const val VERSION = 1

    const val KEY_PROTOCOL_VERSION = "themeProtocolVersion"
    const val KEY_THEME_MODE = "themeMode"
    const val KEY_SYSTEM_DARK = "systemDark"
    const val KEY_RESOLVED_DARK = "resolvedDark"

    // Kuikly's built-in configuration callback uses this key.
    const val KEY_KUIKLY_NIGHT_MODE = "isNightMode"
}

data class ThemeBootstrap(
    val themeMode: ThemeMode,
    val systemDark: Boolean?,
    val resolvedDark: Boolean
)

/** Pure construction logic so both native hosts can be covered by JVM tests. */
object ThemeBootstrapFactory {
    fun create(persistedMode: String?, systemDark: Boolean?): ThemeBootstrap {
        val themeMode = ThemeMode.fromWireValue(persistedMode.orEmpty()) ?: ThemeMode.SYSTEM
        val resolvedDark = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemDark ?: false
        }
        return ThemeBootstrap(themeMode, systemDark, resolvedDark)
    }
}
