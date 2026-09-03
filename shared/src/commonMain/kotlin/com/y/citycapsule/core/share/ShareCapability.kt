package com.y.citycapsule.core.share

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

fun interface ShareCapability { fun shareText(title: String, text: String, callback: (Boolean) -> Unit) }

class KuiklyShareCapability(private val pager: Pager) : ShareCapability {
    override fun shareText(title: String, text: String, callback: (Boolean) -> Unit) {
        try { pager.acquireModule<KuiklyShareModule>(MODULE).share(JSONObject().apply { put("title", title); put("text", text) }, callback) }
        catch (_: Throwable) { callback(false) }
    }
    companion object { const val MODULE = "CCShareModule"; const val METHOD = "shareText" }
}

internal class KuiklyShareModule : Module() {
    override fun moduleName() = KuiklyShareCapability.MODULE
    fun share(request: JSONObject, callback: (Boolean) -> Unit) = asyncToNativeMethod(KuiklyShareCapability.METHOD, request) { callback(it?.optBoolean("success") == true) }
}
