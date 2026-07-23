package com.y.citycapsule.core.navigation

/**
 * The single source of truth for stable route keys and platform-neutral destinations.
 */
object AppRouteTable {
    const val ROUTE_LAUNCH_GATE = "launch_gate"
    const val ROUTE_ONBOARDING = "onboarding"
    const val ROUTE_HOME = "home"
    const val ROUTE_PLACE_LIST = "place_list"
    const val ROUTE_PLACE_DETAIL = "place_detail"
    const val ROUTE_PLACE_EDITOR = "place_editor"
    const val ROUTE_MAP_EXPLORE = "map_explore"
    const val ROUTE_CAPSULE_EDITOR = "capsule_editor"
    const val ROUTE_CAPSULE_DETAIL = "capsule_detail"
    const val ROUTE_TIMELINE = "timeline"
    const val ROUTE_GALLERY = "gallery"
    const val ROUTE_FAVORITES = "favorites"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_NATIVE_PERMISSION = "native_permission"
    const val ROUTE_NATIVE_FILE_IMPORT = "native_file_import"

    const val PAGE_LAUNCH_GATE = "launch_gate"
    const val PAGE_ONBOARDING = "onboarding"
    const val PAGE_HOME = "home"
    const val PAGE_PLACE_LIST = "place_list"
    const val PAGE_PLACE_DETAIL = "place_detail"
    const val PAGE_PLACE_EDITOR = "place_editor"
    const val PAGE_MAP_EXPLORE = "map_explore"
    const val PAGE_CAPSULE_EDITOR = "capsule_editor"
    const val PAGE_CAPSULE_DETAIL = "capsule_detail"
    const val PAGE_TIMELINE = "timeline"
    const val PAGE_GALLERY = "gallery"
    const val PAGE_FAVORITES = "favorites"
    const val PAGE_PROFILE = "profile"
    const val PAGE_SETTINGS = "settings"

    // Non-business diagnostic pages. They are registration constants, not navigation APIs.
    const val PAGE_ROUTER_DIAGNOSTICS = "router"
    const val PAGE_IMAGE_ADAPTER_DIAGNOSTICS = "image_adapter"

    const val NATIVE_PERMISSION = "/native/permission"
    const val NATIVE_FILE_IMPORT = "/native/file-import"

    fun resolve(
        route: AppRoute,
        action: RouteAction = RouteAction.PUSH
    ): RouteRequest = when (route) {
        AppRoute.LaunchGate -> kuikly(action, ROUTE_LAUNCH_GATE, PAGE_LAUNCH_GATE)
        AppRoute.Onboarding -> kuikly(action, ROUTE_ONBOARDING, PAGE_ONBOARDING)
        AppRoute.Home -> kuikly(action, ROUTE_HOME, PAGE_HOME)
        AppRoute.PlaceList -> kuikly(action, ROUTE_PLACE_LIST, PAGE_PLACE_LIST)
        is AppRoute.PlaceDetail -> kuikly(
            action = action,
            routeKey = ROUTE_PLACE_DETAIL,
            pageName = PAGE_PLACE_DETAIL,
            params = mapOf("placeId" to route.placeId)
        )

        is AppRoute.PlaceEditor -> kuikly(
            action = action,
            routeKey = ROUTE_PLACE_EDITOR,
            pageName = PAGE_PLACE_EDITOR,
            params = optionalParams("placeId" to route.placeId)
        )

        AppRoute.MapExplore -> kuikly(action, ROUTE_MAP_EXPLORE, PAGE_MAP_EXPLORE)
        is AppRoute.CapsuleEditor -> kuikly(
            action = action,
            routeKey = ROUTE_CAPSULE_EDITOR,
            pageName = PAGE_CAPSULE_EDITOR,
            params = optionalParams(
                "capsuleId" to route.capsuleId,
                "placeId" to route.placeId
            )
        )

        is AppRoute.CapsuleDetail -> kuikly(
            action = action,
            routeKey = ROUTE_CAPSULE_DETAIL,
            pageName = PAGE_CAPSULE_DETAIL,
            params = mapOf("capsuleId" to route.capsuleId)
        )

        AppRoute.Timeline -> kuikly(action, ROUTE_TIMELINE, PAGE_TIMELINE)
        AppRoute.Gallery -> kuikly(action, ROUTE_GALLERY, PAGE_GALLERY)
        AppRoute.Favorites -> kuikly(action, ROUTE_FAVORITES, PAGE_FAVORITES)
        AppRoute.Profile -> kuikly(action, ROUTE_PROFILE, PAGE_PROFILE)
        AppRoute.Settings -> kuikly(action, ROUTE_SETTINGS, PAGE_SETTINGS)
        is AppRoute.NativePermission -> native(
            action = action,
            routeKey = ROUTE_NATIVE_PERMISSION,
            path = NATIVE_PERMISSION,
            params = mapOf("permissionType" to route.permissionType)
        )

        is AppRoute.NativeFileImport -> native(
            action = action,
            routeKey = ROUTE_NATIVE_FILE_IMPORT,
            path = NATIVE_FILE_IMPORT,
            params = mapOf("requestId" to route.requestId)
        )
    }

