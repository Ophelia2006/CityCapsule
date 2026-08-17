package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.route.DefaultLocalRouteRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppCard
import com.y.citycapsule.designsystem.component.AppFixedHeaderScaffold
import com.y.citycapsule.designsystem.component.AppIconButton
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.route.LocalRouteEffect
import com.y.citycapsule.feature.route.LocalRouteFeatureRuntime
import com.y.citycapsule.feature.route.LocalRouteIntent
import com.y.citycapsule.feature.route.LocalRouteMode
import com.y.citycapsule.feature.route.LocalRouteStore
import kotlinx.coroutines.flow.collect

@Page(AppRouteTable.PAGE_LOCAL_ROUTES, supportInLocal = true)
internal class LocalRoutesPager : BasePager() { override fun willInit() { super.willInit(); install(LocalRouteMode.LIST, null) } }
@Page(AppRouteTable.PAGE_LOCAL_ROUTE_EDITOR, supportInLocal = true)
internal class LocalRouteEditorPager : BasePager() { override fun willInit() { super.willInit(); install(LocalRouteMode.EDITOR, pageData.params.optString(AppRouteTable.PARAM_ROUTE_ID).takeIf(String::isNotBlank)) } }

private fun BasePager.install(mode: LocalRouteMode, routeId: String?) {
    val navigator = KuiklyAppNavigator(this); val storage = KuiklyKeyValueStore(this); val places = LocalPlaceRepository(storage)
    val routes = DefaultLocalRouteRepository(storage, places); val theme = KuiklyAppThemeHost(this)
    setContent { RuntimeAppTheme(theme) { LocalRouteScreen(mode, routeId, navigator, routes, places) } }
}

@Composable private fun LocalRouteScreen(mode: LocalRouteMode, routeId: String?, navigator: AppNavigator, routes: DefaultLocalRouteRepository, places: LocalPlaceRepository) {
    val scope = rememberCoroutineScope(); val store = remember(routes, places, mode, routeId) { LocalRouteStore(routes, places, mode, routeId, scope) }
    val state by store.state.collectAsState(); val revision = LocalRouteFeatureRuntime.revision
    DisposableEffect(store) { onDispose(store::dispose) }
    LaunchedEffect(store, revision) { store.dispatch(LocalRouteIntent.Load) }
    LaunchedEffect(store) { store.effects.collect { when (it) {
        LocalRouteEffect.Back -> navigator.back(); is LocalRouteEffect.Editor -> navigator.navigate(AppRoute.LocalRouteEditor(it.id))
        is LocalRouteEffect.Roaming -> navigator.navigate(AppRoute.RoamingSession(it.routeId))
        LocalRouteEffect.Changed -> LocalRouteFeatureRuntime.invalidate()
    } } }
    AppFixedHeaderScaffold(statusBarHeight = LocalActivity.current.pageData.statusBarHeight, header = {
        AppActionTopBar(if (mode == LocalRouteMode.LIST) "我的路线" else if (routeId == null) "新建路线" else "编辑路线", { store.dispatch(LocalRouteIntent.Back) },
            actionLabel = if (mode == LocalRouteMode.LIST) "新建" else "保存", onActionClick = { store.dispatch(if (mode == LocalRouteMode.LIST) LocalRouteIntent.Create else LocalRouteIntent.Save) }, actionEnabled = mode == LocalRouteMode.LIST || state.canSave)
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    }, content = {
        if (state.loading) LoadingState("正在读取本地路线…") else if (mode == LocalRouteMode.LIST) RouteList(state, store::dispatch) else RouteEditor(state, store::dispatch)
    })
}

@Composable private fun RouteList(state: com.y.citycapsule.feature.route.LocalRouteUiState, dispatch: (LocalRouteIntent) -> Unit) {
    state.message?.let { AppStatusMessage(it); Spacer(Modifier.height(AppTheme.dimensions.spacingSm)) }
    AppSecondaryText("路线只保存在本机，地点顺序由你手动安排。")
    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    AppButton("自由漫游", { dispatch(LocalRouteIntent.StartRoaming(null)) }, variant = AppButtonVariant.SECONDARY)
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    if (state.routes.isEmpty()) EmptyState("还没有路线", "挑选想去的地点，安排一次简单的城市探索。", actionLabel = "新建路线") { dispatch(LocalRouteIntent.Create) }
    else state.routes.forEach { route ->
        AppCard(Modifier.clickable { dispatch(LocalRouteIntent.Open(route.id)) }) {
            AppSectionTitle(route.name); Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(route.orderedPlaceIds.mapIndexed { index, id -> "${index + 1}. ${state.places.firstOrNull { it.id == id }?.name ?: "地点已不存在"}" }.joinToString("  ·  "))
        }; Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    }
}

@Composable private fun RouteEditor(state: com.y.citycapsule.feature.route.LocalRouteUiState, dispatch: (LocalRouteIntent) -> Unit) {
    state.message?.let { AppStatusMessage(it); Spacer(Modifier.height(AppTheme.dimensions.spacingSm)) }
    AppTextField(state.name, { dispatch(LocalRouteIntent.NameChanged(it)) }, "路线名称", placeholder = "例如：周六午后散步", maxLength = 40)
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg)); AppSectionTitle("地点顺序"); Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    if (state.orderedPlaceIds.isEmpty()) AppSecondaryText("至少选择一个地点。")
    state.orderedPlaceIds.forEachIndexed { index, id -> val place = state.places.firstOrNull { it.id == id }
        AppCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AppCaptionText("${index + 1}"); Spacer(Modifier.weight(0.15f)); Column(Modifier.weight(1f)) { AppBodyText(place?.name ?: "地点已不存在"); place?.let { AppSecondaryText(listOfNotNull(it.district, it.city).joinToString(" · ")) } }
            AppIconButton(AppIconName.BACK, "上移", { dispatch(LocalRouteIntent.Move(id, -1)) }, enabled = index > 0)
            AppIconButton(AppIconName.FORWARD, "下移", { dispatch(LocalRouteIntent.Move(id, 1)) }, enabled = index < state.orderedPlaceIds.lastIndex)
            AppIconButton(AppIconName.CLOSE, "移除", { dispatch(LocalRouteIntent.RemovePlace(id)) })
        } }; Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg)); AppSectionTitle("添加地点"); Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    state.places.filterNot { it.id in state.orderedPlaceIds }.forEach { place ->
        Row(Modifier.fillMaxWidth().clickable { dispatch(LocalRouteIntent.AddPlace(place.id)) }.padding(vertical = AppTheme.dimensions.spacingSm), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { AppBodyText(place.name); AppSecondaryText(listOfNotNull(place.district, place.city).joinToString(" · ")) }
            AppIconButton(AppIconName.ADD, "添加 ${place.name}", { dispatch(LocalRouteIntent.AddPlace(place.id)) })
        }
    }
    if (state.editingId != null) { Spacer(Modifier.height(AppTheme.dimensions.spacingXl)); AppButton("删除路线", { dispatch(LocalRouteIntent.Delete) }, variant = AppButtonVariant.DANGER, loading = state.saving) }
    if (state.editingId != null) { Spacer(Modifier.height(AppTheme.dimensions.spacingSm)); AppButton("按此路线开始漫游", { dispatch(LocalRouteIntent.StartRoaming(state.editingId)) }, variant = AppButtonVariant.SECONDARY) }
}
