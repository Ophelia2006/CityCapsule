package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.*
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.*
import com.y.citycapsule.core.navigation.*
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.*
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_TIMELINE, supportInLocal = true)
internal class TimelinePager : BasePager() {
    override fun willInit() {
        super.willInit(); val storage = KuiklyKeyValueStore(this)
        setContent {
            TimelineScreen(
                KuiklyAppNavigator(this),
                LocalCapsuleRepository(storage),
                LocalPlaceRepository(storage),
                KuiklyLocalCapsuleDateFormatter(this),
                KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun TimelineScreen(
    navigator: AppNavigator,
    capsules: CapsuleRepository,
    places: LocalPlaceRepository,
    dateFormatter: CapsuleDateFormatter,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    val holder = remember {
        CapsuleTimelineStateHolder(capsules, places, dateFormatter) { state = it }
    }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }
    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar("我的城市记忆", "按时间回看那些值得留下的城市片段。")
            Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
            AppSegmentedControl(
                options = listOf("时间轴", "相册"),
                selectedIndex = 0,
                onSelected = { index ->
                    if (index == 1) navigator.replace(AppRoute.Gallery)
                }
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when {
                state.status == CapsuleUiStatus.LOADING -> LoadingState("正在翻阅城市记忆…")
                state.status == CapsuleUiStatus.ERROR -> ErrorState(state.notice.orEmpty(), onRetry = holder::load)
                state.items.isEmpty() -> {
                    EmptyState("还没有城市碎片", "从一个地点开始，写下第一条城市记忆。", actionLabel = "去探索地点") {
                        navigator.navigate(AppRoute.PlaceList)
                    }
                }
                else -> state.items.forEach { item ->
                    CapsuleCard(
                        model = CapsuleCardModel(
                            dateLabel = item.dateLabel,
                            placeLabel = item.place?.let { "${it.city} · ${it.name}" } ?: "曾经到访的地点",
                            excerpt = item.capsule.content,
                            metadata = listOfNotNull(item.capsule.mood?.displayName(), item.capsule.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" }).joinToString(" · ").ifBlank { null }
                        ),
                        onOpen = { navigator.navigate(AppRoute.CapsuleDetail(item.capsule.id)) },
                        media = item.capsule.imagePaths.firstOrNull()?.let { path -> {
                            CapsulePhoto(
                                path = path,
                                description = "${item.place?.name ?: "城市"}的记忆照片"
                            )
                        }}
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                }
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton("返回上一页", navigator::back, variant = AppButtonVariant.TEXT)
        }
    }
}
