package com.y.citycapsule.core.place

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object PlaceCatalogCodec : StorageCodec<PlaceCatalog> {
    override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

    override fun encode(value: PlaceCatalog): String {
        val catalog = requireNotNull(PlaceCatalogValidator.normalizeOrNull(value)) {
            "Place catalog does not satisfy schema v1 validation."
        }
        return JSONObject().apply {
            put(PlaceContract.FIELD_SCHEMA_VERSION, catalog.schemaVersion)
            put(PlaceContract.FIELD_SEED_VERSION, catalog.seedVersion)
            put(
                PlaceContract.FIELD_PLACES,
                JSONArray().also { array ->
                    catalog.places
                        .sortedBy(Place::id)
                        .forEach { place -> array.put(place.toJson()) }
                }
            )
        }.toString()
    }

    override fun decode(encoded: String): PlaceCatalog? {
        return try {
            val json = JSONObject(encoded)
            val placeArray = json.optJSONArray(PlaceContract.FIELD_PLACES) ?: return null
            val places = mutableListOf<Place>()
            for (index in 0 until placeArray.length()) {
                val placeJson = placeArray.optJSONObject(index) ?: return null
                places.add(placeJson.toPlaceOrNull() ?: return null)
            }
            PlaceCatalogValidator.normalizeOrNull(
                PlaceCatalog(
                    schemaVersion = json.optInt(
                        PlaceContract.FIELD_SCHEMA_VERSION,
                        UNSUPPORTED_SCHEMA
                    ),
                    seedVersion = json.optInt(
                        PlaceContract.FIELD_SEED_VERSION,
                        INVALID_SEED_VERSION
                    ),
                    places = places
                )
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun Place.toJson(): JSONObject = JSONObject().apply {
        put(PlaceContract.FIELD_SCHEMA_VERSION, schemaVersion)
        put(PlaceContract.FIELD_ID, id)
        put(PlaceContract.FIELD_NAME, name)
        put(PlaceContract.FIELD_CITY, city)
        district?.let { put(PlaceContract.FIELD_DISTRICT, it) }
        put(PlaceContract.FIELD_CATEGORY, category.wireValue)
        address?.let { put(PlaceContract.FIELD_ADDRESS, it) }
        put(
            PlaceContract.FIELD_TAGS,
            JSONArray().also { array -> tags.forEach { tag -> array.put(tag) } }
        )
        note?.let { put(PlaceContract.FIELD_NOTE, it) }
        put(PlaceContract.FIELD_CREATED_AT_EPOCH_MS, createdAtEpochMs)
        put(PlaceContract.FIELD_UPDATED_AT_EPOCH_MS, updatedAtEpochMs)
    }

    private fun JSONObject.toPlaceOrNull(): Place? {
        val category = PlaceCategory.fromWireValue(
            optString(PlaceContract.FIELD_CATEGORY)
        ) ?: return null
        val tagArray = optJSONArray(PlaceContract.FIELD_TAGS) ?: return null
        val tags = mutableListOf<String>()
        for (index in 0 until tagArray.length()) {
            tags.add(tagArray.optString(index) ?: return null)
        }
        val createdAt = optString(PlaceContract.FIELD_CREATED_AT_EPOCH_MS)
            .toLongOrNull() ?: return null
        val updatedAt = optString(PlaceContract.FIELD_UPDATED_AT_EPOCH_MS)
            .toLongOrNull() ?: return null
        return PlaceValidator.normalizeOrNull(
            Place(
                schemaVersion = optInt(
                    PlaceContract.FIELD_SCHEMA_VERSION,
                    UNSUPPORTED_SCHEMA
                ),
                id = optString(PlaceContract.FIELD_ID),
                name = optString(PlaceContract.FIELD_NAME),
                city = optString(PlaceContract.FIELD_CITY),
                district = optionalString(PlaceContract.FIELD_DISTRICT),
                category = category,
                address = optionalString(PlaceContract.FIELD_ADDRESS),
                tags = tags,
                note = optionalString(PlaceContract.FIELD_NOTE),
                createdAtEpochMs = createdAt,
                updatedAtEpochMs = updatedAt
            )
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key)) optString(key) else null

    private const val UNSUPPORTED_SCHEMA = -1
    private const val INVALID_SEED_VERSION = -1
}
