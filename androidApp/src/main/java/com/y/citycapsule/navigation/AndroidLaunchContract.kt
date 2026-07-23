package com.y.citycapsule.navigation

import com.y.citycapsule.core.navigation.AppRouteTable

/**
 * Android launcher contract.
 *
 * Explicit route requests always win. Only a system launch without route extras
 * falls back to LaunchGate, so every cold start re-evaluates the shared local
 * profile/onboarding state before a business page becomes the root.
 */
internal object AndroidLaunchContract {
    const val DEFAULT_PAGE_NAME: String = AppRouteTable.PAGE_LAUNCH_GATE
    const val DEFAULT_ROUTE_KEY: String = AppRouteTable.ROUTE_LAUNCH_GATE

    fun resolvePageName(requestedPageName: String?): String =
        requestedPageName?.trim()?.takeIf(String::isNotEmpty)
            ?: DEFAULT_PAGE_NAME

    fun resolveRouteKey(
        requestedRouteKey: String?,
        resolvedPageName: String
    ): String = requestedRouteKey?.trim()?.takeIf(String::isNotEmpty)
        ?: resolvedPageName
}
