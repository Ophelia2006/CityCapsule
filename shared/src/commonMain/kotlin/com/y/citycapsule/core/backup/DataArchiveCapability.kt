package com.y.citycapsule.core.backup

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

data class PlatformStorageUsage(
    val mediaBytes: Long,
    val cacheBytes: Long,
    val recoveryBytes: Long,
    val cacheCount: Int = 0,
    val recoveryCount: Int = 0
)

sealed interface ArchiveResult<out T> {
    data class Success<T>(val value: T) : ArchiveResult<T>
    data object Cancelled : ArchiveResult<Nothing>
    data class Failure(val message: String) : ArchiveResult<Nothing>
    data object Unsupported : ArchiveResult<Nothing>
}

data class ImportSelection(
    val sessionId: String,
    val payload: String,
    val fileName: String
)

data class ImportedMedia(
    val pathMapping: Map<String, String>,
    val createdPaths: List<String>
)

interface DataArchiveCapability {
    fun storageUsage(callback: (ArchiveResult<PlatformStorageUsage>) -> Unit)
    fun clearTemporaryFiles(callback: (ArchiveResult<Long>) -> Unit)
    fun export(
        payload: String,
        mediaPaths: List<String>,
        callback: (ArchiveResult<String>) -> Unit
    )
    fun selectImport(callback: (ArchiveResult<ImportSelection>) -> Unit)
    fun createRecovery(
        payload: String,
        mediaPaths: List<String>,
        callback: (ArchiveResult<String>) -> Unit
    )
    fun commitImportedMedia(
        sessionId: String,
        callback: (ArchiveResult<ImportedMedia>) -> Unit
    )
    fun discardImport(sessionId: String, callback: (ArchiveResult<Unit>) -> Unit)
}

