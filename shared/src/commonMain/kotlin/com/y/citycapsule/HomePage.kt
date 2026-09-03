package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.material3.Text
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.city.ExploreCityRepository
import com.y.citycapsule.core.city.ExploreCityRuntime
import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.city.CityRegistry
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.location.CurrentLocationRuntime
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlacePhotoCacheRepository
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.media.ImageLoadPriority
import com.y.citycapsule.core.media.PlaceImageLoadRuntime
import com.y.citycapsule.core.map.AmapNativeView
import com.y.citycapsule.core.map.ExploreMapViewState
import com.y.citycapsule.core.map.MapCameraModel
import com.y.citycapsule.core.map.MapMarkerModel
import com.y.citycapsule.core.map.MapViewEvent
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppBottomSheet
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppChoiceChip
import com.y.citycapsule.designsystem.component.AppIcon
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppProfileAvatar
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.CapsuleCard
import com.y.citycapsule.designsystem.component.CapsuleCardModel
import com.y.citycapsule.designsystem.component.CapsuleCardVariant
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.PlaceCard
import com.y.citycapsule.designsystem.component.PlaceCardModel
import com.y.citycapsule.designsystem.component.PlaceCardVariant
import com.y.citycapsule.designsystem.component.PlaceFallbackKind
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.capsule.CapsuleFeatureRuntime
import com.y.citycapsule.feature.capsule.CapsulePhoto
import com.y.citycapsule.feature.home.HomeStateHolder
import com.y.citycapsule.feature.home.HomeUiState
import com.y.citycapsule.feature.home.HomeUiStatus
import com.y.citycapsule.feature.home.HomeOnlineStatus
import com.y.citycapsule.feature.home.HomeCityLookupStatus
import com.y.citycapsule.feature.place.PlaceFeatureRuntime
import com.y.citycapsule.feature.place.displayName
import com.y.citycapsule.feature.place.PlaceMedia
import com.y.citycapsule.feature.place.RemotePlaceSummaryCard
import com.y.citycapsule.core.map.MapPrivacyConsentRepository
import com.y.citycapsule.core.map.MapPrivacyConsentRuntime

/** Explore root content hosted inside the single AppShellPage. */
@Composable
internal fun HomeRootContent(
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    cityRepository: ExploreCityRepository,
    placeRepository: PlaceRepository,
    photoCacheRepository: PlacePhotoCacheRepository,
    remoteDataSource: PlaceRemoteDataSource,
    favoriteRepository: FavoriteRepository,
    capsuleRepository: CapsuleRepository,
    dateFormatter: CapsuleDateFormatter,
    mapConsentRepository: MapPrivacyConsentRepository,
    active: Boolean,
    statusBarHeight: Float,
    listState: LazyListState
) {
    var uiState by remember { mutableStateOf(HomeUiState()) }
    var showPlacePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    val holder = remember(profileRepository, cityRepository, placeRepository, favoriteRepository, capsuleRepository, photoCacheRepository, remoteDataSource, dateFormatter) {
        HomeStateHolder(
            profileRepository,
            cityRepository,
            placeRepository,
            favoriteRepository,
            capsuleRepository,
            dateFormatter,
            photoCacheRepository,
            remoteDataSource,
            onStateChanged = { uiState = it }
        )
    }
    DisposableEffect(holder) {
        onDispose(holder::dispose)
    }
    val placeRevision = PlaceFeatureRuntime.revision
    val capsuleRevision = CapsuleFeatureRuntime.revision
    val cityRevision = ExploreCityRuntime.revision
    val locationRevision = CurrentLocationRuntime.revision
    LaunchedEffect(holder, active, placeRevision, capsuleRevision, cityRevision, locationRevision) {
        if (active) holder.load()
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
                HomeProfileHeader(uiState, onCityClick = { showCityPicker = true })
                Spacer(Modifier.height(dimensions.spacingLg))
                AppSectionTitle("你好，${uiState.profile.displayName}")
                Spacer(Modifier.height(dimensions.spacingXxs))
                AppSecondaryText("今天想去哪里？也看看最近留在城市里的片段。")
                Spacer(Modifier.height(dimensions.spacingMd))
                HomeSearchEntry { navigator.navigate(AppRoute.PlaceList()) }
                uiState.notice?.let {
                    Spacer(Modifier.height(dimensions.spacingMd))
                    AppStatusMessage(it, tone = AppStatusTone.NEUTRAL)
                }
                Spacer(Modifier.height(dimensions.spacingXl))
                if (uiState.status == HomeUiStatus.LOADING) {
                    LoadingState("正在整理这座城里的地点与记忆…")
                } else {
                    HomeContent(
                        state = uiState,
                        navigator = navigator,
                        onToggleFavorite = holder::toggleFavorite,
                        onCachedPhotoFailed = holder::invalidateCachedPhoto,
                        onQuickRecord = { showPlacePicker = true },
                        mapConsentRepository = mapConsentRepository,
                        onRetry = holder::load
                    )
                }
            }
        }
    }

    HomePlacePicker(
        visible = showPlacePicker,
        state = uiState,
        onDismiss = { showPlacePicker = false },
        onSelect = {
            showPlacePicker = false
            navigator.navigate(AppRoute.CapsuleEditor(placeId = it.id))
        },
        onCreatePlace = {
            showPlacePicker = false
            navigator.navigate(AppRoute.PlaceEditor())
        }
    )
    HomeCityPicker(
        visible = showCityPicker,
        state = uiState,
        onDismiss = { showCityPicker = false },
        onSelect = { city ->
            showCityPicker = false
            holder.selectCity(city)
        },
        onSearch = holder::searchAndSelectCity
    )
}

