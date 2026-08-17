package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
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
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceValidator
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
import com.y.citycapsule.designsystem.component.AppFixedHeaderScaffold
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
        val cityRepository = LocalExploreCityRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = mode,
                navigator = navigator,
                cityRepository = cityRepository,
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
                locationCapability = KuiklyLocationCapability(this),
                reverseGeocodeCapability = SupportedCityReverseGeocoder,
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
        val cityRepository = LocalExploreCityRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = PlaceListMode.FAVORITES,
                navigator = navigator,
                cityRepository = cityRepository,
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
                locationCapability = KuiklyLocationCapability(this),
                reverseGeocodeCapability = SupportedCityReverseGeocoder,
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
    locationCapability: LocationCapability,
    reverseGeocodeCapability: com.y.citycapsule.core.city.ReverseGeocodeCapability,
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
        locationCapability,
        reverseGeocodeCapability,
        mode,
        initialCategory
    ) {
        PlaceListStore(
            cityRepository = cityRepository,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            locationCapability = locationCapability,
            reverseGeocodeCapability = reverseGeocodeCapability,
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

    RuntimeAppTheme(themeHost = themeHost) {
        AppFixedHeaderScaffold(
            statusBarHeight = statusBarHeight,
            contentMaxWidth = AppTheme.dimensions.adaptiveContentMaxWidth,
            header = {
                AppActionTopBar(
                title = if (mode == PlaceListMode.FAVORITES) "想去的地方" else "探索地点",
                onLeadingClick = { store.dispatch(PlaceListIntent.BackClicked) },
                actionIcon = if (mode == PlaceListMode.ALL) AppIconName.MORE else null,
                actionDescription = "更多操作",
                onActionClick = if (mode == PlaceListMode.ALL) {
                    { showMenu = true }
                } else {
                    null
                }
                )
                if (mode == PlaceListMode.ALL) {
                    AppButton(
                        text = if (uiState.browseAllCities) "全部城市" else uiState.selectedCity.displayName,
                        onClick = { showCities = true },
                        variant = AppButtonVariant.TEXT,
                        enabled = uiState.status == PlaceListUiStatus.READY
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                }
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
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                CategoryChips(uiState, store::dispatch)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            }
        ) {
            uiState.notice?.let { notice ->
                AppStatusMessage(notice.message, tone = notice.tone.toAppStatusTone())
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
            }
            ResultHeader(uiState, onFilterClick = { showFilters = true })
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            DirectoryViewSelector(uiState, store::dispatch)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            LocationControl(uiState, store::dispatch)
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            if (uiState.viewMode == PlaceDirectoryViewMode.MAP) {
                PlaceMapContent(uiState, store::dispatch)
            } else {
                PlaceListContent(
                    state = uiState,
                    dispatch = store::dispatch,
                    expanded = expanded,
                    selectedPlaceId = selectedPlaceId,
                    onPlaceSelected = { id ->
                        if (expanded) selectedPlaceId = id
                        else store.dispatch(PlaceListIntent.PlaceClicked(id))
                    }
                )
            }
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

@Composable
private fun CityPickerContent(
    state: PlaceListUiState,
    onCitySelected: (String) -> Unit,
    onAllCitiesSelected: () -> Unit,
    onUseCurrentLocation: () -> Unit
) {
    val recent = state.recentCityIds.mapNotNull(CityRegistry::byId)
    val ordered = (recent + CityRegistry.supportedCities).distinctBy { it.id }
    ordered.forEach { city ->
        AppButton(
            text = if (!state.browseAllCities && city.id == state.selectedCity.id) "${city.displayName} · 当前" else city.displayName,
            onClick = { onCitySelected(city.id) },
            variant = AppButtonVariant.TEXT,
            modifier = Modifier.fillMaxWidth()
        )
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
                selected = state.filter.categories.isEmpty(),
                onClick = { dispatch(PlaceListIntent.CategoriesCleared) },
                enabled = state.status == PlaceListUiStatus.READY
            )
        }
        PlaceCategory.entries.forEach { category ->
            item {
                Spacer(Modifier.width(AppTheme.dimensions.spacingXs))
                AppFilterChip(
                    text = category.displayName(),
                    selected = category in state.filter.categories,
                    onClick = {
                        dispatch(PlaceListIntent.CategoryToggled(category))
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
            title = "还没有可探索的地点",
            message = "添加第一个地点后，就可以从这里开始探索。",
            actionLabel = if (state.mode == PlaceListMode.ALL) "新建地点" else null,
            onAction = if (state.mode == PlaceListMode.ALL) {
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
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    if (place == null) {
        EmptyState("选择一个地点", "从左侧列表选择地点，在这里查看地点信息。")
        return
    }
    Column {
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
