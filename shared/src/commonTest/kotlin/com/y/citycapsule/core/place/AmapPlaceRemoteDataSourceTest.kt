package com.y.citycapsule.core.place

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AmapPlaceRemoteDataSourceTest {
    @Test
    fun cityRecommendationsFillAcrossCategoriesAndStopAtLimit() {
        val queries = mutableListOf<String>()
        val remote = object : PlaceRemoteDataSource {
            override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
                queries += query
                val start = if (query == "景点") 0 else 8
                callback(RemotePlaceResult.Success((start until start + 8).map { index ->
                    RemotePlace("poi-$index", "地点$index", city, null, null, PlaceCategory.OTHER, emptyList(), near ?: GeoPoint(0.0, 0.0), null)
                }))
            }
        }
        var result: RemotePlaceResult? = null

        loadCityPlaceRecommendations(remote, "西安", GeoPoint(34.3, 108.9), 12) { result = it }

        assertEquals(listOf("景点", "博物馆"), queries)
        assertEquals(12, assertIs<RemotePlaceResult.Success>(result).places.size)
    }

    @Test
    fun normalizesClearTextPhotoUrlForHarmonyImageTransport() {
        assertEquals(
            "https://store.is.autonavi.com/showpic/example.jpg",
            normalizeRemoteImageUrl(" http://store.is.autonavi.com/showpic/example.jpg ")
        )
        assertEquals(
            "https://example.com/photo.jpg",
            normalizeRemoteImageUrl("https://example.com/photo.jpg")
        )
        assertEquals(null, normalizeRemoteImageUrl("file:///tmp/photo.jpg"))
    }

    @Test
    fun parsesPoiAndConvertsItToImportedDraft() {
        val body = """{
          "status":"1","info":"OK","pois":[{
            "id":"B001","name":"测试博物馆","type":"科教文化服务;博物馆",
            "location":"121.480000,31.230000","cityname":"上海市","adname":"黄浦区",
            "address":"测试路1号","tag":"展览,建筑",
            "photos":[{"title":"外观","url":"https://example.com/photo.jpg"}]
          }]}
        """.trimIndent()
        val response = JSONObject().apply {
            put("status", "success")
            put("body", body)
        }

        val result = assertIs<RemotePlaceResult.Success>(
            AmapPlaceRemoteDataSource.parseSearchResponse(response)
        )
        val place = result.places.single()
        assertEquals(PlaceCategory.MUSEUM, place.category)
        assertEquals("https://example.com/photo.jpg", place.photoUrl)
        assertTrue(abs(place.geoPoint.longitude - 121.48) > 0.0001)
        val draft = place.toImportedDraft()
        assertEquals(PlaceSource.IMPORTED, draft.source)
        assertTrue(draft.contentSource?.contains("B001") == true)
    }

    @Test
    fun specificParkTypeIsNotSwallowedByGenericScenicCategory() {
        val response = JSONObject().apply {
            put("status", "success")
            put("body", """{"status":"1","pois":[{"id":"PARK-1","name":"测试公园","type":"风景名胜;公园广场;公园","location":"108.900000,34.300000","cityname":"西安市"}]}""")
        }

        val result = assertIs<RemotePlaceResult.Success>(
            AmapPlaceRemoteDataSource.parseSearchResponse(response)
        )

        assertEquals(PlaceCategory.PARK, result.places.single().category)
    }

    @Test
    fun coordinateConversionRoundTripStaysWithinDemoAccuracy() {
        val original = GeoPoint(31.2304, 121.4737)
        val gcj = ChinaCoordinate.wgs84ToGcj02(original)
        val restored = ChinaCoordinate.gcj02ToWgs84(gcj)

        assertTrue(abs(restored.latitude - original.latitude) < 0.00005)
        assertTrue(abs(restored.longitude - original.longitude) < 0.00005)
        assertNotNull(restored)
    }

    @Test
    fun serviceFailureDoesNotProduceFakePlaces() {
        val response = JSONObject().apply {
            put("status", "success")
            put("body", "{\"status\":\"0\",\"info\":\"INVALID_USER_KEY\"}")
        }
        assertIs<RemotePlaceResult.Failure>(AmapPlaceRemoteDataSource.parseSearchResponse(response))
    }
}