@Composable
private fun HomeProfileHeader(state: HomeUiState, onCityClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(onClick = onCityClick)) {
            AppSectionTitle("${state.selectedCity.displayName}  ⌔")
            AppCaptionText("点击切换探索城市")
        }
        AppProfileAvatar(
            preset = state.profile.avatarPreset,
            managedPath = state.profile.avatarManagedPath
        )
    }
}

@Composable
private fun HomeSearchEntry(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
            .background(AppTheme.colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(AppTheme.dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(AppIconName.SEARCH, "搜索地点")
        Spacer(Modifier.weight(HOME_SEARCH_GAP_WEIGHT))
        AppSecondaryText("搜索地点、分类或区域")
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    navigator: AppNavigator,
    onToggleFavorite: (String) -> Unit,
    onCachedPhotoFailed: (String) -> Unit,
    onQuickRecord: () -> Unit,
    mapConsentRepository: MapPrivacyConsentRepository,
    onRetry: () -> Unit
) {
    val dimensions = AppTheme.dimensions
    var recommendationMapAccepted by remember { mutableStateOf(MapPrivacyConsentRuntime.accepted) }
    var showRecommendationMapPrompt by remember { mutableStateOf(false) }
    var selectedRecommendationId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mapConsentRepository) { mapConsentRepository.load { recommendationMapAccepted = it } }
    AppSectionTitle("今天想去哪里？")
    Spacer(Modifier.height(dimensions.spacingSm))
    val featured = state.featuredPlace
    if (state.catalogReadOnly) {
        ErrorState(
            "地点数据暂时无法安全读取，当前不会执行新建、想去或记录操作。",
            onRetry = onRetry
        )
    } else if (state.featuredPlaceWithMedia == null && state.onlineStatus == HomeOnlineStatus.LOADING) {
            LoadingState("正在发现${state.selectedCity.displayName}的地点…")
    } else if (state.featuredPlaceWithMedia == null && state.onlineRecommendations.isNotEmpty()) {
            AppSecondaryText("以下来自高德地点服务，只作在线候选，不会自动保存。")
            Spacer(Modifier.height(dimensions.spacingSm))
            state.onlineRecommendations.forEachIndexed { index, place ->
                RemotePlaceSummaryCard(
                    place = place,
                    imagePriority = if (index < PlaceImageLoadRuntime.INITIAL_VISIBLE_LIMIT) ImageLoadPriority.VISIBLE else ImageLoadPriority.PREFETCH,
                    variant = if (index == 0) PlaceCardVariant.HERO else PlaceCardVariant.COMPACT,
                    onOpen = { navigator.navigate(AppRoute.PlaceList()) }
                )
                Spacer(Modifier.height(dimensions.spacingXs))
            }
            AppButton("在探索中查看并保存", { navigator.navigate(AppRoute.PlaceList()) }, variant = AppButtonVariant.TEXT)
    } else if (featured == null) {
        if (!state.selectedCity.supported) {
            EmptyState(
                "${state.selectedCity.displayName}内容尚未开放",
                "当前不会用其他城市的地点冒充推荐，可以前往探索页切换城市。",
                actionLabel = "选择其他城市",
                onAction = { navigator.navigate(AppRoute.PlaceList()) }
            )
        } else {
            EmptyState(
                "还没有可探索的地点",
                "新建一个真实地点，再从这里开始记录城市。",
                actionLabel = "新建地点",
                onAction = { navigator.navigate(AppRoute.PlaceEditor()) }
            )
        }
    } else {
        HomePlaceCard(
            featured,
            featured.id in state.favoriteIds,
            state.busyFavoriteId == null,
            PlaceCardVariant.HERO,
            photo = state.photoByPlaceId[featured.id],
            onCachedPhotoFailed = { onCachedPhotoFailed(featured.id) },
            onOpen = { navigator.navigate(AppRoute.PlaceDetail(featured.id)) },
            onToggleFavorite = { onToggleFavorite(featured.id) }
        )
    }

    if (state.categories.isNotEmpty()) {
        Spacer(Modifier.height(dimensions.spacingXl))
        AppSectionTitle("换一种逛法")
        Spacer(Modifier.height(dimensions.spacingSm))
        state.categories.take(HOME_CATEGORY_LIMIT).chunked(HOME_CATEGORY_COLUMNS).forEach { categories ->
            Row(Modifier.fillMaxWidth()) {
                categories.forEachIndexed { index, category ->
                    AppChoiceChip(
                        category.displayName(),
                        selected = false,
                        onClick = { navigator.navigate(AppRoute.PlaceList(category)) },
                        modifier = Modifier.weight(1f)
                    )
                    if (index < categories.lastIndex) Spacer(Modifier.weight(HOME_CATEGORY_GAP_WEIGHT))
                }
                if (categories.size < HOME_CATEGORY_COLUMNS) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(dimensions.spacingXs))
        }
    }

    if (state.supportingPlaces.isNotEmpty()) {
        Spacer(Modifier.height(dimensions.spacingLg))
        AppSectionTitle(state.supportingTitle)
        Spacer(Modifier.height(dimensions.spacingSm))
        state.supportingPlaces.forEach { place ->
            HomePlaceCard(
                place,
                place.id in state.favoriteIds,
                state.busyFavoriteId == null,
                PlaceCardVariant.COMPACT,
                photo = state.photoByPlaceId[place.id],
                onCachedPhotoFailed = { onCachedPhotoFailed(place.id) },
                onOpen = { navigator.navigate(AppRoute.PlaceDetail(place.id)) },
                onToggleFavorite = { onToggleFavorite(place.id) }
            )
            Spacer(Modifier.height(dimensions.spacingXs))
        }
        AppButton(
            if (state.favoriteIds.isNotEmpty()) "查看全部想去" else "查看全部地点",
            onClick = {
                navigator.navigate(if (state.favoriteIds.isNotEmpty()) AppRoute.Favorites else AppRoute.PlaceList())
            },
            variant = AppButtonVariant.TEXT
        )
    }

    val mapRecommendations = (listOfNotNull(state.featuredPlace) + state.supportingPlaces)
        .distinctBy(Place::id)
        .filter { it.geoPoint != null }
        .take(HOME_MAP_RECOMMENDATION_LIMIT)
    if (mapRecommendations.isNotEmpty()) {
        Spacer(Modifier.height(dimensions.spacingXl))
        AppSectionTitle("在地图上发现")
        Spacer(Modifier.height(dimensions.spacingXxs))
        AppSecondaryText("地图标出本次推荐地点；下方用类别 Emoji 展示，最多 5 个。")
        Spacer(Modifier.height(dimensions.spacingSm))
        if (recommendationMapAccepted) {
            AmapNativeView(
                state = ExploreMapViewState(
                    markers = mapRecommendations.map { place -> MapMarkerModel(place.id, place.name, place.geoPoint!!) },
                    selectedPlaceId = selectedRecommendationId,
                    camera = mapRecommendations.first().geoPoint?.let { MapCameraModel(it, 12.5) }
                ),
                privacyAccepted = true,
                onEvent = { event -> if (event is MapViewEvent.MarkerSelected) selectedRecommendationId = event.placeId },
                modifier = Modifier.fillMaxWidth().height(dimensions.mapViewportHeight)
            )
            Spacer(Modifier.height(dimensions.spacingSm))
            mapRecommendations.forEach { place ->
                AppChoiceChip(
                    text = "${place.category.recommendationEmoji()}  ${place.name}",
                    selected = place.id == selectedRecommendationId,
                    onClick = { selectedRecommendationId = place.id }
                )
                Spacer(Modifier.height(dimensions.spacingXs))
            }
            val selectedRecommendation = mapRecommendations.firstOrNull { it.id == selectedRecommendationId }
                ?: mapRecommendations.first()
            AppButton(
                text = "查看 ${selectedRecommendation.name}",
                onClick = { navigator.navigate(AppRoute.PlaceDetail(selectedRecommendation.id)) },
                variant = AppButtonVariant.TEXT
            )
        } else {
            AppButton("打开推荐地图", { showRecommendationMapPrompt = true }, variant = AppButtonVariant.SECONDARY)
        }
    }

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("规划下一次探索")
    Spacer(Modifier.height(dimensions.spacingXxs))
    AppSecondaryText("挑选地点并手动安排顺序；真实步行路线接通前，不绘制可能穿越湖面或建筑的直线。")
    Spacer(Modifier.height(dimensions.spacingSm))
    AppButton("我的路线", { navigator.navigate(AppRoute.LocalRoutes) }, variant = AppButtonVariant.SECONDARY)

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("最近留下的城市记忆")
    Spacer(Modifier.height(dimensions.spacingSm))
    if (state.recentMemories.isEmpty()) {
        AppSecondaryText("还没有城市碎片。选一个真实地点，留下第一条城市记忆。")
    } else {
        state.recentMemories.forEach { memory ->
            CapsuleCard(
                CapsuleCardModel(
                    memory.dateLabel,
                    memory.place?.name ?: "曾经到访的地点",
                    memory.capsule.content,
                    memory.capsule.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" }
                ),
                onOpen = { navigator.navigate(AppRoute.CapsuleDetail(memory.capsule.id)) },
                variant = CapsuleCardVariant.RECENT,
                media = memory.capsule.imagePaths.firstOrNull()?.let { path ->
                    { CapsulePhoto(path, "${memory.place?.name ?: "城市"}的记忆照片") }
                }
            )
            Spacer(Modifier.height(dimensions.spacingSm))
        }
    }

    if (!state.catalogReadOnly) {
        Spacer(Modifier.height(dimensions.spacingLg))
        AppSectionTitle("留住这一刻")
        Spacer(Modifier.height(dimensions.spacingXxs))
        AppBodyText("先选择所在地点，再写下照片、心情和文字。")
        Spacer(Modifier.height(dimensions.spacingSm))
        AppButton("选择地点并快速记录", onQuickRecord, variant = AppButtonVariant.SECONDARY)
    }
    if (showRecommendationMapPrompt) {
        AppConfirmDialog(
            title = "显示推荐地图",
            message = "地图由高德地图 SDK 提供，启用后会联网加载底图。这里只展示本地规则选出的真实地点，不使用个性化算法。",
            confirmText = "同意并打开地图",
            onConfirm = { mapConsentRepository.accept(); recommendationMapAccepted = true; showRecommendationMapPrompt = false },
            onDismiss = { showRecommendationMapPrompt = false }
        )
    }
}

@Composable
private fun HomeCityPicker(
    visible: Boolean,
    state: HomeUiState,
    onDismiss: () -> Unit,
    onSelect: (CityDefinition) -> Unit,
    onSearch: (String) -> Unit
) {
    var cityQuery by remember { mutableStateOf("") }
    AppBottomSheet(visible, "选择探索城市", onDismiss) {
        AppTextField(
            value = cityQuery,
            onValueChange = { cityQuery = it },
            label = "查找其他城市",
            placeholder = "例如：成都、广州、南京",
            supportingText = "根据真实在线地点确认城市，不会创建虚构地点。",
            errorMessage = state.cityLookupMessage,
            maxLength = 20,
            enabled = state.cityLookupStatus != HomeCityLookupStatus.LOADING
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppButton(
            text = if (state.cityLookupStatus == HomeCityLookupStatus.LOADING) "正在查找…" else "切换到该城市",
            onClick = { onSearch(cityQuery) },
            variant = AppButtonVariant.SECONDARY,
            enabled = cityQuery.trim().length >= 2 && state.cityLookupStatus != HomeCityLookupStatus.LOADING,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        (listOf(state.selectedCity) + CityRegistry.cities).distinctBy(CityDefinition::id).forEach { city ->
            val selected = city.id == state.selectedCity.id
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onSelect(city) }
                    .padding(vertical = AppTheme.dimensions.spacingMd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selected) "✓ ${city.displayName}" else city.displayName,
                    color = if (selected) AppTheme.colors.primary else AppTheme.colors.textPrimary,
                    style = AppTheme.typography.body.copy(
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (!city.supported) AppCaptionText("在线获取")
            }
        }
    }
}

@Composable
private fun HomePlaceCard(
    place: Place,
    favorite: Boolean,
    favoriteEnabled: Boolean,
    variant: PlaceCardVariant,
    photo: com.y.citycapsule.core.place.PlacePhotoCacheEntry? = null,
    onCachedPhotoFailed: () -> Unit = {},
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    PlaceCard(
        PlaceCardModel(
            place.name,
            listOfNotNull(place.category.displayName(), place.district, place.city).joinToString(" · "),
            place.description ?: place.address,
            favorite,
            place.category.toFallbackKind()
        ),
        onOpen,
        onToggleFavorite,
        variant = variant,
        favoriteEnabled = favoriteEnabled,
        media = {
            PlaceMedia(place, photo, onCachedPhotoFailed = onCachedPhotoFailed)
        }
    )
}

@Composable
private fun HomePlacePicker(
    visible: Boolean,
    state: HomeUiState,
    onDismiss: () -> Unit,
    onSelect: (Place) -> Unit,
    onCreatePlace: () -> Unit
) {
    AppBottomSheet(visible, "在哪里留下这一刻？", onDismiss) {
        if (state.catalogReadOnly) {
            ErrorState("地点数据暂时无法安全读取，请关闭后重试。")
        } else if (state.rankedPlaces.isEmpty()) {
            EmptyState("还没有地点", "先新建真实地点，再开始记录。", actionLabel = "新建地点", onAction = onCreatePlace)
        } else {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = AppTheme.dimensions.placeHeroHeight)) {
                items(state.rankedPlaces.size) { index ->
                    val place = state.rankedPlaces[index]
                    HomePlaceCard(
                        place,
                        place.id in state.favoriteIds,
                        favoriteEnabled = false,
                        PlaceCardVariant.COMPACT,
                        onOpen = { onSelect(place) },
                        onToggleFavorite = {}
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                }
            }
        }
    }
}

private fun PlaceCategory.toFallbackKind(): PlaceFallbackKind = when (this) {
    PlaceCategory.LANDMARK, PlaceCategory.HISTORIC_SITE, PlaceCategory.CHURCH, PlaceCategory.TEMPLE -> PlaceFallbackKind.LANDMARK
    PlaceCategory.MUSEUM, PlaceCategory.ART_SPACE, PlaceCategory.ENTERTAINMENT, PlaceCategory.CULTURE -> PlaceFallbackKind.CULTURE
    PlaceCategory.COFFEE, PlaceCategory.RESTAURANT, PlaceCategory.DESSERT, PlaceCategory.FOOD -> PlaceFallbackKind.FOOD
    PlaceCategory.PARK, PlaceCategory.NATURAL_SCENERY, PlaceCategory.WATERFRONT, PlaceCategory.NATURE -> PlaceFallbackKind.NATURE
    PlaceCategory.SHOPPING, PlaceCategory.MARKET, PlaceCategory.NEIGHBORHOOD -> PlaceFallbackKind.SHOPPING
    PlaceCategory.OTHER -> PlaceFallbackKind.OTHER
}

private fun PlaceCategory.recommendationEmoji(): String = emoji

private const val HOME_CATEGORY_LIMIT = 6
private const val HOME_MAP_RECOMMENDATION_LIMIT = 5
private const val HOME_CATEGORY_COLUMNS = 2
private const val HOME_CATEGORY_GAP_WEIGHT = 0.08f
private const val HOME_SEARCH_GAP_WEIGHT = 0.4f
