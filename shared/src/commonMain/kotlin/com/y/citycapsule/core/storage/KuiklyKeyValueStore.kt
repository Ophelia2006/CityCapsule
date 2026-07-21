package com.y.citycapsule.core.storage

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

class KuiklyKeyValueStore internal constructor(
    private val transport: StorageTransport
) : KeyValueStore {

    constructor(pager: Pager) : this(PagerStorageTransport(pager))

    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        transport.execute(StorageProtocol.METHOD_GET, StorageProtocol.getRequest(key)) { response ->
            callback(response.toTypedResult(key))
        }
    }

    override fun <T> put(
        key: StorageKey<T>,
        value: T,
        callback: StorageCallback<Unit>
    ) {
        val request = try {
            StorageProtocol.putRequest(key, value)
        } catch (error: Throwable) {
            callback(
                StorageResult.Failure(
                    StorageError(
                        StorageErrorCode.INVALID_REQUEST,
                        error.message ?: "Failed to encode '${key.wireKey}'."
                    )
                )
            )
            return
        }
        transport.execute(StorageProtocol.METHOD_PUT, request) { response ->
            callback(response.toUnitResult())
        }
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        transport.execute(
            StorageProtocol.METHOD_REMOVE,
            StorageProtocol.removeRequest(key)
        ) { response -> callback(response.toUnitResult()) }
    }

    override fun contains(key: StorageKey<*>, callback: StorageCallback<Boolean>) {
        transport.execute(
            StorageProtocol.METHOD_CONTAINS,
            StorageProtocol.containsRequest(key)
        ) { response ->
            callback(
                when (response.code) {
                    StorageProtocol.CODE_SUCCESS -> StorageResult.Success(response.exists)
                    StorageProtocol.CODE_MISSING -> StorageResult.Success(false)
                    else -> response.toFailure()
                }
            )
        }
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        if (keys.isEmpty()) {
            callback(StorageResult.Success(StorageBatchResult(emptyList())))
            return
        }
        transport.execute(
            StorageProtocol.METHOD_GET_MANY,
            StorageProtocol.getManyRequest(keys)
        ) { response ->
            if (response.code != StorageProtocol.CODE_SUCCESS) {
                callback(response.toFailure())
                return@execute
            }
            val wireEntries = response.entries.associateBy(StorageWireEntry::key)
            val entries = keys.map { key ->
                val wireEntry = wireEntries[key.wireKey]
                StorageBatchEntry(
                    key = key,
                    result = if (wireEntry == null) {
                        StorageResult.Failure(
                            StorageError(
                                StorageErrorCode.NATIVE_FAILURE,
                                "Native batch response omitted '${key.wireKey}'."
                            )
                        )
                    } else {
                        wireEntry.toUntypedResult(key)
                    }
                )
            }
            callback(StorageResult.Success(StorageBatchResult(entries)))
        }
    }
}

internal interface StorageTransport {
    fun execute(
        method: String,
        request: JSONObject,
        callback: (StorageWireResponse) -> Unit
    )
}

internal class KuiklyStorageModule : Module() {
    override fun moduleName(): String = StorageProtocol.MODULE_NAME

    fun execute(method: String, request: JSONObject, callback: (JSONObject?) -> Unit) {
        asyncToNativeMethod(method, request, callback)
    }
}

private class PagerStorageTransport(
    private val pager: Pager
) : StorageTransport {
    override fun execute(
        method: String,
        request: JSONObject,
        callback: (StorageWireResponse) -> Unit
    ) {
        storageModule().execute(method, request) { response ->
            callback(StorageWireResponse.fromJson(response))
        }
    }

    private fun storageModule(): KuiklyStorageModule =
        pager.acquireModule(StorageProtocol.MODULE_NAME)
}

private fun <T> StorageWireResponse.toTypedResult(key: StorageKey<T>): StorageResult<T> =
    when (code) {
        StorageProtocol.CODE_MISSING -> StorageResult.Missing
        StorageProtocol.CODE_SUCCESS -> {
            if (!exists || value == null) {
                StorageResult.Missing
            } else {
                key.codec.decode(value)?.let { StorageResult.Success(it) }
                    ?: StorageResult.Failure(
                        StorageError(
                            StorageErrorCode.DECODE_FAILED,
                            "Native value for '${key.wireKey}' cannot be decoded."
                        )
                    )
            }
        }
        else -> toFailure()
    }

private fun StorageWireResponse.toUnitResult(): StorageResult<Unit> = when (code) {
    StorageProtocol.CODE_SUCCESS -> StorageResult.Success(Unit)
    else -> toFailure()
}

private fun StorageWireResponse.toFailure(): StorageResult.Failure = StorageResult.Failure(
    StorageError(
        code = StorageErrorCode.fromWireCode(code),
        message = message.ifBlank { "Native storage failed with code $code." }
    )
)

@Suppress("UNCHECKED_CAST")
private fun StorageWireEntry.toUntypedResult(key: StorageKey<*>): StorageResult<Any?> =
    when (code) {
        StorageProtocol.CODE_MISSING -> StorageResult.Missing
        StorageProtocol.CODE_SUCCESS -> {
            if (!exists || value == null) {
                StorageResult.Missing
            } else {
                val codec = key.codec as StorageCodec<Any?>
                codec.decode(value)?.let { StorageResult.Success(it) }
                    ?: StorageResult.Failure(
                        StorageError(
                            StorageErrorCode.DECODE_FAILED,
                            "Native value for '${key.wireKey}' cannot be decoded."
                        )
                    )
            }
        }
        else -> StorageResult.Failure(
            StorageError(
                StorageErrorCode.fromWireCode(code),
                message.ifBlank { "Native storage failed with code $code." }
            )
        )
    }
