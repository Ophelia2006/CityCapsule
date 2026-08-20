package com.y.citycapsule.core.navigation

import com.y.citycapsule.core.place.PlaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class AppRouteTableTest {

    @Test
    fun homeResolvesToExploreInsideTheSingleAppShell() {
        val request = AppRouteTable.resolve(AppRoute.Home)

        assertEquals(RouteAction.PUSH, request.action)
        assertEquals(AppRouteTable.ROUTE_APP_SHELL, request.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_APP_SHELL),
            request.destination
        )
        assertEquals(
            mapOf(AppRouteTable.PARAM_INITIAL_ROOT_TAB to AppRouteTable.ROUTE_HOME),
            request.params
        )
    }

    @Test
    fun startupAndOnboardingRoutesUseStableKuiklyDestinations() {
        val launchGate = AppRouteTable.resolve(AppRoute.LaunchGate)
        val onboarding = AppRouteTable.resolve(AppRoute.Onboarding)
        val profile = AppRouteTable.resolve(AppRoute.Profile)

        assertEquals(AppRouteTable.ROUTE_LAUNCH_GATE, launchGate.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_LAUNCH_GATE),
            launchGate.destination
        )
        assertEquals(AppRouteTable.ROUTE_ONBOARDING, onboarding.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_ONBOARDING),
            onboarding.destination
        )
        assertEquals(AppRouteTable.ROUTE_APP_SHELL, profile.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_APP_SHELL),
            profile.destination
        )
        assertEquals(AppRouteTable.ROUTE_PROFILE, profile.params[AppRouteTable.PARAM_INITIAL_ROOT_TAB])
    }

    @Test
    fun detailRouteKeepsOnlyBusinessIdentifier() {
        val request = AppRouteTable.resolve(AppRoute.PlaceDetail("place-42"))

        assertEquals(AppRouteTable.ROUTE_PLACE_DETAIL, request.routeKey)
        assertEquals(mapOf("placeId" to "place-42"), request.params)
    }

    @Test
    fun placeListEditorAndFavoritesUseFrozenDestinations() {
        val placeList = AppRouteTable.resolve(AppRoute.PlaceList())
        val newPlace = AppRouteTable.resolve(AppRoute.PlaceEditor())
        val editPlace = AppRouteTable.resolve(AppRoute.PlaceEditor("place-7"))
        val favorites = AppRouteTable.resolve(AppRoute.Favorites)

        assertEquals(AppRouteTable.ROUTE_PLACE_LIST, placeList.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_PLACE_LIST),
            placeList.destination
        )
        assertEquals(emptyMap(), newPlace.params)
        assertEquals(mapOf("placeId" to "place-7"), editPlace.params)
        assertEquals(AppRouteTable.ROUTE_FAVORITES, favorites.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_FAVORITES),
            favorites.destination
        )
    }

    @Test
    fun placeListCarriesOptionalInitialCategory() {
        val request = AppRouteTable.resolve(AppRoute.PlaceList(PlaceCategory.CULTURE))

        assertEquals("culture", request.params[AppRouteTable.PARAM_INITIAL_CATEGORY])
    }

    @Test
    fun optionalArgumentsAreOmittedWhenNull() {
        val request = AppRouteTable.resolve(
            AppRoute.CapsuleEditor(capsuleId = null, placeId = "place-1")
        )

        assertFalse(request.params.containsKey("capsuleId"))
        assertEquals("place-1", request.params["placeId"])
    }

    @Test
    fun capsuleEditorCarriesOptionalRoamingSessionIdentifier() {
        val request = AppRouteTable.resolve(
            AppRoute.CapsuleEditor(placeId = "place-1", roamingSessionId = "1700")
        )

        assertEquals("place-1", request.params[AppRouteTable.PARAM_PLACE_ID])
        assertEquals("1700", request.params[AppRouteTable.PARAM_ROAMING_SESSION_ID])
    }

    @Test
    fun localRoutesUseSecondaryPagesAndOnlyPassRouteId() {
        val list = AppRouteTable.resolve(AppRoute.LocalRoutes)
        val create = AppRouteTable.resolve(AppRoute.LocalRouteEditor())
        val edit = AppRouteTable.resolve(AppRoute.LocalRouteEditor("route-1"))

        assertEquals(RouteDestination.Kuikly(AppRouteTable.PAGE_LOCAL_ROUTES), list.destination)
        assertEquals(emptyMap(), create.params)
        assertEquals(mapOf(AppRouteTable.PARAM_ROUTE_ID to "route-1"), edit.params)
    }

    @Test
    fun roamingSessionOnlyPassesOptionalRouteId() {
        val free = AppRouteTable.resolve(AppRoute.RoamingSession())
        val routed = AppRouteTable.resolve(AppRoute.RoamingSession("route-1"))

        assertEquals(RouteDestination.Kuikly(AppRouteTable.PAGE_ROAMING_SESSION), free.destination)
        assertEquals(emptyMap(), free.params)
        assertEquals(mapOf(AppRouteTable.PARAM_ROUTE_ID to "route-1"), routed.params)
    }

    @Test
    fun recordRoutesUseStablePagesAndOnlyPassIdentifiers() {
        val editor = AppRouteTable.resolve(
            AppRoute.CapsuleEditor(capsuleId = "capsule-1", placeId = "place-1")
        )
        val detail = AppRouteTable.resolve(AppRoute.CapsuleDetail("capsule-1"))
        val timeline = AppRouteTable.resolve(AppRoute.Timeline)
        val gallery = AppRouteTable.resolve(AppRoute.Gallery)

        assertEquals(mapOf("capsuleId" to "capsule-1", "placeId" to "place-1"), editor.params)
        assertEquals(mapOf("capsuleId" to "capsule-1"), detail.params)
        assertEquals(RouteDestination.Kuikly(AppRouteTable.PAGE_APP_SHELL), timeline.destination)
        assertEquals(AppRouteTable.ROUTE_APP_SHELL, timeline.routeKey)
        assertEquals(AppRouteTable.ROUTE_TIMELINE, timeline.params[AppRouteTable.PARAM_INITIAL_ROOT_TAB])
        assertEquals(RouteDestination.Kuikly(AppRouteTable.PAGE_GALLERY), gallery.destination)
    }

    @Test
    fun profileEditIsASecondaryPageOutsideTheAppShell() {
        val request = AppRouteTable.resolve(AppRoute.ProfileEdit)

        assertEquals(AppRouteTable.ROUTE_PROFILE_EDIT, request.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_PROFILE_EDIT),
            request.destination
        )
        assertEquals(emptyMap(), request.params)
    }

    @Test
    fun nativeRouteResolvesToRegisteredPath() {
        val request = AppRouteTable.resolve(
            AppRoute.NativePermission("location")
        )

        assertIs<RouteDestination.Native>(request.destination)
        assertEquals(AppRouteTable.NATIVE_PERMISSION, request.destination.target)
        assertEquals("location", request.params["permissionType"])
    }

    @Test
    fun blankRequiredArgumentIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            AppRoute.PlaceDetail(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            AppRoute.PlaceEditor(" ")
        }
    }

    @Test
    fun everyTypedBackToKeyResolvesToItsStableWireKey() {
        AppRouteKey.entries.forEach { routeKey ->
            val request = AppRouteTable.resolveBackTo(routeKey)

            assertEquals(RouteAction.BACK_TO, request.action)
            assertEquals(AppRouteTable.wireRouteKey(routeKey), request.routeKey)
            assertEquals(AppRouteTable.destinationForRouteKey(routeKey), request.destination)
        }
    }

    @Test
    fun rootBackToFallbackCarriesTheRequestedInitialTab() {
        val timeline = AppRouteTable.resolveBackTo(AppRouteKey.TIMELINE)

        assertEquals(AppRouteTable.ROUTE_APP_SHELL, timeline.routeKey)
        assertEquals(RouteDestination.Kuikly(AppRouteTable.PAGE_APP_SHELL), timeline.destination)
        assertEquals(
            AppRouteTable.ROUTE_TIMELINE,
            timeline.params[AppRouteTable.PARAM_INITIAL_ROOT_TAB]
        )
    }
}
