package com.y.citycapsule.core.place

/**
 * Stable wire contract for the bounded, local-only place catalog.
 *
 * It deliberately contains no platform URI, map SDK object, account id, image bytes,
 * latitude/longitude, or remote-service identifier.
 */
object PlaceContract {
    const val SCHEMA_VERSION = 1
    const val CURRENT_SEED_VERSION = 1
    const val MAX_CATALOG_SIZE = 500

    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_SEED_VERSION = "seedVersion"
    const val FIELD_PLACES = "places"
    const val FIELD_ID = "id"
    const val FIELD_NAME = "name"
    const val FIELD_CITY = "city"
    const val FIELD_DISTRICT = "district"
    const val FIELD_CATEGORY = "category"
    const val FIELD_ADDRESS = "address"
    const val FIELD_TAGS = "tags"
    const val FIELD_NOTE = "note"
    const val FIELD_CREATED_AT_EPOCH_MS = "createdAtEpochMs"
    const val FIELD_UPDATED_AT_EPOCH_MS = "updatedAtEpochMs"
}

enum class PlaceCategory(val wireValue: String) {
    LANDMARK("landmark"),
    CULTURE("culture"),
    FOOD("food"),
    NATURE("nature"),
    SHOPPING("shopping"),
    OTHER("other");

    companion object {
        fun fromWireValue(value: String): PlaceCategory? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}

data class Place(
    val schemaVersion: Int = PlaceContract.SCHEMA_VERSION,
    val id: String,
    val name: String,
    val city: String,
    val district: String? = null,
    val category: PlaceCategory,
    val address: String? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

/** Input accepted by the future create flow; identity and timestamps belong to the repository. */
data class PlaceDraft(
    val name: String,
    val city: String,
    val district: String? = null,
    val category: PlaceCategory,
    val address: String? = null,
    val tags: List<String> = emptyList(),
    val note: String? = null
)

data class PlaceCatalog(
    val schemaVersion: Int = PlaceContract.SCHEMA_VERSION,
    val seedVersion: Int = PlaceContract.CURRENT_SEED_VERSION,
    val places: List<Place> = emptyList()
) {
    companion object {
        val EMPTY = PlaceCatalog()
    }
}
