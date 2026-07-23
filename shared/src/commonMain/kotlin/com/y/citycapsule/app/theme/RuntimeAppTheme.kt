package com.y.citycapsule.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.y.citycapsule.core.theme.ThemeHostProtocol
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.designsystem.theme.ThemeResolver

interface AppThemeHost {
    fun applyAppearance(isDark: Boolean, themeMode: ThemeMode)
}

class KuiklyAppThemeHost(private val pager: Pager) : AppThemeHost {
    override fun applyAppearance(isDark: Boolean, themeMode: ThemeMode) {
        val request = JSONObject().apply {
            put(ThemeHostProtocol.FIELD_PROTOCOL_VERSION, ThemeHostProtocol.VERSION)
            put(ThemeHostProtocol.FIELD_THEME_MODE, themeMode.wireValue)
            put(ThemeHostProtocol.FIELD_IS_DARK, isDark)
        }
        themeModule().applyAppearance(request)
    }

    private fun themeModule(): KuiklyThemeHostModule =
        pager.acquireModule(ThemeHostProtocol.MODULE_NAME)
}

internal class KuiklyThemeHostModule : Module() {
    override fun moduleName(): String = ThemeHostProtocol.MODULE_NAME

    fun applyAppearance(request: JSONObject) {
        asyncToNativeMethod(ThemeHostProtocol.METHOD_APPLY_APPEARANCE, request) { }
    }
}

/** Applies one resolved theme to shared tokens and the native system chrome. */
@Composable
fun RuntimeAppTheme(
    themeHost: AppThemeHost,
    content: @Composable () -> Unit
) {
    val themeMode = AppThemeRuntime.themeMode
    val systemDark = AppThemeRuntime.systemDark
    val resolvedTheme = ThemeResolver.resolve(themeMode, systemDark)

    LaunchedEffect(themeHost, themeMode, resolvedTheme.isDark) {
        themeHost.applyAppearance(resolvedTheme.isDark, themeMode)
    }

    AppTheme(
        themeMode = themeMode,
        systemDark = systemDark,
        content = content
    )
}
