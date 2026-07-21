package com.y.citycapsule.core.navigation

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KuiklyAppNavigatorTest {

    @Test
    fun navigateWritesPushEnvelope() {
        val transport = FakeRouteTransport()
        val navigator = KuiklyAppNavigator(transport)

        navigator.navigate(AppRoute.PlaceDetail("place-42"))

        assertEquals(AppRouteTable.PAGE_PLACE_DETAIL, transport.openedPageName)
        val pageData = assertNotNull(transport.openedPageData)
        assertEquals("push", pageData.optString(RouteProtocol.PARAM_ACTION))
        assertEquals(
            AppRouteTable.ROUTE_PLACE_DETAIL,
            pageData.optString(RouteProtocol.PARAM_ROUTE_KEY)
        )
        assertEquals(
            RouteProtocol.TARGET_KUIKLY,
            pageData.optString(RouteProtocol.PARAM_TARGET_TYPE)
        )
        assertEquals("place-42", pageData.optString("placeId"))
    }

    @Test
    fun replaceWritesReplaceEnvelope() {
        val transport = FakeRouteTransport()
        val navigator = KuiklyAppNavigator(transport)

        navigator.replace(AppRoute.Settings)

        val pageData = assertNotNull(transport.openedPageData)
        assertEquals("replace", pageData.optString(RouteProtocol.PARAM_ACTION))
        assertEquals(AppRouteTable.PAGE_SETTINGS, transport.openedPageName)
    }

    @Test
    fun backClosesCurrentPage() {
        val transport = FakeRouteTransport()
        val navigator = KuiklyAppNavigator(transport)

        navigator.back()

        assertEquals(1, transport.closeCount)
    }

    @Test
    fun backToCarriesStableTargetRouteKey() {
        val transport = FakeRouteTransport()
        val navigator = KuiklyAppNavigator(transport)

        navigator.backTo(AppRouteKey.HOME)

        val pageData = assertNotNull(transport.openedPageData)
        assertEquals("backTo", pageData.optString(RouteProtocol.PARAM_ACTION))
        assertEquals(
            AppRouteTable.ROUTE_HOME,
            pageData.optString(RouteProtocol.PARAM_ROUTE_KEY)
        )
        assertEquals(AppRouteTable.PAGE_HOME, transport.openedPageName)
    }
}

private class FakeRouteTransport : RouteTransport {
    var openedPageName: String? = null
    var openedPageData: JSONObject? = null
    var closeCount: Int = 0

    override fun openPage(pageName: String, pageData: JSONObject) {
        openedPageName = pageName
        openedPageData = pageData
    }

    override fun closePage() {
        closeCount += 1
    }
}
