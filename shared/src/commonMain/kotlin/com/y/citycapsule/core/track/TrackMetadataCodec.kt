package com.y.citycapsule.core.track

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object TrackMetadataCodec : StorageCodec<TrackMetadata> {
    override val valueType = StorageValueType.JSON_OBJECT
    override fun encode(value: TrackMetadata): String = JSONObject().apply {
        put("schemaVersion", 1); put("sessionStartedAtEpochMs", value.sessionStartedAtEpochMs)
        put("chunkPaths", JSONArray().apply { value.chunkPaths.forEach(::put) }); put("pointCount", value.pointCount)
        put("status", value.status.wireValue); value.interruptionReason?.let { put("interruptionReason", it) }
        value.lastPointAtEpochMs?.let { put("lastPointAtEpochMs", it) }
    }.toString()
    override fun decode(encoded: String): TrackMetadata? { return try { val j = JSONObject(encoded); if (j.optInt("schemaVersion", -1) != 1) return null; val a = j.optJSONArray("chunkPaths") ?: return null; val paths = mutableListOf<String>(); for (i in 0 until a.length()) paths += a.optString(i) ?: return null; if (paths.size > TrackContract.MAX_CHUNKS) return null; TrackMetadata(j.optString("sessionStartedAtEpochMs").toLongOrNull() ?: return null, paths, j.optInt("pointCount", -1).takeIf { it >= 0 } ?: return null, TrackStatus.fromWireValue(j.optString("status")) ?: return null, if (j.has("interruptionReason")) j.optString("interruptionReason") else null, if (j.has("lastPointAtEpochMs")) j.optString("lastPointAtEpochMs").toLongOrNull() ?: return null else null) } catch (_: Throwable) { null } }
}
