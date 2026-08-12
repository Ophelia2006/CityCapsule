package com.y.citycapsule.core.media

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

sealed interface PhotoPickerResult {
    data class Success(val paths: List<String>) : PhotoPickerResult
    data object Cancelled : PhotoPickerResult
    data class Failure(val message: String) : PhotoPickerResult
    data object Unsupported : PhotoPickerResult
}

sealed interface CameraCaptureResult {
    data class Success(val path: String) : CameraCaptureResult
    data object Cancelled : CameraCaptureResult
    data class Failure(val message: String) : CameraCaptureResult
    data object Unsupported : CameraCaptureResult
}

sealed interface ManagedMediaDeleteResult {
    data class Success(val deletedPaths: List<String>) : ManagedMediaDeleteResult
    data class Failure(val message: String) : ManagedMediaDeleteResult
    data object Unsupported : ManagedMediaDeleteResult
}

fun interface PhotoPickerCapability {
    fun pickImages(maxCount: Int, callback: (PhotoPickerResult) -> Unit)
}

fun interface CameraCapability {
    fun captureImage(callback: (CameraCaptureResult) -> Unit)
}

fun interface ManagedMediaFileCapability {
    fun deleteManagedImages(
        paths: List<String>,
        callback: (ManagedMediaDeleteResult) -> Unit
    )
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

class KuiklyCameraCapability internal constructor(
    private val transport: CameraTransport
) : CameraCapability {
    constructor(pager: Pager) : this(PagerCameraTransport(pager))

    override fun captureImage(callback: (CameraCaptureResult) -> Unit) {
        try {
            transport.capture(JSONObject()) { response ->
                if (response == null) {
                    callback(CameraCaptureResult.Failure("相机没有返回结果。"))
                    return@capture
                }
                when (response.optString(FIELD_STATUS)) {
                    STATUS_SUCCESS -> {
                        val path = response.optJSONArray(FIELD_PATHS)
                            ?.optString(0)
                            ?.takeIf(String::isNotBlank)
                        if (path == null) callback(CameraCaptureResult.Failure("相机没有返回照片。"))
                        else callback(CameraCaptureResult.Success(path))
                    }
                    STATUS_CANCELLED -> callback(CameraCaptureResult.Cancelled)
                    STATUS_UNSUPPORTED -> callback(CameraCaptureResult.Unsupported)
                    else -> callback(
                        CameraCaptureResult.Failure(
                            response.optString(FIELD_MESSAGE).ifBlank { "拍照失败，请重试。" }
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            callback(CameraCaptureResult.Failure("相机能力不可用，可以改用相册或纯文字记录。"))
        }
    }

    companion object {
        const val METHOD_CAPTURE_IMAGE = "captureImage"
        const val FIELD_STATUS = KuiklyPhotoPicker.FIELD_STATUS
        const val FIELD_PATHS = KuiklyPhotoPicker.FIELD_PATHS
        const val FIELD_MESSAGE = KuiklyPhotoPicker.FIELD_MESSAGE
        const val STATUS_SUCCESS = KuiklyPhotoPicker.STATUS_SUCCESS
        const val STATUS_CANCELLED = KuiklyPhotoPicker.STATUS_CANCELLED
        const val STATUS_UNSUPPORTED = KuiklyPhotoPicker.STATUS_UNSUPPORTED
    }
}

class KuiklyManagedMediaFiles internal constructor(
    private val transport: ManagedMediaTransport
) : ManagedMediaFileCapability {
    constructor(pager: Pager) : this(PagerManagedMediaTransport(pager))

    override fun deleteManagedImages(
        paths: List<String>,
        callback: (ManagedMediaDeleteResult) -> Unit
    ) {
        val candidates = paths
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (candidates.isEmpty()) {
            callback(ManagedMediaDeleteResult.Success(emptyList()))
            return
        }
        val request = JSONObject().apply {
            put(FIELD_PATHS, JSONArray().apply { candidates.forEach(::put) })
        }
        try {
            transport.delete(request) { response ->
                if (response == null) {
                    callback(ManagedMediaDeleteResult.Failure("照片文件清理没有返回结果。"))
                    return@delete
                }
                when (response.optString(FIELD_STATUS)) {
                    STATUS_SUCCESS -> {
                        val deletedJson = response.optJSONArray(FIELD_PATHS)
                        val deleted = buildList {
                            if (deletedJson != null) {
                                for (index in 0 until deletedJson.length()) {
                                    deletedJson.optString(index)
                                        ?.takeIf(String::isNotBlank)
                                        ?.let(::add)
                                }
                            }
                        }.distinct()
                        callback(ManagedMediaDeleteResult.Success(deleted))
                    }
                    STATUS_UNSUPPORTED -> callback(ManagedMediaDeleteResult.Unsupported)
                    else -> callback(
                        ManagedMediaDeleteResult.Failure(
                            response.optString(FIELD_MESSAGE).ifBlank { "照片文件暂时无法清理。" }
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            callback(ManagedMediaDeleteResult.Failure("照片文件清理能力不可用。"))
        }
    }

    companion object {
        const val MODULE_NAME = KuiklyPhotoPicker.MODULE_NAME
        const val METHOD_DELETE_IMAGES = "deleteImages"
        const val FIELD_STATUS = KuiklyPhotoPicker.FIELD_STATUS
        const val FIELD_PATHS = KuiklyPhotoPicker.FIELD_PATHS
        const val FIELD_MESSAGE = KuiklyPhotoPicker.FIELD_MESSAGE
        const val STATUS_SUCCESS = KuiklyPhotoPicker.STATUS_SUCCESS
        const val STATUS_UNSUPPORTED = KuiklyPhotoPicker.STATUS_UNSUPPORTED
    }
}

internal fun interface PhotoPickerTransport {
    fun pick(request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal fun interface CameraTransport {
    fun capture(request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal fun interface ManagedMediaTransport {
    fun delete(request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal class KuiklyMediaModule : Module() {
    override fun moduleName(): String = KuiklyPhotoPicker.MODULE_NAME

    fun pick(request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(KuiklyPhotoPicker.METHOD_PICK_IMAGES, request, callback)
    }

    fun capture(request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(KuiklyCameraCapability.METHOD_CAPTURE_IMAGE, request, callback)
    }

    fun delete(request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(KuiklyManagedMediaFiles.METHOD_DELETE_IMAGES, request, callback)
    }

    fun request(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(method, request, callback)
    }
}

private class PagerCameraTransport(
    private val pager: Pager
) : CameraTransport {
    override fun capture(request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyMediaModule>(KuiklyPhotoPicker.MODULE_NAME)
            .capture(request, callback)
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

private class PagerManagedMediaTransport(
    private val pager: Pager
) : ManagedMediaTransport {
    override fun delete(request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyMediaModule>(KuiklyManagedMediaFiles.MODULE_NAME)
            .delete(request, callback)
    }
}
