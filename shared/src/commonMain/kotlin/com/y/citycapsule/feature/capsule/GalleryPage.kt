package com.y.citycapsule.feature.capsule

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
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.KuiklyLocalCapsuleDateFormatter
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.ErrorState
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.theme.AppTheme

private const val COMPAT_GALLERY_INITIAL_PHOTO_COUNT = 18

/** Compatibility route. The formal product gallery lives inside RecordRootContent. */
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
                dateFormatter = KuiklyLocalCapsuleDateFormatter(this),
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
    dateFormatter: CapsuleDateFormatter,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleTimelineState()) }
    var visiblePhotoCount by remember { mutableStateOf(COMPAT_GALLERY_INITIAL_PHOTO_COUNT) }
    val holder = remember(capsules, places, dateFormatter) {
        CapsuleTimelineStateHolder(capsules, places, dateFormatter) { state = it }
    }
    val revision = CapsuleFeatureRuntime.revision
    LaunchedEffect(holder, revision) { holder.load() }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppActionTopBar(
                title = "城市相册",
                subtitle = "从照片回到当时的地点与心情。",
                onLeadingClick = navigator::back
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when (state.status) {
                CapsuleUiStatus.LOADING -> LoadingState("正在整理城市照片…")
                CapsuleUiStatus.ERROR -> ErrorState(
                    state.notice.orEmpty(),
                    onRetry = holder::load
                )
                else -> GalleryView(
                    state = state,
                    navigator = navigator,
                    visiblePhotoCount = visiblePhotoCount,
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
}
