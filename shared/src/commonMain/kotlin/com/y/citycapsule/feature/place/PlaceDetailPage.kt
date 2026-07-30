package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.app.navigation.AppRootTab
import com.y.citycapsule.app.navigation.backToRoot
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
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.KuiklyLocalCapsuleDateFormatter
import com.y.citycapsule.feature.capsule.CapsuleFeatureRuntime
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.component.AppPageTitle
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppIconButton
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppMenuItem
import com.y.citycapsule.designsystem.component.AppOverflowMenu
import com.y.citycapsule.designsystem.component.CapsuleCard
import com.y.citycapsule.designsystem.component.CapsuleCardModel
import com.y.citycapsule.designsystem.component.CapsuleCardVariant
import com.y.citycapsule.designsystem.component.PlaceMediaFallback
import com.y.citycapsule.feature.capsule.CapsulePhoto
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
                LocalCapsuleRepository(storage),
                KuiklyLocalCapsuleDateFormatter(this),
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
    capsuleRepository: CapsuleRepository,
    dateFormatter: KuiklyLocalCapsuleDateFormatter,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember { mutableStateOf(PlaceDetailUiState()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val invalidationOwner = remember { PlaceFeatureRuntime.newOwnerToken() }
    val holder = remember(placeId, placeRepository, favoriteRepository, capsuleRepository) {
        PlaceDetailStateHolder(
            placeId = placeId,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            capsuleRepository = capsuleRepository,
            onDataChanged = { PlaceFeatureRuntime.invalidateFrom(invalidationOwner) },
            onStateChanged = { uiState = it }
        )
    }
    val catalogRevision = PlaceFeatureRuntime.revision
    val capsuleRevision = CapsuleFeatureRuntime.revision

    LaunchedEffect(holder, catalogRevision) {
        if (PlaceFeatureRuntime.shouldReload(invalidationOwner)) {
            holder.load()
        }
    }
    LaunchedEffect(holder, capsuleRevision) {
        if (uiState.status != PlaceDetailUiStatus.LOADING) {
            holder.load()
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AppTopBar(
                    title = "地点详情",
                    subtitle = "发现地点，也留下属于你的城市片段。",
                    modifier = Modifier.weight(1f)
                )
                AppIconButton(
                    icon = AppIconName.MORE,
                    contentDescription = "更多地点操作",
                    onClick = { menuExpanded = true },
                    enabled = uiState.place != null && uiState.status == PlaceDetailUiStatus.READY
                )
            }
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
                Box(
                    Modifier.fillMaxWidth()
                        .height(AppTheme.dimensions.placeHeroHeight)
                        .clip(RoundedCornerShape(AppTheme.dimensions.radiusLg))
                ) {
                    PlaceMediaFallback(place.category.toFallbackKind())
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                PlaceDetails(place)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AppBodyText(if (uiState.favorite) "已加入想去" else "想去")
                    Spacer(Modifier.weight(1f))
                    AppIconButton(
                        icon = if (uiState.favorite) AppIconName.FAVORITE_FILLED else AppIconName.FAVORITE,
                        contentDescription = if (uiState.favorite) "移出想去" else "加入想去",
                        onClick = holder::toggleFavorite,
                        selected = uiState.favorite,
                        enabled = !uiState.isBusy
                    )
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSection(
                    title = "我的城市记忆",
                    description = if (uiState.memoryCount == 0) {
                        "你还没有在这里留下记录。"
                    } else {
                        "已经在这里留下 ${uiState.memoryCount} 条城市碎片。"
                    }
                ) {
                    if (uiState.recentMemories.isNotEmpty()) {
                        uiState.recentMemories.forEachIndexed { index, memory ->
                            CapsuleCard(
                                model = CapsuleCardModel(
                                    dateLabel = dateFormatter.format(memory.createdAtEpochMs),
                                    placeLabel = place.name,
                                    excerpt = memory.content.ifBlank { "一段只属于这里的城市记忆" },
                                    metadata = memory.tags.takeIf { it.isNotEmpty() }
                                        ?.joinToString("  ") { "#$it" }
                                ),
                                onOpen = { navigator.navigate(AppRoute.CapsuleDetail(memory.id)) },
                                variant = CapsuleCardVariant.RECENT,
                                media = memory.imagePaths.firstOrNull()?.let { path ->
                                    { CapsulePhoto(path, "${place.name}的城市记忆", compact = true) }
                                }
                            )
                            if (index < uiState.recentMemories.lastIndex) {
                                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                            }
                        }
                        if (uiState.memoryCount > 0) {
                            AppButton(
                                text = "查看全部城市记忆",
                                onClick = { navigator.backToRoot(AppRootTab.RECORD) },
                                variant = AppButtonVariant.TEXT,
                                enabled = !uiState.isBusy
                            )
                        }
                    } else {
                        AppSecondaryText("一张照片、一句话，也可以成为以后想起这里的入口。")
                    }
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppButton(
                    text = "在这里留下城市碎片",
                    onClick = { navigator.navigate(AppRoute.CapsuleEditor(placeId = place.id)) },
                    enabled = uiState.status == PlaceDetailUiStatus.READY
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

        AppOverflowMenu(
            expanded = menuExpanded,
            items = listOf(
                AppMenuItem("edit", "编辑地点", enabled = !uiState.isBusy),
                AppMenuItem("delete", "删除地点", destructive = true, enabled = !uiState.isBusy)
            ),
            onSelected = { action ->
                menuExpanded = false
                when (action) {
                    "edit" -> uiState.place?.let { navigator.navigate(AppRoute.PlaceEditor(it.id)) }
                    "delete" -> holder.requestDelete()
                }
            },
            onDismiss = { menuExpanded = false }
        )

        if (uiState.showDeleteConfirmation) {
            AppConfirmDialog(
                title = "删除这个地点？",
                message = "地点和对应想去状态会从当前设备删除。有关联城市记忆的地点不会被删除，此操作无法撤销。",
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
    Column {
        AppPageTitle(place.name)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(
            listOfNotNull(place.city, place.district, place.category.displayName()).joinToString(" · ")
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
        AppSection(title = "关于这里") {
            if (!place.note.isNullOrBlank()) AppBodyText(place.note)
            else AppSecondaryText("这个地点暂时还没有补充介绍。")
        if (place.tags.isNotEmpty()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppCaptionText(place.tags.joinToString("  ") { "#$it" })
            }
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
        AppSection(title = "地址") {
            AppBodyText(place.address ?: listOfNotNull(place.city, place.district).joinToString(" · "))
        }
    }
}
