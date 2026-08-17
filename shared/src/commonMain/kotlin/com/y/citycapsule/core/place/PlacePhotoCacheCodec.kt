package com.y.citycapsule.core.place

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object PlacePhotoCacheCodec : StorageCodec<PlacePhotoCache> {
    override val valueType = StorageValueType.JSON_OBJECT

    override fun encode(value: PlacePhotoCache): String {
        require(value.schemaVersion == PlacePhotoCacheContract.SCHEMA_VERSION)
        require(value.entries.size <= PlacePhotoCacheContract.MAX_ENTRIES)
        return JSONObject().apply {
            put("schemaVersion", value.schemaVersion)
            put("entries", JSONArray().apply {
                value.entries.forEach { entry ->
                    put(JSONObject().apply {
                        put("placeId", entry.placeId)
                        put("url", entry.url)
                        put("source", entry.source)
                        put("updatedAtEpochMs", entry.updatedAtEpochMs.toString())
                    })
                }
            })
        }.toString()
    }

    override fun decode(encoded: String): PlacePhotoCache? {
        return try {
            val root = JSONObject(encoded)
            if (root.optInt("schemaVersion", -1) != PlacePhotoCacheContract.SCHEMA_VERSION) return null
            val array = root.optJSONArray("entries") ?: return null
            if (array.length() > PlacePhotoCacheContract.MAX_ENTRIES) return null
            val entries = buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: return null
                    val entry = PlacePhotoCacheEntry(
                        placeId = item.optString("placeId").trim(),
                        url = item.optString("url").trim(),
                        source = item.optString("source").trim(),
                        updatedAtEpochMs = item.optString("updatedAtEpochMs").toLongOrNull() ?: return null
                    )
                    if (!entry.isValid()) return null
                    add(entry)
                }
            }
            if (entries.map(PlacePhotoCacheEntry::placeId).distinct().size != entries.size) return null
            PlacePhotoCache(entries = entries)
        } catch (_: Throwable) {
            null
        }
    }

    internal fun PlacePhotoCacheEntry.isValid(): Boolean =
        placeId.isNotBlank() && source.isNotBlank() && updatedAtEpochMs >= 0L &&
            (url.startsWith("https://") || url.startsWith("http://"))
}
