package com.y.citycapsule.core.place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CachingPlaceRemoteDataSourceTest {
    @Test
    fun hitRefreshesLruAndLeastRecentlyUsedPageIsEvicted() {
        var calls = 0
        val delegate = fakeRemote { query ->
            calls += 1
            RemotePlaceResult.Success(listOf(place(query)))
        }
        val cache = PlaceSearchPageCache(maxEntries = 2, ttlMillis = 1_000L, now = { 0L })
        val source = CachingPlaceRemoteDataSource(delegate, cache)

        source.result("a")
        source.result("b")
        source.result("a") // refresh a; b is now least recently used
        source.result("c")
        source.result("b")

        assertEquals(4, calls)
        assertEquals(1, cache.stats().hits)
        assertEquals(2, cache.stats().evictions)
    }

    @Test
    fun ttlExpiresSuccessAndFailuresAreNeverCached() {
        var time = 0L
        var calls = 0
        val delegate = fakeRemote { query ->
            calls += 1
            if (query == "bad") RemotePlaceResult.Failure("offline")
            else RemotePlaceResult.Success(listOf(place(query)))
        }
        val source = CachingPlaceRemoteDataSource(
            delegate,
            PlaceSearchPageCache(maxEntries = 4, ttlMillis = 100L, now = { time })
        )

        assertIs<RemotePlaceResult.Success>(source.result("good"))
        time = 99L
        assertIs<RemotePlaceResult.Success>(source.result("good"))
        time = 100L
        assertIs<RemotePlaceResult.Success>(source.result("good"))
        source.result("bad")
        source.result("bad")

        assertEquals(4, calls)
    }

    @Test
    fun nearbyCoordinatesInSameHundredMeterGridShareEntry() {
        var calls = 0
        val source = CachingPlaceRemoteDataSource(
            fakeRemote { query -> calls += 1; RemotePlaceResult.Success(listOf(place(query))) },
            PlaceSearchPageCache(now = { 0L })
        )
        source.result("coffee", GeoPoint(31.23041, 121.47371))
        source.result(" coffee ", GeoPoint(31.23044, 121.47374))
        assertEquals(1, calls)
    }

    private fun CachingPlaceRemoteDataSource.result(query: String, near: GeoPoint? = null): RemotePlaceResult {
        var result: RemotePlaceResult? = null
        searchPage(query, "上海市", near, 1, 12) { result = it }
        return requireNotNull(result)
    }

    private fun fakeRemote(result: (String) -> RemotePlaceResult) = object : PlaceRemoteDataSource {
        override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) = callback(result(query.trim()))
        override fun searchPage(query: String, city: String, near: GeoPoint?, page: Int, pageSize: Int, callback: (RemotePlaceResult) -> Unit) = callback(result(query.trim()))
    }

    private fun place(id: String) = RemotePlace(id, id, "上海", null, null, PlaceCategory.OTHER, emptyList(), GeoPoint(31.2, 121.4), null)
}
