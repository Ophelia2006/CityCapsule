package com.y.citycapsule.core.roaming

import com.y.citycapsule.core.route.LocalRoute
import com.y.citycapsule.core.route.LocalRouteCatalog
import com.y.citycapsule.core.route.LocalRouteDraft
import com.y.citycapsule.core.route.LocalRouteRepository
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LocalRoamingSessionRepositoryTest {
    @Test fun startPauseResumeEndFollowsStateMachine() {
        var time = 10L
        val repository = LocalRoamingSessionRepository(InMemoryKeyValueStore(), FakeRoutes(), now = { time++ })

        assertEquals(RoamingStatus.ACTIVE, success { repository.start("route-1", it) }.status)
        assertEquals(RoamingStatus.PAUSED, success(repository::pause).status)
        assertEquals(RoamingStatus.ACTIVE, success(repository::resume).status)
        val ended = success(repository::end)
        assertEquals(RoamingStatus.ENDED, ended.status)
        assertEquals(11L, ended.endedAtEpochMs)
        assertIs<StorageResult.Failure>(result(repository::resume))
    }

    @Test fun unknownRouteCannotStart() {
        val repository = LocalRoamingSessionRepository(InMemoryKeyValueStore(), FakeRoutes(), now = { 10L })
        assertIs<StorageResult.Failure>(result { repository.start("missing", it) })
    }

    @Test fun activeSessionCannotBeSilentlyOverwritten() {
        val repository = LocalRoamingSessionRepository(InMemoryKeyValueStore(), FakeRoutes(), now = { 10L })
        assertIs<StorageResult.Success<RoamingSession>>(result { repository.start(null, it) })
        assertIs<StorageResult.Failure>(result { repository.start("route-1", it) })
    }

    private fun success(block: (StorageCallback<RoamingSession>) -> Unit): RoamingSession = assertIs<StorageResult.Success<RoamingSession>>(result(block)).value
    private fun result(block: (StorageCallback<RoamingSession>) -> Unit): StorageResult<RoamingSession> {
        var value: StorageResult<RoamingSession>? = null
        block { value = it }
        return requireNotNull(value)
    }

    private class FakeRoutes : LocalRouteRepository {
        private val catalog = LocalRouteCatalog(routes = listOf(LocalRoute("route-1", "路线", listOf("p1"), 1L)))
        override fun getCatalog(callback: StorageCallback<LocalRouteCatalog>) = callback(StorageResult.Success(catalog))
        override fun create(draft: LocalRouteDraft, callback: StorageCallback<LocalRoute>) = error("unused")
        override fun update(route: LocalRoute, callback: StorageCallback<LocalRoute>) = error("unused")
        override fun delete(routeId: String, callback: StorageCallback<Unit>) = error("unused")
    }
}
