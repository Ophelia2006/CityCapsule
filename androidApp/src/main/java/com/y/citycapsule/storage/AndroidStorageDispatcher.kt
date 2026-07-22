package com.y.citycapsule.storage

import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageProtocol
import com.y.citycapsule.core.storage.StorageStore
import com.y.citycapsule.core.storage.StorageValueType
import org.json.JSONArray
import org.json.JSONObject

internal class AndroidStorageDispatcher(
    private val provider: AndroidStorageProvider = AndroidMmkvStorage
) {
    fun dispatch(method: String, params: String?): JSONObject {
        val request = try {
            params?.let(::JSONObject)
        } catch (_: Throwable) {
            null
        } ?: return failure(
            StorageErrorCode.INVALID_REQUEST,
            "Storage request body is invalid."
        )
        return try {
        when (method) {
            StorageProtocol.METHOD_GET -> get(request)
            StorageProtocol.METHOD_PUT -> put(request)
            StorageProtocol.METHOD_REMOVE -> remove(request)
            StorageProtocol.METHOD_CONTAINS -> contains(request)
            StorageProtocol.METHOD_GET_MANY -> getMany(request)
            else -> failure(
                StorageErrorCode.UNKNOWN_METHOD,
                "Unknown storage method '$method'."
            )
            }
        } catch (_: Throwable) {
            failure(
                StorageErrorCode.NATIVE_FAILURE,
                "Android storage operation failed."
            )
        }
    }

    private fun get(request: JSONObject): JSONObject {
        val decoded = decodeSingle(request) ?: return invalidRequest()
        val store = readyStore(decoded.store.wireValue) ?: return notInitialized()
        return read(store, decoded)
    }

    private fun put(request: JSONObject): JSONObject {
        val decoded = decodeSingle(request, requireValue = true) ?: return invalidRequest()
        val store = readyStore(decoded.store.wireValue) ?: return notInitialized()
        val value = decoded.value ?: return invalidRequest()
        val typeKey = typeKey(decoded.key)
        val typeWritten = store.write(typeKey, decoded.valueType.wireValue)
        val valueWritten = typeWritten && store.write(decoded.key, value)
        if (!valueWritten) {
            if (typeWritten) {
                store.remove(typeKey)
            }
            return failure(
                StorageErrorCode.NATIVE_FAILURE,
                "Failed to write '${decoded.key}'."
            )
        }
        return success(exists = true)
    }

    private fun remove(request: JSONObject): JSONObject {
        val decoded = decodeSingle(request) ?: return invalidRequest()
        val store = readyStore(decoded.store.wireValue) ?: return notInitialized()
        store.remove(decoded.key)
        store.remove(typeKey(decoded.key))
        return success(exists = false)
    }

    private fun contains(request: JSONObject): JSONObject {
        val decoded = decodeSingle(request) ?: return invalidRequest()
        val store = readyStore(decoded.store.wireValue) ?: return notInitialized()
        return success(exists = store.contains(decoded.key))
    }

    private fun getMany(request: JSONObject): JSONObject {
        if (!hasSupportedVersion(request)) {
            return invalidRequest()
        }
        val requests = request.optJSONArray(StorageProtocol.FIELD_ENTRIES)
            ?: return invalidRequest()
        val entries = JSONArray()
        for (index in 0 until requests.length()) {
            val itemRequest = requests.optJSONObject(index)
            if (itemRequest == null) {
                entries.put(
                    failure(
                        StorageErrorCode.INVALID_REQUEST,
                        "Batch entry at index $index is invalid."
                    )
                )
                continue
            }
            val itemResponse = get(itemRequest)
            itemResponse.put(
                StorageProtocol.FIELD_KEY,
                itemRequest.optString(StorageProtocol.FIELD_KEY)
            )
            entries.put(itemResponse)
        }
        return success(exists = false).put(StorageProtocol.FIELD_ENTRIES, entries)
    }

    private fun read(store: AndroidStringStore, request: AndroidStorageRequest): JSONObject {
        if (!store.contains(request.key)) {
            return JSONObject()
                .put(StorageProtocol.FIELD_CODE, StorageProtocol.CODE_MISSING)
                .put(StorageProtocol.FIELD_MESSAGE, "")
                .put(StorageProtocol.FIELD_EXISTS, false)
        }
        val storedType = store.read(typeKey(request.key))
        if (storedType != null && storedType != request.valueType.wireValue) {
            return failure(
                StorageErrorCode.TYPE_MISMATCH,
                "Stored type for '${request.key}' is '$storedType'."
            ).put(StorageProtocol.FIELD_EXISTS, true)
        }
        val value = store.read(request.key)
            ?: return failure(
                StorageErrorCode.NATIVE_FAILURE,
                "Stored value for '${request.key}' is unavailable."
            ).put(StorageProtocol.FIELD_EXISTS, true)
        return success(exists = true).put(StorageProtocol.FIELD_VALUE, value)
    }

    private fun decodeSingle(
        json: JSONObject,
        requireValue: Boolean = false
    ): AndroidStorageRequest? {
        if (!hasSupportedVersion(json)) {
            return null
        }
        val store = StorageStore.fromWireValue(json.optString(StorageProtocol.FIELD_STORE))
            ?: return null
        val key = json.optString(StorageProtocol.FIELD_KEY)
        if (!WIRE_KEY.matches(key)) {
            return null
        }
        val valueType = StorageValueType.fromWireValue(
            json.optString(StorageProtocol.FIELD_TYPE)
        ) ?: return null
        val value = if (json.has(StorageProtocol.FIELD_VALUE)) {
            json.optString(StorageProtocol.FIELD_VALUE)
        } else {
            null
        }
        if (requireValue && value == null) {
            return null
        }
        return AndroidStorageRequest(store, key, valueType, value)
    }

    private fun hasSupportedVersion(json: JSONObject): Boolean =
        json.optInt(StorageProtocol.FIELD_PROTOCOL_VERSION, -1) == StorageProtocol.VERSION

    private fun readyStore(storeId: String): AndroidStringStore? {
        if (!provider.isReady) {
            return null
        }
        return provider.store(storeId)
    }

    private fun success(exists: Boolean): JSONObject = JSONObject()
        .put(StorageProtocol.FIELD_CODE, StorageProtocol.CODE_SUCCESS)
        .put(StorageProtocol.FIELD_MESSAGE, "")
        .put(StorageProtocol.FIELD_EXISTS, exists)

    private fun invalidRequest(): JSONObject = failure(
        StorageErrorCode.INVALID_REQUEST,
        "Storage request does not match protocol v${StorageProtocol.VERSION}."
    )

    private fun notInitialized(): JSONObject = failure(
        StorageErrorCode.NOT_INITIALIZED,
        "Android MMKV storage is not initialized."
    )

    private fun failure(code: StorageErrorCode, message: String): JSONObject = JSONObject()
        .put(StorageProtocol.FIELD_CODE, code.wireCode)
        .put(StorageProtocol.FIELD_MESSAGE, message)
        .put(StorageProtocol.FIELD_EXISTS, false)

    private fun typeKey(key: String): String = "$TYPE_KEY_PREFIX$key"

    private companion object {
        const val TYPE_KEY_PREFIX = "__cc_type__."
        val WIRE_KEY = Regex("[a-z][a-z0-9_]*\\.[a-z][a-z0-9_]*")
    }
}

private data class AndroidStorageRequest(
    val store: StorageStore,
    val key: String,
    val valueType: StorageValueType,
    val value: String?
)
