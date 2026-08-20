package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.roaming.LocalRoamingSessionRepository
import com.y.citycapsule.core.roaming.RoamingStatus
import com.y.citycapsule.core.route.DefaultLocalRouteRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.location.KuiklyLocationCapability
import com.y.citycapsule.core.track.KuiklyTrackFiles
import com.y.citycapsule.core.track.LocalTrackRepository
import com.y.citycapsule.core.track.TrackStatus
import com.y.citycapsule.core.checkin.CheckInRepository
import com.y.citycapsule.core.checkin.CheckInMethod
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppFixedHeaderScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.roaming.RoamingSessionEffect
import com.y.citycapsule.feature.roaming.RoamingSessionIntent
import com.y.citycapsule.feature.roaming.RoamingSessionStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay

@Page(AppRouteTable.PAGE_ROAMING_SESSION, supportInLocal = true)
internal class RoamingSessionPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val places = LocalPlaceRepository(storage)
        val routes = DefaultLocalRouteRepository(storage, places)
        val sessions = LocalRoamingSessionRepository(storage, routes)
        val trackFiles = KuiklyTrackFiles(this)
        val tracks = LocalTrackRepository(storage, trackFiles)
        val routeId = pageData.params.optString(AppRouteTable.PARAM_ROUTE_ID).takeIf(String::isNotBlank)
        val theme = KuiklyAppThemeHost(this)
        setContent { RuntimeAppTheme(theme) { RoamingSessionScreen(routeId, navigator, routes, sessions, tracks, KuiklyLocationCapability(this), places, CheckInRepository(storage), LocalCapsuleRepository(storage), trackFiles) } }
    }
}

