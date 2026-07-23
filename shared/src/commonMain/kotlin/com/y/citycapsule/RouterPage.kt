package com.y.citycapsule

import androidx.compose.runtime.Composable
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
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

/** Developer-only route console. All actions deliberately use the typed navigation API. */
@Page(AppRouteTable.PAGE_ROUTER_DIAGNOSTICS, supportInLocal = true)
internal class ComposeRoutePager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            RouteDiagnosticsScreen(navigator, themeHost)
        }
    }
}

@Composable
private fun RouteDiagnosticsScreen(
    navigator: AppNavigator,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight

    RuntimeAppTheme(themeHost = themeHost) {
        val dimensions = AppTheme.dimensions
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = "Typed route diagnostics",
                subtitle = "All actions use AppRoute and AppNavigator."
            )
            Spacer(Modifier.height(dimensions.spacingXl))
            AppSection(
                title = "Route actions",
                description = "Use this developer-only page to verify the native route stack."
            ) {
                RouteActionButton("Push Home") { navigator.navigate(AppRoute.Home) }
                RouteActionButton("Push Settings") { navigator.navigate(AppRoute.Settings) }
                RouteActionButton("Replace with Settings") {
                    navigator.replace(AppRoute.Settings)
                }
                RouteActionButton("Back to Home") { navigator.backTo(AppRouteKey.HOME) }
                AppButton(
                    text = "Back",
                    variant = AppButtonVariant.TEXT,
                    onClick = navigator::back
                )
            }
        }
    }
}

@Composable
private fun RouteActionButton(label: String, action: () -> Unit) {
    AppButton(
        text = label,
        variant = AppButtonVariant.SECONDARY,
        onClick = action
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
}
