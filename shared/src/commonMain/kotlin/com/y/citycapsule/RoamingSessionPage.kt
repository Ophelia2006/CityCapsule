package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.animation.core.animateDpAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectVerticalDragGestures
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.LocalPlacePhotoCacheRepository
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
import com.y.citycapsule.core.capsule.CapsuleMood
import com.y.citycapsule.core.capsule.RepositoryCapsuleMediaCleanup
import com.y.citycapsule.core.media.KuiklyCameraCapability
import com.y.citycapsule.core.media.KuiklyManagedMediaFiles
import com.y.citycapsule.core.media.KuiklyPhotoPicker
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.roaming.LocalRoamingHistoryRepository
import com.y.citycapsule.core.map.AmapNativeView
import com.y.citycapsule.core.map.ExploreMapViewState
import com.y.citycapsule.core.map.MapCameraModel
import com.y.citycapsule.core.map.MapMarkerModel
import com.y.citycapsule.core.map.MapPrivacyConsentRepository
import com.y.citycapsule.core.map.MapViewportPolicy
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.designsystem.component.*
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.place.PlaceMedia
import com.y.citycapsule.feature.roaming.RoamingSessionEffect
import com.y.citycapsule.feature.roaming.RoamingSessionIntent
import com.y.citycapsule.feature.roaming.RoamingSessionStore
import com.y.citycapsule.feature.roaming.RoamingSessionUiState
import com.y.citycapsule.feature.capsule.RoamingCapsuleEditorModal
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.delay

@Page(AppRouteTable.PAGE_ROAMING_SESSION, supportInLocal = true)
internal class RoamingSessionPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val places = LocalPlaceRepository(storage)
        val placePhotos = LocalPlacePhotoCacheRepository(storage)
        val routes = DefaultLocalRouteRepository(storage, places)
        val sessions = LocalRoamingSessionRepository(storage, routes)
        val trackFiles = KuiklyTrackFiles(this)
        val tracks = LocalTrackRepository(storage, trackFiles)
        val favorites = LocalFavoriteRepository(storage, places)
        val history = LocalRoamingHistoryRepository(storage)
        val capsules = LocalCapsuleRepository(storage)
        val routeId = pageData.params.optString(AppRouteTable.PARAM_ROUTE_ID).takeIf(String::isNotBlank)
        val theme = KuiklyAppThemeHost(this)
        setContent { RuntimeAppTheme(theme) { RoamingSessionScreen(
            routeId, navigator, routes, sessions, tracks, KuiklyLocationCapability(this),
            places, CheckInRepository(storage), capsules, trackFiles, favorites, history,
            MapPrivacyConsentRepository(storage), KuiklyCameraCapability(this),
            KuiklyPhotoPicker(this), RepositoryCapsuleMediaCleanup(capsules, KuiklyManagedMediaFiles(this)),
            placePhotos
        ) } }
    }
}

