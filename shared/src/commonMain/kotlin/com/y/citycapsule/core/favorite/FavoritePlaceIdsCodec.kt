package com.y.citycapsule.core.favorite

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object FavoritePlaceIdsCodec : StorageCodec<FavoritePlaceIds> {
    override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

    override fun encode(value: FavoritePlaceIds): String {
        val normalized = requireNotNull(normalizeOrNull(value)) {
            "Favorite place ids do not satisfy schema v1 validation."
        }
        return JSONObject().apply {
            put(FavoritePlaceIdsContract.FIELD_SCHEMA_VERSION, normalized.schemaVersion)
            put(
                FavoritePlaceIdsContract.FIELD_PLACE_IDS,
                JSONArray().also { array ->
                    normalized.placeIds.sorted().forEach { id -> array.put(id) }
                }
            )
        }.toString()
    }

    override fun decode(encoded: String): FavoritePlaceIds? {
        return try {
            val json = JSONObject(encoded)
            val idsJson = json.optJSONArray(
                FavoritePlaceIdsContract.FIELD_PLACE_IDS
            ) ?: return null
            val ids = mutableSetOf<String>()
            for (index in 0 until idsJson.length()) {
                ids.add(idsJson.optString(index) ?: return null)
            }
            normalizeOrNull(
                FavoritePlaceIds(
                    schemaVersion = json.optInt(
                        FavoritePlaceIdsContract.FIELD_SCHEMA_VERSION,
                        UNSUPPORTED_SCHEMA
                    ),
                    placeIds = ids
                )
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun normalizeOrNull(value: FavoritePlaceIds): FavoritePlaceIds? {
        if (value.schemaVersion != FavoritePlaceIdsContract.SCHEMA_VERSION ||
            value.placeIds.size > FavoritePlaceIdsContract.MAX_SIZE
        ) {
            return null
        }
        val normalizedIds = value.placeIds.map(String::trim).toSet()
        if (normalizedIds.any { !PlaceValidator.isValidId(it) }) {
            return null
        }
        return value.copy(placeIds = normalizedIds)
    }

    private const val UNSUPPORTED_SCHEMA = -1
}
