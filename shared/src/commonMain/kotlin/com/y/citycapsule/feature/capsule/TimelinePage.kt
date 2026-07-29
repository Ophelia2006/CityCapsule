package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.app.navigation.RecordRootView
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppSegmentedControl
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.component.CapsuleCard
import com.y.citycapsule.designsystem.component.CapsuleCardModel
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.component.PhotoGrid
import com.y.citycapsule.designsystem.theme.AppTheme

/** Record root content. Timeline and Gallery are retained views, not platform routes. */
@Composable
internal fun RecordRootContent(
    navigator: AppNavigator,
    capsules: CapsuleRepository,
    places: PlaceRepository,
    dateFormatter: CapsuleDateFormatter,
    statusBarHeight: Float,
    listState: LazyListState,
    selectedView: RecordRootView,
    onViewSelected: (RecordRootView) -> Unit
) {
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    val holder = remember(capsules, places, dateFormatter) {
        CapsuleTimelineStateHolder(capsules, places, dateFormatter) { state = it }
    }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }

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
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar("我的城市记忆", "按时间或照片回看那些值得留下的城市片段。")
                Spacer(Modifier.height(dimensions.spacingMd))
                AppSegmentedControl(
                    options = listOf("时间轴", "相册"),
                    selectedIndex = if (selectedView == RecordRootView.TIMELINE) 0 else 1,
                    onSelected = { index ->
                        onViewSelected(
                            if (index == 0) RecordRootView.TIMELINE else RecordRootView.GALLERY
                        )
                    }
                )
                Spacer(Modifier.height(dimensions.spacingLg))
                when {
                    state.status == CapsuleUiStatus.LOADING -> LoadingState("正在翻阅城市记忆…")
                    state.status == CapsuleUiStatus.ERROR ->
                        ErrorState(state.notice.orEmpty(), onRetry = holder::load)
                    selectedView == RecordRootView.TIMELINE ->
                        TimelineView(state, navigator)
                    else -> GalleryView(state, navigator)
                }
            }
        }
    }
}

@Composable
private fun TimelineView(state: CapsuleTimelineState, navigator: AppNavigator) {
    if (state.items.isEmpty()) {
        EmptyState(
            title = "还没有城市碎片",
            message = "从一个地点开始，写下第一条城市记忆。",
            actionLabel = "去探索地点"
        ) {
            navigator.navigate(AppRoute.PlaceList())
        }
        return
    }

    state.items.forEach { item ->
        CapsuleCard(
            model = CapsuleCardModel(
                dateLabel = item.dateLabel,
                placeLabel = item.place?.let { "${it.city} · ${it.name}" } ?: "曾经到访的地点",
                excerpt = item.capsule.content,
                metadata = listOfNotNull(
                    item.capsule.mood?.displayName(),
                    item.capsule.tags.takeIf { it.isNotEmpty() }
                        ?.joinToString("  ") { "#$it" }
                ).joinToString(" · ").ifBlank { null }
            ),
            onOpen = { navigator.navigate(AppRoute.CapsuleDetail(item.capsule.id)) },
            media = item.capsule.imagePaths.firstOrNull()?.let { path ->
                {
                    CapsulePhoto(
                        path = path,
                        description = "${item.place?.name ?: "城市"}的记忆照片"
                    )
                }
            }
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    }
}

private data class RecordGalleryPhoto(
    val capsule: CityCapsule,
    val place: Place?,
    val dateLabel: String,
    val path: String,
    val index: Int
)

@Composable
private fun GalleryView(state: CapsuleTimelineState, navigator: AppNavigator) {
    val photos = state.items.flatMap { item ->
        item.capsule.imagePaths.mapIndexed { index, path ->
            RecordGalleryPhoto(item.capsule, item.place, item.dateLabel, path, index)
        }
    }
    if (photos.isEmpty()) {
        EmptyState(
            title = "还没有城市照片",
            message = "带照片的城市碎片会在这里形成相册。"
        )
        return
    }

    PhotoGrid(items = photos) { photo ->
        Column {
            CapsulePhoto(
                path = photo.path,
                description = "${photo.place?.name ?: "城市"}的照片 ${photo.index + 1}",
                modifier = Modifier.clickable {
                    navigator.navigate(AppRoute.CapsuleDetail(photo.capsule.id))
                },
                compact = true
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppCaptionText(photo.place?.name ?: photo.dateLabel)
        }
    }
}
