package com.y.citycapsule.feature.place

import com.y.citycapsule.core.city.LocalExploreCityRepository
import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.city.ReverseGeocodeCapability
import com.y.citycapsule.core.city.ReverseGeocodeResult
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.location.LocationCapability
import com.y.citycapsule.core.location.LocationResult
import com.y.citycapsule.core.map.MapAvailability
import com.y.citycapsule.core.map.MapViewEvent
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceFilter
import com.y.citycapsule.core.place.PlaceSeedData
import com.y.citycapsule.core.place.RemotePlace
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.place.PlaceRemoteDataSource
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
    fun refinedTopicsSeparateCoffeeFromRestaurantsAndParksFromNaturalScenery() {
        val places = listOf(
            Place(id = "coffee", name = "街角咖啡", city = "西安", category = PlaceCategory.FOOD, tags = listOf("咖啡店"), createdAtEpochMs = 1L, updatedAtEpochMs = 1L),
            Place(id = "restaurant", name = "长安餐厅", city = "西安", category = PlaceCategory.FOOD, tags = listOf("餐饮"), createdAtEpochMs = 1L, updatedAtEpochMs = 1L),
            Place(id = "park", name = "城市公园", city = "西安", category = PlaceCategory.NATURE, tags = listOf("公园"), createdAtEpochMs = 1L, updatedAtEpochMs = 1L),
            Place(id = "mountain", name = "翠华山", city = "西安", category = PlaceCategory.NATURE, tags = listOf("自然风景"), createdAtEpochMs = 1L, updatedAtEpochMs = 1L)
        )
        val initial = PlaceListUiState(
            status = PlaceListUiStatus.READY,
            catalogPlaces = places,
            visiblePlaces = places
        )

        val coffee = PlaceListReducer.reduce(initial, PlaceListMutation.TopicSelected(ExplorePlaceTopic.COFFEE))
        val restaurant = PlaceListReducer.reduce(initial, PlaceListMutation.TopicSelected(ExplorePlaceTopic.RESTAURANT))
        val park = PlaceListReducer.reduce(initial, PlaceListMutation.TopicSelected(ExplorePlaceTopic.PARK))
        val natural = PlaceListReducer.reduce(initial, PlaceListMutation.TopicSelected(ExplorePlaceTopic.NATURAL_SCENERY))

        assertEquals(listOf("coffee"), coffee.visiblePlaces.map(Place::id))
        assertEquals(listOf("restaurant"), restaurant.visiblePlaces.map(Place::id))
        assertEquals(listOf("park"), park.visiblePlaces.map(Place::id))
        assertEquals(listOf("mountain"), natural.visiblePlaces.map(Place::id))
    }

    @Test
    fun onlineTopicResultsExcludeUnrelatedPlaces() {
        val state = PlaceListReducer.reduce(
            PlaceListUiState(selectedTopic = ExplorePlaceTopic.COFFEE),
            PlaceListMutation.OnlineSearchFinished(RemotePlaceResult.Success(listOf(
                RemotePlace("coffee", "街角咖啡", "西安", null, null, PlaceCategory.FOOD, listOf("咖啡店"), GeoPoint(34.3, 108.9), null),
                RemotePlace("restaurant", "长安餐厅", "西安", null, null, PlaceCategory.FOOD, listOf("餐饮"), GeoPoint(34.3, 108.9), null)
            )))
        )

        assertEquals(listOf("coffee"), state.onlinePlaces.map(RemotePlace::providerId))
    }

    @Test
    fun onlineResultsAreBoundedBeforeTheyReachTheExploreList() {
        val places = (0 until ONLINE_PLACE_RESULT_LIMIT + 5).map { index ->
            RemotePlace("poi-$index", "地点$index", "西安", null, null, PlaceCategory.OTHER, emptyList(), GeoPoint(34.3, 108.9), null)
        }

        val state = PlaceListReducer.reduce(
            PlaceListUiState(),
            PlaceListMutation.OnlineSearchFinished(RemotePlaceResult.Success(places))
        )

        assertEquals(ONLINE_PLACE_RESULT_LIMIT, state.onlinePlaces.size)
    }

    @Test
    fun onlineAppendPreservesExistingPrefixAndDeduplicatesProviderIds() {
        val firstPage = remotePlaces("first", ONLINE_PLACE_RESULT_LIMIT)
        val initial = PlaceListReducer.reduce(
            PlaceListUiState(),
            PlaceListMutation.OnlineSearchFinished(RemotePlaceResult.Success(firstPage))
        )
        val duplicate = firstPage.last()
        val appended = PlaceListReducer.reduce(
            initial,
            PlaceListMutation.OnlineSearchFinished(
                RemotePlaceResult.Success(listOf(duplicate) + remotePlaces("second", 3)),
                append = true,
                page = 2
            )
        )

        assertEquals(firstPage, appended.onlinePlaces.take(firstPage.size))
        assertEquals(firstPage.size + 3, appended.onlinePlaces.size)
        assertEquals(appended.onlinePlaces.size, appended.onlinePlaces.distinctBy(RemotePlace::providerId).size)
    }

    @Test
    fun repeatedNextPageIntentsStartOnlyOneRequestForThatPage() = runTest {
        val remote = ControllablePlaceRemoteDataSource()
        val fixture = fixture()
        val store = fixture.store(remoteDataSource = remote)
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        store.dispatch(PlaceListIntent.QueryChanged("咖啡"))
        store.dispatch(PlaceListIntent.OnlineSearchRequested)
        advanceUntilIdle()
        remote.completeFirst("咖啡", 1, RemotePlaceResult.Success(remotePlaces("page-1", ONLINE_PLACE_RESULT_LIMIT)))
        advanceUntilIdle()

        repeat(5) { store.dispatch(PlaceListIntent.OnlineNextPageRequested) }
        advanceUntilIdle()

        assertEquals(1, remote.requests.count { it.page == 2 })
        assertTrue(store.state.value.onlineLoadingMore)
        store.dispose()
    }

    @Test
    fun appendFailureKeepsExistingRowsAndAllowsExplicitRetry() = runTest {
        val remote = ControllablePlaceRemoteDataSource()
        val fixture = fixture()
        val store = fixture.store(remoteDataSource = remote)
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        store.dispatch(PlaceListIntent.QueryChanged("咖啡"))
        store.dispatch(PlaceListIntent.OnlineSearchRequested)
        advanceUntilIdle()
        val firstPage = remotePlaces("page-1", ONLINE_PLACE_RESULT_LIMIT)
        remote.completeFirst("咖啡", 1, RemotePlaceResult.Success(firstPage))
        advanceUntilIdle()

        store.dispatch(PlaceListIntent.OnlineNextPageRequested)
        advanceUntilIdle()
        remote.completeLast("咖啡", 2, RemotePlaceResult.Failure("弱网"))
        advanceUntilIdle()
        assertEquals(firstPage, store.state.value.onlinePlaces)
        assertTrue(store.state.value.onlineLoadMoreFailed)

        store.dispatch(PlaceListIntent.OnlineNextPageRequested)
        advanceUntilIdle()
        assertEquals(2, remote.requests.count { it.query == "咖啡" && it.page == 2 })
        store.dispose()
    }

    @Test
    fun staleOnlineResponseCannotOverwriteNewQueryResults() = runTest {
        val remote = ControllablePlaceRemoteDataSource()
        val fixture = fixture()
        val store = fixture.store(remoteDataSource = remote)
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()

        store.dispatch(PlaceListIntent.QueryChanged("旧查询"))
        store.dispatch(PlaceListIntent.OnlineSearchRequested)
        advanceUntilIdle()
        store.dispatch(PlaceListIntent.QueryChanged("新查询"))
        store.dispatch(PlaceListIntent.OnlineSearchRequested)
        advanceUntilIdle()

        remote.completeFirst("新查询", 1, RemotePlaceResult.Success(remotePlaces("new", 2)))
        remote.completeFirst("旧查询", 1, RemotePlaceResult.Success(remotePlaces("old", 2)))
        advanceUntilIdle()

        assertEquals(listOf("new-0", "new-1"), store.state.value.onlinePlaces.map(RemotePlace::providerId))
        store.dispose()
    }

    @Test
    fun importedProviderCityWithAdministrativeSuffixRemainsVisibleInSelectedCity() {
        val imported = Place(
            id = "imported-xian-wall",
            name = "西安城墙",
            city = "西安市",
            category = PlaceCategory.LANDMARK,
            contentSource = "高德地图 POI · poi-xian-wall",
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L
        )
        val initial = PlaceListUiState(
            status = PlaceListUiStatus.READY,
            filter = PlaceFilter(city = "西安")
        )

        val state = PlaceListReducer.reduce(initial, PlaceListMutation.RemoteImportFinished(imported))

        assertEquals(listOf(imported.id), state.visiblePlaces.map(Place::id))
    }

    @Test
    fun existingLocalImportsAreExcludedAndOnlineCandidatesFillRemainingSlots() {
        val local = (0 until 8).map { index ->
            Place(
                id = "local-$index", name = "本地$index", city = "西安",
                category = PlaceCategory.OTHER,
                contentSource = "高德地图 POI · poi-$index",
                createdAtEpochMs = 1L, updatedAtEpochMs = 1L
            )
        }
        val remote = (0 until 12).map { index ->
            RemotePlace("poi-$index", "在线$index", "西安", null, null, PlaceCategory.OTHER, emptyList(), GeoPoint(34.3, 108.9), null)
        }
        val initial = PlaceListUiState(
            status = PlaceListUiStatus.READY,
            catalogPlaces = local,
            visiblePlaces = local
        )

        val state = PlaceListReducer.reduce(
            initial,
            PlaceListMutation.OnlineSearchFinished(RemotePlaceResult.Success(remote))
        )

        assertEquals(listOf("poi-8", "poi-9", "poi-10", "poi-11"), state.onlinePlaces.map(RemotePlace::providerId))
        assertEquals(ONLINE_PLACE_RESULT_LIMIT, state.visiblePlaces.size + state.onlinePlaces.size)
    }

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
        assertEquals(15, store.state.value.visiblePlaces.size)

        store.dispatch(PlaceListIntent.QueryChanged("博物馆"))
        advanceUntilIdle()
        assertEquals(
            setOf(
                "seed_shanghai_museum",
                "seed_power_station_of_art",
                "seed_natural_history_museum",
                "seed_shanghai_astronomy_museum"
            ),
            store.state.value.visiblePlaces.map(Place::id).toSet()
        )

        store.dispatch(PlaceListIntent.QueryChanged(""))
        store.dispatch(PlaceListIntent.CategoryToggled(PlaceCategory.WATERFRONT))
        store.dispatch(PlaceListIntent.CityChanged("杭州"))
        advanceUntilIdle()
        assertEquals(listOf("seed_west_lake"), store.state.value.visiblePlaces.map(Place::id))

        store.dispatch(PlaceListIntent.ClearAllFilters)
        advanceUntilIdle()
        assertEquals(15, store.state.value.visiblePlaces.size)
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
    fun selectedExploreCityScopesCatalogAndNeverClaimsNearby() = runTest {
        val fixture = fixture()
        fixture.cityRepository.select("cn-hangzhou") {}
        val store = fixture.store()

        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()

        assertEquals("杭州", store.state.value.selectedCity.displayName)
        assertTrue(store.state.value.directoryContext.contains("杭州"))
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

    @Test
    fun mapRequiresConsentThenExposesOnlyCoordinateMarkers() = runTest {
        val fixture = fixture()
        val store = fixture.store()
        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()

        store.dispatch(PlaceListIntent.MapViewSelected)
        advanceUntilIdle()
        assertTrue(store.state.value.showMapPrivacyPrompt)
        assertEquals(PlaceDirectoryViewMode.LIST, store.state.value.viewMode)

        store.dispatch(PlaceListIntent.MapPrivacyAccepted)
        advanceUntilIdle()
        assertEquals(PlaceDirectoryViewMode.MAP, store.state.value.viewMode)
        assertEquals(15, store.state.value.mapViewState.markers.size)
        assertTrue(store.state.value.mapViewState.markers.all { it.position.latitude != 0.0 })
        store.dispose()
    }

    @Test
    fun markerSelectionIsValidatedAndMapFailureReturnsToList() = runTest {
        val fixture = fixture()
        val store = fixture.store()
        store.dispatch(PlaceListIntent.Load)
        store.dispatch(PlaceListIntent.MapPrivacyAccepted)
        advanceUntilIdle()

        store.dispatch(PlaceListIntent.MapEventReceived(
            MapViewEvent.MarkerSelected("missing_place")
        ))
        store.dispatch(PlaceListIntent.MapEventReceived(
            MapViewEvent.MarkerSelected("seed_shanghai_museum")
        ))
        advanceUntilIdle()
        assertEquals("seed_shanghai_museum", store.state.value.selectedMapPlaceId)

        store.dispatch(PlaceListIntent.MapEventReceived(
            MapViewEvent.Unavailable(MapAvailability.MissingConfiguration)
        ))
        advanceUntilIdle()
        assertEquals(PlaceDirectoryViewMode.LIST, store.state.value.viewMode)
        assertTrue(store.state.value.notice?.message?.contains("未配置") == true)
        store.dispose()
    }

    @Test
    fun locationResultFlowsThroughStoreAndFailureRemovesDistances() = runTest {
        val fixture = fixture()
        var callback: ((LocationResult) -> Unit)? = null
        val store = fixture.store(LocationCapability { callback = it })

        store.dispatch(PlaceListIntent.CurrentLocationRequested)
        advanceUntilIdle()
        assertEquals(PlaceLocationStatus.REQUESTING, store.state.value.locationStatus)

        callback?.invoke(LocationResult.Success(GeoPoint(31.0, 121.0), 12.0))
        advanceUntilIdle()
        assertEquals(PlaceLocationStatus.AVAILABLE, store.state.value.locationStatus)
        assertEquals(GeoPoint(31.0, 121.0), store.state.value.currentLocation)

        store.dispatch(PlaceListIntent.CurrentLocationRequested)
        advanceUntilIdle()
        callback?.invoke(LocationResult.ServiceDisabled)
        advanceUntilIdle()
        assertEquals(PlaceLocationStatus.SERVICE_DISABLED, store.state.value.locationStatus)
        assertEquals(null, store.state.value.currentLocation)
        store.dispose()
    }

    @Test
    fun explicitCurrentLocationAutomaticallySwitchesToResolvedDynamicCity() = runTest {
        val fixture = fixture()
        val xian = CityDefinition("remote-xian", "西安", GeoPoint(34.3416, 108.9398), false, 0)
        val store = fixture.store(
            locationCapability = LocationCapability {
                it(LocationResult.Success(xian.centerPoint, 10.0))
            },
            reverseGeocodeCapability = ReverseGeocodeCapability { _, callback ->
                callback(ReverseGeocodeResult.UnsupportedCity(xian))
            }
        )

        store.dispatch(PlaceListIntent.Load)
        advanceUntilIdle()
        store.dispatch(PlaceListIntent.CurrentLocationRequested)
        advanceUntilIdle()

        assertEquals("西安", store.state.value.selectedCity.displayName)
        assertEquals(null, store.state.value.detectedCity)
        store.dispose()
    }

    @Test
    fun lateLocationCallbackCannotWriteDisposedStore() = runTest {
        val fixture = fixture()
        var callback: ((LocationResult) -> Unit)? = null
        val store = fixture.store(LocationCapability { callback = it })
        store.dispatch(PlaceListIntent.CurrentLocationRequested)
        advanceUntilIdle()
        store.dispose()

        callback?.invoke(LocationResult.Success(GeoPoint(31.0, 121.0)))
        advanceUntilIdle()
        assertEquals(PlaceLocationStatus.REQUESTING, store.state.value.locationStatus)
        assertEquals(null, store.state.value.currentLocation)
    }

    private fun kotlinx.coroutines.test.TestScope.fixture(
        storage: InMemoryKeyValueStore = InMemoryKeyValueStore()
    ): StoreFixture {
        val placeRepository = LocalPlaceRepository(storage)
        return StoreFixture(
            cityRepository = LocalExploreCityRepository(storage),
            placeRepository = placeRepository,
            favoriteRepository = LocalFavoriteRepository(storage, placeRepository),
            scope = this
        )
    }
}

