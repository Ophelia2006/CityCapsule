package com.y.citycapsule.core.media

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

sealed interface PhotoPickerResult {
    data class Success(val paths: List<String>) : PhotoPickerResult
    data object Cancelled : PhotoPickerResult
    data class Failure(val message: String) : PhotoPickerResult
    data object Unsupported : PhotoPickerResult
}

fun interface PhotoPickerCapability {
    fun pickImages(maxCount: Int, callback: (PhotoPickerResult) -> Unit)
}

class KuiklyPhotoPicker internal constructor(
    private val transport: PhotoPickerTransport
) : PhotoPickerCapability {
    constructor(pager: Pager) : this(PagerPhotoPickerTransport(pager))

    override fun pickImages(maxCount: Int, callback: (PhotoPickerResult) -> Unit) {
        if (maxCount <= 0) {
            callback(PhotoPickerResult.Failure("没有可继续添加的照片位置。"))
            return
        }
        val request = JSONObject().apply { put(FIELD_MAX_COUNT, maxCount) }
        try {
            transport.pick(request) { response ->
                if (response == null) {
                    callback(PhotoPickerResult.Failure("照片选择器没有返回结果。"))
                    return@pick
                }
                when (response.optString(FIELD_STATUS)) {
                    STATUS_SUCCESS -> {
                        val pathsJson = response.optJSONArray(FIELD_PATHS)
                        val paths = buildList {
                            if (pathsJson != null) {
                                for (index in 0 until pathsJson.length()) {
                                    pathsJson.optString(index)?.takeIf(String::isNotBlank)?.let(::add)
                                }
                            }
                        }.distinct().take(maxCount)
                        if (paths.isEmpty()) {
                            callback(PhotoPickerResult.Cancelled)
                        } else {
                            callback(PhotoPickerResult.Success(paths))
                        }
                    }
                    STATUS_CANCELLED -> callback(PhotoPickerResult.Cancelled)
                    STATUS_UNSUPPORTED -> callback(PhotoPickerResult.Unsupported)
                    else -> callback(
                        PhotoPickerResult.Failure(
                            response.optString(FIELD_MESSAGE).ifBlank { "照片选择失败。" }
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            callback(
                PhotoPickerResult.Failure(
                    "照片选择能力不可用，请确认已安装包含媒体模块的最新应用版本。"
                )
            )
        }
    }

    companion object {
        const val MODULE_NAME = "CCMediaModule"
        const val METHOD_PICK_IMAGES = "pickImages"
        const val FIELD_MAX_COUNT = "maxCount"
        const val FIELD_STATUS = "status"
        const val FIELD_PATHS = "paths"
        const val FIELD_MESSAGE = "message"
        const val STATUS_SUCCESS = "success"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILURE = "failure"
        const val STATUS_UNSUPPORTED = "unsupported"
    }
}

internal fun interface PhotoPickerTransport {
    fun pick(request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal class KuiklyMediaModule : Module() {
    override fun moduleName(): String = KuiklyPhotoPicker.MODULE_NAME

    fun pick(request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(KuiklyPhotoPicker.METHOD_PICK_IMAGES, request, callback)
    }
}

private class PagerPhotoPickerTransport(
    private val pager: Pager
) : PhotoPickerTransport {
    override fun pick(request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyMediaModule>(KuiklyPhotoPicker.MODULE_NAME)
            .pick(request, callback)
    }
}
