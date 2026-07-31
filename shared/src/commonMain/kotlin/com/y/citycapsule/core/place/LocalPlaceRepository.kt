package com.y.citycapsule.core.place

import com.tencent.kuikly.core.datetime.DateTime
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

class LocalPlaceRepository(
    private val storage: KeyValueStore,
    private val seedCatalog: PlaceCatalog = PlaceSeedData.CATALOG,
    private val clock: PlaceClock = SystemPlaceClock,
    private val idGenerator: PlaceIdGenerator = TimestampPlaceIdGenerator(clock)
) : PlaceRepository {
    private val mutationQueue = mutableListOf<(() -> Unit) -> Unit>()
    private var mutationInFlight = false

    override fun getCatalog(callback: StorageCallback<PlaceCatalog>) {
        storage.get(AppStorageKeys.Places.CATALOG) { result ->
            when (result) {
                is StorageResult.Success -> callback(result)
                StorageResult.Missing -> initializeCatalog(callback)
                is StorageResult.Failure -> callback(result)
            }
        }
    }

    override fun getCatalogSnapshot(callback: (PlaceCatalogSnapshot) -> Unit) {
        storage.get(AppStorageKeys.Places.CATALOG) { result ->
            when (result) {
                is StorageResult.Success -> callback(
                    PlaceCatalogSnapshot(
                        catalog = result.value,
                        source = PlaceCatalogSource.PERSISTED
                    )
                )
                StorageResult.Missing -> storage.put(
                    AppStorageKeys.Places.CATALOG,
                    seedCatalog
                ) { writeResult ->
                    when (writeResult) {
                        is StorageResult.Success -> callback(
                            PlaceCatalogSnapshot(
                                catalog = seedCatalog,
                                source = PlaceCatalogSource.INITIALIZED
                            )
                        )
                        StorageResult.Missing -> callback(
                            PlaceCatalogSnapshot(
                                catalog = seedCatalog,
                                source = PlaceCatalogSource.MEMORY_FALLBACK,
                                warning = nativeFailure("Catalog initialization was not confirmed.")
                            )
                        )
                        is StorageResult.Failure -> callback(
                            PlaceCatalogSnapshot(
                                catalog = seedCatalog,
                                source = PlaceCatalogSource.MEMORY_FALLBACK,
                                warning = writeResult.error
                            )
                        )
                    }
                }
                is StorageResult.Failure -> {
                    val deterministic = result.error.code == StorageErrorCode.TYPE_MISMATCH ||
                        result.error.code == StorageErrorCode.DECODE_FAILED
                    callback(
                        PlaceCatalogSnapshot(
                            catalog = if (deterministic) PlaceCatalog.EMPTY else seedCatalog,
                            source = if (deterministic) {
                                PlaceCatalogSource.RECOVERY_READ_ONLY
                            } else {
                                PlaceCatalogSource.MEMORY_FALLBACK
                            },
                            warning = result.error
                        )
                    )
                }
            }
        }
    }

    override fun getPlace(placeId: String, callback: StorageCallback<Place>) {
        val normalizedId = placeId.trim()
        if (!PlaceValidator.isValidId(normalizedId)) {
            callback(invalidRequest("Place id is invalid."))
            return
        }
        getCatalog { result ->
            when (result) {
                is StorageResult.Success -> {
                    val place = result.value.places.firstOrNull { it.id == normalizedId }
                    callback(
                        if (place != null) {
                            StorageResult.Success(place)
                        } else {
                            StorageResult.Missing
                        }
                    )
                }
                StorageResult.Missing -> callback(StorageResult.Missing)
                is StorageResult.Failure -> callback(result)
            }
        }
    }

    override fun createPlace(
        draft: PlaceDraft,
        callback: StorageCallback<Place>
    ) {
        val normalizedDraft = PlaceValidator.normalizeDraftOrNull(draft)
        if (normalizedDraft == null) {
            callback(invalidRequest("Place draft does not satisfy schema v2 validation."))
            return
        }
        enqueueMutation { complete ->
            getCatalog { result ->
                when (result) {
                    is StorageResult.Success -> {
                        if (result.value.places.size >= PlaceContract.MAX_CATALOG_SIZE) {
                            deliver(
                                callback,
                                invalidRequest("Place catalog reached its frozen size limit."),
                                complete
                            )
                            return@getCatalog
                        }
                        val now = clock.nowEpochMs()
                        val id = generateUniqueId(result.value)
                        val place = if (now >= 0L && id != null) {
                            PlaceValidator.normalizeOrNull(
                                Place(
                                    id = id,
                                    name = normalizedDraft.name,
                                    city = normalizedDraft.city,
                                    district = normalizedDraft.district,
                                    category = normalizedDraft.category,
                                    address = normalizedDraft.address,
                                    tags = normalizedDraft.tags,
                                    note = normalizedDraft.note,
                                    source = PlaceSource.USER,
                                    createdAtEpochMs = now,
                                    updatedAtEpochMs = now
                                )
                            )
                        } else {
                            null
                        }
                        if (place == null) {
                            deliver(
                                callback,
                                invalidRequest("Place identity or timestamp could not be generated."),
                                complete
                            )
                            return@getCatalog
                        }
                        val updated = result.value.copy(
                            places = result.value.places + place
                        )
                        storage.put(AppStorageKeys.Places.CATALOG, updated) { writeResult ->
                            deliver(
                                callback,
                                when (writeResult) {
                                    is StorageResult.Success -> StorageResult.Success(place)
                                    StorageResult.Missing -> StorageResult.Failure(
                                        nativeFailure("Place creation was not confirmed.")
                                    )
                                    is StorageResult.Failure -> writeResult
                                },
                                complete
                            )
                        }
                    }
                    StorageResult.Missing -> deliver(
                        callback,
                        StorageResult.Missing,
                        complete
                    )
                    is StorageResult.Failure -> deliver(callback, result, complete)
                }
            }
        }
    }

    override fun updatePlace(place: Place, callback: StorageCallback<Place>) {
        if (place.schemaVersion != PlaceContract.SCHEMA_VERSION ||
            !PlaceValidator.isValidId(place.id.trim())
        ) {
            callback(invalidRequest("Place update identity is invalid."))
            return
        }
        enqueueMutation { complete ->
            getCatalog { result ->
                when (result) {
                    is StorageResult.Success -> {
                        val index = result.value.places.indexOfFirst { it.id == place.id.trim() }
                        if (index < 0) {
                            deliver(callback, StorageResult.Missing, complete)
                            return@getCatalog
                        }
                        val existing = result.value.places[index]
                        val updatedAt = maxOf(clock.nowEpochMs(), existing.createdAtEpochMs)
                        val normalized = PlaceValidator.normalizeOrNull(
                            place.copy(
                                id = existing.id,
                                source = existing.source,
                                createdAtEpochMs = existing.createdAtEpochMs,
                                updatedAtEpochMs = updatedAt
                            )
                        )
                        if (normalized == null) {
                            deliver(
                                callback,
                                invalidRequest("Place update does not satisfy schema v2 validation."),
                                complete
                            )
                            return@getCatalog
                        }
                        val places = result.value.places.toMutableList().apply {
                            this[index] = normalized
                        }
                        storage.put(
                            AppStorageKeys.Places.CATALOG,
                            result.value.copy(places = places)
                        ) { writeResult ->
                            deliver(
                                callback,
                                when (writeResult) {
                                    is StorageResult.Success -> StorageResult.Success(normalized)
                                    StorageResult.Missing -> StorageResult.Failure(
                                        nativeFailure("Place update was not confirmed.")
                                    )
                                    is StorageResult.Failure -> writeResult
                                },
                                complete
                            )
                        }
                    }
                    StorageResult.Missing -> deliver(
                        callback,
                        StorageResult.Missing,
                        complete
                    )
                    is StorageResult.Failure -> deliver(callback, result, complete)
                }
            }
        }
    }

    override fun deletePlace(placeId: String, callback: StorageCallback<Unit>) {
        val normalizedId = placeId.trim()
        if (!PlaceValidator.isValidId(normalizedId)) {
            callback(invalidRequest("Place id is invalid."))
            return
        }
        enqueueMutation { complete ->
            getCatalog { result ->
                when (result) {
                    is StorageResult.Success -> {
                        val existing = result.value.places.firstOrNull {
                            it.id == normalizedId
                        }
                        if (existing == null) {
                            deliver(callback, StorageResult.Missing, complete)
                            return@getCatalog
                        }
                        if (existing.source == PlaceSource.SEED) {
                            deliver(
                                callback,
                                invalidRequest("Built-in seed places cannot be deleted."),
                                complete
                            )
                            return@getCatalog
                        }
                        val updated = result.value.copy(
                            places = result.value.places.filterNot { it.id == normalizedId }
                        )
                        storage.put(AppStorageKeys.Places.CATALOG, updated) { writeResult ->
                            when (writeResult) {
                                is StorageResult.Success -> cleanupFavoriteId(normalizedId) {
                                    deliver(
                                        callback,
                                        StorageResult.Success(Unit),
                                        complete
                                    )
                                }
                                StorageResult.Missing -> deliver(
                                    callback,
                                    StorageResult.Failure(
                                        nativeFailure("Place deletion was not confirmed.")
                                    ),
                                    complete
                                )
                                is StorageResult.Failure -> deliver(
                                    callback,
                                    writeResult,
                                    complete
                                )
                            }
                        }
                    }
                    StorageResult.Missing -> deliver(
                        callback,
                        StorageResult.Missing,
                        complete
                    )
                    is StorageResult.Failure -> deliver(callback, result, complete)
                }
            }
        }
    }

    private fun initializeCatalog(callback: StorageCallback<PlaceCatalog>) {
        storage.put(AppStorageKeys.Places.CATALOG, seedCatalog) { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> StorageResult.Success(seedCatalog)
                    StorageResult.Missing -> StorageResult.Failure(
                        nativeFailure("Catalog initialization was not confirmed.")
                    )
                    is StorageResult.Failure -> result
                }
            )
        }
    }

    private fun generateUniqueId(catalog: PlaceCatalog): String? {
        repeat(MAX_ID_ATTEMPTS) {
            val candidate = idGenerator.newId().trim()
            if (PlaceValidator.isValidId(candidate) &&
                catalog.places.none { place -> place.id == candidate }
            ) {
                return candidate
            }
        }
        return null
    }

    private fun cleanupFavoriteId(placeId: String, complete: () -> Unit) {
        storage.get(AppStorageKeys.Favorites.PLACE_IDS) { result ->
            val favorites = (result as? StorageResult.Success)?.value
            if (favorites == null || placeId !in favorites.placeIds) {
                complete()
                return@get
            }
            storage.put(
                AppStorageKeys.Favorites.PLACE_IDS,
                favorites.copy(placeIds = favorites.placeIds - placeId)
            ) {
                // The catalog deletion is already committed. Cleanup is best effort and stale
                // ids are pruned again by LocalFavoriteRepository.getFavoriteIds().
                complete()
            }
        }
    }

    private fun enqueueMutation(operation: (() -> Unit) -> Unit) {
        mutationQueue += operation
        startNextMutation()
    }

    private fun startNextMutation() {
        if (mutationInFlight || mutationQueue.isEmpty()) {
            return
        }
        mutationInFlight = true
        val operation = mutationQueue.removeAt(0)
        operation {
            mutationInFlight = false
            startNextMutation()
        }
    }

    private fun <T> deliver(
        callback: StorageCallback<T>,
        result: StorageResult<T>,
        complete: () -> Unit
    ) {
        try {
            callback(result)
        } finally {
            complete()
        }
    }

    private companion object {
        const val MAX_ID_ATTEMPTS = 16
    }
}

object SystemPlaceClock : PlaceClock {
    override fun nowEpochMs(): Long = DateTime.currentTimestamp()
}

class TimestampPlaceIdGenerator(
    private val clock: PlaceClock
) : PlaceIdGenerator {
    private var sequence = 0

    override fun newId(): String = "local_${clock.nowEpochMs()}_${sequence++}"
}

private fun invalidRequest(message: String): StorageResult.Failure =
    StorageResult.Failure(
        StorageError(StorageErrorCode.INVALID_REQUEST, message)
    )

private fun nativeFailure(message: String): StorageError =
    StorageError(StorageErrorCode.NATIVE_FAILURE, message)
