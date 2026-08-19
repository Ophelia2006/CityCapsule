package com.y.citycapsule.core.place

/**
 * Resolves missing place covers without creating a burst of POI requests.
 * Results are persisted through [PlacePhotoCacheRepository]; misses are remembered
 * for this hydrator's lifetime so recomposition/filter changes do not retry them.
 */
class PlacePhotoHydrator(
    private val remoteDataSource: PlaceRemoteDataSource,
    private val cacheRepository: PlacePhotoCacheRepository,
    private val maxConcurrentRequests: Int = 2
) {
    private val queued = ArrayDeque<Place>()
    private val scheduledIds = mutableSetOf<String>()
    private var activeRequests = 0
    private var disposed = false

    fun request(
        places: List<Place>,
        knownPhotoIds: Set<String>,
        onResolved: (PlacePhotoCacheEntry) -> Unit
    ) {
        if (disposed) return
        places.forEach { place ->
            if (
                place.visualRef == null &&
                place.id !in knownPhotoIds &&
                scheduledIds.add(place.id)
            ) queued.addLast(place)
        }
        drain(onResolved)
    }

    fun dispose() {
        disposed = true
        queued.clear()
    }

    private fun drain(onResolved: (PlacePhotoCacheEntry) -> Unit) {
        while (!disposed && activeRequests < maxConcurrentRequests && queued.isNotEmpty()) {
            val place = queued.removeFirst()
            activeRequests++
            remoteDataSource.search(place.name, place.city, place.geoPoint) { result ->
                activeRequests--
                if (!disposed) {
                    selectPlacePhotoUrl(place, result)?.let { url ->
                        val entry = PlacePhotoCacheEntry(
                            placeId = place.id,
                            url = url,
                            source = PlacePhotoCacheContract.SOURCE_AMAP_POI,
                            updatedAtEpochMs = SystemPlaceClock.nowEpochMs()
                        )
                        cacheRepository.put(place.id, url, entry.source) {}
                        onResolved(entry)
                    }
                    drain(onResolved)
                }
            }
        }
    }
}

internal fun selectPlacePhotoUrl(place: Place, result: RemotePlaceResult): String? =
    (result as? RemotePlaceResult.Success)
        ?.places
        ?.asSequence()
        ?.filter { !it.photoUrl.isNullOrBlank() }
        ?.sortedBy { if (it.name.equals(place.name, ignoreCase = true)) 0 else 1 }
        ?.firstOrNull { candidate ->
            candidate.name.equals(place.name, ignoreCase = true) ||
                candidate.name.contains(place.name, ignoreCase = true) ||
                place.name.contains(candidate.name, ignoreCase = true)
        }
        ?.photoUrl
