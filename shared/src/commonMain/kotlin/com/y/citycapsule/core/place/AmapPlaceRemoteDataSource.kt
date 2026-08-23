package com.y.citycapsule.core.place

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.city.CityRegistry
import com.y.citycapsule.core.city.ReverseGeocodeCapability
import com.y.citycapsule.core.city.ReverseGeocodeResult
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

data class RemotePlace(
    val providerId: String,
    val name: String,
    val city: String,
    val district: String?,
    val address: String?,
    val category: PlaceCategory,
    val tags: List<String>,
    val geoPoint: GeoPoint,
    val photoUrl: String?
) {
    fun toImportedDraft(cityOverride: String? = null): PlaceDraft = PlaceDraft(
        name = name,
        city = cityOverride?.trim()?.takeIf(String::isNotEmpty) ?: normalizePlaceCityName(city),
        district = district,
        category = category,
        address = address,
        tags = tags,
        description = null,
        contentSource = "高德地图 POI · $providerId",
        source = PlaceSource.IMPORTED,
        geoPoint = geoPoint
    )
}

sealed interface RemotePlaceResult {
    data class Success(val places: List<RemotePlace>) : RemotePlaceResult
    data class Failure(val message: String) : RemotePlaceResult
    data object Unavailable : RemotePlaceResult
}

interface PlaceRemoteDataSource {
    fun search(
        query: String,
        city: String,
        near: GeoPoint? = null,
        callback: (RemotePlaceResult) -> Unit
    )
}

/** Sequential category fallback used to fill a small, diverse city recommendation set. */
fun loadCityPlaceRecommendations(
    remote: PlaceRemoteDataSource,
    city: String,
    near: GeoPoint,
    limit: Int,
    callback: (RemotePlaceResult) -> Unit
) {
    val collected = linkedMapOf<String, RemotePlace>()
    var queryIndex = 0
    var lastFailure: RemotePlaceResult? = null
    fun requestNext() {
        if (collected.size >= limit || queryIndex >= CITY_RECOMMENDATION_QUERIES.size) {
            callback(
                if (collected.isNotEmpty()) RemotePlaceResult.Success(collected.values.take(limit))
                else lastFailure ?: RemotePlaceResult.Success(emptyList())
            )
            return
        }
        val query = CITY_RECOMMENDATION_QUERIES[queryIndex++]
        remote.search(query, city, near) { result ->
            when (result) {
                is RemotePlaceResult.Success -> result.places.forEach { place ->
                    if (place.providerId !in collected) collected[place.providerId] = place
                }
                is RemotePlaceResult.Failure -> lastFailure = result
                RemotePlaceResult.Unavailable -> lastFailure = result
            }
            requestNext()
        }
    }
    requestNext()
}

private val CITY_RECOMMENDATION_QUERIES = listOf("景点", "博物馆", "公园", "咖啡")

class AmapPlaceRemoteDataSource(pager: Pager) : PlaceRemoteDataSource {
    private val transport: PlaceNetworkTransport = PagerPlaceNetworkTransport(pager)

    override fun search(query: String, city: String, near: GeoPoint?, callback: (RemotePlaceResult) -> Unit) {
        val request = JSONObject().apply {
            put("query", query.trim())
            put("city", city.trim())
            near?.let { point ->
                val gcj = ChinaCoordinate.wgs84ToGcj02(point)
                put("location", "${gcj.longitude},${gcj.latitude}")
            }
        }
        try {
            transport.request(METHOD_SEARCH, request) { callback(parseSearchResponse(it)) }
        } catch (_: Throwable) {
            callback(RemotePlaceResult.Unavailable)
        }
    }

