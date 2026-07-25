package com.y.citycapsule.core.place

import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError

/**
 * Business boundary for the bounded local catalog.
 *
 * T98-T107 freezes the API only. Its KeyValueStore implementation, seed initialization,
 * serialization of mutations, and recovery behavior are delivered in T108-T112.
 */
interface PlaceRepository {
    fun getCatalog(callback: StorageCallback<PlaceCatalog>)

    fun getCatalogSnapshot(callback: (PlaceCatalogSnapshot) -> Unit)

    fun getPlace(placeId: String, callback: StorageCallback<Place>)

    fun createPlace(draft: PlaceDraft, callback: StorageCallback<Place>)

    fun updatePlace(place: Place, callback: StorageCallback<Place>)

    fun deletePlace(placeId: String, callback: StorageCallback<Unit>)
}

enum class PlaceCatalogSource {
    PERSISTED,
    INITIALIZED,
    MEMORY_FALLBACK,
    RECOVERY_READ_ONLY
}

data class PlaceCatalogSnapshot(
    val catalog: PlaceCatalog,
    val source: PlaceCatalogSource,
    val warning: StorageError? = null
)

fun interface PlaceIdGenerator {
    fun newId(): String
}

fun interface PlaceClock {
    fun nowEpochMs(): Long
}