private data class StoreFixture(
    val cityRepository: LocalExploreCityRepository,
    val placeRepository: LocalPlaceRepository,
    val favoriteRepository: LocalFavoriteRepository,
    val scope: kotlinx.coroutines.CoroutineScope
) {
    fun store(
        locationCapability: LocationCapability = LocationCapability {
            it(LocationResult.Unavailable)
        },
        reverseGeocodeCapability: ReverseGeocodeCapability = ReverseGeocodeCapability { _, callback ->
            callback(ReverseGeocodeResult.UnsupportedCity())
        },
        remoteDataSource: PlaceRemoteDataSource? = null,
        mode: PlaceListMode = PlaceListMode.ALL,
        initialCategory: PlaceCategory? = null
    ) = PlaceListStore(
        cityRepository = cityRepository,
        placeRepository = placeRepository,
        favoriteRepository = favoriteRepository,
        locationCapability = locationCapability,
        reverseGeocodeCapability = reverseGeocodeCapability,
        remoteDataSource = remoteDataSource,
        parentScope = scope,
        mode = mode,
        initialCategory = initialCategory
    )
}

private data class RemoteRequest(
    val query: String,
    val page: Int,
    val callback: (RemotePlaceResult) -> Unit
)

private class ControllablePlaceRemoteDataSource : PlaceRemoteDataSource {
    val requests = mutableListOf<RemoteRequest>()

    override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
        searchPage(query, city, near, 1, ONLINE_PLACE_RESULT_LIMIT, callback)
    }

    override fun searchPage(
        query: String,
        city: String,
        near: GeoPoint?,
        page: Int,
        pageSize: Int,
        callback: (RemotePlaceResult) -> Unit
    ) {
        requests += RemoteRequest(query, page, callback)
    }

    fun completeFirst(query: String, page: Int, result: RemotePlaceResult) =
        requests.first { it.query == query && it.page == page }.callback(result)

    fun completeLast(query: String, page: Int, result: RemotePlaceResult) =
        requests.last { it.query == query && it.page == page }.callback(result)
}

private fun remotePlaces(prefix: String, count: Int): List<RemotePlace> = (0 until count).map { index ->
    RemotePlace(
        providerId = "$prefix-$index",
        name = "地点$index",
        city = "上海",
        district = null,
        address = null,
        category = PlaceCategory.OTHER,
        tags = emptyList(),
        geoPoint = GeoPoint(31.2, 121.4),
        photoUrl = null
    )
}
