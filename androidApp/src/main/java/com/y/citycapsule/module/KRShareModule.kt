package com.y.citycapsule.module

import android.content.Intent
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

class KRShareModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        if (method != METHOD_SHARE_TEXT) return false
        return runCatching {
            val json = JSONObject(params ?: "{}")
            val intent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, json.optString("title")); putExtra(Intent.EXTRA_TEXT, json.optString("text")); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }, json.optString("title"))
            context?.startActivity(intent); callback?.invoke(JSONObject().put("success", true)); true
        }.getOrElse { callback?.invoke(JSONObject().put("success", false)); false }
    }
    companion object {
        const val MODULE_NAME = "CCShareModule"
        private const val METHOD_SHARE_TEXT = "shareText"
    }
}
