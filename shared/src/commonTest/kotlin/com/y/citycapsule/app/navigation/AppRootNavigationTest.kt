package com.y.citycapsule.app.navigation

import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRouteKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppRootNavigationTest {

    @Test
    fun everyRootUsesItsRealDefaultPageAndStablePagerIndex() {
        assertEquals(AppRoute.Home, AppRootTab.EXPLORE.route)
        assertEquals(AppRoute.Timeline, AppRootTab.RECORD.route)
        assertEquals(AppRoute.LocalRoutes, AppRootTab.ROAM.route)
        assertEquals(AppRoute.Profile, AppRootTab.PROFILE.route)
        assertEquals(AppRootTab.entries, (0..3).map(AppRootTab::fromPageIndex))
        assertEquals(AppRootTab.EXPLORE, AppRootTab.fromInitialRouteValue("home"))
        assertEquals(AppRootTab.RECORD, AppRootTab.fromInitialRouteValue("timeline"))
        assertEquals(AppRootTab.ROAM, AppRootTab.fromInitialRouteValue("local_routes"))
        assertEquals(AppRootTab.PROFILE, AppRootTab.fromInitialRouteValue("profile"))
    }

    @Test
    fun selectingAnotherTabChangesShellStateWithoutCreatingARoute() {
        val state = AppShellState(AppRootTab.EXPLORE)

        assertTrue(state.selectTab(AppRootTab.RECORD))
        assertEquals(AppRootTab.RECORD, state.selectedTab)
    }

    @Test
    fun selectingCurrentTabAgainIsNoOp() {
        val state = AppShellState(AppRootTab.PROFILE)

        assertFalse(state.selectTab(AppRootTab.PROFILE))
        assertEquals(AppRootTab.PROFILE, state.selectedTab)
    }

    @Test
    fun repeatedSelectionOfEveryCurrentTabIsAlwaysNoOp() {
        AppRootTab.entries.forEach { tab ->
            val state = AppShellState(tab)

            repeat(5) {
                assertFalse(state.selectTab(tab))
            }
            assertEquals(tab, state.selectedTab)
        }
    }

    @Test
    fun rapidRootSelectionEndsAtTheLastRequestedTab() {
        val state = AppShellState(AppRootTab.EXPLORE)

        assertTrue(state.selectTab(AppRootTab.PROFILE))
        assertTrue(state.selectTab(AppRootTab.RECORD))

        assertEquals(AppRootTab.RECORD, state.selectedTab)
    }

    @Test
    fun recordViewStateSurvivesRootTabChanges() {
        val state = AppShellState(AppRootTab.RECORD)
        state.selectRecordView(RecordRootView.GALLERY)

        state.selectTab(AppRootTab.PROFILE)
        state.selectTab(AppRootTab.RECORD)

        assertEquals(RecordRootView.GALLERY, state.recordView)
    }

    @Test
    fun backToRootRequestsTheLiveShellTabAndUsesTypedBackTo() {
        val navigator = RecordingNavigator()

        navigator.backToRoot(AppRootTab.RECORD)

        assertEquals(AppRootTab.RECORD, AppShellRuntime.requestedTab)
        assertEquals(AppRouteKey.TIMELINE, navigator.backToKey)
        AppShellRuntime.consumeTab(AppRootTab.RECORD)
        assertNull(AppShellRuntime.requestedTab)
    }

    @Test
    fun diagnosticsCannotBecomeRootTabs() {
        assertNull(AppRootTab.fromId("router"))
        assertNull(AppRootTab.fromId("image_adapter"))
    }
}

private class RecordingNavigator : AppNavigator {
    var backToKey: AppRouteKey? = null

    override fun navigate(route: AppRoute) = Unit
    override fun replace(route: AppRoute) = Unit
    override fun back() = Unit
    override fun backTo(routeKey: AppRouteKey) {
        backToKey = routeKey
    }
}
