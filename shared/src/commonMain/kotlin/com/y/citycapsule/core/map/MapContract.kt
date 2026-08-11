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
    val showCurrentLocation: Boolean = false
)

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
