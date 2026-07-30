package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceListStoreTest {
    @Test
    fun initialCategoryFromTypedEntryFiltersFirstLoad() = runTest {
        val fixture = fixture()
        val store = fixture.store(initialCategory = PlaceCategory.NATURE)

        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()

        assertEquals(setOf(PlaceCategory.NATURE), store.state.value.filter.categories)
        assertTrue(store.state.value.visiblePlaces.all { it.category == PlaceCategory.NATURE })
        store.dispose()
    }

    @Test
    fun searchAndCombinedFiltersUseSharedEngine() = runTest {
        val fixture = fixture()
        val store = fixture.store()
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        assertEquals(8, store.state.value.visiblePlaces.size)

        store.dispatch(PlaceListIntent.QueryChanged("博物馆"))
        advanceUntilIdle()
        assertEquals(
            setOf("seed_shanghai_museum", "seed_china_tea_museum"),
            store.state.value.visiblePlaces.map(Place::id).toSet()
        )

        store.dispatch(PlaceListIntent.QueryChanged(""))
        store.dispatch(PlaceListIntent.CategoryToggled(PlaceCategory.NATURE))
        store.dispatch(PlaceListIntent.CityChanged("杭州"))
        advanceUntilIdle()
        assertEquals(listOf("seed_west_lake"), store.state.value.visiblePlaces.map(Place::id))

        store.dispatch(PlaceListIntent.ClearAllFilters)
        advanceUntilIdle()
        assertEquals(8, store.state.value.visiblePlaces.size)
        store.dispose()
    }

    @Test
    fun favoritesModePresentsContentAndRemovesUnfavoritedCard() = runTest {
        val fixture = fixture()
        fixture.favoriteRepository.setFavorite("seed_shanghai_museum", true) {}
        val store = fixture.store(mode = PlaceListMode.FAVORITES)
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        assertEquals(listOf("seed_shanghai_museum"), store.state.value.visiblePlaces.map(Place::id))

        store.dispatch(PlaceListIntent.FavoriteToggled("seed_shanghai_museum"))
        advanceUntilIdle()

        assertTrue(store.state.value.visiblePlaces.isEmpty())
        assertEquals(PlaceListContentState.EMPTY_FAVORITES, store.state.value.contentState)
        store.dispose()
    }

    @Test
    fun favoriteToggleKeepsAllPlacesInStableOrderAndEmitsOneEffect() = runTest {
        val fixture = fixture()
        val store = fixture.store()
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        val originalIds = store.state.value.visiblePlaces.map(Place::id)
        val effect = async { store.effects.first() }

        store.dispatch(PlaceListIntent.FavoriteToggled(originalIds.first()))
        advanceUntilIdle()

        assertEquals(originalIds, store.state.value.visiblePlaces.map(Place::id))
        assertIs<PlaceListEffect.FavoritesChanged>(effect.await())
        store.dispose()
    }

    @Test
    fun corruptedCatalogEntersReadOnlyStateAndBlocksFavoriteMutation() = runTest {
        val storage = InMemoryKeyValueStore().apply {
            seedRaw(AppStorageKeys.Places.CATALOG, encodedValue = "{broken")
        }
        val fixture = fixture(storage)
        val store = fixture.store()
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        store.dispatch(PlaceListIntent.FavoriteToggled("seed_shanghai_museum"))
        advanceUntilIdle()

        assertTrue(store.state.value.readOnly)
        assertEquals(PlaceListContentState.STORAGE_ERROR, store.state.value.contentState)
        assertTrue(store.state.value.favoriteIds.isEmpty())
        store.dispose()
    }

    @Test
    fun profileCityOnlyPrioritizesCatalogAndNeverClaimsNearby() = runTest {
        val fixture = fixture()
        fixture.profileRepository.saveProfile(
            LocalProfile(
                displayName = "测试用户",
                avatarPreset = AvatarPreset.SKY,
                homeCity = "杭州"
            )
        ) {}
        val store = fixture.store()

        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()

        assertEquals("杭州", store.state.value.homeCity)
        assertTrue(store.state.value.directoryContext.contains("杭州"))
        assertTrue(store.state.value.directoryContext.contains("优先"))
        assertFalse(store.state.value.directoryContext.contains("附近"))
        assertEquals("杭州", store.state.value.visiblePlaces.first().city)
        store.dispose()
    }

    @Test
    fun navigationIsAOneShotEffectAndDisposeStopsFurtherIntents() = runTest {
        val fixture = fixture()
        val store = fixture.store()
        val effect = async { store.effects.first() }

        store.dispatch(PlaceListIntent.PlaceClicked("seed_shanghai_museum"))
        advanceUntilIdle()
        assertEquals(
            PlaceListEffect.NavigateToDetail("seed_shanghai_museum"),
            effect.await()
        )
        assertEquals(PlaceListUiStatus.LOADING, store.state.value.status)

        store.dispose()
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        assertEquals(PlaceListUiStatus.LOADING, store.state.value.status)
    }

    private fun kotlinx.coroutines.test.TestScope.fixture(
        storage: InMemoryKeyValueStore = InMemoryKeyValueStore()
    ): StoreFixture {
        val placeRepository = LocalPlaceRepository(storage)
        return StoreFixture(
            profileRepository = LocalProfileRepository(storage),
            placeRepository = placeRepository,
            favoriteRepository = LocalFavoriteRepository(storage, placeRepository),
            scope = this
        )
    }
}

private data class StoreFixture(
    val profileRepository: LocalProfileRepository,
    val placeRepository: LocalPlaceRepository,
    val favoriteRepository: LocalFavoriteRepository,
    val scope: kotlinx.coroutines.CoroutineScope
) {
    fun store(
        mode: PlaceListMode = PlaceListMode.ALL,
        initialCategory: PlaceCategory? = null
    ) = PlaceListStore(
        profileRepository = profileRepository,
        placeRepository = placeRepository,
        favoriteRepository = favoriteRepository,
        parentScope = scope,
        mode = mode,
        initialCategory = initialCategory
    )
}
