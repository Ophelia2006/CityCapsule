package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.formatCapsuleDate
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSegmentedControl
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_GALLERY, supportInLocal = true)
internal class GalleryPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val storage = KuiklyKeyValueStore(this)
        setContent {
            GalleryScreen(
                navigator = KuiklyAppNavigator(this),
                capsules = LocalCapsuleRepository(storage),
                places = LocalPlaceRepository(storage),
                themeHost = KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun GalleryScreen(
    navigator: AppNavigator,
    capsules: CapsuleRepository,
    places: PlaceRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    val holder = remember { CapsuleTimelineStateHolder(capsules, places) { state = it } }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar("城市相册", "从照片回到当时的地点与心情。")
            Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
            AppSegmentedControl(
                options = listOf("时间轴", "相册"),
                selectedIndex = 1,
                onSelected = { index ->
                    if (index == 0) navigator.replace(AppRoute.Timeline)
                }
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when {
                state.status == CapsuleUiStatus.LOADING -> AppSecondaryText("正在整理城市照片…")
                state.status == CapsuleUiStatus.ERROR -> AppStatusMessage(
                    state.notice.orEmpty(),
                    tone = AppStatusTone.ERROR
                )
                state.items.none { it.capsule.imagePaths.isNotEmpty() } -> {
                    AppSecondaryText("还没有带照片的城市碎片。你仍可以在时间轴查看文字记忆。")
                    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                    AppButton(
                        text = "查看时间轴",
                        onClick = { navigator.replace(AppRoute.Timeline) }
                    )
                }
                else -> GalleryGrid(
                    items = state.items.flatMap { item ->
                        item.capsule.imagePaths.mapIndexed { index, path ->
                            GalleryPhoto(item.capsule, item.place, path, index)
                        }
                    },
                    onOpen = { navigator.navigate(AppRoute.CapsuleDetail(it.capsule.id)) }
                )
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton("返回上一页", navigator::back, variant = AppButtonVariant.TEXT)
        }
    }
}

private data class GalleryPhoto(
    val capsule: CityCapsule,
    val place: Place?,
    val path: String,
    val index: Int
)

@Composable
private fun GalleryGrid(
    items: List<GalleryPhoto>,
    onOpen: (GalleryPhoto) -> Unit
) {
    items.chunked(2).forEach { rowItems ->
        Row(modifier = Modifier.fillMaxWidth()) {
            rowItems.forEachIndexed { columnIndex, item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(
                            end = if (columnIndex == 0) {
                                AppTheme.dimensions.spacingXxs
                            } else {
                                AppTheme.dimensions.spacingNone
                            }
                        )
                ) {
                    CapsulePhoto(
                        path = item.path,
                        description = "${item.place?.name ?: "城市"}的照片 ${item.index + 1}",
                        modifier = Modifier.clickable { onOpen(item) },
                        compact = true
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                    AppCaptionText(
                        item.place?.name ?: formatCapsuleDate(item.capsule.createdAtEpochMs)
                    )
                }
            }
            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    }
}
