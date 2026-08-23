package com.y.citycapsule.feature.home

import com.y.citycapsule.core.capsule.CapsuleClock
import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.UtcCapsuleDateFormatter
import com.y.citycapsule.core.city.LocalExploreCityRepository
import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceDraft
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.PlaceVisualRef
import com.y.citycapsule.core.place.PlaceVisualType
import com.y.citycapsule.core.place.PlacePhotoCacheEntry
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.RemotePlace
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeRecommendationPolicyTest {
    @Test
    fun heroPrefersFirstPlaceWithConfirmedPhoto() {
        val withoutPhoto = place("without", "上海", PlaceCategory.CULTURE)
        val withPhoto = place("with", "上海", PlaceCategory.NATURE)
        val state = HomeUiState(
            rankedPlaces = listOf(withoutPhoto, withPhoto),
            photoByPlaceId = mapOf(
                withPhoto.id to PlacePhotoCacheEntry(withPhoto.id, "https://example.com/photo.jpg", "test", 1L)
            )
        )

        assertEquals(withPhoto.id, state.featuredPlace?.id)
    }

    @Test
    fun recoveryReadOnlyCatalogIsNotTreatedAsWritableEmptyState() {
        val storage = InMemoryKeyValueStore().apply {
            seedRaw(AppStorageKeys.Places.CATALOG, encodedValue = "{broken")
        }
        val places = LocalPlaceRepository(storage)
        val holder = HomeStateHolder(
            LocalProfileRepository(storage),
            LocalExploreCityRepository(storage),
            places,
            LocalFavoriteRepository(storage, places),
            LocalCapsuleRepository(storage),
            UtcCapsuleDateFormatter
        )

        holder.load()

        assertTrue(holder.state.catalogReadOnly)
        assertTrue(holder.state.rankedPlaces.isEmpty())
    }

    @Test
    fun homeKeepsOnlyThreeNewestPublishedMemoriesAndJoinsPlaces() {
        val storage = InMemoryKeyValueStore()
        val places = LocalPlaceRepository(storage)
        places.getCatalogSnapshot { }
        var now = 100L
        var id = 0
        val capsules = LocalCapsuleRepository(
            storage,
            CapsuleClock { now++ },
            CapsuleIdGenerator { "capsule_${id++}" }
        )
        repeat(4) { index ->
            capsules.publish(
                CapsuleDraft(
                    content = "memory_$index",
                    placeId = "seed_shanghai_museum"
                )
            ) { }
        }
        val holder = HomeStateHolder(
            LocalProfileRepository(storage),
            LocalExploreCityRepository(storage),
            places,
            LocalFavoriteRepository(storage, places),
            capsules,
            UtcCapsuleDateFormatter
        )

        holder.load()

        assertEquals(listOf("memory_3", "memory_2", "memory_1"), holder.state.recentMemories.map { it.capsule.content })
        assertTrue(holder.state.recentMemories.all { it.place?.id == "seed_shanghai_museum" })
    }

    @Test
    fun currentCityOutranksOtherCities() {
        val ranked = HomeRecommendationPolicy.rank(
            listOf(
                place("a_other", "杭州", PlaceCategory.NATURE),
                place("z_current", "上海", PlaceCategory.CULTURE)
            ),
            currentCity = "上海",
            favoriteIds = setOf("a_other"),
            recordedPlaceIds = emptySet()
        )
        assertEquals("z_current", ranked.first().id)
    }

    @Test
    fun wantedOrUnrecordedOutranksRecordedPlaceWithinCity() {
        val ranked = HomeRecommendationPolicy.rank(
            listOf(
                place("a_recorded", "上海", PlaceCategory.CULTURE),
                place("b_unrecorded", "上海", PlaceCategory.NATURE),
                place("c_wanted", "上海", PlaceCategory.FOOD)
            ),
            currentCity = "上海",
            favoriteIds = setOf("c_wanted"),
            recordedPlaceIds = setOf("a_recorded", "c_wanted")
        )
        assertTrue(ranked.take(2).map { it.id }.containsAll(listOf("b_unrecorded", "c_wanted")))
        assertEquals("a_recorded", ranked.last().id)
    }

    @Test
    fun categoryDiversityUsesStableCategoryAndIdOrder() {
        val ranked = HomeRecommendationPolicy.rank(
            listOf(
                place("culture_b", "上海", PlaceCategory.CULTURE),
                place("culture_a", "上海", PlaceCategory.CULTURE),
                place("nature_a", "上海", PlaceCategory.NATURE)
            ),
            currentCity = "上海",
            favoriteIds = emptySet(),
            recordedPlaceIds = emptySet()
        )
        assertEquals(listOf("culture_a", "nature_a", "culture_b"), ranked.map { it.id })
    }

    @Test
    fun coverAndDistanceAreUsedWithoutBreakingDiscoveryPriority() {
        val near = place("near", "上海", PlaceCategory.CULTURE).copy(
            geoPoint = GeoPoint(31.2305, 121.4737),
            visualRef = PlaceVisualRef(PlaceVisualType.MANAGED_FILE, "file:///near.jpg")
        )
        val far = place("far", "上海", PlaceCategory.CULTURE).copy(
            geoPoint = GeoPoint(31.9, 121.9),
            visualRef = PlaceVisualRef(PlaceVisualType.MANAGED_FILE, "file:///far.jpg")
        )
        val noCover = place("no_cover", "上海", PlaceCategory.CULTURE).copy(
            geoPoint = GeoPoint(31.2304, 121.4737)
        )
        val ranked = HomeRecommendationPolicy.rank(
            listOf(far, noCover, near),
            "上海",
            favoriteIds = emptySet(),
            recordedPlaceIds = emptySet(),
            currentLocation = GeoPoint(31.2304, 121.4737)
        )
        assertEquals(listOf("near", "far", "no_cover"), ranked.map(Place::id))
    }

    @Test
    fun favoriteToggleKeepsVisibleHomeRecommendationSnapshotStable() {
        val storage = InMemoryKeyValueStore()
        val places = LocalPlaceRepository(storage)
        val holder = HomeStateHolder(
            LocalProfileRepository(storage),
            LocalExploreCityRepository(storage),
            places,
            LocalFavoriteRepository(storage, places),
            LocalCapsuleRepository(storage),
            UtcCapsuleDateFormatter
        )
        holder.load()
        val rankedIds = holder.state.rankedPlaces.map(Place::id)
        val featuredId = holder.state.featuredPlace?.id
        val supportingIds = holder.state.supportingPlaceIds
        val supportingTitle = holder.state.supportingTitle
        val toggledId = requireNotNull(holder.state.featuredPlace).id

        holder.toggleFavorite(toggledId)

        assertTrue(toggledId in holder.state.favoriteIds)
        assertEquals(rankedIds, holder.state.rankedPlaces.map(Place::id))
        assertEquals(featuredId, holder.state.featuredPlace?.id)
        assertEquals(supportingIds, holder.state.supportingPlaceIds)
        assertEquals(supportingTitle, holder.state.supportingTitle)
        assertEquals(null, holder.state.notice)
    }

    @Test
    fun dynamicExploreCityIsKeptAndLoadsOnlineRecommendationsWhenLocalCatalogIsEmpty() {
        val storage = InMemoryKeyValueStore()
        val cities = LocalExploreCityRepository(storage)
        val xian = CityDefinition("remote-xian", "西安", GeoPoint(34.3416, 108.9398), false, 0)
        cities.select(xian) { }
        var requestedCity: String? = null
        val requestedQueries = mutableListOf<String>()
        val remote = object : PlaceRemoteDataSource {
            override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
                requestedCity = city
                requestedQueries += query
                callback(RemotePlaceResult.Success(listOf(
                    RemotePlace("poi-xian", "西安城墙", "西安", "碑林区", "南大街", PlaceCategory.LANDMARK, emptyList(), xian.centerPoint, null)
                )))
            }
        }
        val places = LocalPlaceRepository(storage)
        val holder = HomeStateHolder(
            LocalProfileRepository(storage), cities, places,
            LocalFavoriteRepository(storage, places), LocalCapsuleRepository(storage),
            UtcCapsuleDateFormatter, remoteDataSource = remote
        )

        holder.load()

        assertEquals("西安", holder.state.selectedCity.displayName)
        assertEquals("西安", requestedCity)
        assertEquals("景点", requestedQueries.first())
        assertEquals(listOf("西安城墙"), holder.state.onlineRecommendations.map(RemotePlace::name))
    }

    @Test
    fun cityWithLocalPlacesButNoUsableHeroLoadsOnlineHeroCandidate() {
        val storage = InMemoryKeyValueStore()
        val cities = LocalExploreCityRepository(storage)
        val xian = CityDefinition("remote-xian", "西安", GeoPoint(34.3416, 108.9398), false, 0)
        cities.select(xian) { }
        val places = LocalPlaceRepository(storage)
        places.createPlace(PlaceDraft("本地西安地点", "西安", category = PlaceCategory.LANDMARK)) { }
        val remote = object : PlaceRemoteDataSource {
            override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
                callback(RemotePlaceResult.Success(listOf(
                    RemotePlace("online-xian", "西安城墙", "西安", "碑林区", "南大街", PlaceCategory.LANDMARK, emptyList(), xian.centerPoint, "https://example.com/xian.jpg")
                )))
            }
        }
        val holder = HomeStateHolder(
            LocalProfileRepository(storage), cities, places,
            LocalFavoriteRepository(storage, places), LocalCapsuleRepository(storage),
            UtcCapsuleDateFormatter, remoteDataSource = remote
        )

        holder.load()

        assertEquals("本地西安地点", holder.state.rankedPlaces.single().name)
        assertEquals(listOf("online-xian"), holder.state.onlineRecommendations.map(RemotePlace::providerId))
    }

    @Test
    fun cityNameSearchPersistsResolvedDynamicCityAndReloadsHome() {
        val storage = InMemoryKeyValueStore()
        val cities = LocalExploreCityRepository(storage)
        val center = GeoPoint(30.5728, 104.0668)
        val remote = object : PlaceRemoteDataSource {
            override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
                callback(RemotePlaceResult.Success(listOf(
                    RemotePlace("chengdu-poi", "成都博物馆", "成都", "青羊区", "小河街", PlaceCategory.CULTURE, listOf("博物馆"), center, null)
                )))
            }
        }
        val places = LocalPlaceRepository(storage)
        val holder = HomeStateHolder(
            LocalProfileRepository(storage), cities, places,
            LocalFavoriteRepository(storage, places), LocalCapsuleRepository(storage),
            UtcCapsuleDateFormatter, remoteDataSource = remote
        )

        holder.searchAndSelectCity("成都市")

        assertEquals("成都", holder.state.selectedCity.displayName)
        assertEquals(center, holder.state.selectedCity.centerPoint)
        var restored: CityDefinition? = null
        cities.get { restored = (it as? com.y.citycapsule.core.storage.StorageResult.Success)?.value?.selectedCity }
        assertEquals("成都", restored?.displayName)
    }

    private fun place(id: String, city: String, category: PlaceCategory) = Place(
        id = id,
        name = id,
        city = city,
        category = category,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L
    )
}
