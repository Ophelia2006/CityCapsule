package com.y.citycapsule.core.map

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.place.GeoPoint

private const val VIEW_NAME = "CCAmapView"
private const val PROP_STATE = "mapState"
private const val PROP_PRIVACY_ACCEPTED = "privacyAccepted"
private const val EVENT_MAP = "onMapEvent"

internal class AmapNativeGroupView : GroupView<AmapNativeAttr, AmapNativeEvent>() {
    override fun createAttr() = AmapNativeAttr().apply { overflow(true) }
    override fun createEvent() = AmapNativeEvent()
    override fun viewName(): String = VIEW_NAME
}

internal class AmapNativeAttr : GroupAttr() {
    fun mapState(value: String) = apply { PROP_STATE with value }
    fun privacyAccepted(value: Boolean) = apply { PROP_PRIVACY_ACCEPTED with value }
}

internal class AmapNativeEvent : GroupEvent() {
    fun onMapEvent(handler: (String) -> Unit) {
        register(EVENT_MAP) { value -> handler(value?.toString().orEmpty()) }
    }
}

@Composable
fun AmapNativeView(
    state: ExploreMapViewState,
    privacyAccepted: Boolean,
    onEvent: (MapViewEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    MakeKuiklyComposeNode<AmapNativeGroupView>(
        factory = ::AmapNativeGroupView,
        modifier = modifier,
        viewInit = {
            getViewEvent().onMapEvent { encoded ->
                decodeMapEvent(encoded)?.let(onEvent)
            }
        },
        viewUpdate = { view ->
            view.getViewAttr().apply {
                mapState(encodeMapState(state))
                privacyAccepted(privacyAccepted)
            }
        },
        content = {}
    )
}

internal fun encodeMapState(state: ExploreMapViewState): String = JSONObject().apply {
    put("markers", JSONArray().also { array ->
        state.markers.forEach { marker ->
            array.put(JSONObject().apply {
                put("placeId", marker.placeId)
                put("title", marker.title)
                put("latitude", marker.position.latitude)
                put("longitude", marker.position.longitude)
            })
        }
    })
    state.selectedPlaceId?.let { put("selectedPlaceId", it) }
    state.camera?.let { camera ->
        put("camera", JSONObject().apply {
            put("latitude", camera.center.latitude)
            put("longitude", camera.center.longitude)
            put("zoom", camera.zoom)
        })
    }
    state.currentLocation?.let { point ->
        put("currentLocation", JSONObject().apply {
            put("latitude", point.latitude)
            put("longitude", point.longitude)
        })
    }
    put("showCurrentLocation", state.showCurrentLocation)
    put("trackPoints", JSONArray().also { array ->
        MapTrackDisplayPolicy.sample(state.trackPoints).forEach { point ->
            array.put(JSONObject().apply {
                put("latitude", point.latitude)
                put("longitude", point.longitude)
            })
        }
    })
    put("plannedTrackPoints", JSONArray().also { array ->
        MapTrackDisplayPolicy.sample(state.plannedTrackPoints).forEach { point ->
            array.put(JSONObject().apply { put("latitude", point.latitude); put("longitude", point.longitude) })
        }
    })
}.toString()

internal fun decodeMapEvent(encoded: String): MapViewEvent? = try {
    val json = JSONObject(encoded)
    when (json.optString("type")) {
        "ready" -> MapViewEvent.Ready(json.optJSONObject("camera")?.toCamera())
        "markerSelected" -> json.optString("placeId")
            .takeIf(String::isNotBlank)?.let(MapViewEvent::MarkerSelected)
        "cameraChanged" -> json.optJSONObject("camera")?.toCamera()
            ?.let(MapViewEvent::CameraChanged)
        "unavailable" -> MapViewEvent.Unavailable(
            when (json.optString("reason")) {
                "missingConfiguration" -> MapAvailability.MissingConfiguration
                "offline" -> MapAvailability.Offline
                "unsupported" -> MapAvailability.Unsupported
                else -> MapAvailability.Failure(json.optString("message").ifBlank {
                    "地图暂时不可用"
                })
            }
        )
        else -> null
    }
} catch (_: Throwable) {
    null
}

private fun JSONObject.toCamera(): MapCameraModel? {
    val latitude = optString("latitude").toDoubleOrNull() ?: return null
    val longitude = optString("longitude").toDoubleOrNull() ?: return null
    val zoom = optString("zoom").toDoubleOrNull() ?: return null
    return MapCameraModel(GeoPoint(latitude, longitude), zoom)
}
