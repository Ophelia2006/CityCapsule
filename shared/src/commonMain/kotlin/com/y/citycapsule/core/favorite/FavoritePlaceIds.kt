package com.y.citycapsule.core.favorite

object FavoritePlaceIdsContract {
    const val SCHEMA_VERSION = 1
    const val MAX_SIZE = 500

    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_PLACE_IDS = "placeIds"
}

data class FavoritePlaceIds(
    val schemaVersion: Int = FavoritePlaceIdsContract.SCHEMA_VERSION,
    val placeIds: Set<String> = emptySet()
) {
    companion object {
        val EMPTY = FavoritePlaceIds()
    }
}
