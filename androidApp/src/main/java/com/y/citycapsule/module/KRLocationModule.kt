package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.KuiklyHostActivity
import org.json.JSONObject

class KRLocationModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (callback == null) return null
        if (method != METHOD_GET_CURRENT_LOCATION) {
            callback.invoke(response(STATUS_UNAVAILABLE, "当前定位方法不受支持。"))
            return null
        }
        val host = activity as? KuiklyHostActivity
        if (host == null) callback.invoke(response(STATUS_UNAVAILABLE, "当前页面不支持定位。"))
        else host.requestCurrentLocation(callback)
        return null
    }

    companion object {
        const val MODULE_NAME = "CCLocationModule"
        const val METHOD_GET_CURRENT_LOCATION = "getCurrentLocation"
        const val STATUS_SUCCESS = "success"
        const val STATUS_PERMISSION_DENIED = "permission_denied"
        const val STATUS_PERMISSION_PERMANENTLY_DENIED = "permission_permanently_denied"
        const val STATUS_SERVICE_DISABLED = "service_disabled"
        const val STATUS_UNAVAILABLE = "unavailable"
        const val STATUS_FAILURE = "failure"

        fun response(
            status: String,
            message: String = "",
            latitude: Double? = null,
            longitude: Double? = null,
            accuracyMeters: Double? = null
        ): String = JSONObject().apply {
            put("status", status)
            put("message", message)
            latitude?.let { put("latitude", it) }
            longitude?.let { put("longitude", it) }
            accuracyMeters?.let { put("accuracyMeters", it) }
        }.toString()
    }
}
