package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.LazyListScope
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.navigation.AppRootTab
import com.y.citycapsule.app.navigation.backToRoot
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.city.CityRegistry
import com.y.citycapsule.core.city.LocalExploreCityRepository
import com.y.citycapsule.core.city.SupportedCityReverseGeocoder
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.location.KuiklyLocationCapability
import com.y.citycapsule.core.location.LocationCapability
import com.y.citycapsule.core.map.AmapNativeView
import com.y.citycapsule.core.map.MapPrivacyConsentRepository
import com.y.citycapsule.core.media.ImageLoadPriority
import com.y.citycapsule.core.media.PlaceImageLoadRuntime
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.LocalPlacePhotoCacheRepository
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.place.AmapReverseGeocodeCapability
import com.y.citycapsule.core.place.AmapPlaceRemoteDataSource
import com.y.citycapsule.core.place.CachingPlaceRemoteDataSource
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.FallbackReverseGeocodeCapability
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppBottomSheet
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppChoiceChip
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppDivider
import com.y.citycapsule.designsystem.component.AppFilterChip
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppMenuItem
import com.y.citycapsule.designsystem.component.AppOverflowMenu
import com.y.citycapsule.designsystem.component.AppFixedHeaderLazyScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.component.SearchField
import com.y.citycapsule.designsystem.theme.AppTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Page(AppRouteTable.PAGE_PLACE_LIST, supportInLocal = true)
internal class PlaceListPager : BasePager() {
    override fun willInit() {
        super.willInit()
        installPlaceList(
            PlaceListMode.ALL,
            PlaceCategory.fromWireValue(
                pageData.params.optString(AppRouteTable.PARAM_INITIAL_CATEGORY)
            )
        )
    }

    private fun installPlaceList(
        mode: PlaceListMode,
        initialCategory: PlaceCategory? = null
    ) {
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val placeRepository = LocalPlaceRepository(storage)
        val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
        val photoCacheRepository = LocalPlacePhotoCacheRepository(storage)
        val cityRepository = LocalExploreCityRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = mode,
                navigator = navigator,
                cityRepository = cityRepository,
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
                photoCacheRepository = photoCacheRepository,
                locationCapability = KuiklyLocationCapability(this),
                reverseGeocodeCapability = FallbackReverseGeocodeCapability(
                    AmapReverseGeocodeCapability(this), SupportedCityReverseGeocoder
                ),
                remoteDataSource = CachingPlaceRemoteDataSource(AmapPlaceRemoteDataSource(this)),
                mapConsentRepository = MapPrivacyConsentRepository(storage),
                themeHost = themeHost,
                initialCategory = initialCategory
            )
        }
    }
}

@Page(AppRouteTable.PAGE_FAVORITES, supportInLocal = true)
internal class FavoritesPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val placeRepository = LocalPlaceRepository(storage)
        val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
        val photoCacheRepository = LocalPlacePhotoCacheRepository(storage)
        val cityRepository = LocalExploreCityRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = PlaceListMode.FAVORITES,
                navigator = navigator,
                cityRepository = cityRepository,
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
                photoCacheRepository = photoCacheRepository,
                locationCapability = KuiklyLocationCapability(this),
                reverseGeocodeCapability = FallbackReverseGeocodeCapability(
                    AmapReverseGeocodeCapability(this), SupportedCityReverseGeocoder
                ),
                remoteDataSource = CachingPlaceRemoteDataSource(AmapPlaceRemoteDataSource(this)),
                mapConsentRepository = MapPrivacyConsentRepository(storage),
                themeHost = themeHost
            )
        }
    }
}

