package com.y.citycapsule.designsystem

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowManager
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.StorageStore
import com.y.citycapsule.core.theme.ThemeBootstrap
import com.y.citycapsule.core.theme.ThemeBootstrapFactory
import com.y.citycapsule.storage.AndroidMmkvStorage

/** Android boundary for system appearance, persisted bootstrap data and system bars. */
internal object AndroidThemeHost {
    private val darkNavigationBarColor = Color.rgb(17, 19, 26)
    private val lightNavigationBarColor = Color.rgb(245, 246, 250)

    fun bootstrap(context: Context): ThemeBootstrap {
        val systemDark = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val persistedMode = AndroidMmkvStorage
            .store(StorageStore.PREFERENCES.wireValue)
            ?.read(AppStorageKeys.Settings.THEME_MODE.wireKey)
        return ThemeBootstrapFactory.create(persistedMode, systemDark)
    }

    @Suppress("DEPRECATION")
    fun applySystemBars(activity: Activity, isDark: Boolean) {
        activity.window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = if (isDark) darkNavigationBarColor else lightNavigationBarColor

            var flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (!isDark) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
            decorView.systemUiVisibility = flags
        }
    }
}
