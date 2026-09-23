package com.y.citycapsule.map

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

class KRAmapView(context: Context) : KRView(context) {
    private var mapView: TextureMapView? = null
    private var aMap: AMap? = null
    private var eventCallback: KuiklyRenderCallback? = null
    private var pendingState: String? = null
    private var privacyAccepted = false
    private var destroyed = false
    private val placeMarkers = mutableListOf<Marker>()
    private var currentLocationMarker: Marker? = null
    private var plannedTrack: Polyline? = null
    private var actualTrack: Polyline? = null
    private var renderedMarkers = ""
    private var renderedCurrentLocation = ""
    private var renderedPlannedTrack = ""
    private var renderedActualTrack = ""
    private var renderedCamera = ""

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP ->
                parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun setProp(propKey: String, propValue: Any): Boolean = when (propKey) {
        PROP_STATE -> {
            val nextState = propValue.toString()
            if (nextState != pendingState) {
                pendingState = nextState
                Log.d(TAG, "Received changed map state")
                renderState()
            }
            true
        }
        PROP_PRIVACY_ACCEPTED -> {
            privacyAccepted = propValue as? Boolean ?: propValue.toString().toBoolean()
            Log.d(TAG, "Privacy accepted=$privacyAccepted")
            ensureMapCreated()
            true
        }
        EVENT_MAP -> {
            eventCallback = propValue as? KuiklyRenderCallback
            Log.d(TAG, "Map callback registered=${eventCallback != null}")
            ensureMapCreated()
            true
        }
        else -> super.setProp(propKey, propValue)
    }

