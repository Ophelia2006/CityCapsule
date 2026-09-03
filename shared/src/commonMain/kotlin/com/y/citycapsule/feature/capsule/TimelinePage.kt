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
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.LazyListScope
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalConfiguration
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.app.navigation.RecordRootView
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.media.MediaMaintenanceCapability
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.designsystem.component.AdaptivePhotoGrid
import com.y.citycapsule.designsystem.component.AdaptivePane
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppDisplayText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppSegmentedControl
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.component.EmptyState
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.theme.AppTheme

private const val GALLERY_INITIAL_PHOTO_COUNT = 18
private const val GALLERY_NEXT_PHOTO_COUNT = 18

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
    onViewSelected: (RecordRootView) -> Unit,
    thumbnailCapability: MediaMaintenanceCapability
) {
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    var visiblePhotoCount by remember { mutableStateOf(GALLERY_INITIAL_PHOTO_COUNT) }
    var selectedCapsuleId by remember { mutableStateOf<String?>(null) }
    val holder = remember(capsules, places, dateFormatter) {
        CapsuleTimelineStateHolder(capsules, places, dateFormatter) { state = it }
    }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }

    val dimensions = AppTheme.dimensions
    val expanded = LocalConfiguration.current.pageViewWidth.dp >= dimensions.adaptiveGridBreakpoint
    val selectedItem = state.items.firstOrNull { it.capsule.id == selectedCapsuleId }
        ?: state.items.firstOrNull()
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions.screenHorizontalPadding,
                    top = statusBarHeight.dp + dimensions.spacingXxl,
                    end = dimensions.screenHorizontalPadding
                )
        ) {
            RecordHeader(selectedView, onViewSelected) { navigator.navigate(AppRoute.RoamingHistory()) }
        }
        AdaptivePane(
            primaryTitle = if (selectedView == RecordRootView.TIMELINE) "时间轴" else "城市相册",
            secondaryTitle = "城市碎片详情",
            modifier = Modifier.weight(1f).fillMaxWidth(),
            primary = {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = dimensions.screenHorizontalPadding,
                        end = dimensions.screenHorizontalPadding,
                        bottom = dimensions.spacingXl
                    )
                ) {
                    when {
                        state.status == CapsuleUiStatus.LOADING -> item(key = "record_loading") {
                            LoadingState("正在翻阅城市记忆…")
                        }
                        state.status == CapsuleUiStatus.ERROR -> item(key = "record_error") {
                            ErrorState(state.notice.orEmpty(), onRetry = holder::load)
                        }
                        selectedView == RecordRootView.TIMELINE -> timelineItems(
                            state = state,
                            thumbnailCapability = thumbnailCapability,
                            onExplore = { navigator.navigate(AppRoute.PlaceList()) },
                            onOpen = { id ->
                                if (expanded) selectedCapsuleId = id
                                else navigator.navigate(AppRoute.CapsuleDetail(id))
                            }
                        )
                        else -> item(key = "record_gallery") {
                            GalleryView(
                                state = state,
                                navigator = navigator,
                                visiblePhotoCount = visiblePhotoCount,
                                thumbnailCapability = thumbnailCapability,
                                onLoadMore = {
                                    visiblePhotoCount = nextGalleryVisibleCount(
                                        visiblePhotoCount,
                                        galleryPhotos(state).size
                                    )
                                }
                            )
                        }
                    }
                }
            },
            secondary = {
                if (selectedView == RecordRootView.TIMELINE) {
                    RecordDetailPane(selectedItem, navigator)
                } else {
                    EmptyState(
                        title = "选择一张照片",
                        message = "从左侧相册打开照片后查看对应的城市碎片。"
                    )
                }
            }
        )
    }
}

@Composable
private fun RecordHeader(
    selectedView: RecordRootView,
    onViewSelected: (RecordRootView) -> Unit,
    onRoamingHistory: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppTopBar("我的城市记忆", "按时间或照片，回到那些值得留下的片刻。")
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppSegmentedControl(
            options = listOf("时间轴", "相册"),
            selectedIndex = if (selectedView == RecordRootView.TIMELINE) 0 else 1,
            onSelected = { index ->
                onViewSelected(
                    if (index == 0) RecordRootView.TIMELINE else RecordRootView.GALLERY
                )
            }
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppButton("漫游回顾", onRoamingHistory, variant = AppButtonVariant.TEXT)
        Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    }
}