    internal companion object {
        const val MODULE_NAME = "CCPlaceNetworkModule"
        const val METHOD_SEARCH = "searchPlaces"
        const val METHOD_REVERSE = "reverseGeocode"

        fun parseSearchResponse(response: JSONObject?): RemotePlaceResult {
            if (response == null) return RemotePlaceResult.Failure("地点服务没有返回结果。")
            if (response.optString("status") == "unavailable") return RemotePlaceResult.Unavailable
            if (response.optString("status") != "success") {
                return RemotePlaceResult.Failure(response.optString("message").ifBlank { "地点搜索失败。" })
            }
            return try {
                val root = JSONObject(response.optString("body"))
                if (root.optString("status") != "1") {
                    return RemotePlaceResult.Failure(root.optString("info").ifBlank { "高德地点服务暂不可用。" })
                }
                val array = root.optJSONArray("pois") ?: return RemotePlaceResult.Success(emptyList())
                val places = buildList {
                    for (index in 0 until array.length()) {
                        val poi = array.optJSONObject(index) ?: continue
                        parsePlace(poi)?.let(::add)
                    }
                }.distinctBy(RemotePlace::providerId)
                RemotePlaceResult.Success(places)
            } catch (_: Throwable) {
                RemotePlaceResult.Failure("地点服务返回了无法识别的数据。")
            }
        }

        private fun parsePlace(poi: JSONObject): RemotePlace? {
            val providerId = poi.optString("id").trim().takeIf(String::isNotEmpty) ?: return null
            val name = poi.optString("name").trim().takeIf(String::isNotEmpty) ?: return null
            val coordinate = poi.optString("location").split(',').map(String::trim)
            if (coordinate.size != 2) return null
            val gcjLongitude = coordinate[0].toDoubleOrNull() ?: return null
            val gcjLatitude = coordinate[1].toDoubleOrNull() ?: return null
            val type = poi.optString("type")
            val photos = poi.optJSONArray("photos")
            val photo = photos?.optJSONObject(0)?.optString("url")
                ?.let(::normalizeRemoteImageUrl)
            val tag = poi.optString("tag").split(',', ';', '；').map(String::trim).filter(String::isNotEmpty)
            return RemotePlace(
                providerId = providerId,
                name = name,
                city = normalizePlaceCityName(
                    poi.optString("cityname").trim().ifBlank { poi.optString("pname").trim() }
                ),
                district = poi.optString("adname").trim().takeIf(String::isNotEmpty),
                address = poi.optString("address").trim().takeIf(String::isNotEmpty),
                category = mapCategory(type),
                tags = (type.split(';').drop(1) + tag).filter(String::isNotBlank).distinct().take(4),
                geoPoint = ChinaCoordinate.gcj02ToWgs84(GeoPoint(gcjLatitude, gcjLongitude)),
                photoUrl = photo
            )
        }

        private fun mapCategory(type: String): PlaceCategory = when {
            listOf("餐饮", "咖啡", "甜品").any(type::contains) -> PlaceCategory.FOOD
            listOf("公园", "自然地物").any(type::contains) -> PlaceCategory.NATURE
            listOf("科教文化", "博物馆", "展览馆", "体育休闲").any(type::contains) -> PlaceCategory.CULTURE
            listOf("购物", "商场").any(type::contains) -> PlaceCategory.SHOPPING
            listOf("风景名胜", "地名地址", "交通设施").any(type::contains) -> PlaceCategory.LANDMARK
            else -> PlaceCategory.OTHER
        }
    }
}

internal fun normalizeRemoteImageUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null
    return when {
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("http://", ignoreCase = true) -> "https://${trimmed.substring(7)}"
        else -> null
    }
}

class AmapReverseGeocodeCapability(pager: Pager) : ReverseGeocodeCapability {
    private val transport: PlaceNetworkTransport = PagerPlaceNetworkTransport(pager)

    override fun resolve(point: GeoPoint, callback: (ReverseGeocodeResult) -> Unit) {
        val gcj = ChinaCoordinate.wgs84ToGcj02(point)
        val request = JSONObject().apply { put("location", "${gcj.longitude},${gcj.latitude}") }
        try {
            transport.request(AmapPlaceRemoteDataSource.METHOD_REVERSE, request) { response ->
                callback(parse(response, point))
            }
        } catch (_: Throwable) {
            callback(ReverseGeocodeResult.Failure("当前无法连接城市识别服务。"))
        }
    }

