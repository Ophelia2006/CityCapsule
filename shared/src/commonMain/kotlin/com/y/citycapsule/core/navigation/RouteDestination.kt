package com.y.citycapsule.core.navigation

/**
 * A platform-neutral route destination.
 *
 * Kuikly destinations are opened in a Kuikly host. Native destinations are interpreted by
 * AndroidRouteDispatcher or HarmonyRouteDispatcher.
 */
sealed interface RouteDestination {
    val target: String
    val wireType: String

    data class Kuikly(val pageName: String) : RouteDestination {
        init {
            require(pageName.isNotBlank()) { "Kuikly pageName must not be blank." }
        }

        override val target: String = pageName
        override val wireType: String = RouteProtocol.TARGET_KUIKLY
    }

    data class Native(val path: String) : RouteDestination {
        init {
            require(path.startsWith("/native/")) {
                "Native route path must start with '/native/'."
            }
        }

        override val target: String = path
        override val wireType: String = RouteProtocol.TARGET_NATIVE
    }
}

