package com.y.citycapsule.core.route

import com.tencent.kuikly.core.datetime.DateTime
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

interface LocalRouteRepository {
    fun getCatalog(callback: StorageCallback<LocalRouteCatalog>)
    fun create(draft: LocalRouteDraft, callback: StorageCallback<LocalRoute>)
    fun update(route: LocalRoute, callback: StorageCallback<LocalRoute>)
    fun delete(routeId: String, callback: StorageCallback<Unit>)
}

class DefaultLocalRouteRepository(
    private val storage: KeyValueStore,
    private val placeRepository: PlaceRepository,
    private val now: () -> Long = { DateTime.currentTimestamp() },
    private val newId: (Long) -> String = { "route_$it" }
) : LocalRouteRepository {
    private val queue = mutableListOf<(() -> Unit) -> Unit>()
    private var busy = false

    override fun getCatalog(callback: StorageCallback<LocalRouteCatalog>) = storage.get(AppStorageKeys.Routes.CATALOG) {
        callback(if (it === StorageResult.Missing) StorageResult.Success(LocalRouteCatalog.EMPTY) else it)
    }

    override fun create(draft: LocalRouteDraft, callback: StorageCallback<LocalRoute>) {
        val normalized = LocalRouteValidator.normalizeDraftOrNull(draft) ?: return callback(invalid("Route draft is invalid."))
        mutate(callback) { catalog, places, complete ->
            if (catalog.routes.size >= LocalRouteContract.MAX_ROUTES || !places.containsAll(normalized.orderedPlaceIds)) {
                complete(invalid("Route contains unavailable places or the catalog is full.")); return@mutate
            }
            val timestamp = now()
            var id = newId(timestamp)
            var suffix = 1
            while (catalog.routes.any { it.id == id }) id = "${newId(timestamp)}_${suffix++}"
            val route = LocalRoute(id, normalized.name, normalized.orderedPlaceIds, timestamp)
            persist(catalog.copy(routes = catalog.routes + route), StorageResult.Success(route), complete)
        }
    }

    override fun update(route: LocalRoute, callback: StorageCallback<LocalRoute>) {
        val normalized = LocalRouteValidator.normalizeOrNull(route) ?: return callback(invalid("Route is invalid."))
        mutate(callback) { catalog, places, complete ->
            val index = catalog.routes.indexOfFirst { it.id == normalized.id }
            if (index < 0) { complete(StorageResult.Missing); return@mutate }
            if (!places.containsAll(normalized.orderedPlaceIds)) { complete(invalid("Route contains unavailable places.")); return@mutate }
            val saved = normalized.copy(createdAtEpochMs = catalog.routes[index].createdAtEpochMs)
            val routes = catalog.routes.toMutableList().apply { this[index] = saved }
            persist(catalog.copy(routes = routes), StorageResult.Success(saved), complete)
        }
    }

    override fun delete(routeId: String, callback: StorageCallback<Unit>) {
        enqueue { done -> getCatalog { result ->
            if (result !is StorageResult.Success) { callbackResult(result, callback, done); return@getCatalog }
            if (result.value.routes.none { it.id == routeId }) { callback(StorageResult.Missing); done(); return@getCatalog }
            persist(result.value.copy(routes = result.value.routes.filterNot { it.id == routeId }), StorageResult.Success(Unit)) { callback(it); done() }
        } }
    }

    private fun <T> mutate(callback: StorageCallback<T>, block: (LocalRouteCatalog, Set<String>, StorageCallback<T>) -> Unit) = enqueue { done ->
        getCatalog { catalog ->
            if (catalog !is StorageResult.Success) { callbackResult(catalog, callback, done); return@getCatalog }
            placeRepository.getCatalog { places ->
                if (places !is StorageResult.Success) { callbackResult(places, callback, done); return@getCatalog }
                block(catalog.value, places.value.places.map { it.id }.toSet()) { callback(it); done() }
            }
        }
    }

    private fun <T> persist(catalog: LocalRouteCatalog, success: StorageResult<T>, callback: StorageCallback<T>) =
        storage.put(AppStorageKeys.Routes.CATALOG, catalog) { result -> callback(if (result is StorageResult.Success) success else invalid("Route write failed.")) }

    private fun enqueue(task: (() -> Unit) -> Unit) { queue += task; drain() }
    private fun drain() { if (busy || queue.isEmpty()) return; busy = true; val task = queue.removeAt(0); task { busy = false; drain() } }
    private fun <T> callbackResult(result: StorageResult<*>, callback: StorageCallback<T>, done: () -> Unit) { callback(if (result is StorageResult.Failure) result else StorageResult.Missing); done() }
    private fun invalid(message: String) = StorageResult.Failure(StorageError(StorageErrorCode.INVALID_REQUEST, message))
}
