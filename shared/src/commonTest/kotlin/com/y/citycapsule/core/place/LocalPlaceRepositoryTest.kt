package com.y.citycapsule.core.place

import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageBatchResult
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LocalPlaceRepositoryTest {
    @Test
    fun missingCatalogInitializesStableSeedDataOnce() {
        val storage = InMemoryKeyValueStore()
        val repository = repository(storage)

        val first = repository.catalogNow()
        val second = repository.catalogNow()

        assertEquals(8, first.successValue().places.size)
        assertEquals(first, second)
        assertEquals(
            first,
            storage.getNow(AppStorageKeys.Places.CATALOG)
        )
    }

    @Test
    fun initializationFailureReturnsMemorySnapshotWithoutPretendingPersistence() {
        val storage = FailingCatalogPutStore(InMemoryKeyValueStore())
        val repository = repository(storage)
        var snapshot: PlaceCatalogSnapshot? = null

        repository.getCatalogSnapshot { snapshot = it }

        assertEquals(PlaceCatalogSource.MEMORY_FALLBACK, snapshot?.source)
        assertEquals(8, snapshot?.catalog?.places?.size)
        assertEquals(StorageErrorCode.NATIVE_FAILURE, snapshot?.warning?.code)
    }

    @Test
    fun createUpdateAndDeletePreserveIdentityAndCleanFavorite() {
        val storage = InMemoryKeyValueStore()
        val clock = MutableClock(1_000L)
        val repository = LocalPlaceRepository(
            storage = storage,
            clock = clock,
            idGenerator = PlaceIdGenerator { "local_test" }
        )
        val created = repository.createNow(
            PlaceDraft(
                name = "  新地点 ",
                city = " 上海 ",
                category = PlaceCategory.OTHER,
                tags = listOf(" 新建 ", "新建")
            )
        ).successValue()
        storage.putNow(
            AppStorageKeys.Favorites.PLACE_IDS,
            FavoritePlaceIds(placeIds = setOf(created.id))
        )

        clock.value = 2_000L
        val updated = repository.updateNow(
            created.copy(
                name = "更新后的地点",
                createdAtEpochMs = 999_999L,
                updatedAtEpochMs = 999_999L
            )
        ).successValue()
        val deleteResult = repository.deleteNow(created.id)

        assertEquals("local_test", created.id)
        assertEquals(1_000L, created.createdAtEpochMs)
        assertEquals(listOf("新建"), created.tags)
        assertEquals(1_000L, updated.createdAtEpochMs)
        assertEquals(2_000L, updated.updatedAtEpochMs)
        assertIs<StorageResult.Success<Unit>>(deleteResult)
        assertIs<StorageResult.Missing>(repository.placeNow(created.id))
        assertTrue(
            storage.getNow(AppStorageKeys.Favorites.PLACE_IDS)
                .successValue().placeIds.isEmpty()
        )
    }

    @Test
    fun duplicateGeneratedIdsAreRetried() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(
            AppStorageKeys.Places.CATALOG,
            PlaceCatalog(places = listOf(placeFixture(id = "duplicate")))
        )
        var attempts = 0
        val repository = LocalPlaceRepository(
            storage = storage,
            clock = PlaceClock { 10L },
            idGenerator = PlaceIdGenerator {
                attempts++
                if (attempts == 1) "duplicate" else "unique"
            }
        )

        val created = repository.createNow(
            PlaceDraft("A", "上海", category = PlaceCategory.OTHER)
        ).successValue()

        assertEquals("unique", created.id)
        assertEquals(2, attempts)
    }

    @Test
    fun deterministicCatalogCorruptionIsReadOnlyAndNotOverwritten() {
        val storage = InMemoryKeyValueStore()
        storage.seedRaw(AppStorageKeys.Places.CATALOG, encodedValue = "{broken")
        val repository = repository(storage)
        var snapshot: PlaceCatalogSnapshot? = null

        repository.getCatalogSnapshot { snapshot = it }

        assertEquals(PlaceCatalogSource.RECOVERY_READ_ONLY, snapshot?.source)
        assertTrue(snapshot?.catalog?.places?.isEmpty() == true)
        assertEquals(StorageErrorCode.DECODE_FAILED, snapshot?.warning?.code)
        assertIs<StorageResult.Failure>(
            storage.getNow(AppStorageKeys.Places.CATALOG)
        )
    }

    @Test
    fun failedCatalogMutationKeepsPreviouslyPersistedCatalog() {
        val delegate = InMemoryKeyValueStore()
        val original = PlaceCatalog(places = listOf(placeFixture(id = "original")))
        delegate.putNow(AppStorageKeys.Places.CATALOG, original)
        val repository = LocalPlaceRepository(
            storage = FailingCatalogPutStore(delegate),
            clock = PlaceClock { 200L },
            idGenerator = PlaceIdGenerator { "new_place" }
        )

        val result = repository.createNow(
            PlaceDraft("新地点", "上海", category = PlaceCategory.OTHER)
        )

        assertEquals(
            StorageErrorCode.NATIVE_FAILURE,
            assertIs<StorageResult.Failure>(result).error.code
        )
        assertEquals(
            original,
            delegate.getNow(AppStorageKeys.Places.CATALOG).successValue()
        )
    }

    private fun repository(storage: KeyValueStore): LocalPlaceRepository =
        LocalPlaceRepository(
            storage = storage,
            clock = PlaceClock { 100L },
            idGenerator = PlaceIdGenerator { "local_fixed" }
        )
}

private class MutableClock(var value: Long) : PlaceClock {
    override fun nowEpochMs(): Long = value
}

private class FailingCatalogPutStore(
    private val delegate: KeyValueStore
) : KeyValueStore {
    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        delegate.get(key, callback)
    }

    override fun <T> put(
        key: StorageKey<T>,
        value: T,
        callback: StorageCallback<Unit>
    ) {
        if (key === AppStorageKeys.Places.CATALOG) {
            callback(
                StorageResult.Failure(
                    StorageError(StorageErrorCode.NATIVE_FAILURE, "Injected failure.")
                )
            )
        } else {
            delegate.put(key, value, callback)
        }
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        delegate.remove(key, callback)
    }

    override fun contains(key: StorageKey<*>, callback: StorageCallback<Boolean>) {
        delegate.contains(key, callback)
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        delegate.getMany(keys, callback)
    }
}

private fun LocalPlaceRepository.catalogNow(): StorageResult<PlaceCatalog> {
    var captured: StorageResult<PlaceCatalog>? = null
    getCatalog { captured = it }
    return requireNotNull(captured)
}

private fun LocalPlaceRepository.placeNow(id: String): StorageResult<Place> {
    var captured: StorageResult<Place>? = null
    getPlace(id) { captured = it }
    return requireNotNull(captured)
}

private fun LocalPlaceRepository.createNow(draft: PlaceDraft): StorageResult<Place> {
    var captured: StorageResult<Place>? = null
    createPlace(draft) { captured = it }
    return requireNotNull(captured)
}

private fun LocalPlaceRepository.updateNow(place: Place): StorageResult<Place> {
    var captured: StorageResult<Place>? = null
    updatePlace(place) { captured = it }
    return requireNotNull(captured)
}

private fun LocalPlaceRepository.deleteNow(id: String): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    deletePlace(id) { captured = it }
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
