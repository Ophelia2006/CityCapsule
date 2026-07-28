package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.KuiklyHostActivity
import org.json.JSONArray
import org.json.JSONObject

class KRMediaModule : KuiklyRenderBaseModule() {
    override fun call(
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    ): Any? {
        if (method != METHOD_PICK_IMAGES || callback == null) {
            return null
        }
        val maxCount = runCatching {
            JSONObject(params ?: "{}").optInt(FIELD_MAX_COUNT, 1).coerceIn(1, MAX_IMAGES)
        }.getOrDefault(1)
        val host = activity as? KuiklyHostActivity
        if (host == null) {
            callback.invoke(response(STATUS_UNSUPPORTED, "当前页面不支持照片选择。"))
            return null
        }
        host.pickImages(maxCount, callback)
        return null
    }

    companion object {
        const val MODULE_NAME = "CCMediaModule"
        private const val METHOD_PICK_IMAGES = "pickImages"
        private const val FIELD_MAX_COUNT = "maxCount"
        private const val MAX_IMAGES = 9
        internal const val STATUS_SUCCESS = "success"
        internal const val STATUS_CANCELLED = "cancelled"
        internal const val STATUS_FAILURE = "failure"
        internal const val STATUS_UNSUPPORTED = "unsupported"

        internal fun response(
            status: String,
            message: String = "",
            paths: List<String> = emptyList()
        ): String = JSONObject().apply {
            put("status", status)
            put("message", message)
            put("paths", JSONArray(paths))
        }.toString()
    }
}
