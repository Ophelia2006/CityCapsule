package com.y.citycapsule.core.route

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.y.citycapsule.core.place.ChinaCoordinate
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.KuiklyPlaceNetworkModule

data class WalkingLeg(
    val fromPlaceId: String,
    val toPlaceId: String,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val points: List<GeoPoint>
)

data class PlannedWalkingRoute(
    val orderedPlaceIds: List<String>,
    val legs: List<WalkingLeg>
) {
    val distanceMeters: Long get() = legs.sumOf(WalkingLeg::distanceMeters)
    val durationSeconds: Long get() = legs.sumOf(WalkingLeg::durationSeconds)
    val points: List<GeoPoint> get() = legs.flatMapIndexed { index, leg ->
        if (index == 0) leg.points else leg.points.drop(1)
    }
}

sealed interface WalkingLegResult {
    data class Success(val leg: WalkingLeg) : WalkingLegResult
    data class Failure(val message: String) : WalkingLegResult
    data object Unavailable : WalkingLegResult
}

interface RoutePlanningRemoteDataSource {
    fun walkingLeg(
        fromPlaceId: String,
        from: GeoPoint,
        toPlaceId: String,
        to: GeoPoint,
        callback: (WalkingLegResult) -> Unit
    )
}

class AmapRoutePlanningRemoteDataSource(private val pager: Pager) : RoutePlanningRemoteDataSource {
    override fun walkingLeg(fromPlaceId: String, from: GeoPoint, toPlaceId: String, to: GeoPoint, callback: (WalkingLegResult) -> Unit) {
        val origin = ChinaCoordinate.wgs84ToGcj02(from)
        val destination = ChinaCoordinate.wgs84ToGcj02(to)
        val request = JSONObject().apply {
            put("origin", "${origin.longitude},${origin.latitude}")
            put("destination", "${destination.longitude},${destination.latitude}")
        }
        try {
            pager.acquireModule<KuiklyPlaceNetworkModule>(MODULE_NAME).request(METHOD_WALKING, request) { response ->
                callback(parseWalkingResponse(response, fromPlaceId, toPlaceId))
            }
        } catch (_: Throwable) {
            callback(WalkingLegResult.Unavailable)
        }
    }

    internal companion object {
        const val MODULE_NAME = "CCPlaceNetworkModule"
        const val METHOD_WALKING = "walkingRoute"

        fun parseWalkingResponse(response: JSONObject?, fromPlaceId: String, toPlaceId: String): WalkingLegResult {
            if (response == null || response.optString("status") == "unavailable") return WalkingLegResult.Unavailable
            if (response.optString("status") != "success") {
                return WalkingLegResult.Failure(response.optString("message").ifBlank { "步行路线请求失败。" })
            }
            return try {
                val root = JSONObject(response.optString("body"))
                if (root.optString("status") != "1") {
                    return WalkingLegResult.Failure(root.optString("info").ifBlank { "高德没有返回可用步行路线。" })
                }
                val path = root.optJSONObject("route")?.optJSONArray("paths")?.optJSONObject(0)
                    ?: return WalkingLegResult.Failure("没有找到可步行的道路路线。")
                val points = buildList {
                    val steps = path.optJSONArray("steps")
                    if (steps != null) for (index in 0 until steps.length()) {
                        val polyline = steps.optJSONObject(index)?.optString("polyline").orEmpty()
                        polyline.split(';').forEach { encoded ->
                            val pair = encoded.split(',')
                            if (pair.size == 2) {
                                val longitude = pair[0].toDoubleOrNull()
                                val latitude = pair[1].toDoubleOrNull()
                                if (longitude != null && latitude != null) {
                                    val wgs = ChinaCoordinate.gcj02ToWgs84(GeoPoint(latitude, longitude))
                                    if (lastOrNull() != wgs) add(wgs)
                                }
                            }
                        }
                    }
                }
                if (points.size < 2) return WalkingLegResult.Failure("路线缺少可显示的道路折线。")
                WalkingLegResult.Success(
                    WalkingLeg(
                        fromPlaceId,
                        toPlaceId,
                        path.optString("distance").toLongOrNull() ?: 0L,
                        path.optString("duration").toLongOrNull() ?: 0L,
                        points
                    )
                )
            } catch (_: Throwable) {
                WalkingLegResult.Failure("路线服务返回了无法识别的数据。")
            }
        }
    }
}

object RouteOrderOptimizer {
    const val MAX_OPTIMIZED_PLACES = 8

    fun recommend(placeIds: List<String>, distances: Map<Pair<String, String>, Long>): List<String>? {
        if (placeIds.size < 2 || placeIds.size > MAX_OPTIMIZED_PLACES) return null
        val remaining = placeIds.drop(1).toMutableSet()
        val ordered = mutableListOf(placeIds.first())
        while (remaining.isNotEmpty()) {
            val next = remaining.minWithOrNull(compareBy<String> { id -> distance(distances, ordered.last(), id) }.thenBy { it })
                ?: return null
            if (distance(distances, ordered.last(), next) == Long.MAX_VALUE) return null
            ordered += next
            remaining -= next
        }
        var improved = true
        while (improved) {
            improved = false
            outer@ for (from in 1 until ordered.lastIndex) for (to in from + 1..ordered.lastIndex) {
                val candidate = ordered.toMutableList().apply { subList(from, to + 1).reverse() }
                if (cost(candidate, distances) < cost(ordered, distances)) {
                    ordered.clear(); ordered.addAll(candidate); improved = true; break@outer
                }
            }
        }
        return ordered
    }

    private fun cost(ids: List<String>, distances: Map<Pair<String, String>, Long>): Long =
        ids.zipWithNext().sumOf { (from, to) -> distance(distances, from, to) }

    private fun distance(values: Map<Pair<String, String>, Long>, from: String, to: String): Long =
        values[from to to] ?: values[to to from] ?: Long.MAX_VALUE
}
