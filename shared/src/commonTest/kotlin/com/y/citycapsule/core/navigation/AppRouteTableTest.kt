package com.y.citycapsule.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class AppRouteTableTest {

    @Test
    fun homeResolvesToStableKuiklyDestination() {
        val request = AppRouteTable.resolve(AppRoute.Home)

        assertEquals(RouteAction.PUSH, request.action)
        assertEquals(AppRouteTable.ROUTE_HOME, request.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_HOME),
            request.destination
        )
        assertEquals(emptyMap(), request.params)
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
        assertEquals(AppRouteTable.ROUTE_PROFILE, profile.routeKey)
        assertEquals(
            RouteDestination.Kuikly(AppRouteTable.PAGE_PROFILE),
            profile.destination
        )
    }

    @Test
    fun detailRouteKeepsOnlyBusinessIdentifier() {
        val request = AppRouteTable.resolve(AppRoute.PlaceDetail("place-42"))

        assertEquals(AppRouteTable.ROUTE_PLACE_DETAIL, request.routeKey)
        assertEquals(mapOf("placeId" to "place-42"), request.params)
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
}
