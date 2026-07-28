package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
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
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppChoiceChip
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_PLACE_LIST, supportInLocal = true)
internal class PlaceListPager : BasePager() {
    override fun willInit() {
        super.willInit()
        installPlaceList(PlaceListMode.ALL)
    }

    private fun installPlaceList(mode: PlaceListMode) {
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val placeRepository = LocalPlaceRepository(storage)
        val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceListScreen(
                mode,
                navigator,
                placeRepository,
                favoriteRepository,
                themeHost
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
                PlaceListMode.FAVORITES,
                navigator,
                placeRepository,
                favoriteRepository,
                themeHost
            )
        }
    }
}

@Composable
private fun PlaceListScreen(
    mode: PlaceListMode,
    navigator: AppNavigator,
    placeRepository: LocalPlaceRepository,
    favoriteRepository: LocalFavoriteRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember { mutableStateOf(PlaceListUiState(mode = mode)) }
    val holder = remember(placeRepository, favoriteRepository, mode) {
        PlaceListStateHolder(
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            mode = mode,
            onDataChanged = PlaceFeatureRuntime::invalidate,
            onStateChanged = { uiState = it }
        )
    }
    val catalogRevision = PlaceFeatureRuntime.revision

    LaunchedEffect(holder, catalogRevision) {
        holder.load()
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = if (mode == PlaceListMode.FAVORITES) "想去的地方" else "地点",
                subtitle = if (mode == PlaceListMode.FAVORITES) {
                    "想去清单只保存在当前设备。"
                } else {
                    "离线搜索、筛选和维护自己的地点目录。"
                }
            )
            uiState.notice?.let {
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppStatusMessage(it.message, tone = it.tone.toAppStatusTone())
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            AppTextField(
                value = uiState.query,
                onValueChange = holder::updateQuery,
                label = "搜索地点",
                placeholder = "名称、标签、城市、地址或备注",
                maxLength = SEARCH_QUERY_MAX_LENGTH,
                enabled = uiState.status == PlaceListUiStatus.READY
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            PlaceFilters(uiState, holder)
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            PlaceListContent(uiState, holder, navigator)
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (mode == PlaceListMode.ALL) {
                AppButton(
                    text = "新建地点",
                    onClick = { navigator.navigate(AppRoute.PlaceEditor()) },
                    enabled = !uiState.readOnly &&
                        uiState.status == PlaceListUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            }
            AppButton(
                text = "重新加载",
                onClick = holder::load,
                variant = AppButtonVariant.SECONDARY,
                enabled = uiState.status == PlaceListUiStatus.READY
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton(
                text = "返回上一页",
                onClick = navigator::back,
                variant = AppButtonVariant.TEXT
            )
        }
    }
}

@Composable
private fun PlaceFilters(
    state: PlaceListUiState,
    holder: PlaceListStateHolder
) {
    AppSection(
        title = "筛选",
        description = "分类内部为任意匹配，分类与城市、区域之间同时满足。"
    ) {
        PlaceCategory.entries.forEach { category ->
            AppChoiceChip(
                text = category.displayName(),
                selected = category in state.filter.categories,
                onClick = { holder.toggleCategory(category) },
                enabled = state.status == PlaceListUiStatus.READY
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        }
        AppTextField(
            value = state.filter.city.orEmpty(),
            onValueChange = holder::updateCity,
            label = "城市",
            placeholder = "例如：上海",
            maxLength = com.y.citycapsule.core.place.PlaceValidator.CITY_MAX_LENGTH,
            enabled = state.status == PlaceListUiStatus.READY
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppTextField(
            value = state.filter.district.orEmpty(),
            onValueChange = holder::updateDistrict,
            label = "区域",
            placeholder = "例如：徐汇区",
            maxLength = com.y.citycapsule.core.place.PlaceValidator.DISTRICT_MAX_LENGTH,
            enabled = state.status == PlaceListUiStatus.READY
        )
        if (state.mode == PlaceListMode.ALL) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            AppChoiceChip(
                text = "只看想去",
                selected = state.filter.favoritesOnly,
                onClick = holder::toggleFavoritesOnly,
                enabled = state.status == PlaceListUiStatus.READY
            )
        }
        if (state.hasActiveFilters) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            AppButton(
                text = "清除筛选",
                onClick = holder::clearFilters,
                variant = AppButtonVariant.TEXT
            )
        }
    }
}

@Composable
private fun PlaceListContent(
    state: PlaceListUiState,
    holder: PlaceListStateHolder,
    navigator: AppNavigator
) {
    when (state.contentState) {
        PlaceListContentState.LOADING ->
            AppSecondaryText("正在读取本地点目录…")
        PlaceListContentState.EMPTY_CATALOG ->
            AppSecondaryText("当前没有地点，可以新建第一条记录。")
        PlaceListContentState.EMPTY_FAVORITES ->
            AppSecondaryText("还没有想去的地方。")
        PlaceListContentState.NO_MATCHES ->
            AppSecondaryText("没有符合当前搜索和筛选条件的地点。")
        PlaceListContentState.STORAGE_ERROR ->
            AppSecondaryText("地点数据暂时无法安全读取，写操作已禁用。")
        PlaceListContentState.RESULTS -> {
            AppSecondaryText("共 ${state.visiblePlaces.size} 个结果")
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            state.visiblePlaces.forEach { place ->
                PlaceSummaryCard(
                    place = place,
                    favorite = place.id in state.favoriteIds,
                    favoriteBusy = state.busyFavoriteId == place.id,
                    onOpen = { navigator.navigate(AppRoute.PlaceDetail(place.id)) },
                    onToggleFavorite = { holder.toggleFavorite(place.id) }
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            }
        }
    }
}

private const val SEARCH_QUERY_MAX_LENGTH = 80