@Composable
private fun PlaceListScreen(
    mode: PlaceListMode,
    navigator: AppNavigator,
    cityRepository: LocalExploreCityRepository,
    placeRepository: LocalPlaceRepository,
    favoriteRepository: LocalFavoriteRepository,
    photoCacheRepository: LocalPlacePhotoCacheRepository,
    locationCapability: LocationCapability,
    reverseGeocodeCapability: com.y.citycapsule.core.city.ReverseGeocodeCapability,
    remoteDataSource: PlaceRemoteDataSource,
    mapConsentRepository: MapPrivacyConsentRepository,
    themeHost: AppThemeHost,
    initialCategory: PlaceCategory? = null
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    val storeScope = rememberCoroutineScope()
    val invalidationOwner = remember { PlaceFeatureRuntime.newOwnerToken() }
    val store = remember(
        cityRepository,
        placeRepository,
        favoriteRepository,
        photoCacheRepository,
        locationCapability,
        reverseGeocodeCapability,
        remoteDataSource,
        mapConsentRepository,
        mode,
        initialCategory
    ) {
        PlaceListStore(
            cityRepository = cityRepository,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            photoCacheRepository = photoCacheRepository,
            locationCapability = locationCapability,
            reverseGeocodeCapability = reverseGeocodeCapability,
            remoteDataSource = remoteDataSource,
            mapConsentRepository = mapConsentRepository,
            parentScope = storeScope,
            mode = mode,
            initialCategory = initialCategory
        )
    }
    val uiState by store.state.collectAsState()
    val catalogRevision = PlaceFeatureRuntime.revision
    var showFilters by remember { mutableStateOf(false) }
    var showCities by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedPlaceId by remember { mutableStateOf<String?>(null) }
    val contentListState = rememberLazyListState()
    val expanded = LocalConfiguration.current.pageViewWidth.dp >=
        AppTheme.dimensions.adaptiveGridBreakpoint

    DisposableEffect(store) {
        onDispose(store::dispose)
    }

    LaunchedEffect(store, catalogRevision) {
        if (PlaceFeatureRuntime.shouldReload(invalidationOwner)) {
            store.dispatch(PlaceListIntent.Load)
        }
    }

    LaunchedEffect(store, navigator) {
        store.effects.collect { effect ->
            when (effect) {
                is PlaceListEffect.NavigateToDetail -> navigator.navigate(
                    AppRoute.PlaceDetail(effect.placeId)
                )
                PlaceListEffect.NavigateToEditor -> navigator.navigate(
                    AppRoute.PlaceEditor()
                )
                PlaceListEffect.NavigateBack -> navigator.back()
                PlaceListEffect.BackToExplore -> navigator.backToRoot(AppRootTab.EXPLORE)
                PlaceListEffect.FavoritesChanged ->
                    PlaceFeatureRuntime.invalidateFrom(invalidationOwner)
            }
        }
    }

    val onlinePrefetchKeys = remember(uiState.onlinePlaces) {
        uiState.onlinePlaces.takeLast(ONLINE_PLACE_PREFETCH_DISTANCE).mapTo(mutableSetOf()) {
            onlinePlaceItemKey(it.providerId)
        }
    }
    LaunchedEffect(
        contentListState,
        onlinePrefetchKeys,
        uiState.onlineHasMore,
        uiState.onlineLoadingMore,
        uiState.onlineLoadMoreFailed,
        uiState.onlineStatus
    ) {
        if (
            uiState.onlineStatus != OnlinePlaceStatus.RESULTS ||
            !uiState.onlineHasMore ||
            uiState.onlineLoadingMore ||
            uiState.onlineLoadMoreFailed ||
            onlinePrefetchKeys.isEmpty()
        ) return@LaunchedEffect
        snapshotFlow { contentListState.layoutInfo.visibleItemsInfo.lastOrNull()?.key }
            .distinctUntilChanged()
            .collect { lastVisibleKey ->
                if (lastVisibleKey in onlinePrefetchKeys) {
                    store.dispatch(PlaceListIntent.OnlineNextPageRequested)
                }
            }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppFixedHeaderLazyScaffold(
            statusBarHeight = statusBarHeight,
            contentMaxWidth = AppTheme.dimensions.adaptiveContentMaxWidth,
            contentListState = contentListState,
            header = {
                AppActionTopBar(
                title = if (mode == PlaceListMode.FAVORITES) {
                    "想去的地方"
                } else if (uiState.browseAllCities) {
                    "全部城市"
                } else {
                    uiState.selectedCity.displayName
                },
                onLeadingClick = { store.dispatch(PlaceListIntent.BackClicked) },
                subtitle = if (mode == PlaceListMode.ALL) "探索地点" else null,
                onTitleClick = if (mode == PlaceListMode.ALL && uiState.status == PlaceListUiStatus.READY) {
                    { showCities = true }
                } else null,
                actionLabel = if (mode == PlaceListMode.FAVORITES && uiState.favoriteIds.isNotEmpty()) "规划漫游" else null,
                actionIcon = if (mode == PlaceListMode.ALL) AppIconName.MORE else null,
                actionDescription = if (mode == PlaceListMode.FAVORITES) "从想去地点规划漫游" else "更多操作",
                onActionClick = when {
                    mode == PlaceListMode.ALL -> { { showMenu = true } }
                    mode == PlaceListMode.FAVORITES && uiState.favoriteIds.isNotEmpty() -> { { navigator.navigate(AppRoute.LocalRouteEditor()) } }
                    else -> null
                }
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                SearchField(
                value = uiState.query,
                onValueChange = {
                    store.dispatch(PlaceListIntent.QueryChanged(it))
                },
                placeholder = if (mode == PlaceListMode.FAVORITES) {
                    "搜索想去地点"
                } else {
                    "搜索地点、分类或区域"
                },
                enabled = uiState.status == PlaceListUiStatus.READY
                )
                if (mode == PlaceListMode.ALL) {
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                    AppButton(
                        text = when {
                            uiState.query.isNotBlank() -> "在线搜索“${uiState.query}”"
                            uiState.currentLocation != null -> "在线发现附近地点"
                            else -> "输入关键词或先获取位置"
                        },
                        onClick = { store.dispatch(PlaceListIntent.OnlineSearchRequested) },
                        variant = AppButtonVariant.TEXT,
                        enabled = uiState.status == PlaceListUiStatus.READY &&
                            uiState.onlineStatus != OnlinePlaceStatus.LOADING &&
                            (uiState.query.isNotBlank() || uiState.currentLocation != null)
                    )
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                CategoryChips(uiState, store::dispatch)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            }
        ) {
            placeDirectoryItems(
                state = uiState,
                dispatch = store::dispatch,
                mode = mode,
                expanded = expanded,
                selectedPlaceId = selectedPlaceId,
                onFilterClick = { showFilters = true },
                onPlaceSelected = { id ->
                    if (expanded) selectedPlaceId = id
                    else store.dispatch(PlaceListIntent.PlaceClicked(id))
                }
            )
        }

        if (uiState.showMapPrivacyPrompt) {
            AppConfirmDialog(
                title = "启用地图服务",
                message = "地图由高德地图 SDK 提供。启用后会联网加载地图；只有在你主动请求定位时才会申请位置权限。",
                confirmText = "同意并打开地图",
                confirmVariant = AppButtonVariant.PRIMARY,
                dismissText = "继续使用列表",
                onConfirm = { store.dispatch(PlaceListIntent.MapPrivacyAccepted) },
                onDismiss = { store.dispatch(PlaceListIntent.MapPrivacyDeclined) }
            )
        }

        AppBottomSheet(
            visible = showFilters,
            title = "筛选地点",
            onDismiss = { showFilters = false },
            dismissLabel = "完成"
        ) {
            AdvancedFilters(uiState, store::dispatch)
        }

        AppBottomSheet(
            visible = showCities,
            title = "选择探索城市",
            onDismiss = { showCities = false },
            dismissLabel = "取消"
        ) {
            CityPickerContent(
                state = uiState,
                onCitySelected = { cityId ->
                    showCities = false
                    store.dispatch(PlaceListIntent.ExploreCitySelected(cityId))
                },
                onAllCitiesSelected = {
                    showCities = false
                    store.dispatch(PlaceListIntent.AllCitiesSelected)
                },
                onUseCurrentLocation = {
                    showCities = false
                    store.dispatch(PlaceListIntent.CurrentLocationRequested)
                }
            )
        }

        uiState.detectedCity?.let { city ->
            AppConfirmDialog(
                title = "切换到${city.displayName}？",
                message = "定位结果只会更新当前探索城市，不会修改你的档案城市。",
                confirmText = "切换城市",
                confirmVariant = AppButtonVariant.PRIMARY,
                dismissText = "暂不切换",
                onConfirm = { store.dispatch(PlaceListIntent.DetectedCityConfirmed) },
                onDismiss = { store.dispatch(PlaceListIntent.DetectedCityDismissed) }
            )
        }

        AppOverflowMenu(
            expanded = showMenu,
            items = listOf(
                AppMenuItem(
                    id = MENU_CREATE_PLACE,
                    label = "新建地点",
                    enabled = !uiState.readOnly && uiState.status == PlaceListUiStatus.READY
                )
            ),
            onSelected = { id ->
                showMenu = false
                if (id == MENU_CREATE_PLACE) {
                    store.dispatch(PlaceListIntent.CreatePlaceClicked)
                }
            },
            onDismiss = { showMenu = false }
        )
    }
}

private fun LazyListScope.placeDirectoryItems(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit,
    mode: PlaceListMode,
    expanded: Boolean,
    selectedPlaceId: String?,
    onFilterClick: () -> Unit,
    onPlaceSelected: (String) -> Unit
) {
    item(key = "place:controls") {
        Column(Modifier.fillMaxWidth()) {
            state.notice?.let { notice ->
                AppStatusMessage(notice.message, tone = notice.tone.toAppStatusTone())
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
            }
            ResultHeader(state, onFilterClick)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            DirectoryViewSelector(state, dispatch)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            LocationControl(state, dispatch)
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        }
    }

    if (state.viewMode == PlaceDirectoryViewMode.MAP) {
        item(key = "place:map") { PlaceMapContent(state, dispatch) }
        return
    }

    if (state.contentState == PlaceListContentState.RESULTS && !expanded) {
        items(
            count = state.visiblePlaces.size,
            key = { index -> localPlaceItemKey(state.visiblePlaces[index].id) }
        ) { index ->
            val place = state.visiblePlaces[index]
            PlaceSummaryCard(
                place = place,
                favorite = place.id in state.favoriteIds,
                favoriteEnabled = !state.readOnly && state.busyFavoriteId == null,
                distanceLabel = state.distanceLabel(place),
                photo = state.photoByPlaceId[place.id],
                imagePriority = if (index < PlaceImageLoadRuntime.INITIAL_VISIBLE_LIMIT) ImageLoadPriority.VISIBLE else ImageLoadPriority.PREFETCH,
                onCachedPhotoFailed = { dispatch(PlaceListIntent.CachedPhotoFailed(place.id)) },
                onOpen = { onPlaceSelected(place.id) },
                onToggleFavorite = { dispatch(PlaceListIntent.FavoriteToggled(place.id)) }
            )
            if (index < state.visiblePlaces.lastIndex) AppDivider()
        }
    } else {
        item(key = "place:local-content") {
            PlaceListContent(state, dispatch, expanded, selectedPlaceId, onPlaceSelected)
        }
    }

    if (mode != PlaceListMode.ALL || state.onlineStatus == OnlinePlaceStatus.IDLE) return
    item(key = "online:header") {
        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            AppSectionTitle("在线发现 · ${state.onlinePlaces.size} 个")
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        }
    }
    when (state.onlineStatus) {
        OnlinePlaceStatus.LOADING -> item(key = "online:loading") {
            LoadingState("正在搜索${state.selectedCity.displayName}的地点…")
        }
        OnlinePlaceStatus.EMPTY -> item(key = "online:empty") {
            EmptyState("没有找到在线地点", "可以更换关键词或城市后重试。")
        }
        OnlinePlaceStatus.ERROR,
        OnlinePlaceStatus.UNAVAILABLE -> item(key = "online:error") {
            ErrorState("本地点目录仍可离线浏览。") { dispatch(PlaceListIntent.OnlineSearchRequested) }
        }
        OnlinePlaceStatus.RESULTS -> {
            items(
                count = state.onlinePlaces.size,
                key = { index -> onlinePlaceItemKey(state.onlinePlaces[index].providerId) }
            ) { index ->
                val place = state.onlinePlaces[index]
                RemotePlaceSummaryCard(
                    place = place,
                    imagePriority = if (index < PlaceImageLoadRuntime.INITIAL_VISIBLE_LIMIT) ImageLoadPriority.VISIBLE else ImageLoadPriority.PREFETCH,
                    onOpen = { dispatch(PlaceListIntent.RemotePlaceImportRequested(place.providerId)) }
                )
                AppButton(
                    text = if (state.importingProviderId == place.providerId) "保存中…" else "保存到本地",
                    onClick = { dispatch(PlaceListIntent.RemotePlaceImportRequested(place.providerId)) },
                    variant = AppButtonVariant.TEXT,
                    enabled = state.importingProviderId == null && !state.readOnly
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            }
            item(key = "online:footer") { OnlinePaginationFooter(state, dispatch) }
        }
        OnlinePlaceStatus.IDLE -> Unit
    }
}

@Composable
private fun OnlinePaginationFooter(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(AppTheme.dimensions.minTouchTarget),
        contentAlignment = Alignment.Center
    ) {
        if (state.onlineLoadMoreFailed) {
            AppButton(
                text = "加载失败，点击重试",
                onClick = { dispatch(PlaceListIntent.OnlineNextPageRequested) },
                variant = AppButtonVariant.TEXT
            )
        } else {
            AppSecondaryText(
                when {
                    state.onlineLoadingMore -> "正在加载更多地点…"
                    state.onlineHasMore -> "继续滑动加载更多"
                    else -> "已经看完本次在线发现"
                }
            )
        }
    }
}

internal fun localPlaceItemKey(placeId: String): String = "local:$placeId"
internal fun onlinePlaceItemKey(providerId: String): String = "online:$providerId"
internal const val ONLINE_PLACE_PREFETCH_DISTANCE = 3

@Composable
private fun CityPickerContent(
    state: PlaceListUiState,
    onCitySelected: (String) -> Unit,
    onAllCitiesSelected: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val recent = state.recentCityIds.mapNotNull(CityRegistry::byId)
    val ordered = (listOf(state.selectedCity) + recent + CityRegistry.cities).distinctBy { it.id }
    ordered.forEach { city ->
        val selected = !state.browseAllCities && city.id == state.selectedCity.id
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCitySelected(city.id) }
                .padding(
                    horizontal = AppTheme.dimensions.spacingMd,
                    vertical = AppTheme.dimensions.spacingSm
                ),
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
            if (!city.supported) {
                Text(
                    text = "在线地点",
                    color = AppTheme.colors.textSecondary,
                    style = AppTheme.typography.caption
                )
            }
        }
    }
    AppButton(
        text = "全部城市",
        onClick = onAllCitiesSelected,
        variant = AppButtonVariant.TEXT,
        modifier = Modifier.fillMaxWidth()
    )
    AppButton(
        text = "使用当前位置",
        onClick = onUseCurrentLocation,
        variant = AppButtonVariant.SECONDARY,
        modifier = Modifier.fillMaxWidth()
    )
    AppSecondaryText("定位失败、拒绝或超时时，仍可继续手动选择城市。")
}

