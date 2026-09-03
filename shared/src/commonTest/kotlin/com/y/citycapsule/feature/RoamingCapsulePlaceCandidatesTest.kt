package com.y.citycapsule.feature.roaming

import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceSource
import kotlin.test.Test
import kotlin.test.assertEquals

class RoamingCapsulePlaceCandidatesTest {
    @Test
    fun plannedRoamingPrioritizesRouteThenNearbyAndStillIncludesEveryPlace() {
        val route = place("route")
        val nearby = place("nearby")
        val other = place("other")
        val state = RoamingSessionUiState(
            activeRouteId = "route-id",
            routePlaces = listOf(route),
            nearbyPlaces = listOf(NearbyRoamingPlace(nearby, 20.0, false)),
            availablePlaces = listOf(other, nearby, route)
        )

        assertEquals(listOf("route", "nearby", "other"), capsulePlaceCandidates(state).map(Place::id))
        assertEquals("nearby", defaultCapsulePlaceId(state))
    }

    @Test
    fun freeRoamingPrioritizesNearbyAndStillIncludesEveryPlace() {
        val nearby = place("nearby")
        val other = place("other")
        val state = RoamingSessionUiState(
            nearbyPlaces = listOf(NearbyRoamingPlace(nearby, 30.0, false)),
            availablePlaces = listOf(other, nearby)
        )

        assertEquals(listOf("nearby", "other"), capsulePlaceCandidates(state).map(Place::id))
        assertEquals("nearby", defaultCapsulePlaceId(state))
    }

    private fun place(id: String) = Place(
        id = id,
        name = id,
        city = "测试城市",
        category = PlaceCategory.OTHER,
        source = PlaceSource.USER,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L
    )
}
