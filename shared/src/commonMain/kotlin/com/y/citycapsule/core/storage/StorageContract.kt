package com.y.citycapsule.core.storage

/** Stable MMKV instance identifiers shared by Android and HarmonyOS. */
enum class StorageStore(val wireValue: String) {
    META("cc_meta"),
    PREFERENCES("cc_preferences"),
    CACHE("cc_cache");

    companion object {
        fun fromWireValue(value: String): StorageStore? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}

/** Value types supported by storage protocol v1. Values cross the bridge as canonical strings. */
enum class StorageValueType(val wireValue: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    LONG("long"),
    DOUBLE("double"),
    JSON_OBJECT("json_object");

    companion object {
        fun fromWireValue(value: String): StorageValueType? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}

interface StorageCodec<T> {
    val valueType: StorageValueType

    fun encode(value: T): String

    /** Returns null when [encoded] is not valid for this codec. */
    fun decode(encoded: String): T?
}

class StorageKey<T>(
    val store: StorageStore,
    val namespace: String,
    val name: String,
    val defaultValue: T,
    val codec: StorageCodec<T>
) {
    val wireKey: String = "$namespace.$name"

    init {
        require(KEY_SEGMENT.matches(namespace)) {
            "Storage key namespace '$namespace' must use lower_snake_case."
        }
        require(KEY_SEGMENT.matches(name)) {
            "Storage key name '$name' must use lower_snake_case."
        }
    }

    override fun toString(): String = "${store.wireValue}:$wireKey"

    private companion object {
        val KEY_SEGMENT = Regex("[a-z][a-z0-9_]*")
    }
}

enum class StorageErrorCode(val wireCode: Int) {
    INVALID_REQUEST(1001),
    NOT_INITIALIZED(1002),
    TYPE_MISMATCH(1003),
    DECODE_FAILED(1004),
    NATIVE_FAILURE(1005),
    UNKNOWN_METHOD(1006),
    UNKNOWN(1999);

    companion object {
        fun fromWireCode(code: Int): StorageErrorCode = entries.firstOrNull {
            it.wireCode == code
        } ?: UNKNOWN
    }
}

data class StorageError(
    val code: StorageErrorCode,
    val message: String
)

sealed class StorageResult<out T> {
    data class Success<T>(val value: T) : StorageResult<T>()

    data object Missing : StorageResult<Nothing>()

    data class Failure(val error: StorageError) : StorageResult<Nothing>()
}

data class StorageBatchEntry(
    val key: StorageKey<*>,
    val result: StorageResult<Any?>
)

data class StorageBatchResult(
    val entries: List<StorageBatchEntry>
) {
    fun find(wireKey: String): StorageBatchEntry? = entries.firstOrNull {
        it.key.wireKey == wireKey
    }
}

typealias StorageCallback<T> = (StorageResult<T>) -> Unit

