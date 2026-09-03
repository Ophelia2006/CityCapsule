package com.y.citycapsule.core.checkin

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object CheckInCodec : StorageCodec<CheckInCatalog> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: CheckInCatalog): String {
        require(value.schemaVersion == 2)
        return JSONObject().apply {
            put("schemaVersion", 2)
            put("sessionStartedAtEpochMs", value.sessionStartedAtEpochMs)
            put("checkIns", JSONArray().apply {
                value.checkIns.forEach { checkIn -> put(JSONObject().apply {
                    put("placeId", checkIn.placeId)
                    put("checkedInAtEpochMs", checkIn.checkedInAtEpochMs)
                    put("method", checkIn.method.wireValue)
                    checkIn.distanceMeters?.let { put("distanceMeters", it) }
                    checkIn.wasWantTo?.let { put("wasWantTo", it) }
                }) }
            })
        }.toString()
    }

    override fun decode(encoded: String): CheckInCatalog? { return try {
        val root = JSONObject(encoded)
        val schema = root.optInt("schemaVersion", -1)
        if (schema !in 1..2) return null
        val array = root.optJSONArray("checkIns") ?: return null
        val values = buildList {
            for (index in 0 until array.length()) {
                val json = array.optJSONObject(index) ?: return null
                add(CheckIn(
                    placeId = json.optString("placeId").takeIf(String::isNotBlank) ?: return null,
                    checkedInAtEpochMs = json.optString("checkedInAtEpochMs").toLongOrNull() ?: return null,
                    method = CheckInMethod.fromWireValue(json.optString("method")) ?: return null,
                    distanceMeters = if (json.has("distanceMeters")) json.optDouble("distanceMeters") else null,
                    wasWantTo = if (schema >= 2 && json.has("wasWantTo")) json.optBoolean("wasWantTo") else null
                ))
            }
        }
        CheckInCatalog(
            schemaVersion = 2,
            sessionStartedAtEpochMs = root.optString("sessionStartedAtEpochMs").toLongOrNull() ?: return null,
            checkIns = values
        )
    } catch (_: Throwable) { null } }
}