@Composable
private fun DirectoryViewSelector(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        AppChoiceChip(
            text = "列表",
              selected = state.viewMode == PlaceDirectoryViewMode.LIST,
              onClick = { dispatch(PlaceListIntent.ListViewSelected) },
              modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(AppTheme.dimensions.spacingXs))
        AppChoiceChip(
            text = "地图",
              selected = state.viewMode == PlaceDirectoryViewMode.MAP,
              onClick = { dispatch(PlaceListIntent.MapViewSelected) },
              modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaceMapContent(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    AmapNativeView(
        state = state.mapViewState,
        privacyAccepted = state.mapPrivacyAccepted,
        onEvent = { dispatch(PlaceListIntent.MapEventReceived(it)) },
        modifier = Modifier.fillMaxWidth().height(AppTheme.dimensions.mapViewportHeight)
    )
    val selected = state.visiblePlaces.firstOrNull { it.id == state.selectedMapPlaceId }
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    if (selected == null) {
        AppSecondaryText(
            if (state.mapViewState.markers.isEmpty()) {
                "当前地点没有可显示的坐标，仍可切回列表浏览。"
            } else {
                "点击地图标记查看地点摘要。"
            }
        )
    } else {
        PlaceListDetailPane(
            place = selected,
            favorite = selected.id in state.favoriteIds,
            photo = state.photoByPlaceId[selected.id],
            onCachedPhotoFailed = { dispatch(PlaceListIntent.CachedPhotoFailed(selected.id)) },
            onOpen = { dispatch(PlaceListIntent.PlaceClicked(selected.id)) },
            onToggleFavorite = { dispatch(PlaceListIntent.FavoriteToggled(selected.id)) }
        )
    }
}

@Composable
private fun LocationControl(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            AppButton(
                text = when (state.locationStatus) {
                    PlaceLocationStatus.REQUESTING -> "定位中…"
                    PlaceLocationStatus.AVAILABLE -> "重新定位"
                    else -> "获取当前位置"
                },
                onClick = { dispatch(PlaceListIntent.CurrentLocationRequested) },
                variant = AppButtonVariant.TEXT,
                enabled = state.locationStatus != PlaceLocationStatus.REQUESTING
            )
        }
        state.locationMessage?.let { message ->
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppStatusMessage(
                message = message,
                tone = when (state.locationStatus) {
                    PlaceLocationStatus.AVAILABLE ->
                        com.y.citycapsule.designsystem.component.AppStatusTone.SUCCESS
                    PlaceLocationStatus.PERMISSION_DENIED,
                    PlaceLocationStatus.PERMISSION_PERMANENTLY_DENIED,
                    PlaceLocationStatus.SERVICE_DISABLED ->
                        com.y.citycapsule.designsystem.component.AppStatusTone.WARNING
                    else -> com.y.citycapsule.designsystem.component.AppStatusTone.ERROR
                }
            )
        }
    }
}