class KuiklyDataArchiveCapability internal constructor(
    private val transport: DataArchiveTransport
) : DataArchiveCapability {
    constructor(pager: Pager) : this(PagerDataArchiveTransport(pager))

    override fun storageUsage(callback: (ArchiveResult<PlatformStorageUsage>) -> Unit) {
        transport.call(METHOD_USAGE, JSONObject(), callback.mapResponse { json ->
            PlatformStorageUsage(
                mediaBytes = json.optString(FIELD_MEDIA_BYTES).toLongOrNull() ?: 0,
                cacheBytes = json.optString(FIELD_CACHE_BYTES).toLongOrNull() ?: 0,
                recoveryBytes = json.optString(FIELD_RECOVERY_BYTES).toLongOrNull() ?: 0,
                cacheCount = json.optString(FIELD_CACHE_COUNT).toIntOrNull() ?: 0,
                recoveryCount = json.optString(FIELD_RECOVERY_COUNT).toIntOrNull() ?: 0
            )
        })
    }

    override fun clearTemporaryFiles(callback: (ArchiveResult<Long>) -> Unit) {
        transport.call(METHOD_CLEAR_TEMP, JSONObject(), callback.mapResponse { json ->
            json.optString(FIELD_CLEARED_BYTES).toLongOrNull() ?: 0
        })
    }

    override fun export(
        payload: String,
        mediaPaths: List<String>,
        callback: (ArchiveResult<String>) -> Unit
    ) {
        transport.call(
            METHOD_EXPORT,
            archiveRequest(payload, mediaPaths),
            callback.mapResponse { it.optString(FIELD_PATH) }
        )
    }

    override fun selectImport(callback: (ArchiveResult<ImportSelection>) -> Unit) {
        transport.call(METHOD_SELECT_IMPORT, JSONObject(), callback.mapResponse { json ->
            ImportSelection(
                sessionId = json.optString(FIELD_SESSION_ID),
                payload = json.optString(FIELD_PAYLOAD),
                fileName = json.optString(FIELD_FILE_NAME)
            )
        })
    }

    override fun createRecovery(
        payload: String,
        mediaPaths: List<String>,
        callback: (ArchiveResult<String>) -> Unit
    ) {
        transport.call(
            METHOD_CREATE_RECOVERY,
            archiveRequest(payload, mediaPaths),
            callback.mapResponse { it.optString(FIELD_PATH) }
        )
    }

    override fun commitImportedMedia(
        sessionId: String,
        callback: (ArchiveResult<ImportedMedia>) -> Unit
    ) {
        transport.call(
            METHOD_COMMIT_MEDIA,
            JSONObject().apply { put(FIELD_SESSION_ID, sessionId) },
            callback.mapResponse { json ->
                val mapping = linkedMapOf<String, String>()
                val mappingJson = json.optJSONObject(FIELD_PATH_MAPPING)
                mappingJson?.keys()?.forEach { oldPath ->
                    mapping[oldPath] = mappingJson.optString(oldPath)
                }
                val created = json.optJSONArray(FIELD_CREATED_PATHS).strings()
                ImportedMedia(mapping, created)
            }
        )
    }

    override fun discardImport(
        sessionId: String,
        callback: (ArchiveResult<Unit>) -> Unit
    ) {
        transport.call(
            METHOD_DISCARD_IMPORT,
            JSONObject().apply { put(FIELD_SESSION_ID, sessionId) },
            callback.mapResponse { Unit }
        )
    }

    private fun archiveRequest(payload: String, paths: List<String>) = JSONObject().apply {
        put(FIELD_PAYLOAD, payload)
        put(FIELD_MEDIA_PATHS, JSONArray().apply { paths.distinct().forEach(::put) })
    }

    private fun <T> ((ArchiveResult<T>) -> Unit).mapResponse(
        decode: (JSONObject) -> T
    ): (JSONObject?) -> Unit = response@ { json ->
        if (json == null) {
            this(ArchiveResult.Failure("本地文件能力没有返回结果。"))
            return@response
        }
        when (json.optString(FIELD_STATUS)) {
            STATUS_SUCCESS -> runCatching { decode(json) }.fold(
                onSuccess = { this(ArchiveResult.Success(it)) },
                onFailure = { this(ArchiveResult.Failure("本地文件结果无法解析。")) }
            )
            STATUS_CANCELLED -> this(ArchiveResult.Cancelled)
            STATUS_UNSUPPORTED -> this(ArchiveResult.Unsupported)
            else -> this(
                ArchiveResult.Failure(
                    json.optString(FIELD_MESSAGE).ifBlank { "本地文件操作失败。" }
                )
            )
        }
    }

    companion object {
        const val MODULE_NAME = "CCDataArchiveModule"
        const val METHOD_USAGE = "storageUsage"
        const val METHOD_CLEAR_TEMP = "clearTemporaryFiles"
        const val METHOD_EXPORT = "exportArchive"
        const val METHOD_SELECT_IMPORT = "selectImport"
        const val METHOD_CREATE_RECOVERY = "createRecovery"
        const val METHOD_COMMIT_MEDIA = "commitImportedMedia"
        const val METHOD_DISCARD_IMPORT = "discardImport"
        const val FIELD_STATUS = "status"
        const val FIELD_MESSAGE = "message"
        const val FIELD_PAYLOAD = "payload"
        const val FIELD_MEDIA_PATHS = "mediaPaths"
        const val FIELD_MEDIA_BYTES = "mediaBytes"
        const val FIELD_CACHE_BYTES = "cacheBytes"
        const val FIELD_RECOVERY_BYTES = "recoveryBytes"
        const val FIELD_CACHE_COUNT = "cacheCount"
        const val FIELD_RECOVERY_COUNT = "recoveryCount"
        const val FIELD_CLEARED_BYTES = "clearedBytes"
        const val FIELD_PATH = "path"
        const val FIELD_SESSION_ID = "sessionId"
        const val FIELD_FILE_NAME = "fileName"
        const val FIELD_PATH_MAPPING = "pathMapping"
        const val FIELD_CREATED_PATHS = "createdPaths"
        const val STATUS_SUCCESS = "success"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_FAILURE = "failure"
        const val STATUS_UNSUPPORTED = "unsupported"
    }
}

internal fun interface DataArchiveTransport {
    fun call(method: String, request: JSONObject, callback: (JSONObject?) -> Unit)
}

internal class KuiklyDataArchiveModule : Module() {
    override fun moduleName(): String = KuiklyDataArchiveCapability.MODULE_NAME
    fun request(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(method, request, callback)
    }
}

private class PagerDataArchiveTransport(private val pager: Pager) : DataArchiveTransport {
    override fun call(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        pager.acquireModule<KuiklyDataArchiveModule>(KuiklyDataArchiveCapability.MODULE_NAME)
            .request(method, request, callback)
    }
}

private fun JSONArray?.strings(): List<String> = buildList {
    val array = this@strings ?: return@buildList
    for (index in 0 until array.length()) {
        array.optString(index)?.takeIf(String::isNotBlank)?.let(::add)
    }
}
