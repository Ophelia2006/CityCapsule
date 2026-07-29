package com.y.citycapsule.navigation

import com.y.citycapsule.core.navigation.AppRouteTable
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidLaunchContractTest {
    @Test
    fun launcherWithoutExtrasStartsAtLaunchGate() {
        assertEquals(
            AppRouteTable.PAGE_LAUNCH_GATE,
            AndroidLaunchContract.resolvePageName(null)
        )
        assertEquals(
            AppRouteTable.ROUTE_LAUNCH_GATE,
            AndroidLaunchContract.resolveRouteKey(
                requestedRouteKey = null,
                resolvedPageName = AndroidLaunchContract.resolvePageName(null)
            )
        )
    }

    @Test
    fun blankExtrasCannotBypassLaunchGate() {
        assertEquals(
            AppRouteTable.PAGE_LAUNCH_GATE,
            AndroidLaunchContract.resolvePageName("   ")
        )
    }

    @Test
    fun explicitBusinessRouteRemainsUnchanged() {
        assertEquals(
            AppRouteTable.PAGE_SETTINGS,
            AndroidLaunchContract.resolvePageName(AppRouteTable.PAGE_SETTINGS)
        )
        assertEquals(
            AppRouteTable.ROUTE_SETTINGS,
            AndroidLaunchContract.resolveRouteKey(
                requestedRouteKey = AppRouteTable.ROUTE_SETTINGS,
                resolvedPageName = AppRouteTable.PAGE_SETTINGS
            )
        )
    }

    @Test
    fun explicitAppShellUsesItsCanonicalRouteKey() {
        assertEquals(
            AppRouteTable.ROUTE_APP_SHELL,
            AndroidLaunchContract.resolveRouteKey(
                requestedRouteKey = AppRouteTable.ROUTE_APP_SHELL,
                resolvedPageName = AppRouteTable.PAGE_APP_SHELL
            )
        )
    }
}
