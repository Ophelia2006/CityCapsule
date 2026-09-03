package com.y.citycapsule.feature.roaming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.media.MediaMaintenanceCapability
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.route.LocalRoute
import com.y.citycapsule.core.route.LocalRouteRepository
import com.y.citycapsule.core.roaming.RoamingHistoryRepository
import com.y.citycapsule.core.roaming.RoamingMode
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.roaming.RoamingSession
import com.y.citycapsule.core.roaming.RoamingSessionRepository
import com.y.citycapsule.core.roaming.RoamingStatus
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppCard
import com.y.citycapsule.designsystem.component.AppPageTitle
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.capsule.CapsulePhoto

@Composable
fun RoamingRootContent(
    navigator: AppNavigator,
    sessions: RoamingSessionRepository,
    routes: LocalRouteRepository,
    places: PlaceRepository,
    favorites: FavoriteRepository,
    history: RoamingHistoryRepository,
    capsules: CapsuleRepository,
    dateFormatter: CapsuleDateFormatter,
    thumbnailCapability: MediaMaintenanceCapability? = null,
    active: Boolean,
    statusBarHeight: Float,
    listState: LazyListState
) {
    var session by remember { mutableStateOf<RoamingSession?>(null) }
    var localRoutes by remember { mutableStateOf(emptyList<LocalRoute>()) }
    var favoriteIds by remember { mutableStateOf(emptySet<String>()) }
    var placeById by remember { mutableStateOf(emptyMap<String, Place>()) }
    var recentRoaming by remember { mutableStateOf<RoamingRecord?>(null) }
    var publishedCapsules by remember { mutableStateOf(emptyList<CityCapsule>()) }
    LaunchedEffect(sessions, routes, places, favorites, history, capsules, active) {
        if (active) {
            sessions.get { result ->
            session = (result as? StorageResult.Success)?.value?.takeIf { it.status != RoamingStatus.ENDED }
            }
            routes.getCatalog { result ->
                localRoutes = (result as? StorageResult.Success)?.value?.routes.orEmpty()
            }
            favorites.getFavoriteIds { result ->
                favoriteIds = (result as? StorageResult.Success)?.value?.placeIds.orEmpty()
            }
            places.getCatalog { result ->
                placeById = (result as? StorageResult.Success)?.value?.places.orEmpty().associateBy(Place::id)
            }
            history.getCatalog { result ->
                recentRoaming = (result as? StorageResult.Success)?.value?.records?.maxByOrNull(RoamingRecord::startedAtEpochMs)
            }
            capsules.getPublished { result ->
                publishedCapsules = (result as? StorageResult.Success)?.value.orEmpty()
            }
        }
    }
    val suggestedRoute = remember(localRoutes, favoriteIds, placeById) {
        selectSuggestedRoute(localRoutes, favoriteIds, placeById.keys)
    }
    val dimensions = AppTheme.dimensions
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = dimensions.screenHorizontalPadding,
            top = statusBarHeight.dp + dimensions.spacingXxl,
            end = dimensions.screenHorizontalPadding,
            bottom = dimensions.spacingXl
        )
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                AppPageTitle("漫游")
                Spacer(Modifier.height(dimensions.spacingXs))
                AppSecondaryText("把想去的地方连成一次真实的城市探索。")
                Spacer(Modifier.height(dimensions.spacingXl))
                AppCard {
                    AppSectionTitle(if (session == null) "开始一次漫游" else "继续正在进行的漫游")
                    Spacer(Modifier.height(dimensions.spacingXs))
                    AppSecondaryText(if (session == null) "自由出发，或先用想去地点创建路线。" else "轨迹、打卡与城市碎片会继续记录在同一次漫游中。")
                    Spacer(Modifier.height(dimensions.spacingMd))
                    AppButton(if (session == null) "自由漫游" else "继续漫游", { navigator.navigate(AppRoute.RoamingSession(session?.routeId)) })
                }
                Spacer(Modifier.height(dimensions.spacingXl))
                AppSectionTitle("规划下一次探索")
                Spacer(Modifier.height(dimensions.spacingSm))
                if (suggestedRoute != null) {
                    val names = suggestedRoute.orderedPlaceIds.mapNotNull { placeById[it]?.name }.take(3)
                    val wantedCount = suggestedRoute.orderedPlaceIds.count(favoriteIds::contains)
                    AppCard(Modifier.clickable { navigator.navigate(AppRoute.LocalRouteEditor(suggestedRoute.id)) }) {
                        AppCaptionText("为你推荐 · ${if (wantedCount > 0) "$wantedCount 个想去地点" else "最近创建的路线"}")
                        Spacer(Modifier.height(dimensions.spacingXs))
                        AppSectionTitle(suggestedRoute.name)
                        if (names.isNotEmpty()) {
                            Spacer(Modifier.height(dimensions.spacingXs))
                            AppSecondaryText(names.joinToString(" → ") + if (suggestedRoute.orderedPlaceIds.size > names.size) " 等 ${suggestedRoute.orderedPlaceIds.size} 站" else "")
                        }
                        suggestedRoute.plannedRoute?.let { plan ->
                            Spacer(Modifier.height(dimensions.spacingXs))
                            AppCaptionText("步行约 ${distanceLabel(plan.distanceMeters)} · ${durationLabel(plan.durationSeconds)}")
                        }
                        Spacer(Modifier.height(dimensions.spacingMd))
                        AppButton(if (suggestedRoute.plannedRoute == null) "查看并规划真实路线" else "查看并出发", { navigator.navigate(AppRoute.LocalRouteEditor(suggestedRoute.id)) })
                    }
                } else {
                    AppCard {
                        AppSectionTitle(if (favoriteIds.isEmpty()) "先收集想去的地方" else "把想去地点连成路线")
                        Spacer(Modifier.height(dimensions.spacingXs))
                        AppSecondaryText(if (favoriteIds.isEmpty()) "在探索页把感兴趣的地点加入想去，这里会自动推荐下一次探索。" else "你已有 ${favoriteIds.size} 个想去地点，可以据此创建第一条漫游路线。")
                    }
                }
                Spacer(Modifier.height(dimensions.spacingSm))
                AppButton("查看全部路线", { navigator.navigate(AppRoute.LocalRoutes) }, variant = AppButtonVariant.SECONDARY)
                Spacer(Modifier.height(dimensions.spacingSm))
                AppButton("想去的地方", { navigator.navigate(AppRoute.Favorites) }, variant = AppButtonVariant.TEXT)
                Spacer(Modifier.height(dimensions.spacingXl))
                AppSectionTitle("漫游记忆")
                Spacer(Modifier.height(dimensions.spacingSm))
                recentRoaming?.let { record ->
                    AppCard(Modifier.clickable { navigator.navigate(AppRoute.RoamingHistory(record.id)) }) {
                        findRoamingMemoryCover(publishedCapsules, record.id)?.let { path ->
                            CapsulePhoto(
                                path = path,
                                description = "${record.routeName ?: "本次漫游"}的城市碎片照片",
                                compact = true,
                                thumbnailCapability = thumbnailCapability
                            )
                            Spacer(Modifier.height(dimensions.spacingSm))
                        }
                        AppCaptionText("上一次漫游 · ${dateFormatter.format(record.startedAtEpochMs)}")
                        Spacer(Modifier.height(dimensions.spacingXs))
                        AppSectionTitle(record.routeName ?: if (record.mode == RoamingMode.FREE) "自由漫游" else "按路线漫游")
                        Spacer(Modifier.height(dimensions.spacingXs))
                        AppSecondaryText("${record.visits.size} 个到达地点 · ${record.distanceMeters?.let { com.y.citycapsule.core.location.GeoDistance.label(it) } ?: "距离未记录"}")
                    }
                    Spacer(Modifier.height(dimensions.spacingSm))
                }
                AppButton(if (recentRoaming == null) "还没有漫游回顾" else "查看全部漫游回顾", { navigator.navigate(AppRoute.RoamingHistory()) }, variant = AppButtonVariant.TEXT)
            }
        }
    }
}

internal fun selectSuggestedRoute(
    routes: List<LocalRoute>,
    favoriteIds: Set<String>,
    availablePlaceIds: Set<String>
): LocalRoute? = routes
    .filter { route -> route.orderedPlaceIds.any(availablePlaceIds::contains) }
    .sortedWith(
        compareByDescending<LocalRoute> { route -> route.orderedPlaceIds.count(favoriteIds::contains) }
            .thenByDescending(LocalRoute::createdAtEpochMs)
    )
    .firstOrNull()

private fun distanceLabel(meters: Long): String = if (meters < 1000L) "${meters} 米" else "${meters / 100 / 10.0} 公里"

private fun durationLabel(seconds: Long): String = if (seconds < 3600L) "${(seconds / 60).coerceAtLeast(1)} 分钟" else "${seconds / 3600} 小时 ${seconds % 3600 / 60} 分钟"

internal fun findRoamingMemoryCover(capsules: List<CityCapsule>, roamingSessionId: String): String? = capsules
    .asSequence()
    .filter { it.roamingSessionId == roamingSessionId && it.imagePaths.isNotEmpty() }
    .sortedByDescending(CityCapsule::createdAtEpochMs)
    .mapNotNull { it.imagePaths.firstOrNull() }
    .firstOrNull()
