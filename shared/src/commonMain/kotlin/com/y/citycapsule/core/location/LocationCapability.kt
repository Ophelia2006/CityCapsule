package com.y.citycapsule.core.location

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.y.citycapsule.core.place.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

sealed interface LocationResult {
    data class Success(
        val point: GeoPoint,
        val accuracyMeters: Double? = null
    ) : LocationResult

    data object PermissionDenied : LocationResult
    data object PermissionPermanentlyDenied : LocationResult
    data object ServiceDisabled : LocationResult
    data object Unavailable : LocationResult
    data class Failure(val message: String) : LocationResult
}

fun interface LocationCapability {
    fun getCurrentLocation(callback: (LocationResult) -> Unit)
}

/** Process-only location shared after an explicit user request; never persisted. */
object CurrentLocationRuntime {
    var point: GeoPoint? by mutableStateOf(null)
        private set
    var revision: Long by mutableStateOf(0L)
        private set

    fun update(value: GeoPoint?) {
        point = value
        revision += 1L
    }
}

object GeoDistance {
    fun meters(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = from.latitude.toRadians()
        val lat2 = to.latitude.toRadians()
        val latDelta = (to.latitude - from.latitude).toRadians()
        val lonDelta = (to.longitude - from.longitude).toRadians()
        val a = sin(latDelta / 2) * sin(latDelta / 2) +
            cos(lat1) * cos(lat2) * sin(lonDelta / 2) * sin(lonDelta / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun label(meters: Double): String = if (meters < 1_000) {
        "${meters.toInt().coerceAtLeast(0)} m"
    } else {
        val tenths = (meters / 100).toInt() / 10.0
        "$tenths km"
    }

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    private fun Double.toRadians(): Double = this * PI / 180.0
}

class KuiklyLocationCapability(pager: Pager) : LocationCapability {
    private val transport: LocationTransport = PagerLocationTransport(pager)

    override fun getCurrentLocation(callback: (LocationResult) -> Unit) {
        try {
            transport.request(JSONObject()) { response -> callback(parse(response)) }
        } catch (_: Throwable) {
            callback(LocationResult.Unavailable)
        }
    }

    internal companion object {
        const val MODULE_NAME = "CCLocationModule"
        const val METHOD_GET_CURRENT_LOCATION = "getCurrentLocation"

        fun parse(response: JSONObject?): LocationResult {
            if (response == null) return LocationResult.Failure("定位没有返回结果。")
            return when (response.optString("status")) {
                "success" -> {
                    if (!response.has("latitude") || !response.has("longitude")) {
                        return LocationResult.Failure("定位结果无效。")
                    }
                    val latitude = response.optDouble("latitude")
                    val longitude = response.optDouble("longitude")
                    if (!latitude.isFinite() || !longitude.isFinite() ||
                        latitude !in -90.0..90.0 || longitude !in -180.0..180.0
                    ) {
                        LocationResult.Failure("定位结果无效。")
                    } else {
                        val accuracy = response.takeIf { it.has("accuracyMeters") }
                            ?.optDouble("accuracyMeters")
                            ?.takeIf { it.isFinite() && it >= 0.0 }
                        LocationResult.Success(GeoPoint(latitude, longitude), accuracy)
                    }
                }
                "permission_denied" -> LocationResult.PermissionDenied
                "permission_permanently_denied" -> LocationResult.PermissionPermanentlyDenied
                "service_disabled" -> LocationResult.ServiceDisabled
                "unavailable" -> LocationResult.Unavailable
                else -> LocationResult.Failure(
                    response.optString("message").ifBlank { "暂时无法获取当前位置。" }
                )
            }
        }
    }
}

internal interface LocationTransport {
    fun request(request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal class KuiklyLocationModule : Module() {
    override fun moduleName(): String = KuiklyLocationCapability.MODULE_NAME
    fun request(request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(KuiklyLocationCapability.METHOD_GET_CURRENT_LOCATION, request, callback)
    }
}

private class PagerLocationTransport(private val pager: Pager) : LocationTransport {
    override fun request(request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyLocationModule>(KuiklyLocationCapability.MODULE_NAME)
            .request(request, callback)
    }
}
