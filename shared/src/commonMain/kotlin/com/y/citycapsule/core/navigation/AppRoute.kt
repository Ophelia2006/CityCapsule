package com.y.citycapsule.core.navigation

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

    data object PlaceList : AppRoute

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

    data class CapsuleEditor(
        val capsuleId: String? = null,
        val placeId: String? = null
    ) : AppRoute {
        init {
            requireOptionalRouteArgument("capsuleId", capsuleId)
            requireOptionalRouteArgument("placeId", placeId)
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
    CAPSULE_EDITOR,
    CAPSULE_DETAIL,
    TIMELINE,
    GALLERY,
    FAVORITES,
    PROFILE,
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

