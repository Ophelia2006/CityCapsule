package com.y.citycapsule.core.roaming

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object RoamingSessionCodec : StorageCodec<RoamingSession> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: RoamingSession): String {
        val session = requireNotNull(RoamingSessionValidator.normalizeOrNull(value))
        return JSONObject().apply {
            put("schemaVersion", RoamingSessionContract.SCHEMA_VERSION)
            session.routeId?.let { put("routeId", it) }
            put("startedAtEpochMs", session.startedAtEpochMs)
            session.endedAtEpochMs?.let { put("endedAtEpochMs", it) }
            put("status", session.status.wireValue)
        }.toString()
    }

    override fun decode(encoded: String): RoamingSession? {
        return try {
            val json = JSONObject(encoded)
            if (json.optInt("schemaVersion", -1) != RoamingSessionContract.SCHEMA_VERSION) null else {
                RoamingSessionValidator.normalizeOrNull(
                    RoamingSession(
                        routeId = if (json.has("routeId")) json.optString("routeId") else null,
                        startedAtEpochMs = json.optString("startedAtEpochMs").toLongOrNull() ?: return null,
                        endedAtEpochMs = if (json.has("endedAtEpochMs")) json.optString("endedAtEpochMs").toLongOrNull() ?: return null else null,
                        status = RoamingStatus.fromWireValue(json.optString("status")) ?: return null
                    )
                )
            }
        } catch (_: Throwable) { null }
    }
}
