package com.y.citycapsule.feature

import com.y.citycapsule.core.capsule.CapsuleMood
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.checkin.CheckInMethod
import com.y.citycapsule.core.roaming.RoamingMode
import com.y.citycapsule.core.roaming.RoamingPlaceSnapshot
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.roaming.RoamingVisit
import com.y.citycapsule.feature.roaming.buildRoamingReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoamingReportTest {
    @Test fun buildsTruthfulOrderedReport() {
        val record = RoamingRecord("1", RoamingMode.PLANNED, orderedPlaceIds = listOf("a", "b"), startedAtEpochMs = 0, endedAtEpochMs = 10_000,
            distanceMeters = 1_500.0, visits = listOf(visit("a", 100, true), visit("c", 300, false)), plannedDistanceMeters = 1_000)
        val capsules = listOf(capsule("ca", "a", 200, CapsuleMood.CALM, listOf("散步"), "cover.jpg"), capsule("cc", "c", 400, CapsuleMood.SURPRISED, listOf("散步", "夜景"), null))
        val report = buildRoamingReport(record, capsules)
        assertEquals(listOf("a", "c"), report.moments.mapNotNull { it.visit?.place?.placeId })
        assertEquals("cover.jpg", report.coverImagePath); assertEquals(1, report.completedWantTo); assertEquals(1, report.spontaneousVisits)
        assertEquals(listOf("b"), report.skippedPlaceIds); assertEquals(500.0, report.detourMeters)
        assertEquals("😌 平静 → 🤯 震撼", report.moodSummary); assertEquals("#散步  #夜景", report.tagSummary)
    }
    @Test fun legacyWantToStateIsUnknown() {
        val report = buildRoamingReport(RoamingRecord("1", RoamingMode.FREE, startedAtEpochMs = 0, endedAtEpochMs = 1, visits = listOf(visit("a", 1, null))), emptyList())
        assertNull(report.completedWantTo)
    }
    private fun visit(id: String, time: Long, wantTo: Boolean?) = RoamingVisit(RoamingPlaceSnapshot(id, id, "杭州"), time, CheckInMethod.MANUAL, wasWantTo = wantTo)
    private fun capsule(id: String, place: String, time: Long, mood: CapsuleMood, tags: List<String>, image: String?) = CityCapsule(id = id, content = id, mood = mood, tags = tags, placeId = place, roamingSessionId = "1", imagePaths = listOfNotNull(image), createdAtEpochMs = time, updatedAtEpochMs = time)
}
