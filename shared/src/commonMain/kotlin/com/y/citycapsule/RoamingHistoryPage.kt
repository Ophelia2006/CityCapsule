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
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.KuiklyLocalCapsuleDateFormatter
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.roaming.LocalRoamingHistoryRepository
import com.y.citycapsule.core.roaming.RoamingMode
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.track.KuiklyTrackFiles
import com.y.citycapsule.core.map.AmapNativeView
import com.y.citycapsule.core.map.ExploreMapViewState
import com.y.citycapsule.core.map.MapPrivacyConsentRepository
import com.y.citycapsule.core.map.MapPrivacyConsentRuntime
import com.y.citycapsule.core.map.MapViewportPolicy
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.share.KuiklyShareCapability
import com.y.citycapsule.core.share.ShareCapability
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppCard
import com.y.citycapsule.designsystem.component.AppFixedHeaderScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.CapsuleCard
import com.y.citycapsule.designsystem.component.CapsuleCardModel
import com.y.citycapsule.designsystem.component.CapsuleCardVariant
import com.y.citycapsule.feature.capsule.CapsulePhoto
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.roaming.RoamingHistoryEffect
import com.y.citycapsule.feature.roaming.RoamingHistoryIntent
import com.y.citycapsule.feature.roaming.RoamingHistoryStatus
import com.y.citycapsule.feature.roaming.RoamingHistoryStore
import com.y.citycapsule.feature.roaming.buildRoamingReport
import kotlinx.coroutines.flow.collect

@Page(AppRouteTable.PAGE_ROAMING_HISTORY, supportInLocal = true)
internal class RoamingHistoryPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val storage = KuiklyKeyValueStore(this)
        val recordId = pageData.params.optString(AppRouteTable.PARAM_ROAMING_RECORD_ID).takeIf(String::isNotBlank)
        val navigator = KuiklyAppNavigator(this)
        val theme = KuiklyAppThemeHost(this)
        setContent {
            RuntimeAppTheme(theme) {
                RoamingHistoryScreen(
                    recordId,
                    navigator,
                    LocalRoamingHistoryRepository(storage),
                    LocalCapsuleRepository(storage),
                    KuiklyTrackFiles(this),
                    KuiklyLocalCapsuleDateFormatter(this),
                    MapPrivacyConsentRepository(storage),
                    KuiklyShareCapability(this)
                )
            }
        }
    }
}

@Composable
private fun RoamingHistoryScreen(
    recordId: String?,
    navigator: KuiklyAppNavigator,
    history: LocalRoamingHistoryRepository,
    capsules: LocalCapsuleRepository,
    trackFiles: KuiklyTrackFiles,
    dateFormatter: KuiklyLocalCapsuleDateFormatter,
    mapConsent: MapPrivacyConsentRepository,
    share: ShareCapability
) {
    val scope = rememberCoroutineScope()
    val store = remember(recordId, history, capsules, trackFiles) { RoamingHistoryStore(history, capsules, trackFiles, recordId, scope) }
    val state by store.state.collectAsState()
    DisposableEffect(store) { onDispose(store::dispose) }
    LaunchedEffect(store) { store.dispatch(RoamingHistoryIntent.Load) }
    LaunchedEffect(store, navigator) { store.effects.collect { effect -> when (effect) {
        RoamingHistoryEffect.Back -> navigator.back()
        is RoamingHistoryEffect.OpenRecord -> navigator.navigate(AppRoute.RoamingHistory(effect.id))
        is RoamingHistoryEffect.OpenCapsule -> navigator.navigate(AppRoute.CapsuleDetail(effect.id))
        is RoamingHistoryEffect.AddCapsule -> navigator.navigate(AppRoute.CapsuleEditor(placeId = effect.placeId, roamingSessionId = effect.sessionId))
    } } }
    AppFixedHeaderScaffold(
        statusBarHeight = LocalActivity.current.pageData.statusBarHeight,
        header = {
            AppActionTopBar(if (recordId == null) "漫游回顾" else "这次漫游", { store.dispatch(RoamingHistoryIntent.Back) })
            Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        },
        content = {
            when (state.status) {
                RoamingHistoryStatus.LOADING -> LoadingState("正在整理漫游记忆…")
                RoamingHistoryStatus.ERROR -> ErrorState(state.message.orEmpty()) { store.dispatch(RoamingHistoryIntent.Load) }
                RoamingHistoryStatus.READY -> if (recordId == null) {
                    if (state.records.isEmpty()) EmptyState("还没有漫游记录", "从想去地点规划一次探索，或开始自由漫游。")
                    else state.records.forEach { record ->
                        RoamingRecordCard(record, dateFormatter) { store.dispatch(RoamingHistoryIntent.OpenRecord(record.id)) }
                        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    }
                } else {
                    val record = state.selectedRecord
                    if (record == null) EmptyState("漫游记录不存在", "它可能尚未结束，或本地记录已经丢失。")
                    else RoamingRecordDetail(record, dateFormatter, state.capsules, state.trackPoints, mapConsent, share, store::dispatch)
                }
            }
        }
    )
}

