package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.y.citycapsule.KuiklyHostActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI

class KRMediaModule : KuiklyRenderBaseModule() {
    override fun call(
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    ): Any? {
        if (callback == null) {
            return null
        }
        when (method) {
            METHOD_PICK_IMAGES -> {
                val maxCount = runCatching {
                    JSONObject(params ?: "{}").optInt(FIELD_MAX_COUNT, 1)
                        .coerceIn(1, MAX_IMAGES)
                }.getOrDefault(1)
                val host = activity as? KuiklyHostActivity
                if (host == null) {
                    callback.invoke(response(STATUS_UNSUPPORTED, "当前页面不支持照片选择。"))
                    return null
                }
                host.pickImages(maxCount, callback)
            }
            METHOD_CAPTURE_IMAGE -> {
                val host = activity as? KuiklyHostActivity
                if (host == null) {
                    callback.invoke(response(STATUS_UNSUPPORTED, "当前页面不支持拍照。"))
                    return null
                }
                host.captureImage(callback)
            }
            METHOD_DELETE_IMAGES -> deleteImages(params, callback)
            else -> callback.invoke(response(STATUS_UNSUPPORTED, "当前媒体方法不受支持。"))
        }
        return null
    }

    private fun deleteImages(params: String?, callback: KuiklyRenderCallback) {
        val paths = runCatching {
            val values = JSONObject(params ?: "{}").optJSONArray(FIELD_PATHS) ?: JSONArray()
            buildList {
                for (index in 0 until values.length()) {
                    values.optString(index).takeIf(String::isNotBlank)?.let(::add)
                }
            }.distinct().take(MAX_DELETE_BATCH)
        }.getOrElse {
            callback.invoke(response(STATUS_FAILURE, "照片清理请求无效。"))
            return
        }
        val host = activity as? KuiklyHostActivity
        if (host == null) {
            callback.invoke(response(STATUS_UNSUPPORTED, "当前页面不支持照片文件清理。"))
            return
        }
        val result = ManagedImageFileStore(host.filesDir).delete(paths)
        callback.invoke(
            if (result.rejected) {
                response(STATUS_FAILURE, "照片路径不属于应用管理目录，未执行删除。")
            } else {
                response(STATUS_SUCCESS, paths = result.deletedPaths)
            }
        )
    }

    companion object {
        const val MODULE_NAME = "CCMediaModule"
        private const val METHOD_PICK_IMAGES = "pickImages"
        private const val METHOD_CAPTURE_IMAGE = "captureImage"
        private const val METHOD_DELETE_IMAGES = "deleteImages"
        private const val FIELD_MAX_COUNT = "maxCount"
        private const val FIELD_PATHS = "paths"
        private const val MAX_IMAGES = 9
        private const val MAX_DELETE_BATCH = 32
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

internal data class ManagedImageDeleteResult(
    val deletedPaths: List<String>,
    val rejected: Boolean
)

internal class ManagedImageFileStore(filesDir: File) {
    private val managedDirectory = File(filesDir, "images/original").canonicalFile

    fun delete(paths: List<String>): ManagedImageDeleteResult {
        val resolved = paths.map { path -> path to resolveManagedFile(path) }
        if (resolved.any { it.second == null }) {
            return ManagedImageDeleteResult(emptyList(), rejected = true)
        }
        val deleted = buildList {
            resolved.forEach { (path, candidate) ->
                val file = requireNotNull(candidate)
                if (!file.exists() || file.delete()) add(path)
            }
        }
        return ManagedImageDeleteResult(deleted, rejected = false)
    }

    private fun resolveManagedFile(path: String): File? {
        val uri = runCatching { URI(path) }.getOrNull() ?: return null
        if (uri.scheme != FILE_SCHEME) return null
        val decodedPath = uri.path ?: return null
        val candidate = runCatching { File(decodedPath).canonicalFile }.getOrNull() ?: return null
        return candidate.takeIf { it.parentFile == managedDirectory }
    }

    private companion object {
        const val FILE_SCHEME = "file"
    }
}
