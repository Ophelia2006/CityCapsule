package com.y.citycapsule.core.navigation

import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

/**
 * AppNavigator implementation backed by Kuikly's standard RouterModule.
 */
class KuiklyAppNavigator internal constructor(
    private val transport: RouteTransport
) : AppNavigator {

    constructor(pager: Pager) : this(PagerRouteTransport(pager))

    override fun navigate(route: AppRoute) {
        open(AppRouteTable.resolve(route, RouteAction.PUSH))
    }

    override fun replace(route: AppRoute) {
        open(AppRouteTable.resolve(route, RouteAction.REPLACE))
    }

    override fun back() {
        transport.closePage()
    }

    override fun backTo(routeKey: AppRouteKey) {
        open(AppRouteTable.resolveBackTo(routeKey))
    }

    private fun open(request: RouteRequest) {
        val pageData = JSONObject().apply {
            put(RouteProtocol.PARAM_ACTION, request.action.wireValue)
            put(RouteProtocol.PARAM_ROUTE_KEY, request.routeKey)
            put(RouteProtocol.PARAM_TARGET_TYPE, request.destination.wireType)
            request.params.forEach { (key, value) ->
                put(key, value.toJsonValue())
            }
        }
        transport.openPage(request.destination.target, pageData)
    }
}

internal interface RouteTransport {
    fun openPage(pageName: String, pageData: JSONObject)

    fun closePage()
}

private class PagerRouteTransport(
    private val pager: Pager
) : RouteTransport {
    override fun openPage(pageName: String, pageData: JSONObject) {
        routerModule().openPage(pageName, pageData)
    }

    override fun closePage() {
        routerModule().closePage()
    }

    private fun routerModule(): RouterModule =
        pager.acquireModule(RouterModule.MODULE_NAME)
}

private fun Any?.toJsonValue(): Any? = when (this) {
    null,
    is String,
    is Boolean,
    is Int,
    is Long,
    is Double,
    is JSONObject,
    is JSONArray -> this

    is Float -> toDouble()
    is Short -> toInt()
    is Byte -> toInt()
    is List<*> -> JSONArray().also { array ->
        forEach { value -> array.put(value.toJsonValue()) }
    }

    is Map<*, *> -> JSONObject().also { json ->
        forEach { (key, value) ->
            require(key is String) { "Route parameter map keys must be strings." }
            json.put(key, value.toJsonValue())
        }
    }

    else -> throw IllegalArgumentException(
        "Unsupported route parameter type '${this::class.simpleName}'."
    )
}

