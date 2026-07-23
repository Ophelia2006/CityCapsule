package com.y.citycapsule.app.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.citycapsule.core.theme.ThemeMode

/**
 * Process-wide source of truth for every active Kuikly page.
 *
 * Persistence stays in SettingsRepository. This object only coordinates an optimistic preference
 * preview and the latest platform appearance, allowing pages already in the native stack to update.
 */
class AppThemeState(
    initialMode: ThemeMode = ThemeMode.SYSTEM,
    initialSystemDark: Boolean? = null
) {
    var themeMode: ThemeMode by mutableStateOf(initialMode)
        private set

    var systemDark: Boolean? by mutableStateOf(initialSystemDark)
        private set

    fun bootstrap(mode: ThemeMode?, platformSystemDark: Boolean?) {
        if (mode != null) {
            themeMode = mode
        }
        if (platformSystemDark != null) {
            systemDark = platformSystemDark
        }
    }

    fun applyPersistedMode(mode: ThemeMode) {
        themeMode = mode
    }

    fun previewMode(mode: ThemeMode) {
        themeMode = mode
    }

    fun rollbackMode(previousMode: ThemeMode) {
        themeMode = previousMode
    }

    fun updateSystemAppearance(isDark: Boolean) {
        systemDark = isDark
    }
}

object AppThemeRuntime {
    private val state = AppThemeState()

    val themeMode: ThemeMode
        get() = state.themeMode

    val systemDark: Boolean?
        get() = state.systemDark

    fun bootstrap(mode: ThemeMode?, systemDark: Boolean?) = state.bootstrap(mode, systemDark)

    fun applyPersistedMode(mode: ThemeMode) = state.applyPersistedMode(mode)

    fun previewMode(mode: ThemeMode) = state.previewMode(mode)

    fun rollbackMode(previousMode: ThemeMode) = state.rollbackMode(previousMode)

    fun updateSystemAppearance(isDark: Boolean) = state.updateSystemAppearance(isDark)
}