@Composable
private fun CategoryChips(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    LazyRow(modifier = Modifier.fillMaxWidth()) {
        item {
            AppFilterChip(
                text = "全部",
                selected = state.selectedTopic == null && state.filter.categories.isEmpty(),
                onClick = { dispatch(PlaceListIntent.TopicSelected(null)) },
                enabled = state.status == PlaceListUiStatus.READY
            )
        }
        ExplorePlaceTopic.entries.forEach { topic ->
            item {
                Spacer(Modifier.width(AppTheme.dimensions.spacingXs))
                AppFilterChip(
                    text = topic.label,
                    selected = topic == state.selectedTopic,
                    onClick = {
                        dispatch(PlaceListIntent.TopicSelected(topic.takeUnless { it == state.selectedTopic }))
                    },
                    enabled = state.status == PlaceListUiStatus.READY
                )
            }
        }
    }
}

@Composable
private fun ResultHeader(
    state: PlaceListUiState,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            AppSecondaryText(
                if (state.status == PlaceListUiStatus.LOADING) {
                    state.directoryContext
                } else {
                    "${state.visiblePlaces.size} 个地点 · ${state.directoryContext}"
                }
            )
        }
        Box(
            modifier = Modifier
                .clickable(
                    enabled = state.status == PlaceListUiStatus.READY,
                    onClick = onFilterClick
                )
                .padding(AppTheme.dimensions.spacingXs),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (state.activeAdvancedFilterCount > 0) {
                    "筛选 ${state.activeAdvancedFilterCount}"
                } else {
                    "筛选"
                },
                color = if (state.status == PlaceListUiStatus.READY) {
                    AppTheme.colors.primary
                } else {
                    AppTheme.colors.disabledContent
                },
                style = AppTheme.typography.button
            )
        }
    }
}

