package com.y.citycapsule.core.storage

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/** Wire-level constants mirrored by AndroidStorageModule and HarmonyStorageModule. */
object StorageProtocol {
    const val VERSION = 1
    const val MODULE_NAME = "CCStorageModule"

    const val METHOD_GET = "storageGet"
    const val METHOD_PUT = "storagePut"
    const val METHOD_REMOVE = "storageRemove"
    const val METHOD_CONTAINS = "storageContains"
    const val METHOD_GET_MANY = "storageGetMany"

    const val FIELD_PROTOCOL_VERSION = "protocolVersion"
    const val FIELD_STORE = "store"
    const val FIELD_KEY = "key"
    const val FIELD_TYPE = "type"
    const val FIELD_VALUE = "value"
    const val FIELD_ENTRIES = "entries"
    const val FIELD_CODE = "code"
    const val FIELD_MESSAGE = "message"
    const val FIELD_EXISTS = "exists"

    const val CODE_SUCCESS = 0
    const val CODE_MISSING = 1

    fun getRequest(key: StorageKey<*>): JSONObject = baseRequest(key)

    fun <T> putRequest(key: StorageKey<T>, value: T): JSONObject = baseRequest(key).apply {
        put(FIELD_VALUE, key.codec.encode(value))
    }

    fun removeRequest(key: StorageKey<*>): JSONObject = baseRequest(key)

    fun containsRequest(key: StorageKey<*>): JSONObject = baseRequest(key)

    fun getManyRequest(keys: List<StorageKey<*>>): JSONObject = JSONObject().apply {
        put(FIELD_PROTOCOL_VERSION, VERSION)
        put(
            FIELD_ENTRIES,
            JSONArray().also { entries ->
                keys.forEach { key -> entries.put(baseRequest(key)) }
            }
        )
    }

    private fun baseRequest(key: StorageKey<*>): JSONObject = JSONObject().apply {
        put(FIELD_PROTOCOL_VERSION, VERSION)
        put(FIELD_STORE, key.store.wireValue)
        put(FIELD_KEY, key.wireKey)
        put(FIELD_TYPE, key.codec.valueType.wireValue)
    }
}

internal data class StorageWireResponse(
    val code: Int,
    val message: String,
    val exists: Boolean,
    val value: String?,
    val entries: List<StorageWireEntry>
) {
    companion object {
        fun fromJson(json: JSONObject?): StorageWireResponse {
            if (json == null) {
                return StorageWireResponse(
                    code = StorageErrorCode.NATIVE_FAILURE.wireCode,
                    message = "Native storage returned no response.",
                    exists = false,
                    value = null,
                    entries = emptyList()
                )
            }
            val entriesJson = json.optJSONArray(StorageProtocol.FIELD_ENTRIES)
            val entries = buildList {
                if (entriesJson != null) {
                    for (index in 0 until entriesJson.length()) {
                        entriesJson.optJSONObject(index)?.let { add(StorageWireEntry.fromJson(it)) }
                    }
                }
            }
            return StorageWireResponse(
                code = json.optInt(
                    StorageProtocol.FIELD_CODE,
                    StorageErrorCode.UNKNOWN.wireCode
                ),
                message = json.optString(StorageProtocol.FIELD_MESSAGE),
                exists = json.optBoolean(StorageProtocol.FIELD_EXISTS),
                value = if (json.has(StorageProtocol.FIELD_VALUE)) {
                    json.optString(StorageProtocol.FIELD_VALUE)
                } else {
                    null
                },
                entries = entries
            )
        }
    }
}

internal data class StorageWireEntry(
    val key: String,
    val code: Int,
    val message: String,
    val exists: Boolean,
    val value: String?
) {
    companion object {
        fun fromJson(json: JSONObject): StorageWireEntry = StorageWireEntry(
            key = json.optString(StorageProtocol.FIELD_KEY),
            code = json.optInt(
                StorageProtocol.FIELD_CODE,
                StorageErrorCode.UNKNOWN.wireCode
            ),
            message = json.optString(StorageProtocol.FIELD_MESSAGE),
            exists = json.optBoolean(StorageProtocol.FIELD_EXISTS),
            value = if (json.has(StorageProtocol.FIELD_VALUE)) {
                json.optString(StorageProtocol.FIELD_VALUE)
            } else {
                null
            }
        )
    }
}

