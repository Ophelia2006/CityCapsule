package com.y.citycapsule.core.theme

/** Shared-to-native contract used only for system chrome synchronization. */
object ThemeHostProtocol {
    const val VERSION = 1
    const val MODULE_NAME = "CCThemeHostModule"
    const val METHOD_APPLY_APPEARANCE = "applyAppearance"

    const val FIELD_PROTOCOL_VERSION = "protocolVersion"
    const val FIELD_THEME_MODE = "themeMode"
    const val FIELD_IS_DARK = "isDark"
}
