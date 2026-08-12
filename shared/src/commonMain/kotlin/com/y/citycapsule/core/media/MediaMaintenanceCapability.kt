package com.y.citycapsule.core.media

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

data class MediaStorageStatistics(
    val originalBytes: Long,
    val originalCount: Int,
    val thumbnailBytes: Long,
    val thumbnailCount: Int
)

sealed interface ThumbnailResult {
    data class Success(val path: String) : ThumbnailResult
    data class Failure(val message: String) : ThumbnailResult
    data object Unsupported : ThumbnailResult
}

sealed interface MediaMaintenanceResult {
    data class Success(val deletedBytes: Long, val deletedCount: Int) : MediaMaintenanceResult
    data class Failure(val message: String) : MediaMaintenanceResult
    data object Unsupported : MediaMaintenanceResult
}

interface MediaMaintenanceCapability {
    fun ensureThumbnail(originalPath: String, callback: (ThumbnailResult) -> Unit)
    fun storageStatistics(callback: (Result<MediaStorageStatistics>) -> Unit)
    fun clearThumbnails(callback: (MediaMaintenanceResult) -> Unit)
    fun cleanupUnreferenced(
        referencedOriginalPaths: Set<String>,
        gracePeriodMillis: Long,
        callback: (MediaMaintenanceResult) -> Unit
    )
}

class KuiklyMediaMaintenance(private val pager: Pager) : MediaMaintenanceCapability {
    override fun ensureThumbnail(originalPath: String, callback: (ThumbnailResult) -> Unit) {
        request(METHOD_THUMBNAIL, JSONObject().apply { put(FIELD_PATH, originalPath) }) { json ->
            when (json?.optString(FIELD_STATUS)) {
                STATUS_SUCCESS -> callback(ThumbnailResult.Success(json.optString(FIELD_PATH)))
                STATUS_UNSUPPORTED -> callback(ThumbnailResult.Unsupported)
                else -> callback(ThumbnailResult.Failure(json?.optString(FIELD_MESSAGE).orEmpty()))
            }
        }
    }

    override fun storageStatistics(callback: (Result<MediaStorageStatistics>) -> Unit) {
        request(METHOD_STATISTICS, JSONObject()) { json ->
            if (json?.optString(FIELD_STATUS) != STATUS_SUCCESS) {
                callback(Result.failure(IllegalStateException(json?.optString(FIELD_MESSAGE).orEmpty())))
            } else callback(Result.success(MediaStorageStatistics(
                json.optString(FIELD_ORIGINAL_BYTES).toLongOrNull() ?: 0,
                json.optString(FIELD_ORIGINAL_COUNT).toIntOrNull() ?: 0,
                json.optString(FIELD_THUMBNAIL_BYTES).toLongOrNull() ?: 0,
                json.optString(FIELD_THUMBNAIL_COUNT).toIntOrNull() ?: 0
            )))
        }
    }

    override fun clearThumbnails(callback: (MediaMaintenanceResult) -> Unit) =
        maintenance(METHOD_CLEAR_THUMBNAILS, JSONObject(), callback)

    override fun cleanupUnreferenced(
        referencedOriginalPaths: Set<String>,
        gracePeriodMillis: Long,
        callback: (MediaMaintenanceResult) -> Unit
    ) = maintenance(METHOD_CLEANUP_UNREFERENCED, JSONObject().apply {
        put(FIELD_PATHS, JSONArray().apply { referencedOriginalPaths.forEach(::put) })
        put(FIELD_GRACE_MILLIS, gracePeriodMillis.toString())
    }, callback)

    private fun maintenance(method: String, body: JSONObject, callback: (MediaMaintenanceResult) -> Unit) {
        request(method, body) { json ->
            when (json?.optString(FIELD_STATUS)) {
                STATUS_SUCCESS -> callback(MediaMaintenanceResult.Success(
                    json.optString(FIELD_DELETED_BYTES).toLongOrNull() ?: 0,
                    json.optString(FIELD_DELETED_COUNT).toIntOrNull() ?: 0
                ))
                STATUS_UNSUPPORTED -> callback(MediaMaintenanceResult.Unsupported)
                else -> callback(MediaMaintenanceResult.Failure(json?.optString(FIELD_MESSAGE).orEmpty()))
            }
        }
    }

    private fun request(method: String, body: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyMediaModule>(MODULE_NAME).request(method, body, callback)
    }

    companion object {
        const val MODULE_NAME = "CCMediaModule"
        const val METHOD_THUMBNAIL = "ensureThumbnail"
        const val METHOD_STATISTICS = "mediaStatistics"
        const val METHOD_CLEAR_THUMBNAILS = "clearThumbnails"
        const val METHOD_CLEANUP_UNREFERENCED = "cleanupUnreferenced"
        const val FIELD_STATUS = "status"
        const val FIELD_MESSAGE = "message"
        const val FIELD_PATH = "path"
        const val FIELD_PATHS = "paths"
        const val FIELD_GRACE_MILLIS = "gracePeriodMillis"
        const val FIELD_ORIGINAL_BYTES = "originalBytes"
        const val FIELD_ORIGINAL_COUNT = "originalCount"
        const val FIELD_THUMBNAIL_BYTES = "thumbnailBytes"
        const val FIELD_THUMBNAIL_COUNT = "thumbnailCount"
        const val FIELD_DELETED_BYTES = "deletedBytes"
        const val FIELD_DELETED_COUNT = "deletedCount"
        const val STATUS_SUCCESS = "success"
        const val STATUS_UNSUPPORTED = "unsupported"
    }
}
