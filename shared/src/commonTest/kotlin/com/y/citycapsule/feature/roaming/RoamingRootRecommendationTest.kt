package com.y.citycapsule.feature.roaming

import com.y.citycapsule.core.route.LocalRoute
import com.y.citycapsule.core.capsule.CityCapsule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoamingRootRecommendationTest {
    @Test
    fun routeWithMoreWantToPlacesWinsOverNewerRoute() {
        val result = selectSuggestedRoute(
            routes = listOf(route("wanted", listOf("a", "b"), 1L), route("newer", listOf("c"), 2L)),
            favoriteIds = setOf("a", "b"),
            availablePlaceIds = setOf("a", "b", "c")
        )
        assertEquals("wanted", result?.id)
    }

    @Test
    fun newestRouteBreaksTieAndUnavailableRoutesAreIgnored() {
        val unavailable = route("missing", listOf("missing"), 9L)
        val older = route("older", listOf("a"), 1L)
        val newer = route("newer", listOf("b"), 2L)
        assertEquals("newer", selectSuggestedRoute(listOf(unavailable, older, newer), emptySet(), setOf("a", "b"))?.id)
        assertNull(selectSuggestedRoute(listOf(unavailable), emptySet(), setOf("a", "b")))
    }

    @Test
    fun memoryCoverUsesNewestPhotoFromTheSameRoamingSession() {
        val capsules = listOf(
            capsule("old", "session-a", listOf("old.jpg"), 1L),
            capsule("other", "session-b", listOf("other.jpg"), 9L),
            capsule("new", "session-a", listOf("new.jpg"), 3L)
        )
        assertEquals("new.jpg", findRoamingMemoryCover(capsules, "session-a"))
        assertNull(findRoamingMemoryCover(capsules, "missing"))
    }

    private fun route(id: String, placeIds: List<String>, createdAt: Long) = LocalRoute(id, id, placeIds, createdAt)

    private fun capsule(id: String, sessionId: String, images: List<String>, createdAt: Long) = CityCapsule(
        id = id,
        content = id,
        placeId = "place",
        roamingSessionId = sessionId,
        imagePaths = images,
        createdAtEpochMs = createdAt,
        updatedAtEpochMs = createdAt
    )
}
