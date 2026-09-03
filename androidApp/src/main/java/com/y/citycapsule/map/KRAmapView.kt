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
            map.clear()
            val markers = state.optJSONArray("markers")
            if (markers != null) {
                for (index in 0 until markers.length()) {
                    val marker = markers.getJSONObject(index)
                    val position = toAmap(
                        marker.getDouble("latitude"),
                        marker.getDouble("longitude")
                    )
                    map.addMarker(
                        MarkerOptions().position(position).title(marker.optString("title"))
                    )?.`object` = marker.getString("placeId")
                }
            }
            if (state.optBoolean("showCurrentLocation")) {
                state.optJSONObject("currentLocation")?.let { point ->
                    map.addMarker(
                        MarkerOptions()
                            .position(toAmap(
                                point.getDouble("latitude"),
                                point.getDouble("longitude")
                            ))
                            .title("当前位置")
                            .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE
                            ))
                    )
                }
            }
            state.optJSONArray("plannedTrackPoints")?.let { points ->
                val converted = buildList { for (index in 0 until points.length()) { val point = points.getJSONObject(index); add(toAmap(point.getDouble("latitude"), point.getDouble("longitude"))) } }
                if (converted.size >= 2) map.addPolyline(PolylineOptions().addAll(converted).width(18f).color(0xFF9A9A9A.toInt()))
            }
            state.optJSONArray("trackPoints")?.let { points ->
                val converted = buildList {
                    for (index in 0 until points.length()) {
                        val point = points.getJSONObject(index)
                        add(toAmap(point.getDouble("latitude"), point.getDouble("longitude")))
                    }
                }
                if (converted.size >= 2) {
                    map.addPolyline(
                        PolylineOptions()
                            .addAll(converted)
                            .width(TRACK_WIDTH_PX)
                            .color(TRACK_COLOR)
                    )
                }
            }
            state.optJSONObject("camera")?.let { camera ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    toAmap(camera.getDouble("latitude"), camera.getDouble("longitude")),
                    camera.optDouble("zoom", DEFAULT_ZOOM.toDouble()).toFloat()
                ))
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
        Log.d(TAG, "AMap host added to parent")
        mapView?.onResume()
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
        private const val TRACK_WIDTH_PX = 12f
        private const val TRACK_COLOR = 0xFFFF9F1C.toInt()
    }
}