private fun LazyListScope.timelineItems(
    state: CapsuleTimelineState,
    thumbnailCapability: MediaMaintenanceCapability,
    onExplore: () -> Unit,
    onOpen: (String) -> Unit
) {
    if (state.items.isEmpty()) {
        item(key = "timeline_empty") {
            EmptyState(
                title = "还没有城市碎片",
                message = "从一个地点开始，留下第一条城市记忆。",
                actionLabel = "去探索地点"
            ) {
                onExplore()
            }
        }
        return
    }

    val groups = groupTimelineItems(state.items)
    groups.forEachIndexed { groupIndex, group ->
        item(key = "timeline_month_${group.monthKey}") {
            AppSectionTitle(group.monthLabel)
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        }
        items(
            count = group.items.size,
            key = { index -> "timeline_${group.items[index].capsule.id}" }
        ) { itemIndex ->
            val timelineItem = group.items[itemIndex]
            Column(modifier = Modifier.fillMaxWidth()) {
                TimelineMemoryRow(
                    item = timelineItem,
                    thumbnailCapability = thumbnailCapability,
                    onOpen = {
                        onOpen(timelineItem.capsule.id)
                    }
                )
                if (itemIndex < group.items.lastIndex) TimelineDivider()
            }
        }
        if (groupIndex < groups.lastIndex) {
            item(key = "timeline_gap_${group.monthKey}") {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXl))
            }
        }
    }
}

@Composable
private fun RecordDetailPane(item: CapsuleTimelineItem?, navigator: AppNavigator) {
    val dimensions = AppTheme.dimensions
    if (item == null) {
        EmptyState(
            title = "选择一段城市记忆",
            message = "从左侧时间轴选择一条记录，在这里回看完整内容。"
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = dimensions.screenHorizontalPadding,
            end = dimensions.screenHorizontalPadding,
            bottom = dimensions.spacingXl
        )
    ) {
        item {
            AppSectionTitle(item.place?.name ?: "城市碎片")
            Spacer(Modifier.height(dimensions.spacingXs))
            AppCaptionText(item.dateLabel)
            item.capsule.imagePaths.firstOrNull()?.let { path ->
                Spacer(Modifier.height(dimensions.spacingMd))
                CapsulePhoto(path, "${item.place?.name ?: "城市"}的记忆照片")
            }
            Spacer(Modifier.height(dimensions.spacingMd))
            com.y.citycapsule.designsystem.component.AppBodyText(item.capsule.content)
            Spacer(Modifier.height(dimensions.spacingLg))
            AppButton(
                text = "打开城市碎片",
                onClick = { navigator.navigate(AppRoute.CapsuleDetail(item.capsule.id)) },
                variant = AppButtonVariant.SECONDARY
            )
        }
    }
}

@Composable
private fun TimelineMemoryRow(item: CapsuleTimelineItem, thumbnailCapability: MediaMaintenanceCapability, onOpen: () -> Unit) {
    val dimensions = AppTheme.dimensions
    val calendar = parseCapsuleCalendarLabel(item.dateLabel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = dimensions.spacingSm)
    ) {
        Column(modifier = Modifier.width(dimensions.minTouchTarget)) {
            AppDisplayText(calendar.dayLabel)
            if (calendar.dayLabel != "—") AppCaptionText("日")
        }
        Spacer(Modifier.width(dimensions.spacingSm))
        Column(modifier = Modifier.weight(1f)) {
            AppSectionTitle(item.place?.name ?: "曾经到访的地点")
            item.place?.let { place ->
                Spacer(Modifier.height(dimensions.spacingXxs))
                AppCaptionText(listOfNotNull(place.city, place.district).joinToString(" · "))
            }
            Spacer(Modifier.height(dimensions.spacingSm))
            val firstPhoto = item.capsule.imagePaths.firstOrNull()
            if (firstPhoto == null) {
                TimelineMemoryPreview(item)
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.width(dimensions.mediaThumbnailSize)) {
                        CapsulePhoto(
                            path = firstPhoto,
                            description = "${item.place?.name ?: "城市"}的记忆照片",
                            compact = true,
                            thumbnailCapability = thumbnailCapability,
                            heightOverride = dimensions.mediaThumbnailSize
                        )
                    }
                    Spacer(Modifier.width(dimensions.spacingSm))
                    Column(modifier = Modifier.weight(1f)) {
                        TimelineMemoryPreview(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineMemoryPreview(item: CapsuleTimelineItem) {
    Text(
        text = item.capsule.content,
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.body,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis
    )
    val metadata = listOfNotNull(
        item.capsule.mood?.displayName(),
        item.capsule.tags.takeIf { it.isNotEmpty() }
            ?.joinToString("  ") { "#$it" }
    ).joinToString(" · ")
    if (metadata.isNotBlank()) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppCaptionText(metadata)
    }
}

@Composable
private fun TimelineDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.strokeThin)
            .background(AppTheme.colors.divider)
    )
}

