package com.y.citycapsule.core.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageLoadCoordinatorTest {
    @Test
    fun limitsConcurrencyAndStartsVisibleBeforePrefetch() {
        val coordinator = ImageLoadCoordinator(maxConcurrentRequests = 2)
        val started = mutableListOf<String>()

        coordinator.acquire("https://image/1", ImageLoadPriority.VISIBLE) { if (it) started += "1" }
        coordinator.acquire("https://image/2", ImageLoadPriority.VISIBLE) { if (it) started += "2" }
        coordinator.acquire("https://image/3", ImageLoadPriority.PREFETCH) { if (it) started += "3" }
        coordinator.acquire("https://image/4", ImageLoadPriority.VISIBLE) { if (it) started += "4" }

        assertEquals(listOf("1", "2"), started)
        assertEquals(2, coordinator.metrics().activeRequests)
        coordinator.complete("https://image/1", true)
        assertEquals(listOf("1", "2", "4"), started)
    }

    @Test
    fun deduplicatesUrlUntilOwnerWarmsPlatformCache() {
        val coordinator = ImageLoadCoordinator(maxConcurrentRequests = 3)
        var firstReady = false
        var secondReady = false

        coordinator.acquire("https://image/same", ImageLoadPriority.VISIBLE) { firstReady = it }
        coordinator.acquire("https://image/same", ImageLoadPriority.VISIBLE) { secondReady = it }

        assertTrue(firstReady)
        assertFalse(secondReady)
        assertEquals(1, coordinator.metrics().uniqueRequestsStarted)
        assertEquals(1, coordinator.metrics().deduplicatedSubscribers)

        coordinator.complete("https://image/same", true)

        assertTrue(secondReady)
        assertEquals(1, coordinator.metrics().successfulRequests)
    }

    @Test
    fun releasingActiveLeaseFreesSlotAndCancelsWork() {
        val coordinator = ImageLoadCoordinator(maxConcurrentRequests = 1)
        val started = mutableListOf<String>()
        val first = coordinator.acquire("https://image/1", ImageLoadPriority.VISIBLE) {
            if (it) started += "1"
        }
        coordinator.acquire("https://image/2", ImageLoadPriority.PREFETCH) {
            if (it) started += "2"
        }

        first.release()

        assertEquals(listOf("1", "2"), started)
        assertEquals(1, coordinator.metrics().cancelledRequests)
        assertEquals(1, coordinator.metrics().activeRequests)
    }

    @Test
    fun failedOwnerDoesNotStartDuplicateSubscriberRequest() {
        val coordinator = ImageLoadCoordinator(maxConcurrentRequests = 1)
        var duplicateAllowed = true
        coordinator.acquire("https://image/fail", ImageLoadPriority.VISIBLE) {}
        coordinator.acquire("https://image/fail", ImageLoadPriority.VISIBLE) {
            duplicateAllowed = it
        }

        coordinator.complete("https://image/fail", false)

        assertFalse(duplicateAllowed)
        assertEquals(1, coordinator.metrics().failedRequests)
    }
}
