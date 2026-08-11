package com.y.citycapsule.core.location

import com.y.citycapsule.core.place.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoDistanceTest {
    @Test
    fun samePointIsZeroAndKnownLatitudeDeltaIsReasonable() {
        val origin = GeoPoint(31.2304, 121.4737)
        assertEquals(0.0, GeoDistance.meters(origin, origin), 0.001)
        val oneDegree = GeoDistance.meters(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0))
        assertTrue(oneDegree in 111_000.0..111_300.0)
    }

    @Test
    fun labelsDoNotPretendGreaterPrecisionThanNeeded() {
        assertEquals("420 m", GeoDistance.label(420.9))
        assertEquals("1.2 km", GeoDistance.label(1_260.0))
    }
}
