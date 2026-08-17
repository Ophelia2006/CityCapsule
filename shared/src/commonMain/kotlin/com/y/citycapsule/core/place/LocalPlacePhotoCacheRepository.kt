package com.y.citycapsule.core.place

import com.y.citycapsule.core.place.PlacePhotoCacheCodec.isValid
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageResult

class LocalPlacePhotoCacheRepository(
    private val storage: KeyValueStore,
    private val clock: PlaceClock = SystemPlaceClock
) : PlacePhotoCacheRepository {
    private val mutationQueue = mutableListOf<(() -> Unit) -> Unit>()
    private var mutationInFlight = false

    override fun getValid(callback: (StorageResult<Map<String, PlacePhotoCacheEntry>>) -> Unit) {
        storage.get(AppStorageKeys.Places.PHOTO_CACHE) { result ->
            when (result) {
                is StorageResult.Success -> {
                    val valid = validEntries(result.value.entries)
                    callback(StorageResult.Success(valid.associateBy(PlacePhotoCacheEntry::placeId)))
                    if (valid.size != result.value.entries.size) {
                        storage.put(AppStorageKeys.Places.PHOTO_CACHE, PlacePhotoCache(entries = valid)) {}
                    }
                }
                StorageResult.Missing -> callback(StorageResult.Success(emptyMap()))
                is StorageResult.Failure -> callback(result)
            }
        }
    }

    override fun put(placeId: String, url: String, source: String, callback: StorageCallback<Unit>) {
        val entry = PlacePhotoCacheEntry(placeId.trim(), url.trim(), source.trim(), clock.nowEpochMs())
        if (!entry.isValid()) {
            callback(StorageResult.Failure(com.y.citycapsule.core.storage.StorageError(
                com.y.citycapsule.core.storage.StorageErrorCode.INVALID_REQUEST,
                "Invalid place photo cache entry."
            )))
            return
        }
        enqueue { done ->
            readCache { cacheResult ->
                if (cacheResult !is StorageResult.Success) {
                    callback(cacheResult as StorageResult.Failure)
                    done()
                    return@readCache
                }
                val next = (validEntries(cacheResult.value.entries).filterNot { it.placeId == entry.placeId } + entry)
                    .sortedByDescending(PlacePhotoCacheEntry::updatedAtEpochMs)
                    .take(PlacePhotoCacheContract.MAX_ENTRIES)
                storage.put(AppStorageKeys.Places.PHOTO_CACHE, PlacePhotoCache(entries = next)) {
                    callback(it)
                    done()
                }
            }
        }
    }

    override fun remove(placeId: String, callback: StorageCallback<Unit>) {
        enqueue { done ->
            readCache { cacheResult ->
                if (cacheResult !is StorageResult.Success) {
                    callback(cacheResult as StorageResult.Failure)
                    done()
                    return@readCache
                }
                val next = cacheResult.value.entries.filterNot { it.placeId == placeId }
                if (next.size == cacheResult.value.entries.size) {
                    callback(StorageResult.Success(Unit))
                    done()
                } else storage.put(AppStorageKeys.Places.PHOTO_CACHE, PlacePhotoCache(entries = next)) {
                    callback(it)
                    done()
                }
            }
        }
    }

    private fun readCache(callback: StorageCallback<PlacePhotoCache>) {
        storage.get(AppStorageKeys.Places.PHOTO_CACHE) { result ->
            callback(if (result == StorageResult.Missing) StorageResult.Success(PlacePhotoCache.EMPTY) else result)
        }
    }

    private fun validEntries(entries: List<PlacePhotoCacheEntry>): List<PlacePhotoCacheEntry> {
        val cutoff = (clock.nowEpochMs() - PlacePhotoCacheContract.MAX_AGE_MS).coerceAtLeast(0L)
        return entries.filter { it.isValid() && it.updatedAtEpochMs >= cutoff }
    }

    private fun enqueue(operation: (() -> Unit) -> Unit) {
        mutationQueue += operation
        drain()
    }

    private fun drain() {
        if (mutationInFlight) return
        val next = mutationQueue.removeFirstOrNull() ?: return
        mutationInFlight = true
        next {
            mutationInFlight = false
            drain()
        }
    }
}
