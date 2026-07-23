package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.designsystem.AndroidThemeHost
import com.y.citycapsule.core.theme.ThemeHostProtocol
import org.json.JSONObject

/** Receives the resolved shared theme and keeps Android system chrome in sync. */
class KRThemeHostModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (method != ThemeHostProtocol.METHOD_APPLY_APPEARANCE) {
            callback?.invoke(mapOf("success" to false, "message" to "unknown_method"))
            return null
        }

        val request = runCatching { JSONObject(params ?: "{}") }.getOrNull()
        val protocolVersion = request?.optInt(ThemeHostProtocol.FIELD_PROTOCOL_VERSION, -1) ?: -1
        if (protocolVersion != ThemeHostProtocol.VERSION ||
            request?.has(ThemeHostProtocol.FIELD_IS_DARK) != true
        ) {
            callback?.invoke(mapOf("success" to false, "message" to "invalid_request"))
            return null
        }

        val hostActivity = activity
        if (hostActivity == null) {
            callback?.invoke(mapOf("success" to false, "message" to "activity_unavailable"))
            return null
        }

        val isDark = request.optBoolean(ThemeHostProtocol.FIELD_IS_DARK)
        hostActivity.runOnUiThread {
            AndroidThemeHost.applySystemBars(hostActivity, isDark)
        }
        callback?.invoke(mapOf("success" to true))
        return null
    }

    companion object {
        const val MODULE_NAME = ThemeHostProtocol.MODULE_NAME
    }
}