    private fun ensureMapCreated() {
        if (destroyed || mapView != null || !privacyAccepted || eventCallback == null) {
            Log.d(
                TAG,
                "Map creation deferred: destroyed=$destroyed, created=${mapView != null}, " +
                    "privacyAccepted=$privacyAccepted, callback=${eventCallback != null}"
            )
            return
        }
        if (configuredApiKey().isBlank()) {
            Log.e(TAG, "AMap API key is missing from the merged manifest")
            emitUnavailable("missingConfiguration", "未配置 Android 高德地图 Key")
            return
        }
        runCatching {
            Log.i(TAG, "Creating Android AMap view")
            MapsInitializer.updatePrivacyShow(context, true, true)
            MapsInitializer.updatePrivacyAgree(context, true)
            TextureMapView(context).also { nativeView ->
                addView(nativeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                nativeView.onCreate(Bundle())
                nativeView.onResume()
                mapView = nativeView
                aMap = nativeView.map.apply {
                    uiSettings.isZoomControlsEnabled = false
                    setOnMapLoadedListener {
                        Log.i(
                            TAG,
                            "Android AMap tiles loaded; host=${width}x${height}, " +
                                "map=${nativeView.width}x${nativeView.height}"
                        )
                    }
                    setOnMarkerClickListener { marker ->
                        val placeId = marker.`object` as? String
                        Log.d(TAG, "Marker clicked; placeId=$placeId")
                        placeId?.let(::emitMarkerSelected)
                        true
                    }
                }
                renderState()
                Log.i(TAG, "Android AMap view is ready")
                emit(JSONObject().put("type", "ready"))
            }
        }.onFailure { error ->
            Log.e(TAG, "Android AMap initialization failed", error)
            emitUnavailable("failure", error.message ?: "高德地图初始化失败")
        }
    }

    private fun renderState() {
        val map = aMap ?: return
        val encoded = pendingState ?: return
        runCatching {
            val state = JSONObject(encoded)
            val markers = state.optJSONArray("markers")
            val markerState = markers?.toString().orEmpty()
            if (markerState != renderedMarkers) {
                placeMarkers.forEach(Marker::remove)
                placeMarkers.clear()
                if (markers != null) {
                    for (index in 0 until markers.length()) {
                        val marker = markers.getJSONObject(index)
                        val position = toAmap(
                            marker.getDouble("latitude"),
                            marker.getDouble("longitude")
                        )
                        map.addMarker(
                            MarkerOptions().position(position).title(marker.optString("title"))
                        )?.also { added ->
                            added.`object` = marker.getString("placeId")
                            placeMarkers += added
                        }
                    }
                }
                renderedMarkers = markerState
            }
            val currentState = if (state.optBoolean("showCurrentLocation")) {
                state.optJSONObject("currentLocation")?.toString().orEmpty()
            } else ""
            if (currentState != renderedCurrentLocation) {
                val point = state.optJSONObject("currentLocation")
                if (currentState.isBlank() || point == null) {
                    currentLocationMarker?.remove()
                    currentLocationMarker = null
                } else {
                    val position = toAmap(point.getDouble("latitude"), point.getDouble("longitude"))
                    val existing = currentLocationMarker
                    if (existing == null) currentLocationMarker = map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title("当前位置")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            ))
                    ) else existing.position = position
                }
                renderedCurrentLocation = currentState
            }
            val plannedPoints = state.optJSONArray("plannedTrackPoints")
            val plannedState = plannedPoints?.toString().orEmpty()
            if (plannedState != renderedPlannedTrack) {
                plannedTrack?.remove()
                plannedTrack = null
                val points = plannedPoints
                val converted = buildList {
                    if (points != null) for (index in 0 until points.length()) {
                        val point = points.getJSONObject(index)
                        add(toAmap(point.getDouble("latitude"), point.getDouble("longitude")))
                    }
                }
                if (converted.size >= 2) plannedTrack = map.addPolyline(
                    PolylineOptions().addAll(converted).width(PLANNED_TRACK_WIDTH_PX).color(PLANNED_TRACK_COLOR)
                )
                renderedPlannedTrack = plannedState
            }
            val trackPoints = state.optJSONArray("trackPoints")
            val trackState = trackPoints?.toString().orEmpty()
            if (trackState != renderedActualTrack) {
                actualTrack?.remove()
                actualTrack = null
                val points = trackPoints
                val converted = buildList {
                    if (points != null) for (index in 0 until points.length()) {
                            val point = points.getJSONObject(index)
                            add(toAmap(point.getDouble("latitude"), point.getDouble("longitude")))
                    }
                }
                if (converted.size >= 2) {
                    actualTrack = map.addPolyline(
                        PolylineOptions()
                            .addAll(converted)
                            .width(TRACK_WIDTH_PX)
                            .color(TRACK_COLOR)
                    )
                }
                renderedActualTrack = trackState
            }
            state.optJSONObject("camera")?.let { camera ->
                val cameraState = camera.toString()
                if (cameraState != renderedCamera) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        toAmap(camera.getDouble("latitude"), camera.getDouble("longitude")),
                        camera.optDouble("zoom", DEFAULT_ZOOM.toDouble()).toFloat()
                    ))
                    renderedCamera = cameraState
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "AMap state rendering failed", error)
            emitUnavailable("failure", error.message ?: "地图数据无法解析")
        }
    }

    private fun toAmap(latitude: Double, longitude: Double): LatLng =
        CoordinateConverter(context)
            .from(CoordinateConverter.CoordType.GPS)
            .coord(LatLng(latitude, longitude))
            .convert()

    private fun configuredApiKey(): String = runCatching {
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).metaData?.getString("com.amap.api.v2.apikey").orEmpty()
    }.getOrDefault("")

    override fun onAddToParent(parent: android.view.ViewGroup) {
        super.onAddToParent(parent)
        if (destroyed) {
            // Kuikly may reuse the native wrapper after a retained root/page is shown again.
            // onDestroy() tears down only the provider view; allow this wrapper to mount anew.
            destroyed = false
            renderedMarkers = ""
            renderedCurrentLocation = ""
            renderedPlannedTrack = ""
            renderedActualTrack = ""
            renderedCamera = ""
        }
        Log.d(TAG, "AMap host added to parent")
        if (mapView == null) ensureMapCreated() else mapView?.onResume()
    }

    override fun onRemoveFromParent(parent: android.view.ViewGroup) {
        Log.d(TAG, "AMap host removed from parent")
        mapView?.onPause()
        super.onRemoveFromParent(parent)
    }

    override fun onDestroy() {
        destroyed = true
        eventCallback = null
        mapView?.onDestroy()
        placeMarkers.clear()
        currentLocationMarker = null
        plannedTrack = null
        actualTrack = null
        mapView = null
        aMap = null
        super.onDestroy()
    }

    private fun emitMarkerSelected(placeId: String) = emit(
        JSONObject().put("type", "markerSelected").put("placeId", placeId)
    )

    private fun emitUnavailable(reason: String, message: String) = emit(
        JSONObject().put("type", "unavailable").put("reason", reason).put("message", message)
    )

    private fun emit(payload: JSONObject) {
        if (!destroyed) eventCallback?.invoke(payload.toString())
    }

    companion object {
        const val VIEW_NAME = "CCAmapView"
        private const val TAG = "CCAmapView"
        private const val PROP_STATE = "mapState"
        private const val PROP_PRIVACY_ACCEPTED = "privacyAccepted"
        private const val EVENT_MAP = "onMapEvent"
        private const val DEFAULT_ZOOM = 12f
        private const val PLANNED_TRACK_WIDTH_PX = 16f
        private const val PLANNED_TRACK_COLOR = 0xFF8ABFAE.toInt()
        private const val TRACK_WIDTH_PX = 12f
        private const val TRACK_COLOR = 0xFF3FAF8A.toInt()
    }
}
