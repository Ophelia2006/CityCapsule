package com.y.citycapsule.feature.place

import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceClock
import com.y.citycapsule.core.place.PlaceIdGenerator
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlaceFeatureStateTest {
    @Test
    fun initialCategoryFromTypedEntryFiltersFirstLoad() {
        val fixture = fixture()
        val holder = PlaceListStateHolder(
            fixture.placeRepository,
            fixture.favoriteRepository,
            initialCategory = PlaceCategory.NATURE
        )

        holder.load()

        assertEquals(setOf(PlaceCategory.NATURE), holder.state.filter.categories)
        assertTrue(holder.state.visiblePlaces.all { it.category == PlaceCategory.NATURE })
    }

    @Test
    fun listSearchAndCombinedFiltersUseSharedEngine() {
        val fixture = fixture()
        val holder = PlaceListStateHolder(
            fixture.placeRepository,
            fixture.favoriteRepository
        )

        holder.load()
        assertEquals(8, holder.state.visiblePlaces.size)

        holder.updateQuery("博物馆")
        assertEquals(
            setOf("seed_shanghai_museum", "seed_china_tea_museum"),
            holder.state.visiblePlaces.map(Place::id).toSet()
        )

        holder.updateQuery("")
        holder.toggleCategory(PlaceCategory.NATURE)
        holder.updateCity("杭州")
        assertEquals(
            listOf("seed_west_lake"),
            holder.state.visiblePlaces.map(Place::id)
        )

        holder.clearFilters()
        assertEquals(8, holder.state.visiblePlaces.size)
    }

    @Test
    fun favoritesModeRemovesCardImmediatelyAfterUnfavorite() {
        val fixture = fixture()
        fixture.favoriteRepository.setFavorite("seed_shanghai_museum", true) {}
        val holder = PlaceListStateHolder(
            fixture.placeRepository,
            fixture.favoriteRepository,
            PlaceListMode.FAVORITES
        )

        holder.load()
        assertEquals(
            listOf("seed_shanghai_museum"),
            holder.state.visiblePlaces.map(Place::id)
        )

        holder.toggleFavorite("seed_shanghai_museum")

        assertTrue(holder.state.visiblePlaces.isEmpty())
        assertEquals(PlaceListContentState.EMPTY_FAVORITES, holder.state.contentState)
    }

    @Test
    fun allPlacesFavoriteToggleKeepsListReadyAndInStableOrder() {
        val fixture = fixture()
        var invalidations = 0
        val holder = PlaceListStateHolder(
            fixture.placeRepository,
            fixture.favoriteRepository,
            onDataChanged = { invalidations++ }
        )
        holder.load()
        val originalIds = holder.state.visiblePlaces.map(Place::id)
        val originalNotice = holder.state.notice

        holder.toggleFavorite(originalIds.first())

        assertEquals(PlaceListUiStatus.READY, holder.state.status)
        assertEquals(originalIds, holder.state.visiblePlaces.map(Place::id))
        assertEquals(originalNotice, holder.state.notice)
        assertEquals(1, invalidations)
    }

    @Test
    fun placeListIgnoresItsOwnInvalidationButReloadsForExternalMutation() {
        val owner = PlaceFeatureRuntime.newOwnerToken()

        PlaceFeatureRuntime.invalidateFrom(owner)
        assertFalse(PlaceFeatureRuntime.shouldReload(owner))

        PlaceFeatureRuntime.invalidate()
        assertTrue(PlaceFeatureRuntime.shouldReload(owner))
    }

    @Test
    fun corruptedCatalogEntersReadOnlyStateAndBlocksFavoriteMutation() {
        val storage = InMemoryKeyValueStore()
        storage.seedRaw(AppStorageKeys.Places.CATALOG, encodedValue = "{broken")
        val fixture = fixture(storage)
        val holder = PlaceListStateHolder(
            fixture.placeRepository,
            fixture.favoriteRepository
        )

        holder.load()
        holder.toggleFavorite("seed_shanghai_museum")

        assertTrue(holder.state.readOnly)
        assertEquals(PlaceListContentState.STORAGE_ERROR, holder.state.contentState)
        assertTrue(holder.state.favoriteIds.isEmpty())
    }

    @Test
    fun detailToggleAndDeletePersistThroughRepositories() {
        val fixture = fixture()
        val holder = PlaceDetailStateHolder(
            "seed_shanghai_museum",
            fixture.placeRepository,
            fixture.favoriteRepository,
            fixture.capsuleRepository
        )
        var deleted = false

        holder.load()
        assertEquals(PlaceDetailUiStatus.READY, holder.state.status)
        holder.toggleFavorite()
        assertTrue(holder.state.favorite)
        assertEquals(PlaceDetailUiStatus.READY, holder.state.status)
        assertEquals(null, holder.state.notice)

        holder.requestDelete()
        holder.delete { deleted = true }

        assertTrue(deleted)
        assertTrue(fixture.favoriteIds().placeIds.isEmpty())
        assertEquals(StorageResult.Missing, fixture.place("seed_shanghai_museum"))
    }

    @Test
    fun detailRefusesToDeleteAPlaceThatStillOwnsCityMemories() {
        val fixture = fixture()
        fixture.capsuleRepository.publish(
            CapsuleDraft(
                content = "这条记忆仍然需要它的地点。",
                placeId = "seed_shanghai_museum"
            )
        ) {}
        val holder = PlaceDetailStateHolder(
            "seed_shanghai_museum",
            fixture.placeRepository,
            fixture.favoriteRepository,
            fixture.capsuleRepository
        )
        var deleted = false

        holder.load()
        assertEquals(1, holder.state.memoryCount)
        holder.requestDelete()
        holder.delete { deleted = true }

        assertFalse(deleted)
        assertFalse(holder.state.showDeleteConfirmation)
        assertEquals(PlaceDetailUiStatus.READY, holder.state.status)
        assertIs<StorageResult.Success<Place>>(fixture.place("seed_shanghai_museum"))
        assertTrue(holder.state.notice?.message?.contains("请先处理这些记忆") == true)
    }

    @Test
    fun editorValidationCreateAndEditKeepIdentityStable() {
        val fixture = fixture()
        var invalidations = 0
        val createHolder = PlaceEditorStateHolder(
            placeId = null,
            placeRepository = fixture.placeRepository,
            onDataChanged = { invalidations++ }
        )
        var created: Place? = null

        createHolder.load()
        createHolder.save { _, _ -> error("Invalid empty draft must not save.") }
        assertNotNull(createHolder.state.validationMessage)

        createHolder.updateName("测试地点")
        createHolder.updateCity("上海")
        createHolder.updateDistrict("徐汇区")
        createHolder.updateCategory(PlaceCategory.CULTURE)
        createHolder.updateTags("测试，文化，测试")
        createHolder.save { place, wasCreated ->
            assertTrue(wasCreated)
            created = place
        }

        val saved = requireNotNull(created)
        assertEquals(listOf("测试", "文化"), saved.tags)
        val editHolder = PlaceEditorStateHolder(
            placeId = saved.id,
            placeRepository = fixture.placeRepository,
            onDataChanged = { invalidations++ }
        )
        editHolder.load()
        editHolder.updateName("修改后的地点")
        editHolder.save { place, wasCreated ->
            assertFalse(wasCreated)
            assertEquals(saved.id, place.id)
            assertEquals(saved.createdAtEpochMs, place.createdAtEpochMs)
        }

        assertEquals(
            "修改后的地点",
            (fixture.place(saved.id) as StorageResult.Success).value.name
        )
        assertEquals(2, invalidations)
    }

    @Test
    fun discardConfirmationAppearsOnlyForDirtyDraft() {
        val fixture = fixture()
        val holder = PlaceEditorStateHolder(null, fixture.placeRepository)
        var immediate = false

        holder.load()
        holder.requestDiscard { immediate = true }
        assertTrue(immediate)

        immediate = false
        holder.updateName("尚未保存")
        holder.requestDiscard { immediate = true }

        assertFalse(immediate)
        assertTrue(holder.state.showDiscardConfirmation)
    }

    private fun fixture(
        storage: InMemoryKeyValueStore = InMemoryKeyValueStore()
    ): Fixture {
        var clockValue = 1_000L
        val placeRepository = LocalPlaceRepository(
            storage = storage,
            clock = PlaceClock { clockValue++ },
            idGenerator = PlaceIdGenerator { "local_${clockValue++}" }
        )
        val favoriteRepository = LocalFavoriteRepository(storage, placeRepository)
        val capsuleRepository = LocalCapsuleRepository(
            storage = storage,
            idGenerator = CapsuleIdGenerator { "capsule_for_place_test" }
        )
        return Fixture(storage, placeRepository, favoriteRepository, capsuleRepository)
    }
}

private data class Fixture(
    val storage: InMemoryKeyValueStore,
    val placeRepository: LocalPlaceRepository,
    val favoriteRepository: LocalFavoriteRepository,
    val capsuleRepository: LocalCapsuleRepository
) {
    fun place(id: String): StorageResult<Place> {
        var captured: StorageResult<Place>? = null
        placeRepository.getPlace(id) { captured = it }
        return requireNotNull(captured)
    }

    fun favoriteIds(): FavoritePlaceIds {
        var captured: StorageResult<FavoritePlaceIds>? = null
        favoriteRepository.getFavoriteIds { captured = it }
        return (requireNotNull(captured) as StorageResult.Success).value
    }
}

private fun <T> InMemoryKeyValueStore.putNow(
    key: StorageKey<T>,
    value: T
): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    put(key, value) { captured = it }
    return requireNotNull(captured)
}