    fun resolveBackTo(routeKey: AppRouteKey): RouteRequest {
        val wireRouteKey = wireRouteKey(routeKey)
        return RouteRequest(
            action = RouteAction.BACK_TO,
            routeKey = wireRouteKey,
            destination = destinationForRouteKey(routeKey)
        )
    }

    fun wireRouteKey(routeKey: AppRouteKey): String = when (routeKey) {
        AppRouteKey.LAUNCH_GATE -> ROUTE_LAUNCH_GATE
        AppRouteKey.ONBOARDING -> ROUTE_ONBOARDING
        AppRouteKey.HOME -> ROUTE_HOME
        AppRouteKey.PLACE_LIST -> ROUTE_PLACE_LIST
        AppRouteKey.PLACE_DETAIL -> ROUTE_PLACE_DETAIL
        AppRouteKey.PLACE_EDITOR -> ROUTE_PLACE_EDITOR
        AppRouteKey.MAP_EXPLORE -> ROUTE_MAP_EXPLORE
        AppRouteKey.CAPSULE_EDITOR -> ROUTE_CAPSULE_EDITOR
        AppRouteKey.CAPSULE_DETAIL -> ROUTE_CAPSULE_DETAIL
        AppRouteKey.TIMELINE -> ROUTE_TIMELINE
        AppRouteKey.GALLERY -> ROUTE_GALLERY
        AppRouteKey.FAVORITES -> ROUTE_FAVORITES
        AppRouteKey.PROFILE -> ROUTE_PROFILE
        AppRouteKey.SETTINGS -> ROUTE_SETTINGS
        AppRouteKey.NATIVE_PERMISSION -> ROUTE_NATIVE_PERMISSION
        AppRouteKey.NATIVE_FILE_IMPORT -> ROUTE_NATIVE_FILE_IMPORT
    }

    fun destinationForRouteKey(routeKey: AppRouteKey): RouteDestination = when (routeKey) {
        AppRouteKey.LAUNCH_GATE -> RouteDestination.Kuikly(PAGE_LAUNCH_GATE)
        AppRouteKey.ONBOARDING -> RouteDestination.Kuikly(PAGE_ONBOARDING)
        AppRouteKey.HOME -> RouteDestination.Kuikly(PAGE_HOME)
        AppRouteKey.PLACE_LIST -> RouteDestination.Kuikly(PAGE_PLACE_LIST)
        AppRouteKey.PLACE_DETAIL -> RouteDestination.Kuikly(PAGE_PLACE_DETAIL)
        AppRouteKey.PLACE_EDITOR -> RouteDestination.Kuikly(PAGE_PLACE_EDITOR)
        AppRouteKey.MAP_EXPLORE -> RouteDestination.Kuikly(PAGE_MAP_EXPLORE)
        AppRouteKey.CAPSULE_EDITOR -> RouteDestination.Kuikly(PAGE_CAPSULE_EDITOR)
        AppRouteKey.CAPSULE_DETAIL -> RouteDestination.Kuikly(PAGE_CAPSULE_DETAIL)
        AppRouteKey.TIMELINE -> RouteDestination.Kuikly(PAGE_TIMELINE)
        AppRouteKey.GALLERY -> RouteDestination.Kuikly(PAGE_GALLERY)
        AppRouteKey.FAVORITES -> RouteDestination.Kuikly(PAGE_FAVORITES)
        AppRouteKey.PROFILE -> RouteDestination.Kuikly(PAGE_PROFILE)
        AppRouteKey.SETTINGS -> RouteDestination.Kuikly(PAGE_SETTINGS)
        AppRouteKey.NATIVE_PERMISSION -> RouteDestination.Native(NATIVE_PERMISSION)
        AppRouteKey.NATIVE_FILE_IMPORT -> RouteDestination.Native(NATIVE_FILE_IMPORT)
    }

    private fun kuikly(
        action: RouteAction,
        routeKey: String,
        pageName: String,
        params: Map<String, Any?> = emptyMap()
    ): RouteRequest = RouteRequest(
        action = action,
        routeKey = routeKey,
        destination = RouteDestination.Kuikly(pageName),
        params = params
    )

    private fun native(
        action: RouteAction,
        routeKey: String,
        path: String,
        params: Map<String, Any?> = emptyMap()
    ): RouteRequest = RouteRequest(
        action = action,
        routeKey = routeKey,
        destination = RouteDestination.Native(path),
        params = params
    )

    private fun optionalParams(
        vararg params: Pair<String, Any?>
    ): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        params.forEach { (key, value) ->
            if (value != null) {
                result[key] = value
            }
        }
        return result
    }
}

