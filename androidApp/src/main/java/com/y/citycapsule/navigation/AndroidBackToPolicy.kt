package com.y.citycapsule.navigation

internal enum class AndroidBackToDecision {
    COMPLETE,
    REPLACE_TARGET
}

/**
 * Keeps backTo semantics independent from the Android Activity implementation.
 *
 * A missing target is expected after replace navigation, so it must be restored with another
 * replace instead of being treated as a routing failure.
 */
internal object AndroidBackToPolicy {
    fun decide(closedCount: Int?): AndroidBackToDecision =
        if (closedCount == null) {
            AndroidBackToDecision.REPLACE_TARGET
        } else {
            AndroidBackToDecision.COMPLETE
        }
}
