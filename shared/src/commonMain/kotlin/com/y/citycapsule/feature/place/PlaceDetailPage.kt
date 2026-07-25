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
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_PLACE_DETAIL, supportInLocal = true)
internal class PlaceDetailPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val placeId = pageData.params.optString(AppRouteTable.PARAM_PLACE_ID).orEmpty()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val placeRepository = LocalPlaceRepository(storage)
        val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceDetailScreen(
                placeId,
                navigator,
                placeRepository,
                favoriteRepository,
                themeHost
            )
        }
    }
}

@Composable
private fun PlaceDetailScreen(
    placeId: String,
    navigator: AppNavigator,
    placeRepository: LocalPlaceRepository,
    favoriteRepository: LocalFavoriteRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember { mutableStateOf(PlaceDetailUiState()) }
    val holder = remember(placeId, placeRepository, favoriteRepository) {
        PlaceDetailStateHolder(
            placeId = placeId,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
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
                title = "地点详情",
                subtitle = "地点与收藏均只保存在当前设备。"
            )
            uiState.notice?.let {
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppStatusMessage(it.message, tone = it.tone.toAppStatusTone())
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            val place = uiState.place
            if (place == null) {
                AppSecondaryText(
                    if (uiState.status == PlaceDetailUiStatus.LOADING) {
                        "正在读取地点…"
                    } else {
                        "这个地点不存在或暂时无法读取。"
                    }
                )
            } else {
                PlaceDetails(place)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppButton(
                    text = if (uiState.favorite) "取消收藏" else "加入收藏",
                    onClick = holder::toggleFavorite,
                    enabled = !uiState.isBusy,
                    loading = uiState.togglingFavorite,
                    loadingText = "正在更新…"
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "编辑地点",
                    onClick = { navigator.navigate(AppRoute.PlaceEditor(place.id)) },
                    variant = AppButtonVariant.SECONDARY,
                    enabled = !uiState.isBusy
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "删除地点",
                    onClick = holder::requestDelete,
                    variant = AppButtonVariant.DANGER,
                    enabled = !uiState.isBusy,
                    loading = uiState.status == PlaceDetailUiStatus.DELETING
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "刷新详情",
                    onClick = holder::load,
                    variant = AppButtonVariant.TEXT,
                    enabled = !uiState.isBusy
                )
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton(
                text = "返回上一页",
                onClick = navigator::back,
                variant = AppButtonVariant.TEXT,
                enabled = !uiState.isBusy
            )
        }

        if (uiState.showDeleteConfirmation) {
            AppConfirmDialog(
                title = "删除这个地点？",
                message = "地点和对应收藏状态会从当前设备删除，此操作无法撤销。",
                confirmText = "确认删除",
                onConfirm = {
                    holder.delete {
                        navigator.backTo(AppRouteKey.PLACE_LIST)
                    }
                },
                onDismiss = holder::dismissDelete
            )
        }
    }
}

@Composable
private fun PlaceDetails(place: Place) {
    AppSection(title = place.name, description = place.category.displayName()) {
        AppBodyText(text = "城市：${place.city}")
        place.district?.let {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppBodyText(text = "区域：$it")
        }
        place.address?.let {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppBodyText(text = "地址：$it")
        }
        if (place.tags.isNotEmpty()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(text = "标签：${place.tags.joinToString("、")}")
        }
        place.note?.let {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            AppSecondaryText(text = it)
        }
    }
}
