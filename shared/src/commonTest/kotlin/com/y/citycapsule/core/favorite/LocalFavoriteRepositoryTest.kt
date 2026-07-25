package com.y.citycapsule.core.favorite

import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceCatalog
import com.y.citycapsule.core.place.PlaceClock
import com.y.citycapsule.core.place.PlaceIdGenerator
import com.y.citycapsule.core.place.placeFixture
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalFavoriteRepositoryTest {
    @Test
    fun setFavoriteIsIdempotentAndToggleRemovesIt() {
        val storage = InMemoryKeyValueStore()
        val repository = repository(storage)

        assertEquals(true, repository.setNow("place_1", true).successValue())
        assertEquals(true, repository.setNow("place_1", true).successValue())
        assertEquals(setOf("place_1"), repository.idsNow().successValue().placeIds)
        assertEquals(false, repository.toggleNow("place_1").successValue())
        assertTrue(repository.idsNow().successValue().placeIds.isEmpty())
    }

    @Test
    fun nonexistentPlaceCannotBeFavorited() {
        val storage = InMemoryKeyValueStore()
        val repository = repository(storage)

        assertIs<StorageResult.Missing>(repository.setNow("missing", true))
        assertIs<StorageResult.Missing>(
            storage.getNow(AppStorageKeys.Favorites.PLACE_IDS)
        )
    }

    @Test
    fun staleIdsAreHiddenAndPrunedBestEffort() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(
            AppStorageKeys.Favorites.PLACE_IDS,
            FavoritePlaceIds(placeIds = setOf("place_1", "stale"))
        )
        val repository = repository(storage)

        val visible = repository.idsNow().successValue()
        val persisted = storage.getNow(
            AppStorageKeys.Favorites.PLACE_IDS
        ).successValue()

        assertEquals(setOf("place_1"), visible.placeIds)
        assertEquals(visible, persisted)
    }

    @Test
    fun corruptedFavoritePayloadIsReportedAndNeverOverwritten() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(
            AppStorageKeys.Places.CATALOG,
            PlaceCatalog(places = listOf(placeFixture(id = "place_1")))
        )
        storage.seedRaw(
            AppStorageKeys.Favorites.PLACE_IDS,
            encodedValue = "{broken"
        )
        val repository = LocalFavoriteRepository(
            storage,
            LocalPlaceRepository(
                storage = storage,
                clock = PlaceClock { 100L },
                idGenerator = PlaceIdGenerator { "local_fixed" }
            )
        )

        val result = repository.idsNow()

        assertEquals(
            StorageErrorCode.DECODE_FAILED,
            assertIs<StorageResult.Failure>(result).error.code
        )
        assertEquals(
            StorageErrorCode.DECODE_FAILED,
            assertIs<StorageResult.Failure>(
                storage.getNow(AppStorageKeys.Favorites.PLACE_IDS)
            ).error.code
        )
    }

    private fun repository(storage: InMemoryKeyValueStore): LocalFavoriteRepository {
        storage.putNow(
            AppStorageKeys.Places.CATALOG,
            PlaceCatalog(places = listOf(placeFixture(id = "place_1")))
        )
        val placeRepository = LocalPlaceRepository(
            storage = storage,
            clock = PlaceClock { 100L },
            idGenerator = PlaceIdGenerator { "local_fixed" }
        )
        return LocalFavoriteRepository(storage, placeRepository)
    }
}

private fun LocalFavoriteRepository.idsNow(): StorageResult<FavoritePlaceIds> {
    var captured: StorageResult<FavoritePlaceIds>? = null
    getFavoriteIds { captured = it }
    return requireNotNull(captured)
}

private fun LocalFavoriteRepository.setNow(
    id: String,
    favorite: Boolean
): StorageResult<Boolean> {
    var captured: StorageResult<Boolean>? = null
    setFavorite(id, favorite) { captured = it }
    return requireNotNull(captured)
}

private fun LocalFavoriteRepository.toggleNow(id: String): StorageResult<Boolean> {
    var captured: StorageResult<Boolean>? = null
    toggleFavorite(id) { captured = it }
    return requireNotNull(captured)
}

private fun <T> KeyValueStore.putNow(
    key: StorageKey<T>,
    value: T
): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    put(key, value) { captured = it }
    return requireNotNull(captured)
}

private fun <T> KeyValueStore.getNow(key: StorageKey<T>): StorageResult<T> {
    var captured: StorageResult<T>? = null
    get(key) { captured = it }
    return requireNotNull(captured)
}

private fun <T> StorageResult<T>.successValue(): T =
    assertIs<StorageResult.Success<T>>(this).value