@Composable
private fun AdvancedFilters(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit
) {
    AppTextField(
        value = state.filter.city.orEmpty(),
        onValueChange = { dispatch(PlaceListIntent.CityChanged(it)) },
        label = "城市",
        placeholder = "例如：${state.selectedCity.displayName}",
        maxLength = PlaceValidator.CITY_MAX_LENGTH,
        enabled = state.status == PlaceListUiStatus.READY
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    AppTextField(
        value = state.filter.district.orEmpty(),
        onValueChange = { dispatch(PlaceListIntent.DistrictChanged(it)) },
        label = "区域",
        placeholder = "例如：徐汇区",
        maxLength = PlaceValidator.DISTRICT_MAX_LENGTH,
        enabled = state.status == PlaceListUiStatus.READY
    )
    if (state.mode == PlaceListMode.ALL) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppChoiceChip(
            text = "只看想去",
            selected = state.filter.favoritesOnly,
            onClick = { dispatch(PlaceListIntent.FavoritesOnlyToggled) },
            enabled = state.status == PlaceListUiStatus.READY
        )
    }
    if (state.activeAdvancedFilterCount > 0) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppButton(
            text = "清除高级筛选",
            onClick = { dispatch(PlaceListIntent.ClearAdvancedFilters) },
            variant = AppButtonVariant.TEXT
        )
    }
}

