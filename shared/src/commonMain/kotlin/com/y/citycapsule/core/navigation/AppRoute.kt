package com.y.citycapsule.core.navigation

import com.y.citycapsule.core.place.PlaceCategory

/**
 * Shared, platform-agnostic navigation contract.
 *
 * Feature code may depend on these route types, but must not depend on Android Intent,
 * HarmonyOS HMRouter, or raw page-name strings.
 */
sealed interface AppRoute {
    data object LaunchGate : AppRoute

    data object Onboarding : AppRoute

    data object Home : AppRoute

    data class PlaceList(val initialCategory: PlaceCategory? = null) : AppRoute

    data class PlaceDetail(val placeId: String) : AppRoute {
        init {
            requireRouteArgument("placeId", placeId)
        }
    }

    data class PlaceEditor(val placeId: String? = null) : AppRoute {
        init {
            requireOptionalRouteArgument("placeId", placeId)
        }
    }

    data object MapExplore : AppRoute

    data object LocalRoutes : AppRoute

    data class LocalRouteEditor(val routeId: String? = null) : AppRoute {
        init { requireOptionalRouteArgument("routeId", routeId) }
    }

    data class RoamingSession(val routeId: String? = null) : AppRoute {
        init { requireOptionalRouteArgument("routeId", routeId) }
    }

    data class RoamingHistory(val recordId: String? = null) : AppRoute {
        init { requireOptionalRouteArgument("recordId", recordId) }
    }

    data class CapsuleEditor(
        val capsuleId: String? = null,
        val placeId: String? = null,
        val roamingSessionId: String? = null
    ) : AppRoute {
        init {
            requireOptionalRouteArgument("capsuleId", capsuleId)
            requireOptionalRouteArgument("placeId", placeId)
            requireOptionalRouteArgument("roamingSessionId", roamingSessionId)
        }
    }

    data class CapsuleDetail(val capsuleId: String) : AppRoute {
        init {
            requireRouteArgument("capsuleId", capsuleId)
        }
    }

    data object Timeline : AppRoute

    data object Gallery : AppRoute

    data object Favorites : AppRoute

    data object Profile : AppRoute

    data object ProfileEdit : AppRoute

    data object Settings : AppRoute

    data class NativePermission(val permissionType: String) : AppRoute {
        init {
            requireRouteArgument("permissionType", permissionType)
        }
    }

    data class NativeFileImport(val requestId: String) : AppRoute {
        init {
            requireRouteArgument("requestId", requestId)
        }
    }
}

/** Stable, typed targets accepted by [AppNavigator.backTo]. */
enum class AppRouteKey {
    LAUNCH_GATE,
    ONBOARDING,
    HOME,
    PLACE_LIST,
    PLACE_DETAIL,
    PLACE_EDITOR,
    MAP_EXPLORE,
    LOCAL_ROUTES,
    LOCAL_ROUTE_EDITOR,
    ROAMING_SESSION,
    ROAMING_HISTORY,
    CAPSULE_EDITOR,
    CAPSULE_DETAIL,
    TIMELINE,
    GALLERY,
    FAVORITES,
    PROFILE,
    PROFILE_EDIT,
    SETTINGS,
    NATIVE_PERMISSION,
    NATIVE_FILE_IMPORT
}

private fun requireRouteArgument(name: String, value: String) {
    require(value.isNotBlank()) { "Route argument '$name' must not be blank." }
}

private fun requireOptionalRouteArgument(name: String, value: String?) {
    if (value != null) {
        requireRouteArgument(name, value)
    }
}
