package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import org.json.JSONObject

class KRPlaceNetworkModule : KuiklyRenderBaseModule() {
    private val executor = Executors.newCachedThreadPool()

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (callback == null) return null
        if (BuildConfig.AMAP_WEB_SERVICE_KEY.isBlank()) {
            callback.invoke(response("unavailable", message = "未配置高德 Web 服务 Key。"))
            return null
        }
        val request = runCatching { JSONObject(params.orEmpty()) }.getOrNull() ?: JSONObject()
        val url = when (method) {
            METHOD_SEARCH -> searchUrl(request)
            METHOD_REVERSE -> reverseUrl(request)
            METHOD_WALKING -> walkingUrl(request)
            else -> null
        }
        if (url == null) {
            callback.invoke(response("failure", message = "网络请求参数无效。"))
            return null
        }
        executor.execute {
            val result = runCatching { get(url) }
                .fold(
                    onSuccess = { response("success", body = it) },
                    onFailure = { response("failure", message = "网络请求失败，请稍后重试。") }
                )
            activity?.runOnUiThread { callback.invoke(result) } ?: callback.invoke(result)
        }
        return null
    }

    private fun searchUrl(request: JSONObject): String {
        val query = request.optString("query").trim()
        val city = request.optString("city").trim()
        val location = request.optString("location").trim()
        val endpoint = if (location.isNotBlank() && query.isBlank()) "around" else "text"
        val params = linkedMapOf(
            "key" to BuildConfig.AMAP_WEB_SERVICE_KEY,
            "output" to "JSON",
            "extensions" to "all",
            "offset" to request.optInt("pageSize", 12).coerceIn(1, 20).toString(),
            "page" to request.optInt("page", 1).coerceAtLeast(1).toString()
        )
        if (query.isNotBlank()) params["keywords"] = query
        if (city.isNotBlank()) {
            params["city"] = city
            params["citylimit"] = "true"
        }
        if (location.isNotBlank()) {
            params["location"] = location
            params["radius"] = "10000"
            params["sortrule"] = "distance"
        }
        return "https://restapi.amap.com/v3/place/$endpoint?${encode(params)}"
    }

    private fun reverseUrl(request: JSONObject): String {
        val location = request.optString("location").trim()
        if (location.isBlank()) return ""
        return "https://restapi.amap.com/v3/geocode/regeo?" + encode(linkedMapOf(
            "key" to BuildConfig.AMAP_WEB_SERVICE_KEY,
            "output" to "JSON",
            "extensions" to "base",
            "location" to location
        ))
    }

    private fun walkingUrl(request: JSONObject): String {
        val origin = request.optString("origin").trim()
        val destination = request.optString("destination").trim()
        if (origin.isBlank() || destination.isBlank()) return ""
        return "https://restapi.amap.com/v3/direction/walking?" + encode(linkedMapOf(
            "key" to BuildConfig.AMAP_WEB_SERVICE_KEY,
            "output" to "JSON",
            "origin" to origin,
            "destination" to destination
        ))
    }

    private fun encode(params: Map<String, String>): String = params.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8") }=${URLEncoder.encode(it.value, "UTF-8") }"
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        return try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun response(status: String, body: String = "", message: String = ""): String =
        JSONObject().apply {
            put("status", status)
            if (body.isNotBlank()) put("body", body)
            if (message.isNotBlank()) put("message", message)
        }.toString()

    companion object {
        const val MODULE_NAME = "CCPlaceNetworkModule"
        const val METHOD_SEARCH = "searchPlaces"
        const val METHOD_REVERSE = "reverseGeocode"
        const val METHOD_WALKING = "walkingRoute"
    }
}
