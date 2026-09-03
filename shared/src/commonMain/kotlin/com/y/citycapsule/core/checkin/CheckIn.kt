package com.y.citycapsule.core.checkin

enum class CheckInMethod(val wireValue: String) { GPS_CONFIRMED("gps_confirmed"), MANUAL("manual"); companion object { fun fromWireValue(v: String) = entries.firstOrNull { it.wireValue == v } } }
data class CheckIn(
    val placeId: String,
    val checkedInAtEpochMs: Long,
    val method: CheckInMethod,
    val distanceMeters: Double? = null,
    /** null means the legacy record did not capture favorite state. */
    val wasWantTo: Boolean? = null
)
data class CheckInCatalog(val schemaVersion: Int = 2, val sessionStartedAtEpochMs: Long, val checkIns: List<CheckIn> = emptyList())
