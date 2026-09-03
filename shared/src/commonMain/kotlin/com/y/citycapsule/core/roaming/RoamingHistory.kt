package com.y.citycapsule.core.roaming

import com.y.citycapsule.core.checkin.CheckInMethod
import com.y.citycapsule.core.place.GeoPoint

object RoamingHistoryContract {
    const val SCHEMA_VERSION = 2
    const val MAX_RECORDS = 100
    const val MAX_VISITS_PER_RECORD = 20
}

enum class RoamingMode(val wireValue: String) {
    PLANNED("planned"),
    FREE("free");

    companion object {
        fun fromWireValue(value: String): RoamingMode? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}

data class RoamingPlaceSnapshot(
    val placeId: String,
    val name: String,
    val city: String,
    val district: String? = null
)

data class RoamingVisit(
    val place: RoamingPlaceSnapshot,
    val checkedInAtEpochMs: Long,
    val method: CheckInMethod,
    val distanceMeters: Double? = null,
    val wasWantTo: Boolean? = null
)

data class RoamingRecord(
    /** Stable legacy-compatible identity also used by CityCapsule.roamingSessionId. */
    val id: String,
    val mode: RoamingMode,
    val routeId: String? = null,
    val routeName: String? = null,
    val orderedPlaceIds: List<String> = emptyList(),
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val distanceMeters: Double? = null,
    val trackChunkPaths: List<String> = emptyList(),
    val visits: List<RoamingVisit> = emptyList(),
    val plannedDistanceMeters: Long? = null,
    val plannedDurationSeconds: Long? = null,
    val plannedTrackPoints: List<GeoPoint> = emptyList()
)

data class RoamingHistoryCatalog(
    val schemaVersion: Int = RoamingHistoryContract.SCHEMA_VERSION,
    val records: List<RoamingRecord> = emptyList()
) {
    companion object { val EMPTY = RoamingHistoryCatalog() }
}

object RoamingHistoryValidator {
    fun normalizeOrNull(value: RoamingRecord): RoamingRecord? {
        val id = value.id.trim()
        if (id.isEmpty() || value.startedAtEpochMs < 0L || value.endedAtEpochMs < value.startedAtEpochMs) return null
        if (value.orderedPlaceIds.size > RoamingHistoryContract.MAX_VISITS_PER_RECORD) return null
        if (value.orderedPlaceIds.any(String::isBlank) || value.orderedPlaceIds.distinct().size != value.orderedPlaceIds.size) return null
        if (value.visits.size > RoamingHistoryContract.MAX_VISITS_PER_RECORD) return null
        val normalizedVisits = value.visits.map { visit ->
            val placeId = visit.place.placeId.trim()
            val name = visit.place.name.trim()
            val city = visit.place.city.trim()
            if (placeId.isEmpty() || name.isEmpty() || city.isEmpty()) return null
            if (visit.checkedInAtEpochMs < value.startedAtEpochMs || visit.checkedInAtEpochMs > value.endedAtEpochMs) return null
            if (visit.distanceMeters != null && (!visit.distanceMeters.isFinite() || visit.distanceMeters < 0.0)) return null
            visit.copy(place = visit.place.copy(placeId = placeId, name = name, city = city, district = visit.place.district?.trim()?.takeIf(String::isNotEmpty)))
        }
        if (normalizedVisits.map { it.place.placeId }.distinct().size != normalizedVisits.size) return null
        if (value.distanceMeters != null && (!value.distanceMeters.isFinite() || value.distanceMeters < 0.0)) return null
        if (value.plannedDistanceMeters != null && value.plannedDistanceMeters < 0L) return null
        if (value.plannedDurationSeconds != null && value.plannedDurationSeconds < 0L) return null
        if (value.plannedTrackPoints.size > 500 || value.plannedTrackPoints.any { it.latitude !in -90.0..90.0 || it.longitude !in -180.0..180.0 }) return null
        return value.copy(
            id = id,
            routeId = value.routeId?.trim()?.takeIf(String::isNotEmpty),
            routeName = value.routeName?.trim()?.takeIf(String::isNotEmpty),
            orderedPlaceIds = value.orderedPlaceIds.map(String::trim),
            trackChunkPaths = value.trackChunkPaths.map(String::trim).filter(String::isNotEmpty).distinct(),
            visits = normalizedVisits
        )
    }
}
