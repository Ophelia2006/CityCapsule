package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.navigation.AppRootTab
import com.y.citycapsule.app.navigation.backToRoot
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleMediaCleanup
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.KuiklyLocalCapsuleDateFormatter
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.RepositoryCapsuleMediaCleanup
import com.y.citycapsule.core.media.KuiklyManagedMediaFiles
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppIcon
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppMenuItem
import com.y.citycapsule.designsystem.component.AppOverflowMenu
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.theme.AppTheme

private const val DETAIL_MENU_EDIT = "edit"
private const val DETAIL_MENU_DELETE = "delete"

@Page(AppRouteTable.PAGE_CAPSULE_DETAIL, supportInLocal = true)
internal class CapsuleDetailPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val storage = KuiklyKeyValueStore(this)
        val capsuleRepository = LocalCapsuleRepository(storage)
        val id = pageData.params.optString("capsuleId").orEmpty()
        setContent {
            CapsuleDetailScreen(
                id = id,
                navigator = KuiklyAppNavigator(this),
                capsules = capsuleRepository,
                places = LocalPlaceRepository(storage),
                mediaCleanup = RepositoryCapsuleMediaCleanup(
                    capsuleRepository,
                    KuiklyManagedMediaFiles(this)
                ),
                dateFormatter = KuiklyLocalCapsuleDateFormatter(this),
                themeHost = KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun CapsuleDetailScreen(
    id: String,
    navigator: AppNavigator,
    capsules: CapsuleRepository,
    places: LocalPlaceRepository,
    mediaCleanup: CapsuleMediaCleanup,
    dateFormatter: CapsuleDateFormatter,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleDetailState()) }
    var menuExpanded by remember { mutableStateOf(false) }
    val holder = remember(id, capsules, places, mediaCleanup, dateFormatter) {
        CapsuleDetailStateHolder(
            id,
            capsules,
            places,
            mediaCleanup,
            dateFormatter
        ) { state = it }
    }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppActionTopBar(
                title = "城市碎片",
                subtitle = state.place?.name,
                onLeadingClick = navigator::back,
                actionIcon = AppIconName.MORE,
                actionDescription = "更多操作",
                actionEnabled = state.status == CapsuleUiStatus.READY,
                onActionClick = { menuExpanded = true }
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when (state.status) {
                CapsuleUiStatus.LOADING -> LoadingState("正在读取这段城市记忆…")
                CapsuleUiStatus.NOT_FOUND -> EmptyState(
                    title = "这条城市记忆已不存在",
                    message = "它可能已经在另一处被删除。",
                    actionLabel = "返回记录",
                    onAction = { navigator.backToRoot(AppRootTab.RECORD) }
                )
                CapsuleUiStatus.ERROR -> ErrorState(
                    message = state.notice ?: "暂时无法读取这条城市记忆。",
                    onRetry = holder::load
                )
                CapsuleUiStatus.READY, CapsuleUiStatus.SAVING -> {
                    state.notice?.let { notice ->
                        AppStatusMessage(notice, tone = AppStatusTone.ERROR)
                        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                    }
                    state.capsule?.let { capsule ->
                        CapsuleDetailPhotoLayout(capsule.imagePaths)
                        if (capsule.imagePaths.isNotEmpty()) {
                            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                        }
                        CapsuleDetailMetadata(
                            dateLabel = state.dateLabel,
                            moodLabel = capsule.mood?.displayName()
                        )
                        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                        AppBodyText(capsule.content)
                        if (capsule.tags.isNotEmpty()) {
                            Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                            CapsuleTagRows(capsule.tags)
                        }
                        Spacer(Modifier.height(AppTheme.dimensions.spacingXl))
                        DetailDivider()
                        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                        LinkedPlaceRow(
                            place = state.place,
                            onOpen = state.place?.let { place ->
                                { navigator.navigate(AppRoute.PlaceDetail(place.id)) }
                            }
                        )
                    }
                }
            }
        }

        AppOverflowMenu(
            expanded = menuExpanded,
            items = listOf(
                AppMenuItem(DETAIL_MENU_EDIT, "编辑这条记忆"),
                AppMenuItem(DETAIL_MENU_DELETE, "删除这条记忆", destructive = true)
            ),
            onSelected = { selected ->
                menuExpanded = false
                when (selected) {
                    DETAIL_MENU_EDIT -> state.capsule?.let { capsule ->
                        navigator.navigate(
                            AppRoute.CapsuleEditor(capsule.id, capsule.placeId)
                        )
                    }
                    DETAIL_MENU_DELETE -> holder.requestDelete()
                }
            },
            onDismiss = { menuExpanded = false }
        )

        if (state.showDeleteConfirmation) {
            AppConfirmDialog(
                title = "删除这条城市记忆？",
                message = "照片和文字会从当前设备删除，且无法恢复。",
                confirmText = "确认删除",
                onConfirm = {
                    holder.delete { navigator.backToRoot(AppRootTab.RECORD) }
                },
                onDismiss = holder::dismissDelete
            )
        }
    }
}

@Composable
private fun CapsuleDetailMetadata(dateLabel: String, moodLabel: String?) {
    AppCaptionText(
        listOfNotNull(
            dateLabel.takeIf(String::isNotBlank),
            moodLabel
        ).joinToString(" · ")
    )
}

@Composable
private fun CapsuleTagRows(tags: List<String>) {
    tags.chunked(3).forEachIndexed { rowIndex, rowTags ->
        Row(Modifier.fillMaxWidth()) {
            rowTags.forEachIndexed { index, tag ->
                AppCaptionText("#$tag")
                if (index < rowTags.lastIndex) {
                    Spacer(Modifier.width(AppTheme.dimensions.spacingSm))
                }
            }
        }
        if (rowIndex < tags.chunked(3).lastIndex) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        }
    }
}

@Composable
private fun LinkedPlaceRow(place: Place?, onOpen: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onOpen != null) { onOpen?.invoke() }
            .padding(vertical = AppTheme.dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            name = AppIconName.LOCATION,
            contentDescription = "记录地点",
            tint = AppTheme.colors.primary
        )
        Spacer(Modifier.width(AppTheme.dimensions.spacingSm))
        Column(Modifier.weight(1f)) {
            AppSectionTitle(place?.name ?: "地点信息暂不可用")
            if (place != null) {
                val location = listOfNotNull(place.city, place.district).joinToString(" · ")
                if (location.isNotBlank()) {
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                    AppSecondaryText(location)
                }
            } else {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                AppSecondaryText("这段记忆仍会保留在时间轴中。")
            }
        }
        if (onOpen != null) {
            Spacer(Modifier.width(AppTheme.dimensions.spacingXs))
            AppIcon(AppIconName.FORWARD, "打开关联地点")
        }
    }
}

@Composable
private fun DetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.strokeThin)
            .background(AppTheme.colors.divider)
    )
}
