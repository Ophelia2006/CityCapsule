package com.y.citycapsule.feature.place

import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.CapsuleClock
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceClock
import com.y.citycapsule.core.place.PlaceIdGenerator
import com.y.citycapsule.core.place.PlaceDraft
import com.y.citycapsule.core.place.PlaceVisualRef
import com.y.citycapsule.core.place.PlaceVisualType
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.RemotePlace
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.place.RepositoryPlaceMediaCleanup
import com.y.citycapsule.core.media.ManagedMediaDeleteResult
import com.y.citycapsule.core.media.ManagedMediaFileCapability
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
    fun detailLoadsMatchingRemotePhotoWithoutChangingContentSource() {
        val fixture = fixture()
        val remote = object : PlaceRemoteDataSource {
            override fun search(
                query: String,
                city: String,
                near: GeoPoint?,
                callback: (RemotePlaceResult) -> Unit
            ) {
                assertEquals("上海博物馆", query)
                assertEquals("上海", city)
                callback(
                    RemotePlaceResult.Success(
                        listOf(
                            RemotePlace(
                                providerId = "amap-museum",
                                name = "上海博物馆",
                                city = "上海",
                                district = "黄浦区",
                                address = "人民大道201号",
                                category = PlaceCategory.CULTURE,
                                tags = listOf("博物馆"),
                                geoPoint = GeoPoint(31.2303, 121.4700),
                                photoUrl = "https://example.test/shanghai-museum.jpg"
                            )
                        )
                    )
                )
            }
        }
        val holder = PlaceDetailStateHolder(
            placeId = "seed_shanghai_museum",
            placeRepository = fixture.placeRepository,
            favoriteRepository = fixture.favoriteRepository,
            capsuleRepository = fixture.capsuleRepository,
            remoteDataSource = remote
        )

        holder.load()

        assertEquals("https://example.test/shanghai-museum.jpg", holder.state.remotePhotoUrl)
        assertEquals("CityCapsule 内置城市内容包", holder.state.place?.contentSource)
    }

    @Test
    fun detailExposesThreeNewestMemoriesInDescendingOrder() {
        val fixture = fixture()
        repeat(4) { index ->
            fixture.capsuleRepository.publish(
                CapsuleDraft(content = "记忆 $index", placeId = "seed_shanghai_museum")
            ) {}
        }
        val holder = PlaceDetailStateHolder(
            "seed_shanghai_museum",
            fixture.placeRepository,
            fixture.favoriteRepository,
            fixture.capsuleRepository
        )

        holder.load()

        assertEquals(4, holder.state.memoryCount)
        assertEquals(3, holder.state.recentMemories.size)
        assertEquals(
            holder.state.recentMemories.sortedByDescending { it.createdAtEpochMs },
            holder.state.recentMemories
        )
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
    fun detailToggleAndDeletePersistThroughRepositories() {
        val fixture = fixture()
        val place = fixture.createPlace()
        val holder = PlaceDetailStateHolder(
            place.id,
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
        assertEquals(StorageResult.Missing, fixture.place(place.id))
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

    @Test
    fun placeMediaCleanupDeletesOnlyFilesWithoutPlaceOrCapsuleReferences() {
        val fixture = fixture()
        var created: StorageResult<Place>? = null
        fixture.placeRepository.createPlace(
            PlaceDraft(
                name = "带封面的地点",
                city = "上海",
                category = PlaceCategory.OTHER,
                visualRef = PlaceVisualRef(PlaceVisualType.MANAGED_FILE, "file://kept.jpg")
            )
        ) { created = it }
        assertIs<StorageResult.Success<Place>>(created)
        val deleted = mutableListOf<String>()
        val media = ManagedMediaFileCapability { paths, callback ->
            deleted += paths
            callback(ManagedMediaDeleteResult.Success(paths))
        }
        val cleanup = RepositoryPlaceMediaCleanup(
            fixture.placeRepository,
            fixture.capsuleRepository,
            media
        )
        var completed = false

        cleanup.cleanupCandidates(listOf("file://kept.jpg", "file://orphan.jpg")) {
            completed = it
        }

        assertTrue(completed)
        assertEquals(listOf("file://orphan.jpg"), deleted)
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
        var capsuleSequence = 0L
        val capsuleRepository = LocalCapsuleRepository(
            storage = storage,
            clock = CapsuleClock { ++capsuleSequence },
            idGenerator = CapsuleIdGenerator { "capsule_for_place_test_${++capsuleSequence}" }
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

    fun createPlace(): Place {
        var captured: StorageResult<Place>? = null
        placeRepository.createPlace(
            PlaceDraft(
                name = "用户地点",
                city = "上海",
                category = PlaceCategory.OTHER
            )
        ) { captured = it }
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
