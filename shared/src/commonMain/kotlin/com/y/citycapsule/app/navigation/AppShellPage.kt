package com.y.citycapsule.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.pager.HorizontalPager
import com.tencent.kuikly.compose.foundation.pager.rememberPagerState
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.HomeRootContent
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.AppThemeRuntime
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.KuiklyLocalCapsuleDateFormatter
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.city.LocalExploreCityRepository
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.LocalPlacePhotoCacheRepository
import com.y.citycapsule.core.place.AmapPlaceRemoteDataSource
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.core.media.KuiklyMediaMaintenance
import com.y.citycapsule.feature.capsule.RecordRootContent
import com.y.citycapsule.feature.profile.ProfileRootContent
import kotlinx.coroutines.launch

@Page(AppRouteTable.PAGE_APP_SHELL, supportInLocal = true)
internal class AppShellPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val placeRepository = LocalPlaceRepository(storage)
        val photoCacheRepository = LocalPlacePhotoCacheRepository(storage)
        val initialTab = AppRootTab.fromInitialRouteValue(
            pageData.params.optString(AppRouteTable.PARAM_INITIAL_ROOT_TAB)
        ) ?: AppRootTab.EXPLORE
        setContent {
            AppShellScreen(
                initialTab = initialTab,
                navigator = navigator,
                settingsRepository = SettingsRepository(storage),
                profileRepository = LocalProfileRepository(storage),
                cityRepository = LocalExploreCityRepository(storage),
                placeRepository = placeRepository,
                photoCacheRepository = photoCacheRepository,
                remoteDataSource = AmapPlaceRemoteDataSource(this),
                favoriteRepository = LocalFavoriteRepository(storage, placeRepository),
                capsuleRepository = LocalCapsuleRepository(storage),
                dateFormatter = KuiklyLocalCapsuleDateFormatter(this),
                thumbnailCapability = KuiklyMediaMaintenance(this),
                themeHost = KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun AppShellScreen(
    initialTab: AppRootTab,
    navigator: AppNavigator,
    settingsRepository: SettingsRepository,
    profileRepository: LocalProfileRepository,
    cityRepository: LocalExploreCityRepository,
    placeRepository: LocalPlaceRepository,
    photoCacheRepository: LocalPlacePhotoCacheRepository,
    remoteDataSource: PlaceRemoteDataSource,
    favoriteRepository: LocalFavoriteRepository,
    capsuleRepository: LocalCapsuleRepository,
    dateFormatter: KuiklyLocalCapsuleDateFormatter,
    thumbnailCapability: KuiklyMediaMaintenance,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    val shellState = remember(initialTab) { AppShellState(initialTab) }
    val pagerState = rememberPagerState(initialPage = initialTab.pageIndex) {
        AppRootTab.entries.size
    }
    val exploreListState = rememberLazyListState()
    val recordListState = rememberLazyListState()
    val profileListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val requestedTab = AppShellRuntime.requestedTab

    LaunchedEffect(settingsRepository) {
        settingsRepository.getThemeModeSnapshot { snapshot ->
            AppThemeRuntime.applyPersistedMode(snapshot.mode)
        }
    }

    LaunchedEffect(requestedTab) {
        requestedTab?.let { target ->
            shellState.selectTab(target)
            pagerState.scrollToPage(target.pageIndex)
            AppShellRuntime.consumeTab(target)
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppRootScaffold(
            selectedTab = shellState.selectedTab,
            onTabSelected = { selected ->
                if (shellState.selectTab(selected)) {
                    coroutineScope.launch {
                        pagerState.scrollToPage(selected.pageIndex)
                    }
                }
            }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = AppRootTab.entries.lastIndex,
                userScrollEnabled = false,
                key = { page -> AppRootTab.fromPageIndex(page).id }
            ) { page ->
                when (AppRootTab.fromPageIndex(page)) {
                    AppRootTab.EXPLORE -> HomeRootContent(
                        navigator = navigator,
                        profileRepository = profileRepository,
                        cityRepository = cityRepository,
                        placeRepository = placeRepository,
                        photoCacheRepository = photoCacheRepository,
                        remoteDataSource = remoteDataSource,
                        favoriteRepository = favoriteRepository,
                        capsuleRepository = capsuleRepository,
                        dateFormatter = dateFormatter,
                        active = shellState.selectedTab == AppRootTab.EXPLORE,
                        statusBarHeight = statusBarHeight,
                        listState = exploreListState
                    )

                    AppRootTab.RECORD -> RecordRootContent(
                        navigator = navigator,
                        capsules = capsuleRepository,
                        places = placeRepository,
                        dateFormatter = dateFormatter,
                        statusBarHeight = statusBarHeight,
                        listState = recordListState,
                        selectedView = shellState.recordView,
                        onViewSelected = shellState::selectRecordView,
                        thumbnailCapability = thumbnailCapability
                    )

                    AppRootTab.PROFILE -> ProfileRootContent(
                        navigator = navigator,
                        profileRepository = profileRepository,
                        placeRepository = placeRepository,
                        favoriteRepository = favoriteRepository,
                        capsuleRepository = capsuleRepository,
                        active = shellState.selectedTab == AppRootTab.PROFILE,
                        statusBarHeight = statusBarHeight,
                        listState = profileListState
                    )
                }
            }
        }
    }
}
