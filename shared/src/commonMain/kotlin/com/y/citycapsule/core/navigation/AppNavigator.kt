package com.y.citycapsule.core.navigation

/**
 * The only navigation API exposed to shared pages and stores.
 */
interface AppNavigator {
    fun navigate(route: AppRoute)

    fun replace(route: AppRoute)

    fun back()

    /**
     * Returns to the latest matching route. If the target is absent after a replace operation,
     * the platform dispatcher restores it by replacing the current route.
     */
    fun backTo(routeKey: AppRouteKey)
}

