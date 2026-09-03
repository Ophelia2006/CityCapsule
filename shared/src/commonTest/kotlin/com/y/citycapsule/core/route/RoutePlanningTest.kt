package com.y.citycapsule.core.route

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RoutePlanningTest {
    @Test
    fun parsesRealRoadPolylineDistanceAndDuration() {
        val response = JSONObject().apply {
            put("status", "success")
            put("body", """{"status":"1","route":{"paths":[{"distance":"1234","duration":"900","steps":[{"polyline":"120.1536,30.2507;120.1540,30.2510"}]}]}}""")
        }
        val result = assertIs<WalkingLegResult.Success>(
            AmapRoutePlanningRemoteDataSource.parseWalkingResponse(response, "a", "b")
        )
        assertEquals(1234, result.leg.distanceMeters)
        assertEquals(900, result.leg.durationSeconds)
        assertEquals(2, result.leg.points.size)
    }

    @Test
    fun recommendsOrderFromRoadCostsAndKeepsStartFixed() {
        val distances = mapOf(
            ("a" to "b") to 100L, ("a" to "c") to 10L, ("a" to "d") to 50L,
            ("b" to "c") to 10L, ("b" to "d") to 10L, ("c" to "d") to 100L
        )
        val result = RouteOrderOptimizer.recommend(listOf("a", "b", "c", "d"), distances)
        assertEquals("a", result?.first())
        assertEquals(setOf("a", "b", "c", "d"), result?.toSet())
        assertTrue(result != listOf("a", "b", "c", "d"))
    }

    @Test
    fun refusesUnboundedOptimization() {
        assertEquals(null, RouteOrderOptimizer.recommend((0..8).map(Int::toString), emptyMap()))
    }
}
