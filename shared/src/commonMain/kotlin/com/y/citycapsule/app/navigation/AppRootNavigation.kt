package com.y.citycapsule.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable

/** The four product roots shown in the primary navigation. */
enum class AppRootTab(
    val id: String,
    val route: AppRoute,
    val pageIndex: Int
) {
    EXPLORE("explore", AppRoute.Home, 0),
    RECORD("record", AppRoute.Timeline, 1),
    ROAM("roam", AppRoute.LocalRoutes, 2),
    PROFILE("profile", AppRoute.Profile, 3);

    companion object {
        fun fromId(id: String): AppRootTab? = entries.firstOrNull { it.id == id }
        fun fromInitialRouteValue(value: String): AppRootTab? = when (value) {
            AppRouteTable.ROUTE_HOME -> EXPLORE
            AppRouteTable.ROUTE_TIMELINE -> RECORD
            AppRouteTable.ROUTE_LOCAL_ROUTES -> ROAM
            AppRouteTable.ROUTE_PROFILE -> PROFILE
            else -> fromId(value)
        }
        fun fromPageIndex(index: Int): AppRootTab =
            entries.firstOrNull { it.pageIndex == index } ?: EXPLORE
    }
}

enum class RecordRootView {
    TIMELINE,
    GALLERY
}

/** In-process handoff used when a secondary page returns to a specific root in the live shell. */
object AppShellRuntime {
    var requestedTab: AppRootTab? by mutableStateOf(null)
        private set

    fun requestTab(tab: AppRootTab) {
        requestedTab = tab
    }

    fun consumeTab(tab: AppRootTab) {
        if (requestedTab == tab) requestedTab = null
    }
}

fun com.y.citycapsule.core.navigation.AppNavigator.backToRoot(tab: AppRootTab) {
    AppShellRuntime.requestTab(tab)
    backTo(
        when (tab) {
            AppRootTab.EXPLORE -> com.y.citycapsule.core.navigation.AppRouteKey.HOME
            AppRootTab.RECORD -> com.y.citycapsule.core.navigation.AppRouteKey.TIMELINE
            AppRootTab.ROAM -> com.y.citycapsule.core.navigation.AppRouteKey.HOME
            AppRootTab.PROFILE -> com.y.citycapsule.core.navigation.AppRouteKey.PROFILE
        }
    )
}

/** State owned by the single app shell rather than any platform route stack. */
class AppShellState(initialTab: AppRootTab) {
    var selectedTab: AppRootTab by mutableStateOf(initialTab)
        private set

    var recordView: RecordRootView by mutableStateOf(RecordRootView.TIMELINE)
        private set

    fun selectTab(tab: AppRootTab): Boolean {
        if (tab == selectedTab) return false
        selectedTab = tab
        return true
    }

    fun selectRecordView(view: RecordRootView) {
        recordView = view
    }
}
