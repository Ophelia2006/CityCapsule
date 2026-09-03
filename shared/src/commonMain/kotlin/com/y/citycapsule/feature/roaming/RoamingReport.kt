package com.y.citycapsule.feature.roaming

import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.roaming.RoamingVisit

data class RoamingMoment(
    val visit: RoamingVisit?,
    val capsules: List<CityCapsule>,
    val occurredAtEpochMs: Long
)

data class RoamingReport(
    val moments: List<RoamingMoment>,
    val coverImagePath: String?,
    val completedWantTo: Int?,
    val spontaneousVisits: Int,
    val moodSummary: String?,
    val tagSummary: String?,
    val skippedPlaceIds: List<String>,
    val detourMeters: Double?
)

fun buildRoamingReport(record: RoamingRecord, allCapsules: List<CityCapsule>): RoamingReport {
    val capsules = allCapsules.filter { it.roamingSessionId == record.id }.sortedBy(CityCapsule::createdAtEpochMs)
    val grouped = capsules.groupBy(CityCapsule::placeId)
    val visitIds = record.visits.map { it.place.placeId }.toSet()
    val moments = buildList {
        record.visits.sortedBy(RoamingVisit::checkedInAtEpochMs).forEach { visit ->
            add(RoamingMoment(visit, grouped[visit.place.placeId].orEmpty(), visit.checkedInAtEpochMs))
        }
        capsules.filter { it.placeId !in visitIds }.forEach { add(RoamingMoment(null, listOf(it), it.createdAtEpochMs)) }
    }.sortedBy(RoamingMoment::occurredAtEpochMs)
    val moods = capsules.mapNotNull(CityCapsule::mood).fold(emptyList<com.y.citycapsule.core.capsule.CapsuleMood>()) { values, mood ->
        if (values.lastOrNull() == mood) values else values + mood
    }
    val tags = capsules.flatMap(CityCapsule::tags)
    val tagSummary = tags.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(5)
        .joinToString("  ") { "#${it.key}" }.takeIf(String::isNotBlank)
    return RoamingReport(
        moments = moments,
        coverImagePath = capsules.firstNotNullOfOrNull { it.imagePaths.firstOrNull() },
        completedWantTo = record.visits.takeIf { it.all { visit -> visit.wasWantTo != null } }?.count { it.wasWantTo == true },
        spontaneousVisits = record.visits.count { it.place.placeId !in record.orderedPlaceIds },
        moodSummary = moods.takeIf(List<*>::isNotEmpty)?.joinToString(" → ") { "${it.emoji} ${it.displayName}" },
        tagSummary = tagSummary,
        skippedPlaceIds = record.orderedPlaceIds.filter { it !in visitIds },
        detourMeters = record.plannedDistanceMeters?.let { planned -> record.distanceMeters?.minus(planned.toDouble())?.coerceAtLeast(0.0) }
    )
}
