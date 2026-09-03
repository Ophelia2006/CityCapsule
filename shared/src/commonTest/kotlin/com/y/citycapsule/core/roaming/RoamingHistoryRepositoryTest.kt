package com.y.citycapsule.core.roaming

import com.y.citycapsule.core.checkin.CheckInMethod
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RoamingHistoryRepositoryTest {
    @Test fun codecRoundTripPreservesRouteAndVisitSnapshots() {
        val value = record("session-1")
        assertEquals(
            RoamingHistoryCatalog(records = listOf(value)),
            RoamingHistoryCodec.decode(RoamingHistoryCodec.encode(RoamingHistoryCatalog(records = listOf(value))))
        )
    }

    @Test fun invalidDuplicateVisitsAreRejected() {
        val value = record("session-1")
        assertNull(RoamingHistoryValidator.normalizeOrNull(value.copy(visits = value.visits + value.visits.first())))
    }

    @Test fun archiveIsIdempotentAndKeepsNewestVersion() {
        val repository = LocalRoamingHistoryRepository(InMemoryKeyValueStore())
        assertIs<StorageResult.Success<RoamingRecord>>(archive(repository, record("session-1")))
        assertIs<StorageResult.Success<RoamingRecord>>(archive(repository, record("session-1").copy(distanceMeters = 42.0)))

        val catalog = assertIs<StorageResult.Success<RoamingHistoryCatalog>>(catalog(repository)).value
        assertEquals(1, catalog.records.size)
        assertEquals(42.0, catalog.records.single().distanceMeters)
    }

    private fun record(id: String) = RoamingRecord(
        id = id,
        mode = RoamingMode.PLANNED,
        routeId = "route-1",
        routeName = "周末散步",
        orderedPlaceIds = listOf("place-1"),
        startedAtEpochMs = 10L,
        endedAtEpochMs = 30L,
        distanceMeters = 20.0,
        visits = listOf(RoamingVisit(RoamingPlaceSnapshot("place-1", "西岸美术馆", "上海", "徐汇区"), 20L, CheckInMethod.GPS_CONFIRMED, 10.0))
    )

    private fun archive(repository: LocalRoamingHistoryRepository, value: RoamingRecord): StorageResult<RoamingRecord> {
        var result: StorageResult<RoamingRecord>? = null
        repository.archive(value) { result = it }
        return requireNotNull(result)
    }

    private fun catalog(repository: LocalRoamingHistoryRepository): StorageResult<RoamingHistoryCatalog> {
        var result: StorageResult<RoamingHistoryCatalog>? = null
        repository.getCatalog { result = it }
        return requireNotNull(result)
    }
}
