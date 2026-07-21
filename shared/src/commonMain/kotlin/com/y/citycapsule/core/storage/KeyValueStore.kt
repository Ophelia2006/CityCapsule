package com.y.citycapsule.core.storage

interface KeyValueStore {
    fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>)

    fun <T> put(key: StorageKey<T>, value: T, callback: StorageCallback<Unit>)

    fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>)

    fun contains(key: StorageKey<*>, callback: StorageCallback<Boolean>)

    fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    )
}

/** In-memory implementation for shared tests, previews and repository development. */
class InMemoryKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<StorageAddress, StoredValue>()

    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        callback(read(key))
    }

    override fun <T> put(
        key: StorageKey<T>,
        value: T,
        callback: StorageCallback<Unit>
    ) {
        val encoded = try {
            key.codec.encode(value)
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
        values[key.address()] = StoredValue(key.codec.valueType, encoded)
        callback(StorageResult.Success(Unit))
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        values.remove(key.address())
        callback(StorageResult.Success(Unit))
    }

    override fun contains(key: StorageKey<*>, callback: StorageCallback<Boolean>) {
        callback(StorageResult.Success(values.containsKey(key.address())))
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        val entries = keys.map { key ->
            StorageBatchEntry(key, readUntyped(key))
        }
        callback(StorageResult.Success(StorageBatchResult(entries)))
    }

    internal fun seedRaw(
        key: StorageKey<*>,
        valueType: StorageValueType = key.codec.valueType,
        encodedValue: String
    ) {
        values[key.address()] = StoredValue(valueType, encodedValue)
    }

    private fun <T> read(key: StorageKey<T>): StorageResult<T> {
        val stored = values[key.address()] ?: return StorageResult.Missing
        if (stored.valueType != key.codec.valueType) {
            return StorageResult.Failure(
                StorageError(
                    StorageErrorCode.TYPE_MISMATCH,
                    "Stored type for '${key.wireKey}' does not match ${key.codec.valueType.wireValue}."
                )
            )
        }
        val decoded = key.codec.decode(stored.encodedValue)
            ?: return StorageResult.Failure(
                StorageError(
                    StorageErrorCode.DECODE_FAILED,
                    "Stored value for '${key.wireKey}' cannot be decoded."
                )
            )
        return StorageResult.Success(decoded)
    }

    @Suppress("UNCHECKED_CAST")
    private fun readUntyped(key: StorageKey<*>): StorageResult<Any?> =
        read(key as StorageKey<Any?>)
}

private data class StorageAddress(
    val store: StorageStore,
    val key: String
)

private data class StoredValue(
    val valueType: StorageValueType,
    val encodedValue: String
)

private fun StorageKey<*>.address(): StorageAddress = StorageAddress(store, wireKey)

