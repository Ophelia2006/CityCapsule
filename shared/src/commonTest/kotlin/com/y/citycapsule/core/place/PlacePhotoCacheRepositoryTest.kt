package com.y.citycapsule.core.place

import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PlacePhotoCacheRepositoryTest {
    @Test
    fun codecRoundTripsValidEntriesAndRejectsUnsafeUrls() {
        val cache = PlacePhotoCache(entries = listOf(entry("place_1", 1_000L)))

        assertEquals(cache, PlacePhotoCacheCodec.decode(PlacePhotoCacheCodec.encode(cache)))
        assertNull(
            PlacePhotoCacheCodec.decode(
                """{"schemaVersion":1,"entries":[{"placeId":"place_1","url":"file:///photo.jpg","source":"test","updatedAtEpochMs":"1000"}]}"""
            )
        )
    }

    @Test
    fun expiredEntriesAreRemovedFromReadableCache() {
        var now = 1_000L
        val repository = LocalPlacePhotoCacheRepository(InMemoryKeyValueStore(), PlaceClock { now })
        repository.putNow("place_1", "https://example.com/1.jpg")

        assertNotNull(repository.getNow()["place_1"])

        now += PlacePhotoCacheContract.MAX_AGE_MS + 1L

        assertFalse(repository.getNow().containsKey("place_1"))
    }

    @Test
    fun cacheKeepsOnlyTheNewestBoundedEntries() {
        var now = 1_000L
        val repository = LocalPlacePhotoCacheRepository(InMemoryKeyValueStore(), PlaceClock { now })

        repeat(PlacePhotoCacheContract.MAX_ENTRIES + 5) { index ->
            repository.putNow("place_$index", "https://example.com/$index.jpg")
            now += 1L
        }

        val cached = repository.getNow()
        assertEquals(PlacePhotoCacheContract.MAX_ENTRIES, cached.size)
        assertFalse(cached.containsKey("place_0"))
        assertNotNull(cached["place_104"])
    }

    @Test
    fun removeInvalidatesAFailedRemotePhoto() {
        val repository = LocalPlacePhotoCacheRepository(InMemoryKeyValueStore(), PlaceClock { 1_000L })
        repository.putNow("place_1", "https://example.com/1.jpg")

        var result: StorageResult<Unit>? = null
        repository.remove("place_1") { result = it }

        assertIs<StorageResult.Success<Unit>>(result)
        assertFalse(repository.getNow().containsKey("place_1"))
    }

    private fun entry(placeId: String, updatedAtEpochMs: Long) = PlacePhotoCacheEntry(
        placeId = placeId,
        url = "https://example.com/$placeId.jpg",
        source = PlacePhotoCacheContract.SOURCE_AMAP_POI,
        updatedAtEpochMs = updatedAtEpochMs
    )

    private fun LocalPlacePhotoCacheRepository.putNow(placeId: String, url: String) {
        var result: StorageResult<Unit>? = null
        put(placeId, url, PlacePhotoCacheContract.SOURCE_AMAP_POI) { result = it }
        assertIs<StorageResult.Success<Unit>>(result)
    }

    private fun LocalPlacePhotoCacheRepository.getNow(): Map<String, PlacePhotoCacheEntry> {
        var result: StorageResult<Map<String, PlacePhotoCacheEntry>>? = null
        getValid { result = it }
        return assertIs<StorageResult.Success<Map<String, PlacePhotoCacheEntry>>>(result).value
    }
}
