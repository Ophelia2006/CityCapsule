package com.y.citycapsule.core.place

import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals

class PlacePhotoHydratorTest {
    @Test
    fun limitsConcurrentLookupsAndPersistsResolvedPhotos() {
        val remote = DeferredRemote()
        val cache = RecordingCache()
        val resolved = mutableListOf<PlacePhotoCacheEntry>()
        val places = (1..3).map { placeFixture(id = "p$it", name = "地点$it") }
        val hydrator = PlacePhotoHydrator(remote, cache, maxConcurrentRequests = 2)

        hydrator.request(places, emptySet(), resolved::add)
        assertEquals(listOf("地点1", "地点2"), remote.queries)

        remote.completeFirst(photoUrl = "https://example.com/1.jpg")
        assertEquals(listOf("地点1", "地点2", "地点3"), remote.queries)
        assertEquals(listOf("p1"), resolved.map(PlacePhotoCacheEntry::placeId))
        assertEquals(listOf("p1"), cache.savedIds)
    }

    @Test
    fun skipsBundledCachedAndDuplicatePlaces() {
        val remote = DeferredRemote()
        val cache = RecordingCache()
        val cached = placeFixture(id = "cached")
        val plain = placeFixture(id = "plain")
        val bundled = placeFixture(id = "bundled").copy(
            visualRef = PlaceVisualRef(PlaceVisualType.BUNDLED_ASSET, "cover")
        )
        val hydrator = PlacePhotoHydrator(remote, cache)

        hydrator.request(listOf(cached, plain, plain, bundled), setOf(cached.id)) {}

        assertEquals(listOf("上海博物馆"), remote.queries)
    }

    private class DeferredRemote : PlaceRemoteDataSource {
        val queries = mutableListOf<String>()
        private val callbacks = ArrayDeque<(RemotePlaceResult) -> Unit>()

        override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
            queries += query
            callbacks.addLast(callback)
        }

        fun completeFirst(photoUrl: String?) {
            val query = queries.first()
            callbacks.removeFirst()(
                RemotePlaceResult.Success(
                    listOf(
                        RemotePlace(
                            providerId = "remote",
                            name = query,
                            city = "上海",
                            district = null,
                            address = null,
                            category = PlaceCategory.CULTURE,
                            tags = emptyList(),
                            geoPoint = GeoPoint(31.0, 121.0),
                            photoUrl = photoUrl
                        )
                    )
                )
            )
        }
    }

    private class RecordingCache : PlacePhotoCacheRepository {
        val savedIds = mutableListOf<String>()
        override fun getValid(callback: (StorageResult<Map<String, PlacePhotoCacheEntry>>) -> Unit) =
            callback(StorageResult.Success(emptyMap()))

        override fun put(placeId: String, url: String, source: String, callback: (StorageResult<Unit>) -> Unit) {
            savedIds += placeId
            callback(StorageResult.Success(Unit))
        }

        override fun remove(placeId: String, callback: (StorageResult<Unit>) -> Unit) =
            callback(StorageResult.Success(Unit))
    }
}
