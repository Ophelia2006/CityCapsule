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
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppBottomSheet
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppChoiceChip
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
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = mode,
                navigator = navigator,
                profileRepository = LocalProfileRepository(storage),
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
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
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode = PlaceListMode.FAVORITES,
                navigator = navigator,
                profileRepository = LocalProfileRepository(storage),
                placeRepository = placeRepository,
                favoriteRepository = favoriteRepository,
                themeHost = themeHost
            )
        }
    }
}

@Composable
private fun PlaceListScreen(
    mode: PlaceListMode,
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    placeRepository: LocalPlaceRepository,
    favoriteRepository: LocalFavoriteRepository,
    themeHost: AppThemeHost,
    initialCategory: PlaceCategory? = null
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    val storeScope = rememberCoroutineScope()
    val invalidationOwner = remember { PlaceFeatureRuntime.newOwnerToken() }
    val store = remember(
        profileRepository,
        placeRepository,
        favoriteRepository,
        mode,
        initialCategory
    ) {
        PlaceListStore(
            profileRepository = profileRepository,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            parentScope = storeScope,
            mode = mode,
            initialCategory = initialCategory
        )
    }
    val uiState by store.state.collectAsState()
    val catalogRevision = PlaceFeatureRuntime.revision
    var showFilters by remember { mutableStateOf(false) }
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
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
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

        AppBottomSheet(
            visible = showFilters,
            title = "筛选地点",
            onDismiss = { showFilters = false },
            dismissLabel = "完成"
        ) {
            AdvancedFilters(uiState, store::dispatch)
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
        placeholder = state.homeCity?.let { "例如：$it" } ?: "例如：上海",
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
        AppBodyText(place.note ?: "这个地点暂时还没有补充介绍。")
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
