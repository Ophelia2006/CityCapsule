package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.city.ExploreCityRepository
import com.y.citycapsule.core.city.ExploreCityRuntime
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.location.CurrentLocationRuntime
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceRepository
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
import com.y.citycapsule.designsystem.component.CapsuleCard
import com.y.citycapsule.designsystem.component.CapsuleCardModel
import com.y.citycapsule.designsystem.component.CapsuleCardVariant
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
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
import com.y.citycapsule.feature.place.PlaceFeatureRuntime
import com.y.citycapsule.feature.place.displayName

/** Explore root content hosted inside the single AppShellPage. */
@Composable
internal fun HomeRootContent(
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    cityRepository: ExploreCityRepository,
    placeRepository: PlaceRepository,
    favoriteRepository: FavoriteRepository,
    capsuleRepository: CapsuleRepository,
    dateFormatter: CapsuleDateFormatter,
    active: Boolean,
    statusBarHeight: Float,
    listState: LazyListState
) {
    var uiState by remember { mutableStateOf(HomeUiState()) }
    var showPlacePicker by remember { mutableStateOf(false) }
    val holder = remember(profileRepository, cityRepository, placeRepository, favoriteRepository, capsuleRepository, dateFormatter) {
        HomeStateHolder(
            profileRepository,
            cityRepository,
            placeRepository,
            favoriteRepository,
            capsuleRepository,
            dateFormatter,
            onStateChanged = { uiState = it }
        )
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
                HomeProfileHeader(uiState)
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
                        onQuickRecord = { showPlacePicker = true },
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
}

@Composable
private fun HomeProfileHeader(state: HomeUiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            AppCaptionText("当前探索城市")
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSectionTitle(state.selectedCity.displayName)
        }
        AppProfileAvatar(state.profile.avatarPreset)
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
    onQuickRecord: () -> Unit,
    onRetry: () -> Unit
) {
    val dimensions = AppTheme.dimensions
    AppSectionTitle("今天想去哪里？")
    Spacer(Modifier.height(dimensions.spacingSm))
    val featured = state.featuredPlace
    if (state.catalogReadOnly) {
        ErrorState(
            "地点数据暂时无法安全读取，当前不会执行新建、想去或记录操作。",
            onRetry = onRetry
        )
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

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("规划下一次探索")
    Spacer(Modifier.height(dimensions.spacingXxs))
    AppSecondaryText("手动挑选地点并安排顺序；当前不会计算交通或推荐路线。")
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
}

@Composable
private fun HomePlaceCard(
    place: Place,
    favorite: Boolean,
    favoriteEnabled: Boolean,
    variant: PlaceCardVariant,
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
        favoriteEnabled = favoriteEnabled
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
    PlaceCategory.LANDMARK -> PlaceFallbackKind.LANDMARK
    PlaceCategory.CULTURE -> PlaceFallbackKind.CULTURE
    PlaceCategory.FOOD -> PlaceFallbackKind.FOOD
    PlaceCategory.NATURE -> PlaceFallbackKind.NATURE
    PlaceCategory.SHOPPING -> PlaceFallbackKind.SHOPPING
    PlaceCategory.OTHER -> PlaceFallbackKind.OTHER
}

private const val HOME_CATEGORY_LIMIT = 6
private const val HOME_CATEGORY_COLUMNS = 2
private const val HOME_CATEGORY_GAP_WEIGHT = 0.08f
private const val HOME_SEARCH_GAP_WEIGHT = 0.4f