internal data class RecordGalleryPhoto(
    val capsule: CityCapsule,
    val place: Place?,
    val dateLabel: String,
    val path: String,
    val index: Int
)

internal fun galleryPhotos(state: CapsuleTimelineState): List<RecordGalleryPhoto> =
    state.items.flatMap { item ->
        item.capsule.imagePaths.mapIndexed { index, path ->
            RecordGalleryPhoto(item.capsule, item.place, item.dateLabel, path, index)
        }
    }

@Composable
internal fun GalleryView(
    state: CapsuleTimelineState,
    navigator: AppNavigator,
    visiblePhotoCount: Int,
    thumbnailCapability: MediaMaintenanceCapability,
    onLoadMore: () -> Unit
) {
    val photos = galleryPhotos(state)
    if (photos.isEmpty()) {
        EmptyState(
            title = if (state.items.isEmpty()) "还没有城市碎片" else "还没有城市照片",
            message = if (state.items.isEmpty()) {
                "从一个地点开始，留下第一条城市记忆。"
            } else {
                "下一次记录时加一张照片，它会出现在这里。"
            },
            actionLabel = if (state.items.isEmpty()) "留下第一条碎片" else "去探索地点"
        ) {
            navigator.navigate(AppRoute.PlaceList())
        }
        return
    }

    val visiblePhotos = photos.take(visiblePhotoCount.coerceAtLeast(0))
    val groups = groupGalleryPhotos(visiblePhotos)
    groups.forEachIndexed { groupIndex, group ->
        AppSectionTitle(group.monthLabel)
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AdaptivePhotoGrid(items = group.items) { photo, tileSize ->
            CapsulePhoto(
                path = photo.path,
                description = "${photo.place?.name ?: "城市"}的照片 ${photo.index + 1}",
                thumbnailCapability = thumbnailCapability,
                modifier = Modifier.clickable {
                    navigator.navigate(AppRoute.CapsuleDetail(photo.capsule.id))
                },
                compact = true,
                heightOverride = tileSize
            )
        }
        if (groupIndex < groups.lastIndex) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXl))
        }
    }

    val remaining = photos.size - visiblePhotos.size
    if (remaining > 0) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppButton(
            text = "继续查看（还有 $remaining 张）",
            onClick = onLoadMore,
            variant = AppButtonVariant.TEXT
        )
    }
}

internal data class CapsuleCalendarLabel(
    val monthKey: String,
    val monthLabel: String,
    val dayLabel: String
)

internal data class CapsuleMonthGroup<T>(
    val monthKey: String,
    val monthLabel: String,
    val items: List<T>
)

internal fun parseCapsuleCalendarLabel(label: String): CapsuleCalendarLabel {
    val match = DATE_LABEL_PATTERN.matchEntire(label.trim())
    if (match == null) return CapsuleCalendarLabel("unknown", "日期未知", "—")
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    return CapsuleCalendarLabel(
        monthKey = "$year-$month",
        monthLabel = "$year 年 $month 月",
        dayLabel = day
    )
}

internal fun groupTimelineItems(items: List<CapsuleTimelineItem>): List<CapsuleMonthGroup<CapsuleTimelineItem>> =
    groupByCapsuleMonth(items) { it.dateLabel }

internal fun groupGalleryPhotos(items: List<RecordGalleryPhoto>): List<CapsuleMonthGroup<RecordGalleryPhoto>> =
    groupByCapsuleMonth(items) { it.dateLabel }

private fun <T> groupByCapsuleMonth(
    items: List<T>,
    dateLabel: (T) -> String
): List<CapsuleMonthGroup<T>> {
    val groups = linkedMapOf<String, MutableList<T>>()
    val labels = mutableMapOf<String, String>()
    items.forEach { item ->
        val calendar = parseCapsuleCalendarLabel(dateLabel(item))
        labels[calendar.monthKey] = calendar.monthLabel
        groups.getOrPut(calendar.monthKey) { mutableListOf() } += item
    }
    return groups.map { (key, values) ->
        CapsuleMonthGroup(key, labels.getValue(key), values)
    }
}

internal fun nextGalleryVisibleCount(current: Int, total: Int): Int =
    (current + GALLERY_NEXT_PHOTO_COUNT).coerceAtMost(total.coerceAtLeast(0))

private val DATE_LABEL_PATTERN = Regex("^(\\d+) 年 (\\d+) 月 (\\d+) 日$")
