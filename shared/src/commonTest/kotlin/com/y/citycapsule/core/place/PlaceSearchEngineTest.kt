package com.y.citycapsule.core.place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceSearchEngineTest {
    private val places = listOf(
        placeFixture(
            id = "museum_exact",
            name = "Museum",
            city = "上海",
            category = PlaceCategory.CULTURE,
            updatedAtEpochMs = 100L
        ),
        placeFixture(
            id = "museum_prefix",
            name = "Museum East",
            city = "上海",
            category = PlaceCategory.CULTURE,
            updatedAtEpochMs = 300L
        ),
        placeFixture(
            id = "museum_tag",
            name = "西岸艺术中心",
            city = "上海",
            district = "徐汇区",
            category = PlaceCategory.CULTURE,
            tags = listOf("Museum"),
            updatedAtEpochMs = 400L
        ),
        placeFixture(
            id = "park",
            name = "湖畔公园",
            city = "杭州",
            district = "西湖区",
            category = PlaceCategory.NATURE,
            address = "北山街",
            tags = listOf("散步"),
            updatedAtEpochMs = 500L
        )
    )

    @Test
    fun exactNameRanksBeforePrefixAndTagMatches() {
        val result = PlaceSearchEngine.search(places, query = "  MUSEUM ")

        assertEquals(
            listOf("museum_exact", "museum_prefix", "museum_tag"),
            result.places.map(Place::id)
        )
        assertEquals("museum", result.normalizedQuery)
    }

    @Test
    fun multipleTermsCanMatchAcrossDifferentFields() {
        val result = PlaceSearchEngine.search(places, query = " 上海   徐汇 ")

        assertEquals(listOf("museum_tag"), result.places.map(Place::id))
    }

    @Test
    fun categoriesAreOrWhileCityAndFavoritesAreAnd() {
        val result = PlaceSearchEngine.search(
            places = places,
            favoriteIds = setOf("museum_tag", "park"),
            filter = PlaceFilter(
                categories = setOf(PlaceCategory.CULTURE, PlaceCategory.NATURE),
                city = " 上海 ",
                favoritesOnly = true
            )
        )

        assertEquals(listOf("museum_tag"), result.places.map(Place::id))
        assertEquals("上海", result.appliedFilter.city)
    }

    @Test
    fun emptyQueryUsesStableUpdatedTimeOrder() {
        val result = PlaceSearchEngine.search(places)

        assertEquals(
            listOf("park", "museum_tag", "museum_prefix", "museum_exact"),
            result.places.map(Place::id)
        )
    }

    @Test
    fun noMatchingQueryReturnsEmptyResult() {
        val result = PlaceSearchEngine.search(places, query = "不存在的地点")

        assertTrue(result.places.isEmpty())
    }
}
