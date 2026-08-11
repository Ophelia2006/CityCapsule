package com.y.citycapsule.module

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

class KRExternalNavigationModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (callback == null) return null
        if (method != "open") {
            callback.invoke(response("unsupported"))
            return null
        }
        val host = activity
        if (host == null) {
            Log.e(TAG, "Cannot open external navigation: host activity is unavailable")
            callback.invoke(response("unsupported", "当前页面不支持外部导航。"))
            return null
        }
        try {
            val request = JSONObject(params.orEmpty())
            val latitude = request.getDouble("latitude")
            val longitude = request.getDouble("longitude")
            val name = Uri.encode(request.optString("placeName"))
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=$latitude,$longitude($name)")
            )
            // Do not preflight with resolveActivity(): Android 11 package visibility can
            // hide a valid map handler. Starting the implicit intent is the authoritative check.
            Log.i(TAG, "Opening external navigation with a geo URI")
            host.startActivity(intent)
            callback.invoke(response("opened"))
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "No application handles geo navigation intents")
            callback.invoke(response("no_compatible_app"))
        } catch (error: Throwable) {
            Log.e(TAG, "External navigation failed", error)
            callback.invoke(response("failure", error.message ?: "无法打开外部地图。"))
        }
        return null
    }

    companion object {
        const val MODULE_NAME = "CCExternalNavigationModule"
        private const val TAG = "CCExternalNavigation"

        private fun response(status: String, message: String = "") =
            JSONObject().put("status", status).put("message", message).toString()
    }
}