@Composable
private fun PlaceListContent(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit,
    expanded: Boolean,
    selectedPlaceId: String?,
    onPlaceSelected: (String) -> Unit
) {
    when (state.contentState) {
        PlaceListContentState.LOADING -> LoadingState("正在整理本地点目录…")
        PlaceListContentState.EMPTY_CATALOG -> EmptyState(
            title = if (!state.selectedCity.supported) "${state.selectedCity.displayName}内容尚未开放" else "还没有可探索的地点",
            message = if (!state.selectedCity.supported) {
                "这里不会显示其他城市的假推荐，请从顶部切换到已有内容的城市。"
            } else {
                "添加第一个地点后，就可以从这里开始探索。"
            },
            actionLabel = if (state.mode == PlaceListMode.ALL && state.selectedCity.supported) "新建地点" else null,
            onAction = if (state.mode == PlaceListMode.ALL && state.selectedCity.supported) {
                { dispatch(PlaceListIntent.CreatePlaceClicked) }
            } else {
                null
            }
        )
        PlaceListContentState.EMPTY_FAVORITES -> EmptyState(
            title = "还没有想去的地方",
            message = "探索地点时点亮心形，它们会留在这里。",
            actionLabel = "去探索地点",
            onAction = { dispatch(PlaceListIntent.ExploreClicked) }
        )
        PlaceListContentState.NO_MATCHES -> EmptyState(
            title = "没有找到匹配地点",
            message = "换个关键词，或者清除当前筛选后再看看。",
            actionLabel = "清除筛选",
            onAction = { dispatch(PlaceListIntent.ClearAllFilters) }
        )
        PlaceListContentState.STORAGE_ERROR -> ErrorState(
            message = "地点数据暂时无法安全读取，当前不会执行写操作。",
            onRetry = { dispatch(PlaceListIntent.Retry) }
        )
        PlaceListContentState.RESULTS -> {
            val selected = state.visiblePlaces.firstOrNull { it.id == selectedPlaceId }
                ?: state.visiblePlaces.firstOrNull()
            if (expanded) {
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.width(AppTheme.dimensions.adaptivePrimaryPaneWidth)) {
                        Column {
                            PlaceResults(state, dispatch, onPlaceSelected)
                        }
                    }
                    Spacer(Modifier.width(AppTheme.dimensions.adaptivePaneGap))
                    Box(Modifier.weight(1f)) {
                        PlaceListDetailPane(
                            place = selected,
                            favorite = selected?.id in state.favoriteIds,
                            photo = selected?.id?.let(state.photoByPlaceId::get),
                            onCachedPhotoFailed = {
                                selected?.let { dispatch(PlaceListIntent.CachedPhotoFailed(it.id)) }
                            },
                            onOpen = {
                                selected?.let {
                                    dispatch(PlaceListIntent.PlaceClicked(it.id))
                                }
                            },
                            onToggleFavorite = {
                                selected?.let {
                                    dispatch(PlaceListIntent.FavoriteToggled(it.id))
                                }
                            }
                        )
                    }
                }
            } else {
                PlaceResults(state, dispatch, onPlaceSelected)
            }
        }
    }
}

