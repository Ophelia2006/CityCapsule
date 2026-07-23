package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.AppThemeRuntime
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCard
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_HOME, supportInLocal = true)
internal class HomePager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val settingsRepository = SettingsRepository(KuiklyKeyValueStore(this))
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            HomeScreen(navigator, settingsRepository, themeHost)
        }
    }
}

@Composable
private fun HomeScreen(
    navigator: AppNavigator,
    settingsRepository: SettingsRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight

    LaunchedEffect(settingsRepository) {
        settingsRepository.getThemeModeSnapshot { snapshot ->
            AppThemeRuntime.applyPersistedMode(snapshot.mode)
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        val dimensions = AppTheme.dimensions
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = "城市胶囊",
                subtitle = "Home 已接入共享 AppTheme、AppRoute 与 AppNavigator。"
            )
            Spacer(Modifier.height(dimensions.spacingXxl))
            AppCard {
                AppSectionTitle(text = "共享设计系统")
                Spacer(Modifier.height(dimensions.spacingXs))
                AppBodyText(
                    text = "页面颜色、字体、间距和组件均来自 shared 语义令牌。"
                )
            }
            Spacer(Modifier.height(dimensions.spacingXl))
            AppButton(
                text = "打开本地档案",
                onClick = { navigator.navigate(AppRoute.Profile) }
            )
            Spacer(Modifier.height(dimensions.spacingSm))
            AppButton(
                text = "打开设置",
                variant = AppButtonVariant.SECONDARY,
                onClick = { navigator.navigate(AppRoute.Settings) }
            )
            Spacer(Modifier.height(dimensions.spacingSm))
            AppButton(
                text = "Replace Settings",
                variant = AppButtonVariant.SECONDARY,
                onClick = { navigator.replace(AppRoute.Settings) }
            )
        }
    }
}
