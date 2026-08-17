package com.y.citycapsule.core.route

object LocalRouteContract {
    const val SCHEMA_VERSION = 1
    const val MAX_ROUTES = 100
    const val MAX_PLACES_PER_ROUTE = 20
    const val MAX_NAME_LENGTH = 40
}

data class LocalRoute(
    val id: String,
    val name: String,
    val orderedPlaceIds: List<String>,
    val createdAtEpochMs: Long
)

data class LocalRouteDraft(
    val name: String,
    val orderedPlaceIds: List<String>
)

data class LocalRouteCatalog(
    val schemaVersion: Int = LocalRouteContract.SCHEMA_VERSION,
    val routes: List<LocalRoute> = emptyList()
) {
    companion object { val EMPTY = LocalRouteCatalog() }
}

object LocalRouteValidator {
    fun normalizeDraftOrNull(value: LocalRouteDraft): LocalRouteDraft? {
        val name = value.name.trim()
        val ids = value.orderedPlaceIds.map(String::trim)
        if (name.isEmpty() || name.length > LocalRouteContract.MAX_NAME_LENGTH) return null
        if (ids.isEmpty() || ids.size > LocalRouteContract.MAX_PLACES_PER_ROUTE) return null
        if (ids.any(String::isEmpty) || ids.distinct().size != ids.size) return null
        return LocalRouteDraft(name, ids)
    }

    fun normalizeOrNull(value: LocalRoute): LocalRoute? {
        val draft = normalizeDraftOrNull(LocalRouteDraft(value.name, value.orderedPlaceIds)) ?: return null
        val id = value.id.trim()
        if (id.isEmpty() || value.createdAtEpochMs < 0L) return null
        return value.copy(id = id, name = draft.name, orderedPlaceIds = draft.orderedPlaceIds)
    }
}
