package com.y.citycapsule.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InMemoryKeyValueStoreTest {
    private val key = AppStorageKeys.Settings.THEME_MODE

    @Test
    fun missingPutReadContainsAndRemoveFollowContract() {
        val store = InMemoryKeyValueStore()

        assertIs<StorageResult.Missing>(store.getNow(key))
        assertEquals(false, store.containsNow(key).successValue())

        assertIs<StorageResult.Success<Unit>>(store.putNow(key, ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, store.getNow(key).successValue())
        assertEquals(true, store.containsNow(key).successValue())

        assertIs<StorageResult.Success<Unit>>(store.removeNow(key))
        assertIs<StorageResult.Missing>(store.getNow(key))
    }

    @Test
    fun wrongStoredTypeReturnsTypeMismatch() {
        val store = InMemoryKeyValueStore().apply {
            seedRaw(key, StorageValueType.BOOLEAN, "true")
        }

        val failure = assertIs<StorageResult.Failure>(store.getNow(key))

        assertEquals(StorageErrorCode.TYPE_MISMATCH, failure.error.code)
    }

    @Test
    fun malformedStoredValueReturnsDecodeFailure() {
        val store = InMemoryKeyValueStore().apply {
            seedRaw(key, encodedValue = "sepia")
        }

        val failure = assertIs<StorageResult.Failure>(store.getNow(key))

        assertEquals(StorageErrorCode.DECODE_FAILED, failure.error.code)
    }

    @Test
    fun batchReadPreservesInputOrderAndPerKeyResult() {
        val store = InMemoryKeyValueStore()
        store.putNow(key, ThemeMode.LIGHT)
        val missingKey = StorageKey(
            store = StorageStore.CACHE,
            namespace = "diagnostics",
            name = "probe",
            defaultValue = "",
            codec = StorageCodecs.STRING
        )

        val batch = store.getManyNow(listOf(key, missingKey)).successValue()

        assertEquals(listOf(key, missingKey), batch.entries.map(StorageBatchEntry::key))
        assertEquals(ThemeMode.LIGHT, batch.entries[0].result.successValue())
        assertIs<StorageResult.Missing>(batch.entries[1].result)
    }
}

private fun <T> KeyValueStore.getNow(key: StorageKey<T>): StorageResult<T> {
    var captured: StorageResult<T>? = null
    get(key) { captured = it }
    return requireNotNull(captured)
}

private fun <T> KeyValueStore.putNow(key: StorageKey<T>, value: T): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    put(key, value) { captured = it }
    return requireNotNull(captured)
}

private fun KeyValueStore.removeNow(key: StorageKey<*>): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    remove(key) { captured = it }
    return requireNotNull(captured)
}

private fun KeyValueStore.containsNow(key: StorageKey<*>): StorageResult<Boolean> {
    var captured: StorageResult<Boolean>? = null
    contains(key) { captured = it }
    return requireNotNull(captured)
}

private fun KeyValueStore.getManyNow(
    keys: List<StorageKey<*>>
): StorageResult<StorageBatchResult> {
    var captured: StorageResult<StorageBatchResult>? = null
    getMany(keys) { captured = it }
    return requireNotNull(captured)
}

private fun <T> StorageResult<T>.successValue(): T =
    assertIs<StorageResult.Success<T>>(this).value

