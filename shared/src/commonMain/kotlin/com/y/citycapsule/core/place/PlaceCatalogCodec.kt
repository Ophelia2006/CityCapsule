package com.y.citycapsule.core.place

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object PlaceCatalogCodec : StorageCodec<PlaceCatalog> {
    override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

    override fun encode(value: PlaceCatalog): String {
        val catalog = requireNotNull(PlaceCatalogValidator.normalizeOrNull(value)) {
            "Place catalog does not satisfy schema v2 validation."
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
            val encodedSchema = json.optInt(
                PlaceContract.FIELD_SCHEMA_VERSION,
                UNSUPPORTED_SCHEMA
            )
            if (encodedSchema != PlaceContract.LEGACY_SCHEMA_VERSION &&
                encodedSchema != PlaceContract.SCHEMA_VERSION
            ) {
                return null
            }
            val placeArray = json.optJSONArray(PlaceContract.FIELD_PLACES) ?: return null
            val places = mutableListOf<Place>()
            for (index in 0 until placeArray.length()) {
                val placeJson = placeArray.optJSONObject(index) ?: return null
                places.add(placeJson.toPlaceOrNull(encodedSchema) ?: return null)
            }
            val encodedSeedVersion = json.optInt(
                PlaceContract.FIELD_SEED_VERSION,
                INVALID_SEED_VERSION
            )
            val migratedPlaces = if (encodedSeedVersion < PlaceContract.CURRENT_SEED_VERSION) {
                places.map { place ->
                    val currentSeed = PlaceSeedData.BY_ID[place.id]
                    if (place.source == PlaceSource.SEED && place.geoPoint == null) {
                        place.copy(geoPoint = currentSeed?.geoPoint)
                    } else {
                        place
                    }
                }
            } else {
                places
            }
            PlaceCatalogValidator.normalizeOrNull(
                PlaceCatalog(
                    schemaVersion = PlaceContract.SCHEMA_VERSION,
                    seedVersion = maxOf(encodedSeedVersion, PlaceContract.CURRENT_SEED_VERSION),
                    places = migratedPlaces
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
        put(PlaceContract.FIELD_SOURCE, source.wireValue)
        geoPoint?.let { point ->
            put(PlaceContract.FIELD_GEO_POINT, JSONObject().apply {
                put(PlaceContract.FIELD_LATITUDE, point.latitude)
                put(PlaceContract.FIELD_LONGITUDE, point.longitude)
            })
        }
        visualRef?.let { ref ->
            put(PlaceContract.FIELD_VISUAL_REF, JSONObject().apply {
                put(PlaceContract.FIELD_VISUAL_TYPE, ref.type.wireValue)
                put(PlaceContract.FIELD_VISUAL_VALUE, ref.value)
            })
        }
        put(PlaceContract.FIELD_CREATED_AT_EPOCH_MS, createdAtEpochMs)
        put(PlaceContract.FIELD_UPDATED_AT_EPOCH_MS, updatedAtEpochMs)
    }

    private fun JSONObject.toPlaceOrNull(catalogSchema: Int): Place? {
        if (optInt(PlaceContract.FIELD_SCHEMA_VERSION, UNSUPPORTED_SCHEMA) != catalogSchema) {
            return null
        }
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
        val id = optString(PlaceContract.FIELD_ID)
        val source = if (catalogSchema == PlaceContract.LEGACY_SCHEMA_VERSION) {
            if (id in PlaceSeedData.IDS) PlaceSource.SEED else PlaceSource.USER
        } else {
            PlaceSource.fromWireValue(optString(PlaceContract.FIELD_SOURCE)) ?: return null
        }
        val geoPoint = if (has(PlaceContract.FIELD_GEO_POINT)) {
            val json = optJSONObject(PlaceContract.FIELD_GEO_POINT) ?: return null
            GeoPoint(
                latitude = json.optString(PlaceContract.FIELD_LATITUDE)
                    .toDoubleOrNull() ?: return null,
                longitude = json.optString(PlaceContract.FIELD_LONGITUDE)
                    .toDoubleOrNull() ?: return null
            )
        } else {
            null
        }
        val visualRef = if (has(PlaceContract.FIELD_VISUAL_REF)) {
            val json = optJSONObject(PlaceContract.FIELD_VISUAL_REF) ?: return null
            PlaceVisualRef(
                type = PlaceVisualType.fromWireValue(
                    json.optString(PlaceContract.FIELD_VISUAL_TYPE)
                ) ?: return null,
                value = json.optString(PlaceContract.FIELD_VISUAL_VALUE)
            )
        } else {
            null
        }
        return PlaceValidator.normalizeOrNull(
            Place(
                schemaVersion = PlaceContract.SCHEMA_VERSION,
                id = id,
                name = optString(PlaceContract.FIELD_NAME),
                city = optString(PlaceContract.FIELD_CITY),
                district = optionalString(PlaceContract.FIELD_DISTRICT),
                category = category,
                address = optionalString(PlaceContract.FIELD_ADDRESS),
                tags = tags,
                note = optionalString(PlaceContract.FIELD_NOTE),
                source = source,
                geoPoint = geoPoint,
                visualRef = visualRef,
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
