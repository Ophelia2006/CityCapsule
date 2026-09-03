package com.y.citycapsule.core.place

import com.tencent.kuikly.core.datetime.DateTime
import kotlin.math.roundToInt

/** Small process-memory LRU for reproducible online POI pages; failures are never cached. */
class CachingPlaceRemoteDataSource(
    private val delegate: PlaceRemoteDataSource,
    private val cache: PlaceSearchPageCache = PlaceSearchCacheRuntime.cache
) : PlaceRemoteDataSource {
    override fun search(
        query: String,
        city: String,
        near: GeoPoint?,
        callback: (RemotePlaceResult) -> Unit
    ) = searchPage(query, city, near, page = 1, pageSize = 12, callback = callback)

    override fun searchPage(
        query: String,
        city: String,
        near: GeoPoint?,
        page: Int,
        pageSize: Int,
        callback: (RemotePlaceResult) -> Unit
    ) {
        val normalizedPage = page.coerceAtLeast(1)
        val normalizedSize = pageSize.coerceIn(1, 20)
        val key = PlaceSearchPageKey.of(query, city, near, normalizedPage, normalizedSize)
        cache.get(key)?.let { cached ->
            callback(cached)
            return
        }
        delegate.searchPage(query, city, near, normalizedPage, normalizedSize) { result ->
            if (result is RemotePlaceResult.Success) cache.put(key, result)
            callback(result)
        }
    }
}

data class PlaceSearchCacheStats(
    val hits: Long,
    val misses: Long,
    val evictions: Long,
    val size: Int
)

class PlaceSearchPageCache(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val now: () -> Long = { DateTime.currentTimestamp() }
) {
    private data class Entry(val value: RemotePlaceResult.Success, val expiresAt: Long, val access: Long)
    private val entries = mutableMapOf<PlaceSearchPageKey, Entry>()
    private var sequence = 0L
    private var hitCount = 0L
    private var missCount = 0L
    private var evictionCount = 0L

    init {
        require(maxEntries > 0)
        require(ttlMillis > 0L)
    }

    fun get(key: PlaceSearchPageKey): RemotePlaceResult.Success? {
        val time = now()
        val entry = entries[key]
        if (entry == null || entry.expiresAt <= time) {
            if (entry != null) entries.remove(key)
            missCount += 1
            return null
        }
        hitCount += 1
        sequence += 1
        entries[key] = entry.copy(access = sequence)
        return entry.value
    }

    fun put(key: PlaceSearchPageKey, value: RemotePlaceResult.Success) {
        val time = now()
        entries.entries.removeAll { it.value.expiresAt <= time }
        sequence += 1
        entries[key] = Entry(value, safeExpiry(time), sequence)
        while (entries.size > maxEntries) {
            val oldest = entries.minByOrNull { it.value.access }?.key ?: break
            entries.remove(oldest)
            evictionCount += 1
        }
    }

    fun clear() = entries.clear()

    fun stats() = PlaceSearchCacheStats(hitCount, missCount, evictionCount, entries.size)

    private fun safeExpiry(time: Long): Long = if (Long.MAX_VALUE - time < ttlMillis) Long.MAX_VALUE else time + ttlMillis

    companion object {
        const val DEFAULT_MAX_ENTRIES = 32
        const val DEFAULT_TTL_MILLIS = 10 * 60 * 1000L
    }
}

data class PlaceSearchPageKey(
    val query: String,
    val city: String,
    val latitudeGrid: Int?,
    val longitudeGrid: Int?,
    val page: Int,
    val pageSize: Int
) {
    companion object {
        fun of(query: String, city: String, near: GeoPoint?, page: Int, pageSize: Int) = PlaceSearchPageKey(
            query = query.trim().lowercase(),
            city = city.trim().removeSuffix("市").lowercase(),
            latitudeGrid = near?.latitude?.times(1000.0)?.roundToInt(),
            longitudeGrid = near?.longitude?.times(1000.0)?.roundToInt(),
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 20)
        )
    }
}

private object PlaceSearchCacheRuntime {
    val cache = PlaceSearchPageCache()
}
