package com.y.citycapsule.core.navigation

enum class RouteAction(val wireValue: String) {
    PUSH("push"),
    REPLACE("replace"),
    BACK_TO("backTo")
}

data class RouteRequest(
    val action: RouteAction,
    val routeKey: String,
    val destination: RouteDestination,
    val params: Map<String, Any?> = emptyMap()
) {
    init {
        require(routeKey.isNotBlank()) { "routeKey must not be blank." }
        require(params.keys.none(RouteProtocol::isReservedKey)) {
            "Route params must not use reserved '${RouteProtocol.RESERVED_PREFIX}' keys."
        }
    }
}

/**
 * Wire-level constants shared with Android. HarmonyOS declares the same values in ArkTS
 * because it cannot directly import Kotlin constants.
 */
object RouteProtocol {
    const val RESERVED_PREFIX = "__cc_"
    const val PARAM_ACTION = "${RESERVED_PREFIX}route_action"
    const val PARAM_ROUTE_KEY = "${RESERVED_PREFIX}route_key"
    const val PARAM_TARGET_TYPE = "${RESERVED_PREFIX}target_type"

    const val TARGET_KUIKLY = "kuikly"
    const val TARGET_NATIVE = "native"

    fun isReservedKey(key: String): Boolean = key.startsWith(RESERVED_PREFIX)
}

