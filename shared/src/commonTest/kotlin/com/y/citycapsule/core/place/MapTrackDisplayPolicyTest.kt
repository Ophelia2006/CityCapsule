package com.y.citycapsule.core.place

import com.y.citycapsule.core.map.MapTrackDisplayPolicy
import com.y.citycapsule.core.map.MapViewportPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class MapTrackDisplayPolicyTest {
    @Test
    fun samplingBoundsLongTracksAndPreservesBothEnds() {
        val points = (0 until 1_001).map { GeoPoint(it.toDouble(), -it.toDouble()) }
        val sampled = MapTrackDisplayPolicy.sample(points, maxPoints = 100)
        assertEquals(100, sampled.size)
        assertEquals(points.first(), sampled.first())
        assertEquals(points.last(), sampled.last())
    }

    @Test
    fun shortTrackIsNotChanged() {
        val points = listOf(GeoPoint(1.0, 2.0), GeoPoint(3.0, 4.0))
        assertEquals(points, MapTrackDisplayPolicy.sample(points))
    }

    @Test
    fun comparisonCameraContainsPlanAndActualTrack() {
        val camera = MapViewportPolicy.cameraFor(listOf(GeoPoint(30.0, 120.0), GeoPoint(30.1, 120.1)))!!
        assertEquals(30.05, camera.center.latitude)
        assertEquals(120.05, camera.center.longitude)
        assertEquals(11.0, camera.zoom)
    }
}
