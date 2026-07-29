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
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.feature.capsule.RecordRootContent
import com.y.citycapsule.feature.profile.ProfileRootContent
import kotlinx.coroutines.launch

@Page(AppRouteTable.PAGE_APP_SHELL, supportInLocal = true)
internal class AppShellPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val initialTab = AppRootTab.fromInitialRouteValue(
            pageData.params.optString(AppRouteTable.PARAM_INITIAL_ROOT_TAB)
        ) ?: AppRootTab.EXPLORE
        setContent {
            AppShellScreen(
                initialTab = initialTab,
                navigator = navigator,
                settingsRepository = SettingsRepository(storage),
                profileRepository = LocalProfileRepository(storage),
                onboardingRepository = OnboardingRepository(storage),
                placeRepository = LocalPlaceRepository(storage),
                capsuleRepository = LocalCapsuleRepository(storage),
                dateFormatter = KuiklyLocalCapsuleDateFormatter(this),
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
    onboardingRepository: OnboardingRepository,
    placeRepository: LocalPlaceRepository,
    capsuleRepository: LocalCapsuleRepository,
    dateFormatter: KuiklyLocalCapsuleDateFormatter,
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
            pagerState.animateScrollToPage(target.pageIndex)
            AppShellRuntime.consumeTab(target)
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppRootScaffold(
            selectedTab = shellState.selectedTab,
            onTabSelected = { selected ->
                if (shellState.selectTab(selected)) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(selected.pageIndex)
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
                        onViewSelected = shellState::selectRecordView
                    )

                    AppRootTab.PROFILE -> ProfileRootContent(
                        navigator = navigator,
                        profileRepository = profileRepository,
                        onboardingRepository = onboardingRepository,
                        statusBarHeight = statusBarHeight,
                        listState = profileListState
                    )
                }
            }
        }
    }
}