    internal fun parse(response: JSONObject?, point: GeoPoint): ReverseGeocodeResult {
        if (response?.optString("status") != "success") {
            return ReverseGeocodeResult.Failure(
                response?.optString("message")?.ifBlank { "城市识别失败。" } ?: "城市识别失败。"
            )
        }
        return try {
            val root = JSONObject(response.optString("body"))
            val component = root.optJSONObject("regeocode")?.optJSONObject("addressComponent")
                ?: return ReverseGeocodeResult.Failure("没有识别到当前位置的城市。")
            val cityName = component.optString("city").trim().ifBlank { component.optString("province").trim() }
            val known = CityRegistry.cities.firstOrNull { city ->
                city.displayName == cityName.removeSuffix("市") || city.displayName == cityName
            }
            if (known?.supported == true) ReverseGeocodeResult.SupportedCity(known)
            else ReverseGeocodeResult.UnsupportedCity(known ?: dynamicCity(cityName, point))
        } catch (_: Throwable) {
            ReverseGeocodeResult.Failure("城市识别服务返回了无法识别的数据。")
        }
    }

    private fun dynamicCity(name: String, point: GeoPoint): CityDefinition? = name.trim()
        .removeSuffix("市")
        .takeIf(String::isNotEmpty)
        ?.let { CityDefinition("remote-${it.hashCode().toUInt().toString(16)}", it, point, false, 0) }
}

class FallbackReverseGeocodeCapability(
    private val primary: ReverseGeocodeCapability,
    private val fallback: ReverseGeocodeCapability
) : ReverseGeocodeCapability {
    override fun resolve(point: GeoPoint, callback: (ReverseGeocodeResult) -> Unit) {
        primary.resolve(point) { result ->
            if (result is ReverseGeocodeResult.Failure) fallback.resolve(point, callback)
            else callback(result)
        }
    }
}

internal interface PlaceNetworkTransport {
    fun request(method: String, request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal class KuiklyPlaceNetworkModule : Module() {
    override fun moduleName(): String = AmapPlaceRemoteDataSource.MODULE_NAME
    fun request(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(method, request, callback)
    }
}

private class PagerPlaceNetworkTransport(private val pager: Pager) : PlaceNetworkTransport {
    override fun request(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyPlaceNetworkModule>(AmapPlaceRemoteDataSource.MODULE_NAME)
            .request(method, request, callback)
    }
}

internal object ChinaCoordinate {
    fun wgs84ToGcj02(point: GeoPoint): GeoPoint {
        if (outsideChina(point.latitude, point.longitude)) return point
        val delta = delta(point.latitude, point.longitude)
        return GeoPoint(point.latitude + delta.first, point.longitude + delta.second)
    }

    fun gcj02ToWgs84(point: GeoPoint): GeoPoint {
        if (outsideChina(point.latitude, point.longitude)) return point
        val delta = delta(point.latitude, point.longitude)
        return GeoPoint(point.latitude - delta.first, point.longitude - delta.second)
    }

    private fun delta(latitude: Double, longitude: Double): Pair<Double, Double> {
        val a = 6378245.0
        val ee = 0.00669342162296594323
        var dLat = transformLat(longitude - 105.0, latitude - 35.0)
        var dLon = transformLon(longitude - 105.0, latitude - 35.0)
        val radLat = latitude / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - ee * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / ((a * (1 - ee)) / (magic * sqrtMagic) * PI)
        dLon = dLon * 180.0 / (a / sqrtMagic * cos(radLat) * PI)
        return dLat to dLon
    }

    private fun transformLat(x: Double, y: Double): Double =
        -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y +
            0.2 * sqrt(kotlin.math.abs(x)) + (20.0 * sin(6.0 * x * PI) +
            20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0 +
            (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0 +
            (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0

    private fun transformLon(x: Double, y: Double): Double =
        300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y +
            0.1 * sqrt(kotlin.math.abs(x)) + (20.0 * sin(6.0 * x * PI) +
            20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0 +
            (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0 +
            (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0

    private fun outsideChina(latitude: Double, longitude: Double): Boolean =
        longitude !in 72.004..137.8347 || latitude !in 0.8293..55.8271
}
