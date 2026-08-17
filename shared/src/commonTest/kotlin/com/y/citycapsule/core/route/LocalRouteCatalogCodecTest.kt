package com.y.citycapsule.core.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalRouteCatalogCodecTest {
    @Test fun roundTripPreservesManualOrder() {
        val route = LocalRoute("route-1", "午后散步", listOf("p3", "p1", "p2"), 42L)
        assertEquals(listOf("p3", "p1", "p2"), LocalRouteCatalogCodec.decode(LocalRouteCatalogCodec.encode(LocalRouteCatalog(routes = listOf(route))))?.routes?.single()?.orderedPlaceIds)
    }

    @Test fun rejectsDuplicateStops() {
        assertNull(LocalRouteValidator.normalizeDraftOrNull(LocalRouteDraft("路线", listOf("p1", "p1"))))
    }
}
