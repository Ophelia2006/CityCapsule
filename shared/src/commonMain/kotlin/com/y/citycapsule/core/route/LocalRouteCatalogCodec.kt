package com.y.citycapsule.core.route

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object LocalRouteCatalogCodec : StorageCodec<LocalRouteCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: LocalRouteCatalog): String {
        require(value.schemaVersion == LocalRouteContract.SCHEMA_VERSION)
        require(value.routes.size <= LocalRouteContract.MAX_ROUTES)
        val routes = value.routes.map { requireNotNull(LocalRouteValidator.normalizeOrNull(it)) }
        require(routes.map(LocalRoute::id).distinct().size == routes.size)
        return JSONObject().apply {
            put("schemaVersion", LocalRouteContract.SCHEMA_VERSION)
            put("routes", JSONArray().apply {
                routes.forEach { route -> put(JSONObject().apply {
                    put("id", route.id)
                    put("name", route.name)
                    put("orderedPlaceIds", JSONArray().apply { route.orderedPlaceIds.forEach(::put) })
                    put("createdAtEpochMs", route.createdAtEpochMs)
                }) }
            })
        }.toString()
    }

    override fun decode(encoded: String): LocalRouteCatalog? {
        return try {
            val root = JSONObject(encoded)
            if (root.optInt("schemaVersion", -1) != LocalRouteContract.SCHEMA_VERSION) return null
            val array = root.optJSONArray("routes") ?: return null
            if (array.length() > LocalRouteContract.MAX_ROUTES) return null
            val routes = mutableListOf<LocalRoute>()
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: return null
                val idsJson = json.optJSONArray("orderedPlaceIds") ?: return null
                val ids = mutableListOf<String>()
                for (i in 0 until idsJson.length()) ids += idsJson.optString(i) ?: return null
                routes += LocalRouteValidator.normalizeOrNull(LocalRoute(
                    id = json.optString("id"), name = json.optString("name"), orderedPlaceIds = ids,
                    createdAtEpochMs = json.optString("createdAtEpochMs").toLongOrNull() ?: return null
                )) ?: return null
            }
            if (routes.map(LocalRoute::id).distinct().size != routes.size) return null
            LocalRouteCatalog(routes = routes)
        } catch (_: Throwable) { null }
    }
}
