package com.y.citycapsule.core.storage

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StorageContractTest {
    @Test
    fun storeIdentifiersAreStable() {
        assertEquals("cc_meta", StorageStore.META.wireValue)
        assertEquals("cc_preferences", StorageStore.PREFERENCES.wireValue)
        assertEquals("cc_cache", StorageStore.CACHE.wireValue)
    }

    @Test
    fun registeredKeysAreUniqueAndNamespaced() {
        val addresses = AppStorageKeys.all.map { "${it.store.wireValue}:${it.wireKey}" }

        assertEquals(addresses.size, addresses.distinct().size)
        assertEquals("settings.theme_mode", AppStorageKeys.Settings.THEME_MODE.wireKey)
        assertEquals(StorageStore.PREFERENCES, AppStorageKeys.Settings.THEME_MODE.store)
    }

    @Test
    fun invalidKeySegmentsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            StorageKey(
                store = StorageStore.PREFERENCES,
                namespace = "Settings",
                name = "theme-mode",
                defaultValue = "system",
                codec = StorageCodecs.STRING
            )
        }
    }

    @Test
    fun primitiveCodecsUseCanonicalStrings() {
        assertEquals("true", StorageCodecs.BOOLEAN.encode(true))
        assertEquals(false, StorageCodecs.BOOLEAN.decode("false"))
        assertNull(StorageCodecs.BOOLEAN.decode("FALSE"))

        val valueBeyondArkTsSafeInteger = 9_007_199_254_740_993L
        assertEquals(
            valueBeyondArkTsSafeInteger,
            StorageCodecs.LONG.decode(StorageCodecs.LONG.encode(valueBeyondArkTsSafeInteger))
        )
        assertNull(StorageCodecs.LONG.decode("1.5"))
        assertNull(StorageCodecs.DOUBLE.decode("NaN"))
    }

    @Test
    fun jsonCodecRejectsNonObjectPayload() {
        val json = JSONObject().apply { put("schema", 1) }

        assertEquals(1, StorageCodecs.JSON_OBJECT.decode(json.toString())?.optInt("schema"))
        assertNull(StorageCodecs.JSON_OBJECT.decode("[]"))
    }
}

