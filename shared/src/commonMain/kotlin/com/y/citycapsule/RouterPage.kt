package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.pager.Pager
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator

/** Developer-only route console. All actions deliberately use the typed navigation API. */
@Page(AppRouteTable.PAGE_ROUTER_DIAGNOSTICS, supportInLocal = true)
internal class ComposeRoutePager : BasePager() {
    override fun willInit() {
        super.willInit()
        setContent { RouteDiagnosticsScreen() }
    }
}

@Composable
private fun RouteDiagnosticsScreen() {
    val pager = LocalActivity.current.getPager() as Pager
    val navigator: AppNavigator = remember { KuiklyAppNavigator(pager) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Typed route diagnostics", fontSize = 22.sp, color = Color(0xFF3F4BA8))
        Spacer(Modifier.height(24.dp))
        RouteActionButton("Push Home") { navigator.navigate(AppRoute.Home) }
        RouteActionButton("Push Settings") { navigator.navigate(AppRoute.Settings) }
        RouteActionButton("Replace with Settings") { navigator.replace(AppRoute.Settings) }
        RouteActionButton("Back to Home") { navigator.backTo(AppRouteKey.HOME) }
        RouteActionButton("Back") { navigator.back() }
    }
}

@Composable
private fun RouteActionButton(label: String, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color(0xFFE9ECF8))
            .padding(vertical = 14.dp)
    ) {
        Text(label, color = Color(0xFF3F4BA8), fontSize = 16.sp)
    }
}
