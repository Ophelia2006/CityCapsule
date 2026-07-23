package com.y.citycapsule.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.onboarding.StartupDestination
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_LAUNCH_GATE, supportInLocal = true)
internal class LaunchGatePager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val repository = OnboardingRepository(KuiklyKeyValueStore(this))
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            LaunchGateScreen(navigator, repository, themeHost)
        }
    }
}

@Composable
private fun LaunchGateScreen(
    navigator: AppNavigator,
    repository: OnboardingRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var routed by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在读取本地档案…") }

    LaunchedEffect(repository) {
        repository.getStartupDecision { decision ->
            if (!routed) {
                routed = true
                status = when (decision.destination) {
                    StartupDestination.HOME -> "档案已就绪，正在进入首页…"
                    StartupDestination.ONBOARDING -> "正在准备首次引导…"
                }
                when (decision.destination) {
                    StartupDestination.HOME -> navigator.replace(AppRoute.Home)
                    StartupDestination.ONBOARDING -> navigator.replace(AppRoute.Onboarding)
                }
            }
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = "城市胶囊",
                subtitle = "正在准备你的本地空间。"
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            AppStatusMessage(
                message = status,
                tone = AppStatusTone.NEUTRAL
            )
        }
    }
}
