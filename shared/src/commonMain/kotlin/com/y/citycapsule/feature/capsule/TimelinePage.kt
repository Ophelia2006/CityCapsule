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
        setContent { TimelineScreen(KuiklyAppNavigator(this), LocalCapsuleRepository(storage), LocalPlaceRepository(storage), KuiklyAppThemeHost(this)) }
    }
}

@Composable
private fun TimelineScreen(navigator: AppNavigator, capsules: CapsuleRepository, places: LocalPlaceRepository, themeHost: AppThemeHost) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    val holder = remember { CapsuleTimelineStateHolder(capsules, places) { state = it } }
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
                state.status == CapsuleUiStatus.LOADING -> AppSecondaryText("正在翻阅城市记忆…")
                state.status == CapsuleUiStatus.ERROR -> AppStatusMessage(state.notice.orEmpty(), tone = AppStatusTone.ERROR)
                state.items.isEmpty() -> {
                    AppSection("还没有城市碎片", description = "从一个地点开始，写下第一条城市记忆。") {
                        AppButton("去探索地点", { navigator.navigate(AppRoute.PlaceList) })
                    }
                }
                else -> state.items.forEach { item ->
                    AppCard(Modifier.clickable { navigator.navigate(AppRoute.CapsuleDetail(item.capsule.id)) }) {
                        AppCaptionText(formatCapsuleDate(item.capsule.createdAtEpochMs))
                        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                        AppSecondaryText(item.place?.let { "${it.city} · ${it.name}" } ?: "曾经到访的地点")
                        if (item.capsule.imagePaths.isNotEmpty()) {
                            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                            CapsulePhoto(
                                path = item.capsule.imagePaths.first(),
                                description = "${item.place?.name ?: "城市"}的记忆照片"
                            )
                        }
                        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                        AppBodyText(item.capsule.content)
                        item.capsule.mood?.let { Spacer(Modifier.height(AppTheme.dimensions.spacingXs)); AppSecondaryText(it.displayName()) }
                        if (item.capsule.tags.isNotEmpty()) {
                            Spacer(Modifier.height(AppTheme.dimensions.spacingXs)); AppCaptionText(item.capsule.tags.joinToString("  ") { "#$it" })
                        }
                    }
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                }
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton("返回上一页", navigator::back, variant = AppButtonVariant.TEXT)
        }
    }
}
