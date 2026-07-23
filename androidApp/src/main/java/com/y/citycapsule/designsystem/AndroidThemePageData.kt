package com.y.citycapsule.designsystem

import com.y.citycapsule.core.theme.ThemeBootstrap
import com.y.citycapsule.core.theme.ThemeBootstrapContract

/** Pure Android page-data projection kept separate from Activity and framework APIs for tests. */
internal object AndroidThemePageData {
    fun create(theme: ThemeBootstrap): Map<String, Any> = mapOf(
        ThemeBootstrapContract.KEY_PROTOCOL_VERSION to ThemeBootstrapContract.VERSION,
        ThemeBootstrapContract.KEY_THEME_MODE to theme.themeMode.wireValue,
        ThemeBootstrapContract.KEY_SYSTEM_DARK to (theme.systemDark ?: false),
        ThemeBootstrapContract.KEY_RESOLVED_DARK to theme.resolvedDark,
        ThemeBootstrapContract.KEY_KUIKLY_NIGHT_MODE to (theme.systemDark ?: false)
    )
}