@Composable
private fun PlaceResults(
    state: PlaceListUiState,
    dispatch: (PlaceListIntent) -> Unit,
    onPlaceSelected: (String) -> Unit
) {
    state.visiblePlaces.forEachIndexed { index, place ->
        PlaceSummaryCard(
            place = place,
            favorite = place.id in state.favoriteIds,
            favoriteEnabled = !state.readOnly && state.busyFavoriteId == null,
            distanceLabel = state.distanceLabel(place),
            photo = state.photoByPlaceId[place.id],
            imagePriority = if (index < PlaceImageLoadRuntime.INITIAL_VISIBLE_LIMIT) {
                ImageLoadPriority.VISIBLE
            } else {
                ImageLoadPriority.PREFETCH
            },
            onCachedPhotoFailed = { dispatch(PlaceListIntent.CachedPhotoFailed(place.id)) },
            onOpen = { onPlaceSelected(place.id) },
            onToggleFavorite = { dispatch(PlaceListIntent.FavoriteToggled(place.id)) }
        )
        if (index < state.visiblePlaces.lastIndex) AppDivider()
    }
}

@Composable
private fun PlaceListDetailPane(
    place: com.y.citycapsule.core.place.Place?,
    favorite: Boolean,
    photo: com.y.citycapsule.core.place.PlacePhotoCacheEntry? = null,
    onCachedPhotoFailed: () -> Unit = {},
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    if (place == null) {
        EmptyState("选择一个地点", "从左侧列表选择地点，在这里查看地点信息。")
        return
    }
    Column {
        Box(
            Modifier.fillMaxWidth()
                .height(AppTheme.dimensions.placeHeroHeight)
                .clip(RoundedCornerShape(AppTheme.dimensions.radiusLg))
        ) {
            PlaceMedia(place, photo, onCachedPhotoFailed = onCachedPhotoFailed)
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppSectionTitle(place.name)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppSecondaryText(
            listOfNotNull(place.city, place.district, place.category.displayName())
                .joinToString(" · ")
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppBodyText(place.description ?: "这个地点暂时还没有补充介绍。")
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
        AppButton(
            text = if (favorite) "移出想去" else "加入想去",
            onClick = onToggleFavorite,
            variant = AppButtonVariant.SECONDARY
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppButton(text = "打开地点详情", onClick = onOpen)
    }
}

private const val MENU_CREATE_PLACE = "create_place"
