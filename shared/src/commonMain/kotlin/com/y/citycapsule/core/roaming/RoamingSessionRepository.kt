package com.y.citycapsule.core.roaming

import com.tencent.kuikly.core.datetime.DateTime
import com.y.citycapsule.core.route.LocalRouteRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

interface RoamingSessionRepository {
    fun get(callback: StorageCallback<RoamingSession>)
    fun start(routeId: String?, callback: StorageCallback<RoamingSession>)
    fun pause(callback: StorageCallback<RoamingSession>)
    fun resume(callback: StorageCallback<RoamingSession>)
    fun end(callback: StorageCallback<RoamingSession>)
}

class LocalRoamingSessionRepository(
    private val storage: KeyValueStore,
    private val routes: LocalRouteRepository,
    private val now: () -> Long = { DateTime.currentTimestamp() }
) : RoamingSessionRepository {
    private val queue = mutableListOf<(() -> Unit) -> Unit>()
    private var busy = false

    override fun get(callback: StorageCallback<RoamingSession>) = storage.get(AppStorageKeys.Roaming.SESSION, callback)

    override fun start(routeId: String?, callback: StorageCallback<RoamingSession>) {
        val normalizedId = routeId?.trim()?.takeIf(String::isNotEmpty)
        enqueue { done ->
            val persist = {
                val session = RoamingSession(normalizedId, now(), status = RoamingStatus.ACTIVE)
                write(session, callback, done)
            }
            if (normalizedId == null) persist() else routes.getCatalog { result ->
                if (result is StorageResult.Success && result.value.routes.any { it.id == normalizedId }) persist()
                else deliver(callback, invalid("Roaming route is unavailable."), done)
            }
        }
    }

    override fun pause(callback: StorageCallback<RoamingSession>) = transition(RoamingStatus.ACTIVE, callback) { it.copy(status = RoamingStatus.PAUSED) }
    override fun resume(callback: StorageCallback<RoamingSession>) = transition(RoamingStatus.PAUSED, callback) { it.copy(status = RoamingStatus.ACTIVE) }
    override fun end(callback: StorageCallback<RoamingSession>) = transition(setOf(RoamingStatus.ACTIVE, RoamingStatus.PAUSED), callback) {
        it.copy(status = RoamingStatus.ENDED, endedAtEpochMs = maxOf(now(), it.startedAtEpochMs))
    }

    private fun transition(expected: RoamingStatus, callback: StorageCallback<RoamingSession>, transform: (RoamingSession) -> RoamingSession) = transition(setOf(expected), callback, transform)
    private fun transition(expected: Set<RoamingStatus>, callback: StorageCallback<RoamingSession>, transform: (RoamingSession) -> RoamingSession) = enqueue { done ->
        get { result ->
            if (result is StorageResult.Success && result.value.status in expected) write(transform(result.value), callback, done)
            else deliver(callback, if (result is StorageResult.Failure) result else invalid("Roaming transition is not allowed."), done)
        }
    }

    private fun write(session: RoamingSession, callback: StorageCallback<RoamingSession>, done: () -> Unit) {
        storage.put(AppStorageKeys.Roaming.SESSION, session) { result ->
            deliver(callback, if (result is StorageResult.Success) StorageResult.Success(session) else invalid("Roaming session write failed."), done)
        }
    }
    private fun enqueue(task: (() -> Unit) -> Unit) { queue += task; drain() }
    private fun drain() { if (busy || queue.isEmpty()) return; busy = true; val task = queue.removeAt(0); task { busy = false; drain() } }
    private fun <T> deliver(callback: StorageCallback<T>, result: StorageResult<T>, done: () -> Unit) { try { callback(result) } finally { done() } }
    private fun invalid(message: String) = StorageResult.Failure(StorageError(StorageErrorCode.INVALID_REQUEST, message))
}
