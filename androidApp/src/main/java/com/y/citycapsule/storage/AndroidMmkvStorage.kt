package com.y.citycapsule.storage

import android.content.Context
import android.util.Log
import com.tencent.mmkv.MMKV
import com.y.citycapsule.core.storage.StorageStore

internal interface AndroidStringStore {
    fun contains(key: String): Boolean

    fun read(key: String): String?

    fun write(key: String, value: String): Boolean

    fun remove(key: String)
}

internal interface AndroidStorageProvider {
    val isReady: Boolean

    fun store(storeId: String): AndroidStringStore?
}

internal data class AndroidStorageInitialization(
    val success: Boolean,
    val rootPath: String? = null,
    val message: String? = null
)

internal object AndroidMmkvStorage : AndroidStorageProvider {
    private const val LOG_TAG = "CityCapsuleStorage"

    private val stores = mutableMapOf<String, AndroidStringStore>()

    @Volatile
    override var isReady: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    @Synchronized
    fun initialize(context: Context): AndroidStorageInitialization {
        if (isReady) {
            return AndroidStorageInitialization(
                success = true,
                rootPath = MMKV.getRootDir()
            )
        }
        return try {
            val rootPath = MMKV.initialize(context.applicationContext)
            val openedStores = StorageStore.entries.associate { store ->
                store.wireValue to AndroidMmkvStringStore(
                    MMKV.mmkvWithID(store.wireValue, MMKV.SINGLE_PROCESS_MODE)
                )
            }
            stores.clear()
            stores.putAll(openedStores)
            lastError = null
            isReady = true
            Log.i(LOG_TAG, "MMKV initialized at $rootPath")
            AndroidStorageInitialization(success = true, rootPath = rootPath)
        } catch (error: Throwable) {
            stores.clear()
            isReady = false
            lastError = error.message ?: error::class.java.simpleName
            Log.e(LOG_TAG, "MMKV initialization failed: $lastError")
            AndroidStorageInitialization(success = false, message = lastError)
        }
    }

    override fun store(storeId: String): AndroidStringStore? = stores[storeId]
}

private class AndroidMmkvStringStore(
    private val mmkv: MMKV
) : AndroidStringStore {
    override fun contains(key: String): Boolean = mmkv.containsKey(key)

    override fun read(key: String): String? = mmkv.decodeString(key)

    override fun write(key: String, value: String): Boolean = mmkv.encode(key, value)

    override fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }
}

