package com.y.citycapsule.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidRouteStackCoordinatorTest {

    @Test
    fun backToClosesOnlyHostsAboveLatestTarget() {
        val coordinator = AndroidRouteStackCoordinator()
        val home = FakeRouteHost("home")
        val settings = FakeRouteHost("settings")
        val detail = FakeRouteHost("place_detail")
        coordinator.register(home)
        coordinator.register(settings)
        coordinator.register(detail)

        val closedCount = coordinator.backTo("home")

        assertEquals(2, closedCount)
        assertEquals(1, settings.finishCount)
        assertEquals(1, detail.finishCount)
        assertEquals(0, home.finishCount)
        assertEquals(listOf("home"), coordinator.snapshotRouteKeys())
    }

    @Test
    fun backToUsesLatestMatchingRouteInstance() {
        val coordinator = AndroidRouteStackCoordinator()
        val firstHome = FakeRouteHost("home")
        val settings = FakeRouteHost("settings")
        val latestHome = FakeRouteHost("home")
        val detail = FakeRouteHost("place_detail")
        coordinator.register(firstHome)
        coordinator.register(settings)
        coordinator.register(latestHome)
        coordinator.register(detail)

        val closedCount = coordinator.backTo("home")

        assertEquals(1, closedCount)
        assertEquals(1, detail.finishCount)
        assertEquals(
            listOf("home", "settings", "home"),
            coordinator.snapshotRouteKeys()
        )
    }

    @Test
    fun backToCurrentRouteIsNoOp() {
        val coordinator = AndroidRouteStackCoordinator()
        val home = FakeRouteHost("home")
        coordinator.register(home)

        assertEquals(0, coordinator.backTo("home"))
        assertEquals(0, home.finishCount)
    }

    @Test
    fun missingBackToTargetDoesNotMutateStack() {
        val coordinator = AndroidRouteStackCoordinator()
        val home = FakeRouteHost("home")
        val settings = FakeRouteHost("settings")
        coordinator.register(home)
        coordinator.register(settings)

        assertNull(coordinator.backTo("missing"))
        assertEquals(
            listOf("home", "settings"),
            coordinator.snapshotRouteKeys()
        )
    }

    @Test
    fun missingBackToTargetIsRecoveredWithReplace() {
        val coordinator = AndroidRouteStackCoordinator()
        coordinator.register(FakeRouteHost("settings"))

        val decision = AndroidBackToPolicy.decide(coordinator.backTo("home"))

        assertEquals(AndroidBackToDecision.REPLACE_TARGET, decision)
        assertEquals(listOf("settings"), coordinator.snapshotRouteKeys())
    }

    @Test
    fun existingBackToTargetCompletesWithoutReplace() {
        val coordinator = AndroidRouteStackCoordinator()
        coordinator.register(FakeRouteHost("home"))
        coordinator.register(FakeRouteHost("settings"))

        val decision = AndroidBackToPolicy.decide(coordinator.backTo("home"))

        assertEquals(AndroidBackToDecision.COMPLETE, decision)
        assertEquals(listOf("home"), coordinator.snapshotRouteKeys())
    }

    @Test
    fun pushBackReplaceAndBackToFollowHostLifecycle() {
        val coordinator = AndroidRouteStackCoordinator()
        val home = FakeRouteHost("home")
        val settings = FakeRouteHost("settings")
        val detail = FakeRouteHost("place_detail")
        val replacementSettings = FakeRouteHost("settings")
        val gallery = FakeRouteHost("gallery")

        coordinator.register(home)
        coordinator.register(settings)
        coordinator.unregister(settings) // back
        coordinator.register(detail)
        coordinator.register(replacementSettings)
        coordinator.unregister(detail) // replace, after the replacement host appeared
        coordinator.register(gallery)

        assertEquals(
            listOf("home", "settings", "gallery"),
            coordinator.snapshotRouteKeys()
        )
        assertEquals(1, coordinator.backTo("settings"))
        assertEquals(1, gallery.finishCount)
        assertEquals(listOf("home", "settings"), coordinator.snapshotRouteKeys())
    }

    @Test
    fun unregisterAndClearAreIdempotent() {
        val coordinator = AndroidRouteStackCoordinator()
        val home = FakeRouteHost("home")
        coordinator.register(home)

        coordinator.unregister(home)
        coordinator.unregister(home)
        assertEquals(emptyList<String>(), coordinator.snapshotRouteKeys())

        coordinator.register(home)
        coordinator.clear()
        coordinator.clear()
        assertEquals(emptyList<String>(), coordinator.snapshotRouteKeys())
    }
}

private class FakeRouteHost(
    override val routeKey: String
) : AndroidRouteHost {
    var finishCount: Int = 0

    override fun finishRoute() {
        finishCount += 1
    }
}
