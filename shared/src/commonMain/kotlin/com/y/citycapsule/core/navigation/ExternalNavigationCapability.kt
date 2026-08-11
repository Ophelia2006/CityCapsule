package com.y.citycapsule.core.navigation

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

sealed interface ExternalNavigationResult {
    data object Opened : ExternalNavigationResult
    data object NoCompatibleApp : ExternalNavigationResult
    data object Unsupported : ExternalNavigationResult
    data class Failure(val message: String) : ExternalNavigationResult
}

fun interface ExternalNavigationCapability {
    fun open(latitude: Double, longitude: Double, placeName: String, callback: (ExternalNavigationResult) -> Unit)
}

class KuiklyExternalNavigationCapability(pager: Pager) : ExternalNavigationCapability {
    private val module = pager.acquireModule<KuiklyExternalNavigationModule>(MODULE_NAME)

    override fun open(latitude: Double, longitude: Double, placeName: String, callback: (ExternalNavigationResult) -> Unit) {
        if (!latitude.isFinite() || latitude !in -90.0..90.0 || !longitude.isFinite() || longitude !in -180.0..180.0) {
            callback(ExternalNavigationResult.Failure("地点坐标无效。"))
            return
        }
        val request = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("placeName", placeName)
        }
        runCatching {
            module.open(request) { response ->
                callback(when (response?.optString("status")) {
                    "opened" -> ExternalNavigationResult.Opened
                    "no_compatible_app" -> ExternalNavigationResult.NoCompatibleApp
                    "unsupported" -> ExternalNavigationResult.Unsupported
                    else -> ExternalNavigationResult.Failure(
                        response?.optString("message")?.ifBlank { "无法打开外部地图。" } ?: "无法打开外部地图。"
                    )
                })
            }
        }.onFailure { callback(ExternalNavigationResult.Unsupported) }
    }

    companion object { const val MODULE_NAME = "CCExternalNavigationModule" }
}

internal class KuiklyExternalNavigationModule : Module() {
    override fun moduleName(): String = KuiklyExternalNavigationCapability.MODULE_NAME
    fun open(request: JSONObject, callback: (JSONObject?) -> Unit) =
        asyncToNativeMethod("open", request, callback)
}
