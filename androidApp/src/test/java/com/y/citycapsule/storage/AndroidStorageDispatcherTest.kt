package com.y.citycapsule.storage

import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageProtocol
import com.y.citycapsule.core.storage.StorageStore
import com.y.citycapsule.core.storage.StorageValueType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidStorageDispatcherTest {
    private val provider = FakeAndroidStorageProvider()
    private val dispatcher = AndroidStorageDispatcher(provider)

    @Test
    fun putThenGetPersistsCanonicalString() {
        val put = dispatcher.dispatch(
            StorageProtocol.METHOD_PUT,
            request(value = "dark").toString()
        )
        val get = dispatcher.dispatch(StorageProtocol.METHOD_GET, request().toString())

        assertEquals(StorageProtocol.CODE_SUCCESS, put.optInt(StorageProtocol.FIELD_CODE))
        assertEquals(StorageProtocol.CODE_SUCCESS, get.optInt(StorageProtocol.FIELD_CODE))
        assertEquals(true, get.optBoolean(StorageProtocol.FIELD_EXISTS))
        assertEquals("dark", get.optString(StorageProtocol.FIELD_VALUE))
    }

    @Test
    fun typeChangeIsRejected() {
        dispatcher.dispatch(StorageProtocol.METHOD_PUT, request(value = "dark").toString())

        val response = dispatcher.dispatch(
            StorageProtocol.METHOD_GET,
            request(type = StorageValueType.BOOLEAN, value = null).toString()
        )

        assertEquals(StorageErrorCode.TYPE_MISMATCH.wireCode, response.optInt(StorageProtocol.FIELD_CODE))
    }

    @Test
    fun batchReturnsSuccessAndMissingPerKey() {
        dispatcher.dispatch(StorageProtocol.METHOD_PUT, request(value = "light").toString())
        val batchRequest = JSONObject()
            .put(StorageProtocol.FIELD_PROTOCOL_VERSION, StorageProtocol.VERSION)
            .put(
                StorageProtocol.FIELD_ENTRIES,
                JSONArray()
                    .put(request())
                    .put(request(key = "diagnostics.missing"))
            )

        val response = dispatcher.dispatch(
            StorageProtocol.METHOD_GET_MANY,
            batchRequest.toString()
        )
        val entries = response.getJSONArray(StorageProtocol.FIELD_ENTRIES)

        assertEquals(StorageProtocol.CODE_SUCCESS, entries.getJSONObject(0).getInt(StorageProtocol.FIELD_CODE))
        assertEquals(StorageProtocol.CODE_MISSING, entries.getJSONObject(1).getInt(StorageProtocol.FIELD_CODE))
    }

    @Test
    fun unavailableProviderReturnsNotInitialized() {
        provider.ready = false

        val response = dispatcher.dispatch(StorageProtocol.METHOD_GET, request().toString())

        assertEquals(
            StorageErrorCode.NOT_INITIALIZED.wireCode,
            response.optInt(StorageProtocol.FIELD_CODE)
        )
    }

    @Test
    fun malformedJsonReturnsInvalidRequest() {
        val response = dispatcher.dispatch(StorageProtocol.METHOD_GET, "{not-json")

        assertEquals(
            StorageErrorCode.INVALID_REQUEST.wireCode,
            response.optInt(StorageProtocol.FIELD_CODE)
        )
    }

    private fun request(
        key: String = "settings.theme_mode",
        type: StorageValueType = StorageValueType.STRING,
        value: String? = null
    ): JSONObject = JSONObject()
        .put(StorageProtocol.FIELD_PROTOCOL_VERSION, StorageProtocol.VERSION)
        .put(StorageProtocol.FIELD_STORE, StorageStore.PREFERENCES.wireValue)
        .put(StorageProtocol.FIELD_KEY, key)
        .put(StorageProtocol.FIELD_TYPE, type.wireValue)
        .also { json -> value?.let { json.put(StorageProtocol.FIELD_VALUE, it) } }
}

private class FakeAndroidStorageProvider : AndroidStorageProvider {
    var ready: Boolean = true
    private val stores = StorageStore.entries.associate { store ->
        store.wireValue to FakeAndroidStringStore()
    }

    override val isReady: Boolean
        get() = ready

    override fun store(storeId: String): AndroidStringStore? = stores[storeId]
}

private class FakeAndroidStringStore : AndroidStringStore {
    private val values = mutableMapOf<String, String>()

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String): Boolean {
        values[key] = value
        return true
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
