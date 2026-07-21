package com.y.citycapsule.navigation

import com.y.citycapsule.core.navigation.RouteProtocol
import org.json.JSONObject

enum class AndroidRouteAction {
    PUSH,
    REPLACE,
    BACK_TO
}

enum class AndroidRouteTargetType {
    KUIKLY,
    NATIVE
}

data class AndroidRouteRequest(
    val action: AndroidRouteAction,
    val routeKey: String,
    val targetType: AndroidRouteTargetType,
    val target: String,
    val pageDataJson: String
)

object AndroidRouteRequestDecoder {

    fun decode(
        pageName: String,
        pageData: JSONObject
    ): AndroidRouteRequest {
        val businessData = JSONObject(pageData.toString()).apply {
            remove(RouteProtocol.PARAM_ACTION)
            remove(RouteProtocol.PARAM_ROUTE_KEY)
            remove(RouteProtocol.PARAM_TARGET_TYPE)
        }
        return decodeValues(
            pageName = pageName,
            actionValue = pageData.optString(RouteProtocol.PARAM_ACTION),
            routeKeyValue = pageData.optString(RouteProtocol.PARAM_ROUTE_KEY),
            targetTypeValue = pageData.optString(RouteProtocol.PARAM_TARGET_TYPE),
            pageDataJson = businessData.toString()
        )
    }

    internal fun decodeValues(
        pageName: String,
        actionValue: String?,
        routeKeyValue: String?,
        targetTypeValue: String?,
        pageDataJson: String
    ): AndroidRouteRequest {
        val target = pageName.trim()
        require(target.isNotEmpty()) { "Route target must not be blank." }

        val action = when (actionValue?.trim().orEmpty()) {
            "", "push" -> AndroidRouteAction.PUSH
            "replace" -> AndroidRouteAction.REPLACE
            "backTo" -> AndroidRouteAction.BACK_TO
            else -> throw IllegalArgumentException(
                "Unsupported Android route action '$actionValue'."
            )
        }

        val targetType = when (targetTypeValue?.trim().orEmpty()) {
            "" -> if (target.startsWith(NATIVE_ROUTE_PREFIX)) {
                AndroidRouteTargetType.NATIVE
            } else {
                AndroidRouteTargetType.KUIKLY
            }

            RouteProtocol.TARGET_KUIKLY -> AndroidRouteTargetType.KUIKLY
            RouteProtocol.TARGET_NATIVE -> AndroidRouteTargetType.NATIVE
            else -> throw IllegalArgumentException(
                "Unsupported Android route target type '$targetTypeValue'."
            )
        }

        if (targetType == AndroidRouteTargetType.NATIVE) {
            require(target.startsWith(NATIVE_ROUTE_PREFIX)) {
                "Native route target must start with '$NATIVE_ROUTE_PREFIX'."
            }
        }

        val routeKey = routeKeyValue?.trim().orEmpty().ifEmpty { target }
        require(routeKey.isNotEmpty()) { "Android routeKey must not be blank." }

        return AndroidRouteRequest(
            action = action,
            routeKey = routeKey,
            targetType = targetType,
            target = target,
            pageDataJson = pageDataJson
        )
    }

    private const val NATIVE_ROUTE_PREFIX = "/native/"
}
