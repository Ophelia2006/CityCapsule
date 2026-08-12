package com.y.citycapsule.module

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
            METHOD_ENSURE_THUMBNAIL -> maintain(params, callback) { store, json ->
                val path = store.ensureThumbnail(json.optString(FIELD_PATH))
                response(STATUS_SUCCESS, path = path)
            }
            METHOD_MEDIA_STATISTICS -> maintain(params, callback) { store, _ -> store.statistics() }
            METHOD_CLEAR_THUMBNAILS -> maintain(params, callback) { store, _ -> store.clearThumbnails() }
            METHOD_CLEANUP_UNREFERENCED -> maintain(params, callback) { store, json ->
                val values = json.optJSONArray(FIELD_PATHS) ?: JSONArray()
                val referenced = buildSet { for (i in 0 until values.length()) add(values.optString(i)) }
                store.cleanupUnreferenced(referenced, json.optString(FIELD_GRACE_MILLIS).toLongOrNull() ?: 0)
            }
            else -> callback.invoke(response(STATUS_UNSUPPORTED, "当前媒体方法不受支持。"))
        }
        return null
    }

    private fun maintain(params: String?, callback: KuiklyRenderCallback, action: (ManagedImageFileStore, JSONObject) -> String) {
        val host = activity as? KuiklyHostActivity
        if (host == null) {
            callback.invoke(response(STATUS_UNSUPPORTED))
            return
        }
        val output = runCatching { action(ManagedImageFileStore(host.filesDir), JSONObject(params ?: "{}")) }
            .getOrElse { response(STATUS_FAILURE, "媒体文件操作失败。") }
        callback.invoke(output)
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
        private const val METHOD_ENSURE_THUMBNAIL = "ensureThumbnail"
        private const val METHOD_MEDIA_STATISTICS = "mediaStatistics"
        private const val METHOD_CLEAR_THUMBNAILS = "clearThumbnails"
        private const val METHOD_CLEANUP_UNREFERENCED = "cleanupUnreferenced"
        private const val FIELD_MAX_COUNT = "maxCount"
        private const val FIELD_PATHS = "paths"
        private const val FIELD_PATH = "path"
        private const val FIELD_GRACE_MILLIS = "gracePeriodMillis"
        private const val MAX_IMAGES = 9
        private const val MAX_DELETE_BATCH = 32
        internal const val STATUS_SUCCESS = "success"
        internal const val STATUS_CANCELLED = "cancelled"
        internal const val STATUS_FAILURE = "failure"
        internal const val STATUS_UNSUPPORTED = "unsupported"

        internal fun response(
            status: String,
            message: String = "",
            paths: List<String> = emptyList(),
            path: String = "",
            block: JSONObject.() -> Unit = {}
        ): String = JSONObject().apply {
            put("status", status)
            put("message", message)
            put("paths", JSONArray(paths))
            if (path.isNotEmpty()) put("path", path)
            block()
        }.toString()
    }
}

internal data class ManagedImageDeleteResult(
    val deletedPaths: List<String>,
    val rejected: Boolean
)

internal class ManagedImageFileStore(filesDir: File) {
    private val managedDirectory = File(filesDir, "images/original").canonicalFile
    private val thumbnailDirectory = File(filesDir, "images/thumbnail").canonicalFile

    fun delete(paths: List<String>): ManagedImageDeleteResult {
        val resolved = paths.map { path -> path to resolveManagedFile(path) }
        if (resolved.any { it.second == null }) {
            return ManagedImageDeleteResult(emptyList(), rejected = true)
        }
        val deleted = buildList {
            resolved.forEach { (path, candidate) ->
                val file = requireNotNull(candidate)
                val thumbnail = thumbnailFor(file)
                if (!file.exists() || file.delete()) {
                    thumbnail.delete()
                    add(path)
                }
            }
        }
        return ManagedImageDeleteResult(deleted, rejected = false)
    }

    fun ensureThumbnail(path: String): String {
        val source = requireNotNull(resolveManagedFile(path))
        require(source.isFile)
        thumbnailDirectory.mkdirs()
        val target = thumbnailFor(source)
        if (!target.isFile || target.length() == 0L) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0)
            var sample = 1
            while (bounds.outWidth / sample > THUMBNAIL_EDGE * 2 || bounds.outHeight / sample > THUMBNAIL_EDGE * 2) sample *= 2
            val decoded = requireNotNull(BitmapFactory.decodeFile(source.path, BitmapFactory.Options().apply { inSampleSize = sample }))
            val scale = minOf(1f, THUMBNAIL_EDGE.toFloat() / maxOf(decoded.width, decoded.height))
            val bitmap = if (scale < 1f) Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true) else decoded
            val temp = File(thumbnailDirectory, target.name + ".tmp")
            temp.outputStream().use { require(bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it)) }
            if (bitmap !== decoded) bitmap.recycle()
            decoded.recycle()
            require(temp.renameTo(target) || run { temp.copyTo(target, overwrite = true); temp.delete() })
        }
        return "file://${target.absolutePath}"
    }

    fun statistics(): String = KRMediaModule.response(KRMediaModule.STATUS_SUCCESS) {
        put("originalBytes", files(managedDirectory).sumOf(File::length).toString())
        put("originalCount", files(managedDirectory).size.toString())
        put("thumbnailBytes", files(thumbnailDirectory).sumOf(File::length).toString())
        put("thumbnailCount", files(thumbnailDirectory).size.toString())
    }

    fun clearThumbnails(): String = deleteFiles(files(thumbnailDirectory))

    fun cleanupUnreferenced(referenced: Set<String>, graceMillis: Long): String {
        val protected = referenced.mapNotNull(::resolveManagedFile)
            .map { file -> file.canonicalPath }
            .toSet()
        val cutoff = System.currentTimeMillis() - graceMillis.coerceAtLeast(0)
        return deleteFiles(files(managedDirectory).filter { it.canonicalPath !in protected && it.lastModified() <= cutoff }.flatMap { listOf(it, thumbnailFor(it)) })
    }

    private fun deleteFiles(candidates: List<File>): String {
        var bytes = 0L
        var count = 0
        candidates.distinctBy { file -> file.canonicalPath }.forEach { if (it.isFile) { val size = it.length(); if (it.delete()) { bytes += size; count++ } } }
        return KRMediaModule.response(KRMediaModule.STATUS_SUCCESS) { put("deletedBytes", bytes.toString()); put("deletedCount", count.toString()) }
    }

    private fun thumbnailFor(source: File) = File(thumbnailDirectory, source.name + ".jpg")
    private fun files(directory: File) = directory.listFiles()?.filter(File::isFile).orEmpty()

    private fun resolveManagedFile(path: String): File? {
        val uri = runCatching { URI(path) }.getOrNull() ?: return null
        if (uri.scheme != FILE_SCHEME) return null
        val decodedPath = uri.path ?: return null
        val candidate = runCatching { File(decodedPath).canonicalFile }.getOrNull() ?: return null
        return candidate.takeIf { it.parentFile == managedDirectory }
    }

    private companion object {
        const val FILE_SCHEME = "file"
        const val THUMBNAIL_EDGE = 512
    }
}
