package com.y.citycapsule.core.route

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object LocalRouteCatalogCodec : StorageCodec<LocalRouteCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT
    override fun encode(value: LocalRouteCatalog): String {
        require(value.schemaVersion == LocalRouteContract.SCHEMA_VERSION)
        val routes = value.routes.map { requireNotNull(LocalRouteValidator.normalizeOrNull(it)) }
        require(routes.size <= LocalRouteContract.MAX_ROUTES && routes.map(LocalRoute::id).distinct().size == routes.size)
        return JSONObject().apply {
            put("schemaVersion", LocalRouteContract.SCHEMA_VERSION)
            put("routes", JSONArray().apply { routes.forEach { route -> put(JSONObject().apply {
                put("id", route.id); put("name", route.name); put("orderedPlaceIds", strings(route.orderedPlaceIds)); put("createdAtEpochMs", route.createdAtEpochMs)
                route.plannedRoute?.let { plan -> put("plannedRoute", JSONObject().apply {
                    put("orderedPlaceIds", strings(plan.orderedPlaceIds)); put("distanceMeters", plan.distanceMeters)
                    put("durationSeconds", plan.durationSeconds); put("points", points(plan.points))
                }) }
            }) }})
        }.toString()
    }
    override fun decode(encoded: String): LocalRouteCatalog? { return try {
        val root = JSONObject(encoded); val schema = root.optInt("schemaVersion", -1)
        if (schema !in 1..LocalRouteContract.SCHEMA_VERSION) return null
        val array = root.optJSONArray("routes") ?: return null
        if (array.length() > LocalRouteContract.MAX_ROUTES) return null
        val routes = buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: return null
                val ids = readStrings(json.optJSONArray("orderedPlaceIds") ?: return null) ?: return null
                val plan = if (schema >= 2 && json.has("plannedRoute")) decodePlan(json.optJSONObject("plannedRoute") ?: return null) ?: return null else null
                add(LocalRouteValidator.normalizeOrNull(LocalRoute(json.optString("id"), json.optString("name"), ids,
                    json.optString("createdAtEpochMs").toLongOrNull() ?: return null, plan)) ?: return null)
            }
        }
        if (routes.map(LocalRoute::id).distinct().size != routes.size) return null
        LocalRouteCatalog(routes = routes)
    } catch (_: Throwable) { null } }
    private fun decodePlan(json: JSONObject): PlannedRouteSnapshot? {
        val ids = readStrings(json.optJSONArray("orderedPlaceIds") ?: return null) ?: return null
        val array = json.optJSONArray("points") ?: return null
        val routePoints = buildList { for (index in 0 until array.length()) { val point = array.optJSONObject(index) ?: return null; add(GeoPoint(point.optDouble("latitude"), point.optDouble("longitude"))) } }
        return PlannedRouteSnapshot(ids, json.optString("distanceMeters").toLongOrNull() ?: return null,
            json.optString("durationSeconds").toLongOrNull() ?: return null, routePoints)
    }
    private fun strings(values: List<String>) = JSONArray().apply { values.forEach(::put) }
    private fun points(values: List<GeoPoint>) = JSONArray().apply { values.forEach { put(JSONObject().apply { put("latitude", it.latitude); put("longitude", it.longitude) }) } }
    private fun readStrings(array: JSONArray): List<String>? = buildList { for (index in 0 until array.length()) add(array.optString(index) ?: return null) }
}
