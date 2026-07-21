package com.y.citycapsule.core.storage

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class KuiklyKeyValueStoreTest {
    private val key = AppStorageKeys.Settings.THEME_MODE

    @Test
    fun getWritesFrozenEnvelopeAndDecodesResponse() {
        val transport = FakeStorageTransport(
            response = StorageWireResponse(
                code = StorageProtocol.CODE_SUCCESS,
                message = "",
                exists = true,
                value = "dark",
                entries = emptyList()
            )
        )
        val store = KuiklyKeyValueStore(transport)
        var result: StorageResult<ThemeMode>? = null

        store.get(key) { result = it }

        assertEquals(StorageProtocol.METHOD_GET, transport.method)
        val request = assertNotNull(transport.request)
        assertEquals(StorageProtocol.VERSION, request.optInt(StorageProtocol.FIELD_PROTOCOL_VERSION))
        assertEquals("cc_preferences", request.optString(StorageProtocol.FIELD_STORE))
        assertEquals("settings.theme_mode", request.optString(StorageProtocol.FIELD_KEY))
        assertEquals("string", request.optString(StorageProtocol.FIELD_TYPE))
        assertEquals(ThemeMode.DARK, assertIs<StorageResult.Success<ThemeMode>>(result).value)
    }

    @Test
    fun putEncodesTypedValueBeforeCallingNative() {
        val transport = FakeStorageTransport(successResponse())
        val store = KuiklyKeyValueStore(transport)
        var result: StorageResult<Unit>? = null

        store.put(key, ThemeMode.LIGHT) { result = it }

        assertEquals(StorageProtocol.METHOD_PUT, transport.method)
        assertEquals("light", transport.request?.optString(StorageProtocol.FIELD_VALUE))
        assertIs<StorageResult.Success<Unit>>(result)
    }

    @Test
    fun nativeErrorMapsToSharedErrorCode() {
        val transport = FakeStorageTransport(
            StorageWireResponse(
                code = StorageErrorCode.NOT_INITIALIZED.wireCode,
                message = "storage is not ready",
                exists = false,
                value = null,
                entries = emptyList()
            )
        )
        val store = KuiklyKeyValueStore(transport)
        var result: StorageResult<ThemeMode>? = null

        store.get(key) { result = it }

        val failure = assertIs<StorageResult.Failure>(result)
        assertEquals(StorageErrorCode.NOT_INITIALIZED, failure.error.code)
        assertEquals("storage is not ready", failure.error.message)
    }

    @Test
    fun batchResponseIsMatchedByStableWireKey() {
        val transport = FakeStorageTransport(
            StorageWireResponse(
                code = StorageProtocol.CODE_SUCCESS,
                message = "",
                exists = false,
                value = null,
                entries = listOf(
                    StorageWireEntry(
                        key = key.wireKey,
                        code = StorageProtocol.CODE_SUCCESS,
                        message = "",
                        exists = true,
                        value = "system"
                    )
                )
            )
        )
        val store = KuiklyKeyValueStore(transport)
        var result: StorageResult<StorageBatchResult>? = null

        store.getMany(listOf(key)) { result = it }

        val batch = assertIs<StorageResult.Success<StorageBatchResult>>(result).value
        assertEquals(
            ThemeMode.SYSTEM,
            assertIs<StorageResult.Success<Any?>>(batch.entries.single().result).value
        )
    }
}

private class FakeStorageTransport(
    var response: StorageWireResponse
) : StorageTransport {
    var method: String? = null
    var request: JSONObject? = null

    override fun execute(
        method: String,
        request: JSONObject,
        callback: (StorageWireResponse) -> Unit
    ) {
        this.method = method
        this.request = request
        callback(response)
    }
}

private fun successResponse(): StorageWireResponse = StorageWireResponse(
    code = StorageProtocol.CODE_SUCCESS,
    message = "",
    exists = false,
    value = null,
    entries = emptyList()
)