@Composable private fun RoamingSessionScreen(routeId: String?, navigator: KuiklyAppNavigator, routes: DefaultLocalRouteRepository, sessions: LocalRoamingSessionRepository, tracks: LocalTrackRepository, location: com.y.citycapsule.core.location.LocationCapability, places: LocalPlaceRepository, checkIns: CheckInRepository, capsules: LocalCapsuleRepository, files: KuiklyTrackFiles) {
    val scope = rememberCoroutineScope()
    val store = remember(routeId, routes, sessions, tracks, location, places, checkIns, capsules, files) { RoamingSessionStore(sessions, routes, tracks, location, places, checkIns, capsules, files, routeId, scope) }
    val state by store.state.collectAsState()
    DisposableEffect(store) { onDispose(store::dispose) }
    LaunchedEffect(store) { store.dispatch(RoamingSessionIntent.Load) }
    LaunchedEffect(store, state.session?.status) {
        while (state.session?.status == RoamingStatus.ACTIVE) {
            store.dispatch(RoamingSessionIntent.SampleLocation)
            delay(15_000L)
        }
    }
    LaunchedEffect(store) { store.effects.collect { if (it == RoamingSessionEffect.Back) navigator.back() } }
    AppFixedHeaderScaffold(LocalActivity.current.pageData.statusBarHeight, header = {
        AppActionTopBar("漫游会话", { store.dispatch(RoamingSessionIntent.Back) })
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    }, content = {
        if (state.loading) LoadingState("正在读取漫游状态…") else {
            val session = state.session
            AppSectionTitle(state.requestedRouteName ?: if (state.requestedRouteId == null) "自由漫游" else "路线不可用")
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppSecondaryText(when (session?.status) {
                RoamingStatus.ACTIVE -> "正在记录前台轨迹；离开此页面后停止采样。"
                RoamingStatus.PAUSED -> "漫游已暂停，可以继续或结束。"
                RoamingStatus.ENDED -> "上一次漫游已经结束。"
                null -> "准备好后开始；当前只记录会话状态。"
            })
            state.message?.let { Spacer(Modifier.height(AppTheme.dimensions.spacingSm)); AppStatusMessage(it) }
            state.track?.let { track ->
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppSecondaryText("前台轨迹点：${track.pointCount} · 分片：${track.chunkPaths.size}")
                if (track.status == TrackStatus.INTERRUPTED) AppStatusMessage("轨迹已中断：${track.interruptionReason.orEmpty()}。漫游会话仍保持进行中，可重试采样。")
            }
            if (session?.status == RoamingStatus.ACTIVE && (state.routePlaces.isNotEmpty() || state.nearbyPlaces.isNotEmpty())) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSectionTitle(if (state.requestedRouteId == null) "附近地点（500 米内）" else "路线打卡")
                val visiblePlaces = if (state.requestedRouteId == null) state.nearbyPlaces.map { it.place } else state.routePlaces
                visiblePlaces.forEach { place ->
                    val checked = state.checkIns.firstOrNull { it.placeId == place.id }
                    val distance = state.nearbyPlaces.firstOrNull { it.place.id == place.id }?.distanceMeters
                    AppSecondaryText("${if (checked != null) "✓" else "○"} ${place.name}${distance?.let { " · ${com.y.citycapsule.core.location.GeoDistance.label(it)}" }.orEmpty()}${checked?.let { if (it.method == CheckInMethod.MANUAL) " · 手动记录" else " · GPS 确认" }.orEmpty()}")
                    if (checked == null && distance != null && distance <= 200.0) AppButton("确认到达 ${place.name}", { store.dispatch(RoamingSessionIntent.ConfirmCheckIn(place.id)) }, variant = AppButtonVariant.SECONDARY)
                    if (checked == null && state.track?.status == TrackStatus.INTERRUPTED) AppButton("手动记录 ${place.name}", { store.dispatch(RoamingSessionIntent.ManualCheckIn(place.id)) }, variant = AppButtonVariant.TEXT)
                    if (checked != null) AppButton("在这里留下城市碎片", { navigator.navigate(com.y.citycapsule.core.navigation.AppRoute.CapsuleEditor(placeId = place.id, roamingSessionId = session.startedAtEpochMs.toString())) }, variant = AppButtonVariant.TEXT)
                }
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when (session?.status) {
                RoamingStatus.ACTIVE -> AppButton("暂停漫游", { store.dispatch(RoamingSessionIntent.Pause) }, loading = state.busy)
                RoamingStatus.PAUSED -> AppButton("继续漫游", { store.dispatch(RoamingSessionIntent.Resume) }, loading = state.busy)
                RoamingStatus.ENDED, null -> AppButton("开始漫游", { store.dispatch(RoamingSessionIntent.Start) }, loading = state.busy, enabled = state.requestedRouteId == null || state.requestedRouteName != null)
            }
            if (session?.status == RoamingStatus.ACTIVE || session?.status == RoamingStatus.PAUSED) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppButton("结束漫游", { store.dispatch(RoamingSessionIntent.End) }, variant = AppButtonVariant.SECONDARY, enabled = !state.busy)
            }
            if (session?.status == RoamingStatus.ACTIVE) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppButton("记录当前位置", { store.dispatch(RoamingSessionIntent.SampleLocation) }, variant = AppButtonVariant.SECONDARY, loading = state.sampling)
            }
            if (session?.status == RoamingStatus.ENDED) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg)); AppSectionTitle("本次漫游总结")
                AppSecondaryText("经过地点：${state.routePlaces.joinToString("、") { it.name }.ifBlank { "自由漫游" }}")
                AppSecondaryText("打卡地点：${state.checkIns.mapNotNull { c -> state.routePlaces.firstOrNull { it.id == c.placeId }?.name }.joinToString("、").ifBlank { "无" }}")
                AppSecondaryText("开始：${session.startedAtEpochMs} · 结束：${session.endedAtEpochMs ?: 0L}")
                AppSecondaryText("轨迹距离：${state.distanceMeters?.let { com.y.citycapsule.core.location.GeoDistance.label(it) } ?: "无法计算"}")
                AppSecondaryText("关联城市碎片：${state.relatedCapsules.size}")
                if (state.requestedRouteId == null && state.checkIns.isNotEmpty()) {
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    AppButton("按打卡顺序保存为路线", { store.dispatch(RoamingSessionIntent.SaveAsRoute) }, variant = AppButtonVariant.SECONDARY, loading = state.busy)
                }
            }
        }
    })
}
