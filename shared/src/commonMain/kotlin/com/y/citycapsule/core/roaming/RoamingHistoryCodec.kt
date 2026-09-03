package com.y.citycapsule.core.roaming

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.checkin.CheckInMethod
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object RoamingHistoryCodec : StorageCodec<RoamingHistoryCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: RoamingHistoryCatalog): String {
        require(value.schemaVersion == RoamingHistoryContract.SCHEMA_VERSION)
        require(value.records.size <= RoamingHistoryContract.MAX_RECORDS)
        val records = value.records.map { requireNotNull(RoamingHistoryValidator.normalizeOrNull(it)) }
        require(records.map(RoamingRecord::id).distinct().size == records.size)
        return JSONObject().apply {
            put("schemaVersion", RoamingHistoryContract.SCHEMA_VERSION)
            put("records", JSONArray().apply { records.forEach { put(encodeRecord(it)) } })
        }.toString()
    }

    override fun decode(encoded: String): RoamingHistoryCatalog? {
        return try {
            val root = JSONObject(encoded)
            val schema = root.optInt("schemaVersion", -1)
            if (schema !in 1..RoamingHistoryContract.SCHEMA_VERSION) return null
            val array = root.optJSONArray("records") ?: return null
            if (array.length() > RoamingHistoryContract.MAX_RECORDS) return null
            val records = mutableListOf<RoamingRecord>()
            for (index in 0 until array.length()) records += decodeRecord(array.optJSONObject(index) ?: return null, schema) ?: return null
            if (records.map(RoamingRecord::id).distinct().size != records.size) return null
            RoamingHistoryCatalog(records = records)
        } catch (_: Throwable) { null }
    }

    private fun encodeRecord(value: RoamingRecord) = JSONObject().apply {
        put("id", value.id); put("mode", value.mode.wireValue)
        value.routeId?.let { put("routeId", it) }; value.routeName?.let { put("routeName", it) }
        put("orderedPlaceIds", JSONArray().apply { value.orderedPlaceIds.forEach(::put) })
        put("startedAtEpochMs", value.startedAtEpochMs); put("endedAtEpochMs", value.endedAtEpochMs)
        value.distanceMeters?.let { put("distanceMeters", it) }
        value.plannedDistanceMeters?.let { put("plannedDistanceMeters", it) }
        value.plannedDurationSeconds?.let { put("plannedDurationSeconds", it) }
        put("plannedTrackPoints", JSONArray().apply { value.plannedTrackPoints.forEach { point -> put(JSONObject().apply { put("latitude", point.latitude); put("longitude", point.longitude) }) } })
        put("trackChunkPaths", JSONArray().apply { value.trackChunkPaths.forEach(::put) })
        put("visits", JSONArray().apply { value.visits.forEach { visit -> put(JSONObject().apply {
            put("placeId", visit.place.placeId); put("name", visit.place.name); put("city", visit.place.city)
            visit.place.district?.let { put("district", it) }
            put("checkedInAtEpochMs", visit.checkedInAtEpochMs); put("method", visit.method.wireValue)
            visit.distanceMeters?.let { put("distanceMeters", it) }
            visit.wasWantTo?.let { put("wasWantTo", it) }
        }) } })
    }

    private fun decodeRecord(json: JSONObject, schema: Int): RoamingRecord? {
        val idsJson = json.optJSONArray("orderedPlaceIds") ?: return null
        val ids = mutableListOf<String>()
        for (index in 0 until idsJson.length()) ids += idsJson.optString(index) ?: return null
        val chunksJson = json.optJSONArray("trackChunkPaths") ?: return null
        val chunks = mutableListOf<String>()
        for (index in 0 until chunksJson.length()) chunks += chunksJson.optString(index) ?: return null
        val visitsJson = json.optJSONArray("visits") ?: return null
        val visits = buildList {
            for (index in 0 until visitsJson.length()) {
                val visit = visitsJson.optJSONObject(index) ?: return null
                add(RoamingVisit(
                    place = RoamingPlaceSnapshot(visit.optString("placeId"), visit.optString("name"), visit.optString("city"), visit.optString("district").takeIf(String::isNotBlank)),
                    checkedInAtEpochMs = visit.optString("checkedInAtEpochMs").toLongOrNull() ?: return null,
                    method = CheckInMethod.fromWireValue(visit.optString("method")) ?: return null,
                    distanceMeters = if (visit.has("distanceMeters")) visit.optDouble("distanceMeters") else null,
                    wasWantTo = if (schema >= 2 && visit.has("wasWantTo")) visit.optBoolean("wasWantTo") else null
                ))
            }
        }
        val plannedPoints = buildList {
            val array = json.optJSONArray("plannedTrackPoints")
            if (array != null) for (index in 0 until array.length()) {
                val point = array.optJSONObject(index) ?: return null
                add(GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")))
            }
        }
        return RoamingHistoryValidator.normalizeOrNull(RoamingRecord(
            id = json.optString("id"), mode = RoamingMode.fromWireValue(json.optString("mode")) ?: return null,
            routeId = json.optString("routeId").takeIf(String::isNotBlank), routeName = json.optString("routeName").takeIf(String::isNotBlank),
            orderedPlaceIds = ids, startedAtEpochMs = json.optString("startedAtEpochMs").toLongOrNull() ?: return null,
            endedAtEpochMs = json.optString("endedAtEpochMs").toLongOrNull() ?: return null,
            distanceMeters = if (json.has("distanceMeters")) json.optDouble("distanceMeters") else null,
            trackChunkPaths = chunks, visits = visits,
            plannedDistanceMeters = if (json.has("plannedDistanceMeters")) json.optString("plannedDistanceMeters").toLongOrNull() ?: return null else null,
            plannedDurationSeconds = if (json.has("plannedDurationSeconds")) json.optString("plannedDurationSeconds").toLongOrNull() ?: return null else null,
            plannedTrackPoints = plannedPoints
        ))
    }
}
