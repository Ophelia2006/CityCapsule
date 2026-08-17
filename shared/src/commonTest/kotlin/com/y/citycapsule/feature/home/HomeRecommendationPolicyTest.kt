package com.y.citycapsule.feature.home

import com.y.citycapsule.core.capsule.CapsuleClock
import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.UtcCapsuleDateFormatter
import com.y.citycapsule.core.city.LocalExploreCityRepository
import com.y.citycapsule.core.favorite.LocalFavoriteRepository
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.PlaceVisualRef
import com.y.citycapsule.core.place.PlaceVisualType
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeRecommendationPolicyTest {
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

    private fun place(id: String, city: String, category: PlaceCategory) = Place(
        id = id,
        name = id,
        city = city,
        category = category,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L
    )
}
