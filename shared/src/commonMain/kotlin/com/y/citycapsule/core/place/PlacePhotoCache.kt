package com.y.citycapsule.core.place

data class PlacePhotoCacheEntry(
    val placeId: String,
    val url: String,
    val source: String,
    val updatedAtEpochMs: Long
)

data class PlacePhotoCache(
    val schemaVersion: Int = PlacePhotoCacheContract.SCHEMA_VERSION,
    val entries: List<PlacePhotoCacheEntry> = emptyList()
) {
    companion object {
        val EMPTY = PlacePhotoCache()
    }
}

object PlacePhotoCacheContract {
    const val SCHEMA_VERSION = 1
    const val MAX_ENTRIES = 100
    const val MAX_AGE_MS = 30L * 24L * 60L * 60L * 1_000L
    const val SOURCE_AMAP_POI = "高德地图 POI"
}

interface PlacePhotoCacheRepository {
    fun getValid(callback: (com.y.citycapsule.core.storage.StorageResult<Map<String, PlacePhotoCacheEntry>>) -> Unit)
    fun put(placeId: String, url: String, source: String, callback: com.y.citycapsule.core.storage.StorageCallback<Unit>)
    fun remove(placeId: String, callback: com.y.citycapsule.core.storage.StorageCallback<Unit> = {})

    companion object {
        val NONE: PlacePhotoCacheRepository = object : PlacePhotoCacheRepository {
            override fun getValid(callback: (com.y.citycapsule.core.storage.StorageResult<Map<String, PlacePhotoCacheEntry>>) -> Unit) {
                callback(com.y.citycapsule.core.storage.StorageResult.Success(emptyMap()))
            }

            override fun put(placeId: String, url: String, source: String, callback: com.y.citycapsule.core.storage.StorageCallback<Unit>) =
                callback(com.y.citycapsule.core.storage.StorageResult.Success(Unit))

            override fun remove(placeId: String, callback: com.y.citycapsule.core.storage.StorageCallback<Unit>) =
                callback(com.y.citycapsule.core.storage.StorageResult.Success(Unit))
        }
    }
}