@Composable
private fun RoamingRecordCard(record: RoamingRecord, formatter: KuiklyLocalCapsuleDateFormatter, onOpen: () -> Unit) {
    AppCard(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        AppSectionTitle(record.routeName ?: if (record.mode == RoamingMode.FREE) "自由漫游" else "计划漫游")
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText("${formatter.format(record.startedAtEpochMs)} · ${durationLabel(record)}")
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppCaptionText("${record.visits.size} 个地点 · ${record.distanceMeters?.let(com.y.citycapsule.core.location.GeoDistance::label) ?: "距离无法计算"}")
    }
}

@Composable
private fun RoamingRecordDetail(
    record: RoamingRecord,
    formatter: KuiklyLocalCapsuleDateFormatter,
    capsules: List<com.y.citycapsule.core.capsule.CityCapsule>,
    trackPoints: List<com.y.citycapsule.core.track.TrackPoint>,
    mapConsent: MapPrivacyConsentRepository,
    share: ShareCapability,
    dispatch: (RoamingHistoryIntent) -> Unit
) {
    var mapAccepted by remember(record.id) { mutableStateOf(MapPrivacyConsentRuntime.accepted) }
    var showMapPrompt by remember(record.id) { mutableStateOf(false) }
    LaunchedEffect(mapConsent, record.id) { mapConsent.load { accepted -> mapAccepted = accepted } }
    val report = remember(record, capsules) { buildRoamingReport(record, capsules) }
    report.coverImagePath?.let { path ->
        CapsulePhoto(path, "本次漫游精选封面", compact = false)
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    }
    AppSectionTitle(record.routeName ?: if (record.mode == RoamingMode.FREE) "自由漫游" else "计划漫游")
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    AppSecondaryText("${formatter.format(record.startedAtEpochMs)} · ${durationLabel(record)}")
    AppSecondaryText("开始 ${formatter.formatDateTime(record.startedAtEpochMs)}")
    AppSecondaryText("结束 ${formatter.formatDateTime(record.endedAtEpochMs)}")
    AppSecondaryText("总时长 ${durationLabel(record)} · 真实距离 ${record.distanceMeters?.let(com.y.citycapsule.core.location.GeoDistance::label) ?: "无法计算"}")
    AppSecondaryText("${record.visits.size} 个到达地点 · ${capsules.count { it.roamingSessionId == record.id }} 条城市碎片")
    AppSecondaryText("完成想去：${report.completedWantTo?.toString() ?: "旧记录未采集"} · ${if (record.mode == RoamingMode.FREE) "途中发现" else "非路线发现"}：${report.spontaneousVisits}")
    if (trackPoints.isNotEmpty() || record.plannedTrackPoints.isNotEmpty()) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
        AppSectionTitle("走过的城市轨迹")
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        val points = trackPoints.map { GeoPoint(it.latitude, it.longitude) }
        if (mapAccepted) {
            AmapNativeView(
                state = ExploreMapViewState(
                    camera = MapViewportPolicy.cameraFor(points + record.plannedTrackPoints),
                    trackPoints = points,
                    plannedTrackPoints = record.plannedTrackPoints
                ),
                privacyAccepted = true,
                onEvent = {},
                modifier = Modifier.fillMaxWidth().height(AppTheme.dimensions.mapViewportHeight)
            )
            if (points.size < 2) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppStatusMessage("本次只保存了 ${points.size} 个有效 GPS 点，无法绘制实际轨迹线；灰色线仅代表出发前的道路规划。")
            }
        } else {
            AppButton("查看地图轨迹", { showMapPrompt = true }, variant = AppButtonVariant.SECONDARY)
        }
    }
    if (record.plannedDistanceMeters != null) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppCaptionText("灰色为计划道路，琥珀色为实际轨迹 · 计划 ${com.y.citycapsule.core.location.GeoDistance.label(record.plannedDistanceMeters.toDouble())}")
        report.detourMeters?.let { AppCaptionText("绕路距离 ${com.y.citycapsule.core.location.GeoDistance.label(it)}") }
        if (report.skippedPlaceIds.isNotEmpty()) AppCaptionText("跳过 ${report.skippedPlaceIds.size} 个计划地点")
    } else if (record.mode == RoamingMode.PLANNED) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppCaptionText("这条旧漫游没有保存规划道路，无法生成计划/实际对比。")
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppSectionTitle("沿途记忆")
    if (report.moments.isEmpty()) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppSecondaryText("这次漫游没有确认到达地点或留下城市碎片，轨迹仍会保留。")
    } else report.moments.forEachIndexed { index, moment ->
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppBodyText("${index + 1}. ${moment.visit?.place?.name ?: "漫游途中"}")
        AppCaptionText(formatter.formatDateTime(moment.occurredAtEpochMs) + moment.visit?.let { " · ${if (it.method.wireValue == "manual") "手动记录" else "GPS 确认"}" }.orEmpty())
        if (moment.capsules.isEmpty()) {
            moment.visit?.let { AppButton("补记这一刻", { dispatch(RoamingHistoryIntent.AddCapsule(it.place.placeId, record.id)) }, variant = AppButtonVariant.TEXT) }
        } else moment.capsules.forEach { capsule ->
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            CapsuleCard(model = CapsuleCardModel(
                dateLabel = capsule.mood?.let { "${it.emoji} ${it.displayName}" } ?: formatter.format(capsule.createdAtEpochMs),
                placeLabel = moment.visit?.place?.name ?: "漫游途中",
                excerpt = capsule.content.ifBlank { "这一刻被留在了城市里" },
                metadata = capsule.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" }
            ), onOpen = { dispatch(RoamingHistoryIntent.OpenCapsule(capsule.id)) }, variant = CapsuleCardVariant.RECENT,
                media = capsule.imagePaths.firstOrNull()?.let { path -> { CapsulePhoto(path, "沿途照片", compact = true) } })
        }
    }
    if (report.moodSummary != null || report.tagSummary != null) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg)); AppSectionTitle("这次漫游的感觉")
        report.moodSummary?.let { AppSecondaryText(it) }; report.tagSummary?.let { AppCaptionText(it) }
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppCard(Modifier.fillMaxWidth()) {
        AppSectionTitle("城市漫游卡片")
        AppSecondaryText(buildShareText(record, report.completedWantTo, report.spontaneousVisits, report.moodSummary))
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppButton("分享这次漫游", { share.shareText("CityCapsule · 城市漫游", buildShareText(record, report.completedWantTo, report.spontaneousVisits, report.moodSummary)) {} })
    }
    if (showMapPrompt) {
        AppConfirmDialog(
            title = "显示漫游轨迹",
            message = "地图由高德地图 SDK 提供，启用后会联网加载底图；轨迹点仍来自本机保存的这次漫游。",
            confirmText = "同意并打开地图",
            onConfirm = { mapConsent.accept(); mapAccepted = true; showMapPrompt = false },
            onDismiss = { showMapPrompt = false }
        )
    }
}

private fun durationLabel(record: RoamingRecord): String {
    val minutes = ((record.endedAtEpochMs - record.startedAtEpochMs).coerceAtLeast(0L) / 60_000L)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours} 小时 ${rest} 分"
        hours > 0 -> "${hours} 小时"
        else -> "${rest} 分钟"
    }
}

private fun buildShareText(record: RoamingRecord, completedWantTo: Int?, spontaneous: Int, mood: String?): String = buildList {
    add(record.routeName ?: if (record.mode == RoamingMode.FREE) "自由漫游" else "计划漫游")
    add("${durationLabel(record)} · ${record.distanceMeters?.let(com.y.citycapsule.core.location.GeoDistance::label) ?: "距离未记录"}")
    add("到达 ${record.visits.size} 个地点 · 留下的都是真实城市记忆")
    completedWantTo?.let { add("完成 $it 个想去地点") }
    if (spontaneous > 0) add("途中发现 $spontaneous 个新地点")
    mood?.let(::add)
}.joinToString("\n")
