package com.y.citycapsule.core.map

import com.y.citycapsule.core.place.GeoPoint

/** Provider-neutral contract between Explore state and a platform native map view. */
data class MapMarkerModel(val placeId: String, val title: String, val position: GeoPoint)
data class MapCameraModel(val center: GeoPoint, val zoom: Double)
data class ExploreMapViewState(
    val markers: List<MapMarkerModel> = emptyList(),
    val selectedPlaceId: String? = null,
    val camera: MapCameraModel? = null,
    val currentLocation: GeoPoint? = null,
    val showCurrentLocation: Boolean = false,
    /** WGS-84 points rendered as a provider-side polyline. Full-resolution storage stays in Track files. */
    val trackPoints: List<GeoPoint> = emptyList(),
    /** Optional persisted road plan rendered behind the actual GPS track. */
    val plannedTrackPoints: List<GeoPoint> = emptyList()
)

object MapTrackDisplayPolicy {
    const val MAX_POINTS = 500

    fun sample(points: List<GeoPoint>, maxPoints: Int = MAX_POINTS): List<GeoPoint> {
        if (points.size <= maxPoints || maxPoints < 2) return points.take(maxPoints.coerceAtLeast(0))
        val last = points.lastIndex
        return (0 until maxPoints).map { index -> points[index * last / (maxPoints - 1)] }
    }
}

object MapViewportPolicy {
    fun cameraFor(points: List<GeoPoint>): MapCameraModel? {
        if (points.isEmpty()) return null
        val minLat = points.minOf { it.latitude }; val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }; val maxLng = points.maxOf { it.longitude }
        val span = maxOf(maxLat - minLat, maxLng - minLng)
        val zoom = when {
            span > 0.5 -> 9.0; span > 0.2 -> 10.0; span > 0.08 -> 11.0; span > 0.03 -> 12.0
            span > 0.012 -> 13.0; span > 0.005 -> 14.0; else -> 15.0
        }
        return MapCameraModel(GeoPoint((minLat + maxLat) / 2, (minLng + maxLng) / 2), zoom)
    }
}

sealed interface MapAvailability {
    data object Ready : MapAvailability
    data object MissingConfiguration : MapAvailability
    data object Offline : MapAvailability
    data object Unsupported : MapAvailability
    data class Failure(val message: String) : MapAvailability
}

sealed interface MapViewEvent {
    data class Ready(val camera: MapCameraModel?) : MapViewEvent
    data class MarkerSelected(val placeId: String) : MapViewEvent
    data class CameraChanged(val camera: MapCameraModel) : MapViewEvent
    data class Unavailable(val reason: MapAvailability) : MapViewEvent
}

interface NativeMapViewContract {
    fun render(state: ExploreMapViewState)
    fun onStart()
    fun onStop()
    fun dispose()
}