@Composable private fun RoamingSessionScreen(routeId: String?, navigator: KuiklyAppNavigator, routes: DefaultLocalRouteRepository, sessions: LocalRoamingSessionRepository, tracks: LocalTrackRepository, location: com.y.citycapsule.core.location.LocationCapability, places: LocalPlaceRepository, checkIns: CheckInRepository, capsules: LocalCapsuleRepository, files: KuiklyTrackFiles, favorites: LocalFavoriteRepository, history: LocalRoamingHistoryRepository, mapConsent: MapPrivacyConsentRepository, camera: com.y.citycapsule.core.media.CameraCapability, photoPicker: com.y.citycapsule.core.media.PhotoPickerCapability, mediaCleanup: com.y.citycapsule.core.capsule.CapsuleMediaCleanup, photoCache: LocalPlacePhotoCacheRepository) {
    val scope = rememberCoroutineScope()
    val store = remember(routeId, routes, sessions, tracks, location, places, checkIns, capsules, files, favorites, history, photoCache) { RoamingSessionStore(sessions, routes, tracks, location, places, checkIns, capsules, files, favorites, history, routeId, scope, photoCache) }
    val state by store.state.collectAsState()
    val nextPlace = state.routePlaces.firstOrNull { routePlace -> state.checkIns.none { it.placeId == routePlace.id } }
    val nextMemories = nextPlace?.let { place -> state.previousMemories.filter { it.placeId == place.id }.take(2) }.orEmpty()
    var presentedNextPlaceId by remember(store) { mutableStateOf<String?>(null) }
    var showNextSuggestion by remember(store) { mutableStateOf(false) }
    var capsuleEditorPlaceId by remember(store) { mutableStateOf<String?>(null) }
    var capsuleEditorMood by remember(store) { mutableStateOf<CapsuleMood?>(null) }
    var roamingMood by remember(store) { mutableStateOf<CapsuleMood?>(null) }
    var showExitConfirmation by remember(store) { mutableStateOf(false) }
    var exitAfterEnd by remember(store) { mutableStateOf(false) }
    DisposableEffect(store) { onDispose(store::dispose) }
    LaunchedEffect(store) { store.dispatch(RoamingSessionIntent.Load) }
    LaunchedEffect(store, mapConsent) { mapConsent.load { accepted -> if (accepted) store.dispatch(RoamingSessionIntent.MapPrivacyAccepted) } }
    LaunchedEffect(store, state.session?.status) {
        while (state.session?.status == RoamingStatus.ACTIVE) {
            store.dispatch(RoamingSessionIntent.SampleLocation)
            delay(15_000L)
        }
    }
    LaunchedEffect(store, state.session?.status, state.mapPrivacyAccepted) {
        if (state.session?.status == RoamingStatus.ACTIVE && !state.mapPrivacyAccepted) {
            store.dispatch(RoamingSessionIntent.MapRequested)
        }
    }
    LaunchedEffect(store, state.session?.status, nextPlace?.id, state.showMapPrivacyPrompt) {
        val id = nextPlace?.id
        if (state.session?.status == RoamingStatus.ACTIVE && id != null && id != presentedNextPlaceId && !state.showMapPrivacyPrompt) {
            delay(300L)
            presentedNextPlaceId = id
            showNextSuggestion = true
        }
    }
    LaunchedEffect(store, state.session?.status, exitAfterEnd) {
        if (exitAfterEnd && state.session?.status == RoamingStatus.ENDED && !state.busy) {
            exitAfterEnd = false
            store.dispatch(RoamingSessionIntent.Back)
        }
    }
    LaunchedEffect(store) { store.effects.collect { effect ->
        when (effect) {
            RoamingSessionEffect.Back -> navigator.back()
            is RoamingSessionEffect.OpenCapsuleEditor -> {
                capsuleEditorMood = null
                capsuleEditorPlaceId = effect.placeId
            }
            is RoamingSessionEffect.OpenCapsule -> navigator.navigate(com.y.citycapsule.core.navigation.AppRoute.CapsuleDetail(effect.capsuleId))
        }
    } }
    val immersive = state.session?.status == RoamingStatus.ACTIVE || state.session?.status == RoamingStatus.PAUSED
    if (immersive) {
        RoamingImmersiveMap(
            state = state,
            nextPlace = nextPlace,
            statusBarHeight = LocalActivity.current.pageData.statusBarHeight,
            onExit = { showExitConfirmation = true },
            onMapRequested = { store.dispatch(RoamingSessionIntent.MapRequested) },
            onMapEvent = { store.dispatch(RoamingSessionIntent.MapEventReceived(it)) },
            onSampleLocation = { store.dispatch(RoamingSessionIntent.SampleLocation) },
            onPauseOrResume = {
                store.dispatch(if (state.session?.status == RoamingStatus.ACTIVE) RoamingSessionIntent.Pause else RoamingSessionIntent.Resume)
            },
            selectedMood = roamingMood,
            onMoodSelected = { roamingMood = if (roamingMood == it) null else it },
            onRecord = { placeId, mood ->
                if (placeId == null) {
                    store.dispatch(RoamingSessionIntent.OpenCapsulePlacePicker)
                } else {
                    capsuleEditorMood = mood
                    capsuleEditorPlaceId = placeId
                }
            },
            onCheckIn = { placeId, manual ->
                store.dispatch(if (manual) RoamingSessionIntent.ManualCheckIn(placeId) else RoamingSessionIntent.ConfirmCheckIn(placeId))
            },
            onRestoreWantTo = { store.dispatch(RoamingSessionIntent.RestoreWantTo(it)) },
            onOpenMemory = { store.dispatch(RoamingSessionIntent.OpenPreviousMemory(it)) }
        )
    } else AppFixedHeaderScaffold(LocalActivity.current.pageData.statusBarHeight, header = {
        AppActionTopBar("漫游会话", { store.dispatch(RoamingSessionIntent.Back) })
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    }, content = {
        if (state.loading) LoadingState("正在读取漫游状态…") else {
            val session = state.session
            AppSectionTitle(state.requestedRouteName ?: if (state.activeRouteId == null) "自由漫游" else "路线不可用")
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppSecondaryText(when (session?.status) {
                RoamingStatus.ACTIVE -> "正在记录本次漫游轨迹，并在地图上实时更新。"
                RoamingStatus.PAUSED -> "漫游已暂停，可以继续或结束。"
                RoamingStatus.ENDED -> "上一次漫游已经结束。"
                null -> "准备好后开始；当前只记录会话状态。"
            })
            state.message?.let { Spacer(Modifier.height(AppTheme.dimensions.spacingSm)); AppStatusMessage(it) }
            state.lastRemovedFavoriteId?.let { placeId ->
                AppButton("撤销移出想去", { store.dispatch(RoamingSessionIntent.RestoreWantTo(placeId)) }, variant = AppButtonVariant.TEXT)
            }
            state.track?.let { track ->
                if (track.status == TrackStatus.INTERRUPTED) AppStatusMessage("轨迹已中断：${track.interruptionReason.orEmpty()}。漫游会话仍保持进行中，可重试采样。")
            }
            if (session != null) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSectionTitle("漫游轨迹")
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                if (state.mapPrivacyAccepted) {
                    val markerPlaces = (state.routePlaces + state.nearbyPlaces.map { it.place })
                        .distinctBy { it.id }
                    val center = state.currentLocation
                        ?: markerPlaces.firstNotNullOfOrNull { it.geoPoint }
                    AmapNativeView(
                        state = ExploreMapViewState(
                            markers = markerPlaces.mapNotNull { place ->
                                place.geoPoint?.let { MapMarkerModel(place.id, place.name, it) }
                            },
                            camera = center?.let { MapCameraModel(it, 16.0) },
                            currentLocation = state.currentLocation,
                            showCurrentLocation = state.currentLocation != null,
                            trackPoints = state.trackPoints.map { GeoPoint(it.latitude, it.longitude) },
                            plannedTrackPoints = state.plannedRoutePoints
                        ),
                        privacyAccepted = true,
                        onEvent = { store.dispatch(RoamingSessionIntent.MapEventReceived(it)) },
                        modifier = Modifier.fillMaxWidth().height(AppTheme.dimensions.mapViewportHeight)
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                    AppSecondaryText("浅绿色是计划路线，薄荷绿色是这次真正走过的路。")
                    state.mapMessage?.let { AppStatusMessage(it) }
                } else {
                    AppButton("显示实时地图", { store.dispatch(RoamingSessionIntent.MapRequested) }, variant = AppButtonVariant.SECONDARY)
                }
            }
            if (nextPlace != null) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSectionTitle("下一站")
                AppCard(Modifier.fillMaxWidth()) {
                    AppBodyText("${nextPlace.category.emoji}  ${nextPlace.name}")
                    AppSecondaryText(listOfNotNull(nextPlace.district, nextPlace.address).joinToString(" · ").ifBlank { "沿已规划的真实步行路线继续探索" })
                    state.nearbyPlaces.firstOrNull { it.place.id == nextPlace.id }?.let { AppCaptionText("距当前位置 ${com.y.citycapsule.core.location.GeoDistance.label(it.distanceMeters)}") }
                    val remaining = state.routePlaces.count { place -> state.checkIns.none { it.placeId == place.id } }
                    AppCaptionText("本路线还剩 $remaining 个地点")
                }
                val memories = state.previousMemories.filter { it.placeId == nextPlace.id }.take(2)
                if (memories.isNotEmpty()) {
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    AppSecondaryText("你曾在这里留下过 ${memories.size} 段城市记忆")
                    memories.forEach { memory ->
                        AppButton(
                            memory.mood?.let { "${it.emoji} ${memory.content.take(28).ifBlank { "回看这一刻" }}" } ?: memory.content.take(28).ifBlank { "回看这一刻" },
                            { store.dispatch(RoamingSessionIntent.OpenPreviousMemory(memory.id)) },
                            variant = AppButtonVariant.TEXT
                        )
                    }
                }
            }
            if (session?.status == RoamingStatus.ACTIVE && (state.routePlaces.isNotEmpty() || state.nearbyPlaces.isNotEmpty())) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSectionTitle(if (state.activeRouteId == null) "附近地点（500 米内）" else "路线打卡")
                val visiblePlaces = if (state.activeRouteId == null) state.nearbyPlaces.map { it.place } else state.routePlaces
                visiblePlaces.forEach { place ->
                    val checked = state.checkIns.firstOrNull { it.placeId == place.id }
                    val distance = state.nearbyPlaces.firstOrNull { it.place.id == place.id }?.distanceMeters
                    val wantTo = state.nearbyPlaces.firstOrNull { it.place.id == place.id }?.isFavorite == true
                    AppSecondaryText("${if (checked != null) "✓" else "○"} ${if (wantTo) "想去 · " else ""}${place.name}${distance?.let { " · ${com.y.citycapsule.core.location.GeoDistance.label(it)}" }.orEmpty()}${checked?.let { if (it.method == CheckInMethod.MANUAL) " · 手动记录" else " · GPS 确认" }.orEmpty()}")
                    if (checked == null && distance != null && distance <= 200.0) AppButton("确认到达 ${place.name}", { store.dispatch(RoamingSessionIntent.ConfirmCheckIn(place.id)) }, variant = AppButtonVariant.SECONDARY)
                    if (checked == null && state.track?.status == TrackStatus.INTERRUPTED) AppButton("手动记录 ${place.name}", { store.dispatch(RoamingSessionIntent.ManualCheckIn(place.id)) }, variant = AppButtonVariant.TEXT)
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
                AppButton("留下城市碎片", { store.dispatch(RoamingSessionIntent.OpenCapsulePlacePicker) })
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppButton("记录当前位置", { store.dispatch(RoamingSessionIntent.SampleLocation) }, variant = AppButtonVariant.SECONDARY, loading = state.sampling)
            }
            if (session?.status == RoamingStatus.ENDED) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg)); AppSectionTitle("本次漫游总结")
                AppSecondaryText("经过地点：${state.routePlaces.joinToString("、") { it.name }.ifBlank { "自由漫游" }}")
                AppSecondaryText("打卡地点：${state.checkIns.mapNotNull { c -> state.availablePlaces.firstOrNull { it.id == c.placeId }?.name }.joinToString("、").ifBlank { "无" }}")
                AppSecondaryText("开始：${session.startedAtEpochMs} · 结束：${session.endedAtEpochMs ?: 0L}")
                AppSecondaryText("轨迹距离：${state.distanceMeters?.let { com.y.citycapsule.core.location.GeoDistance.label(it) } ?: "无法计算"}")
                AppSecondaryText("关联城市碎片：${state.relatedCapsules.size}")
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppButton("查看漫游回顾", { navigator.navigate(com.y.citycapsule.core.navigation.AppRoute.RoamingHistory(session.startedAtEpochMs.toString())) }, variant = AppButtonVariant.TEXT)
                if (state.activeRouteId == null && state.checkIns.isNotEmpty()) {
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    AppButton("按打卡顺序保存为路线", { store.dispatch(RoamingSessionIntent.SaveAsRoute) }, variant = AppButtonVariant.SECONDARY, loading = state.busy)
                }
            }
        }
    })
    AppBottomSheet(
        visible = showNextSuggestion && nextPlace != null,
        title = nextPlace?.let { "下一站 · ${it.name}" } ?: "下一站",
        onDismiss = { showNextSuggestion = false },
        dismissLabel = "稍后再看",
        footer = { AppButton("继续探索", { showNextSuggestion = false }) }
    ) {
        nextPlace?.let { place ->
            AppBodyText("${place.category.emoji}  ${place.name}")
            AppSecondaryText(listOfNotNull(place.district, place.address).joinToString(" · ").ifBlank { "沿已规划的真实步行路线前往" })
            state.nearbyPlaces.firstOrNull { it.place.id == place.id }?.let { AppCaptionText("距当前位置 ${com.y.citycapsule.core.location.GeoDistance.label(it.distanceMeters)}") }
            val remaining = state.routePlaces.count { routePlace -> state.checkIns.none { it.placeId == routePlace.id } }
            AppCaptionText("本路线还剩 $remaining 个地点")
            if (nextMemories.isNotEmpty()) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppSectionTitle("你曾在这里留下")
                nextMemories.forEach { memory ->
                    AppButton(
                        memory.mood?.let { "${it.emoji} ${memory.content.take(28).ifBlank { "回看这一刻" }}" } ?: memory.content.take(28).ifBlank { "回看这一刻" },
                        { showNextSuggestion = false; store.dispatch(RoamingSessionIntent.OpenPreviousMemory(memory.id)) },
                        variant = AppButtonVariant.TEXT
                    )
                }
            }
        }
    }
    AppBottomSheet(
        visible = state.showMapPrivacyPrompt,
        title = "显示漫游地图",
        onDismiss = { store.dispatch(RoamingSessionIntent.MapPrivacyDismissed) },
        dismissLabel = "暂不显示",
        footer = {
            AppButton("同意并打开地图", { mapConsent.accept(); store.dispatch(RoamingSessionIntent.MapPrivacyAccepted) })
        }
    ) {
        AppSecondaryText("地图会使用高德 SDK 渲染当前位置、地点和本次轨迹。拒绝不影响轨迹文件记录。")
    }
    val capsuleCandidates = com.y.citycapsule.feature.roaming.capsulePlaceCandidates(state)
    AppBottomSheet(
        visible = state.showCapsulePlacePicker,
        title = "选择这一刻的地点",
        onDismiss = { store.dispatch(RoamingSessionIntent.DismissCapsulePlacePicker) },
        dismissLabel = "取消",
        footer = {
            AppButton(
                "在这里留下城市碎片",
                { store.dispatch(RoamingSessionIntent.CreateCapsule) },
                enabled = state.selectedCapsulePlaceId != null
            )
        }
    ) {
        if (capsuleCandidates.isEmpty()) {
            AppSecondaryText("还没有可关联的地点。先记录当前位置，或从带地点的路线开始漫游。")
        } else {
            AppSecondaryText("优先显示本次路线与附近地点，也可以选择地点库中的任何地点。")
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            capsuleCandidates.forEach { place ->
                AppChoiceChip(
                    text = place.name,
                    selected = state.selectedCapsulePlaceId == place.id,
                    onClick = { store.dispatch(RoamingSessionIntent.CapsulePlaceSelected(place.id)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            }
        }
    }
    if (showExitConfirmation) {
        AppConfirmDialog(
            title = "结束这次漫游？",
            message = "轨迹、已到达地点和已经保存的城市碎片会进入漫游回顾。",
            confirmText = "结束并退出",
            onConfirm = {
                showExitConfirmation = false
                exitAfterEnd = true
                store.dispatch(RoamingSessionIntent.End)
            },
            onDismiss = { showExitConfirmation = false }
        )
    }
    RoamingCapsuleEditorModal(
        visible = capsuleEditorPlaceId != null,
        placeId = capsuleEditorPlaceId,
        roamingSessionId = state.session?.startedAtEpochMs?.toString(),
        initialMood = capsuleEditorMood,
        capsuleRepository = capsules,
        placeRepository = places,
        camera = camera,
        photoPicker = photoPicker,
        mediaCleanup = mediaCleanup,
        onDismiss = {
            capsuleEditorPlaceId = null
            capsuleEditorMood = null
        },
        onPublished = {
            capsuleEditorPlaceId = null
            capsuleEditorMood = null
            roamingMood = null
            store.dispatch(RoamingSessionIntent.Load)
        }
    )
}

@Composable
private fun RoamingImmersiveMap(
    state: RoamingSessionUiState,
    nextPlace: com.y.citycapsule.core.place.Place?,
    statusBarHeight: Float,
    onExit: () -> Unit,
    onMapRequested: () -> Unit,
    onMapEvent: (com.y.citycapsule.core.map.MapViewEvent) -> Unit,
    onSampleLocation: () -> Unit,
    onPauseOrResume: () -> Unit,
    selectedMood: CapsuleMood?,
    onMoodSelected: (CapsuleMood) -> Unit,
    onRecord: (String?, CapsuleMood?) -> Unit,
    onCheckIn: (String, Boolean) -> Unit,
    onRestoreWantTo: (String) -> Unit,
    onOpenMemory: (String) -> Unit
) {
    val dimensions = AppTheme.dimensions
    val markerPlaces = (state.routePlaces + state.nearbyPlaces.map { it.place }).distinctBy { it.id }
    var panelExpanded by remember(state.session?.startedAtEpochMs) { mutableStateOf(true) }
    var sessionCamera by remember(state.session?.startedAtEpochMs) { mutableStateOf<MapCameraModel?>(null) }
    val panelHeight by animateDpAsState(
        targetValue = if (panelExpanded) dimensions.roamingPanelExpandedHeight else dimensions.roamingPanelCollapsedHeight,
        animationSpec = tween(durationMillis = AppTheme.motion.emphasizedDurationMillis),
        label = "RoamingBottomPanelHeight"
    )
    val defaultPlace = nextPlace ?: state.nearbyPlaces.firstOrNull()?.place
    val focusPlace = markerPlaces.firstOrNull { it.id == state.selectedMapPlaceId } ?: defaultPlace
    val checkedIn = focusPlace?.let { place -> state.checkIns.firstOrNull { it.placeId == place.id } }
    val nearby = focusPlace?.let { place -> state.nearbyPlaces.firstOrNull { it.place.id == place.id } }
    val isWanted = focusPlace?.id in state.favoriteIds
    val previousMemory = focusPlace?.let { place -> state.previousMemories.firstOrNull { it.placeId == place.id } }
    val routeTitle = state.requestedRouteName ?: if (state.activeRouteId == null) "自由漫游" else "城市漫游"
    val routeViewport = MapViewportPolicy.cameraFor(
        state.plannedRoutePoints.ifEmpty { markerPlaces.mapNotNull { it.geoPoint } }
    )
    LaunchedEffect(state.session?.startedAtEpochMs, routeViewport, state.currentLocation) {
        if (sessionCamera == null) {
            sessionCamera = routeViewport
                ?: state.currentLocation?.let { MapCameraModel(it, 16.0) }
        }
    }
    LaunchedEffect(state.selectedMapPlaceId) {
        if (state.selectedMapPlaceId != null) panelExpanded = true
    }

    Box(Modifier.fillMaxSize().background(AppTheme.colors.surfaceVariant)) {
        if (state.mapPrivacyAccepted) {
            AmapNativeView(
                state = ExploreMapViewState(
                    markers = markerPlaces.mapNotNull { place ->
                        place.geoPoint?.let { MapMarkerModel(place.id, place.name, it) }
                    },
                    selectedPlaceId = focusPlace?.id,
                    // Initialize once per roaming session. GPS sampling must not steal the
                    // viewport after the user pans/zooms or taps a marker.
                    camera = sessionCamera,
                    currentLocation = state.currentLocation,
                    showCurrentLocation = state.currentLocation != null,
                    trackPoints = state.trackPoints.map { GeoPoint(it.latitude, it.longitude) },
                    plannedTrackPoints = state.plannedRoutePoints
                ),
                privacyAccepted = true,
                onEvent = { event ->
                    onMapEvent(event)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppButton(
                    "显示实时地图",
                    onMapRequested,
                    modifier = Modifier.padding(horizontal = dimensions.screenHorizontalPadding),
                    variant = AppButtonVariant.SECONDARY
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions.screenHorizontalPadding,
                    top = statusBarHeight.dp + dimensions.spacingSm,
                    end = dimensions.screenHorizontalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoamingExitButton(onExit)
            Spacer(Modifier.width(dimensions.spacingSm))
            AppCard(
                modifier = Modifier.weight(1f),
                elevation = AppTheme.elevation.overlay
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AppCaptionText(if (state.session?.status == RoamingStatus.PAUSED) "已暂停" else "正在漫游")
                        AppSectionTitle(routeTitle)
                    }
                    AppCaptionText(state.distanceMeters?.let { com.y.citycapsule.core.location.GeoDistance.label(it) } ?: "记录中")
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(panelHeight)
                .shadow(
                    AppTheme.elevation.overlay,
                    RoundedCornerShape(topStart = dimensions.radiusXl, topEnd = dimensions.radiusXl),
                    clip = false,
                    ambientColor = AppTheme.colors.scrim.copy(alpha = 0.08f),
                    spotColor = AppTheme.colors.scrim.copy(alpha = 0.14f)
                )
                .clip(RoundedCornerShape(topStart = dimensions.radiusXl, topEnd = dimensions.radiusXl))
                .background(AppTheme.colors.background)
                .padding(
                    start = dimensions.spacingMd,
                    end = dimensions.spacingMd,
                    bottom = dimensions.spacingMd
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth()
                    .height(dimensions.minTouchTarget)
                    .pointerInput(state.session?.startedAtEpochMs) {
                        var accumulatedDrag = 0f
                        val threshold = dimensions.minTouchTarget.toPx() / 2f
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f },
                            onDragEnd = {
                                panelExpanded = roamingPanelExpandedAfterDrag(
                                    currentExpanded = panelExpanded,
                                    dragDistancePx = accumulatedDrag,
                                    thresholdPx = threshold
                                )
                                accumulatedDrag = 0f
                            },
                            onVerticalDrag = { _, dragAmount -> accumulatedDrag += dragAmount }
                        )
                    }
                    .clickable { panelExpanded = !panelExpanded },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    Modifier
                        .width(dimensions.roamingPanelHandleWidth)
                        .height(dimensions.spacingXxs)
                        .clip(RoundedCornerShape(dimensions.radiusXl))
                        .background(AppTheme.colors.divider)
                )
                AppCaptionText(
                    if (state.selectedMapPlaceId == null) "当前地点" else "地图已选择",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }
            AppCard(elevation = AppTheme.elevation.raised) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(dimensions.placeCompactMediaSize)
                            .clip(RoundedCornerShape(dimensions.radiusMd))
                            .background(AppTheme.colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (focusPlace == null) {
                            Text("🚶", style = AppTheme.typography.pageTitle)
                        } else {
                            PlaceMedia(
                                place = focusPlace,
                                cachedPhoto = state.placePhotos[focusPlace.id],
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(Modifier.width(dimensions.spacingSm))
                    Column(Modifier.weight(1f)) {
                        AppCaptionText(if (focusPlace == null) "漫游地点" else if (nextPlace != null) "下一站" else "附近地点")
                        AppSectionTitle(focusPlace?.name ?: "探索途中")
                        val placeMeta = focusPlace?.let { place ->
                            listOfNotNull(place.district, nearby?.distanceMeters?.let(com.y.citycapsule.core.location.GeoDistance::label)).joinToString(" · ")
                        }.orEmpty()
                        if (placeMeta.isNotBlank()) AppCaptionText(placeMeta)
                    }
                    if (checkedIn != null) AppCaptionText("✓ 已到达")
                    else if (isWanted) AppCaptionText("♡ 想去")
                }
                if (focusPlace != null && checkedIn == null && nearby?.distanceMeters?.let { it <= 200.0 } == true) {
                    Spacer(Modifier.height(dimensions.spacingXs))
                    AppButton("确认到达", { onCheckIn(focusPlace.id, false) }, variant = AppButtonVariant.SECONDARY)
                } else if (focusPlace != null && checkedIn == null && state.track?.status == TrackStatus.INTERRUPTED) {
                    Spacer(Modifier.height(dimensions.spacingXs))
                    AppButton("手动记录到达", { onCheckIn(focusPlace.id, true) }, variant = AppButtonVariant.SECONDARY)
                }
                if (state.lastRemovedFavoriteId == focusPlace?.id) {
                    focusPlace?.let { place ->
                        AppButton("撤销移出想去", { onRestoreWantTo(place.id) }, variant = AppButtonVariant.TEXT)
                    }
                }
                previousMemory?.let { memory ->
                    AppButton(
                        "回看曾经留在这里的一刻",
                        { onOpenMemory(memory.id) },
                        variant = AppButtonVariant.TEXT
                    )
                }
            }

            Spacer(Modifier.height(dimensions.spacingSm))
            AppCaptionText("这一刻的心情")
            Spacer(Modifier.height(dimensions.spacingXxs))
            Row(Modifier.fillMaxWidth()) {
                CapsuleMood.entries.forEach { mood ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .size(dimensions.minTouchTarget)
                            .clip(RoundedCornerShape(dimensions.radiusXl))
                            .background(if (selectedMood == mood) AppTheme.colors.primaryContainer else Color.Transparent)
                            .clickable { onMoodSelected(mood) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(mood.emoji, style = AppTheme.typography.sectionTitle)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppButton(
                    if (state.session?.status == RoamingStatus.PAUSED) "继续" else "暂停",
                    onPauseOrResume,
                    modifier = Modifier.weight(0.32f),
                    variant = AppButtonVariant.SECONDARY,
                    enabled = !state.busy
                )
                Spacer(Modifier.width(dimensions.spacingXs))
                AppButton(
                    "留下城市碎片",
                    { onRecord(focusPlace?.id, selectedMood) },
                    modifier = Modifier.weight(0.68f),
                    enabled = !state.busy
                )
            }
            AppButton(
                if (state.sampling) "正在定位…" else "更新当前位置",
                onSampleLocation,
                variant = AppButtonVariant.TEXT,
                enabled = !state.sampling
            )
            state.mapMessage?.let { AppStatusMessage(it) }
        }
    }
}

internal fun roamingPanelExpandedAfterDrag(
    currentExpanded: Boolean,
    dragDistancePx: Float,
    thresholdPx: Float
): Boolean = when {
    dragDistancePx <= -thresholdPx -> true
    dragDistancePx >= thresholdPx -> false
    else -> currentExpanded
}

@Composable
private fun RoamingExitButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppTheme.dimensions.radiusXl)
    Box(
        modifier = Modifier
            .size(AppTheme.dimensions.minTouchTarget)
            .shadow(
                AppTheme.elevation.overlay,
                shape,
                clip = false,
                ambientColor = AppTheme.colors.scrim.copy(alpha = 0.08f),
                spotColor = AppTheme.colors.scrim.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(AppTheme.colors.error)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("×", color = AppTheme.colors.onError, style = AppTheme.typography.pageTitle)
    }
}
