package com.y.citycapsule.core.roaming

object RoamingSessionContract {
    const val SCHEMA_VERSION = 1
}

enum class RoamingStatus(val wireValue: String) {
    ACTIVE("active"),
    PAUSED("paused"),
    ENDED("ended");

    companion object {
        fun fromWireValue(value: String): RoamingStatus? = entries.firstOrNull { it.wireValue == value }
    }
}

data class RoamingSession(
    val routeId: String? = null,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val status: RoamingStatus
)

object RoamingSessionValidator {
    fun normalizeOrNull(value: RoamingSession): RoamingSession? {
        val routeId = value.routeId?.trim()?.takeIf(String::isNotEmpty)
        if (value.startedAtEpochMs < 0L) return null
        when (value.status) {
            RoamingStatus.ACTIVE, RoamingStatus.PAUSED -> if (value.endedAtEpochMs != null) return null
            RoamingStatus.ENDED -> {
                val endedAt = value.endedAtEpochMs ?: return null
                if (endedAt < value.startedAtEpochMs) return null
            }
        }
        return value.copy(routeId = routeId)
    }
}
