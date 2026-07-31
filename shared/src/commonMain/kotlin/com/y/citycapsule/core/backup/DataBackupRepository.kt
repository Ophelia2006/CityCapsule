package com.y.citycapsule.core.backup

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.capsule.CapsuleCatalog
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageBatchResult
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult

data class BackupPreview(
    val sessionId: String,
    val fileName: String,
    val profileCount: Int,
    val placeCount: Int,
    val favoriteCount: Int,
    val capsuleCount: Int,
    val photoCount: Int,
    internal val entries: List<BackupEntry>
)

data class LocalStorageSnapshot(
    val payload: String,
    val structuredBytesApprox: Long,
    val mediaPaths: List<String>,
    val profileCount: Int,
    val placeCount: Int,
    val favoriteCount: Int,
    val capsuleCount: Int
)

data class BackupEntry(
    val key: StorageKey<*>,
    val exists: Boolean,
    val encodedValue: String?
)

sealed interface BackupDataResult<out T> {
    data class Success<T>(val value: T) : BackupDataResult<T>
    data class Failure(val message: String) : BackupDataResult<Nothing>
}

class DataBackupRepository(private val storage: KeyValueStore) {
    fun snapshot(callback: (BackupDataResult<LocalStorageSnapshot>) -> Unit) {
        storage.getMany(PERSISTENT_KEYS) { result ->
            if (result !is StorageResult.Success) {
                callback(BackupDataResult.Failure("无法完整读取本地数据，未创建备份。"))
                return@getMany
            }
            callback(buildSnapshot(result.value))
        }
    }

    fun preview(
        selection: ImportSelection,
        callback: (BackupDataResult<BackupPreview>) -> Unit
    ) {
        callback(decodePreview(selection))
    }

    fun restore(
        preview: BackupPreview,
        pathMapping: Map<String, String>,
        callback: (BackupDataResult<Unit>) -> Unit
    ) {
        val rewritten = preview.entries.map { entry ->
            if (entry.key == AppStorageKeys.Capsules.CATALOG && entry.exists) {
                val catalog = entry.encodedValue
                    ?.let(AppStorageKeys.Capsules.CATALOG.codec::decode)
                    ?: return callback(BackupDataResult.Failure("城市碎片数据无法解码。"))
                val updated = catalog.copy(
                    capsules = catalog.capsules.map { capsule ->
                        capsule.copy(
                            imagePaths = capsule.imagePaths.mapNotNull(pathMapping::get)
                        )
                    }
                )
                entry.copy(
                    encodedValue = AppStorageKeys.Capsules.CATALOG.codec.encode(updated)
                )
            } else {
                entry
            }
        }
        writeEntries(rewritten, 0, callback)
    }

    fun clearDrafts(callback: (BackupDataResult<Unit>) -> Unit) {
        storage.remove(AppStorageKeys.Onboarding.DRAFT) { first ->
            if (first is StorageResult.Failure) {
                callback(BackupDataResult.Failure("首次引导草稿未能清理。"))
                return@remove
            }
            storage.remove(AppStorageKeys.Capsules.DRAFT) { second ->
                if (second is StorageResult.Failure) {
                    callback(BackupDataResult.Failure("城市碎片草稿未能清理。"))
                } else {
                    callback(BackupDataResult.Success(Unit))
                }
            }
        }
    }

    private fun buildSnapshot(batch: StorageBatchResult): BackupDataResult<LocalStorageSnapshot> {
        val entries = mutableListOf<BackupEntry>()
        for (key in PERSISTENT_KEYS) {
            val result = batch.find(key.wireKey)?.result
                ?: return BackupDataResult.Failure("备份读取结果不完整。")
            when (result) {
                StorageResult.Missing -> entries += BackupEntry(key, false, null)
                is StorageResult.Failure ->
                    return BackupDataResult.Failure("${key.wireKey} 无法读取，未创建备份。")
                is StorageResult.Success -> entries += BackupEntry(
                    key,
                    true,
                    encodeUntyped(key, result.value)
                        ?: return BackupDataResult.Failure("${key.wireKey} 无法编码。")
                )
            }
        }
        val payload = encodePayload(entries)
        val capsules = entries.first { it.key == AppStorageKeys.Capsules.CATALOG }
            .encodedValue?.let(AppStorageKeys.Capsules.CATALOG.codec::decode)
            ?: CapsuleCatalog.EMPTY
        val profile = entries.first { it.key == AppStorageKeys.Profile.LOCAL_PROFILE }.exists
        val places = entries.first { it.key == AppStorageKeys.Places.CATALOG }
            .encodedValue?.let(AppStorageKeys.Places.CATALOG.codec::decode)
        val favorites = entries.first { it.key == AppStorageKeys.Favorites.PLACE_IDS }
            .encodedValue?.let(AppStorageKeys.Favorites.PLACE_IDS.codec::decode)
        return BackupDataResult.Success(
            LocalStorageSnapshot(
                payload = payload,
                structuredBytesApprox = payload.length.toLong(),
                mediaPaths = capsules.capsules.flatMap { it.imagePaths }.distinct(),
                profileCount = if (profile) 1 else 0,
                placeCount = places?.places?.size ?: 0,
                favoriteCount = favorites?.placeIds?.size ?: 0,
                capsuleCount = capsules.capsules.size
            )
        )
    }

    private fun decodePreview(selection: ImportSelection): BackupDataResult<BackupPreview> {
        return try {
            val root = JSONObject(selection.payload)
            if (root.optInt("backupVersion", -1) != BACKUP_VERSION) {
                return BackupDataResult.Failure("备份版本不受支持，未读取任何数据。")
            }
            val array = root.optJSONArray("entries")
                ?: return BackupDataResult.Failure("备份缺少数据清单。")
            val byKey = PERSISTENT_KEYS.associateBy { "${it.store.wireValue}:${it.wireKey}" }
            val entries = buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index)
                        ?: return BackupDataResult.Failure("备份数据项无效。")
                    val key = byKey["${json.optString("store")}:${json.optString("key")}"]
                        ?: return BackupDataResult.Failure("备份包含当前版本不认识的数据项。")
                    val exists = json.optBoolean("exists")
                    val value = if (exists) json.optString("value") else null
                    if (exists && (value == null || decodeUntyped(key, value) == null)) {
                        return BackupDataResult.Failure("${key.wireKey} 校验失败。")
                    }
                    add(BackupEntry(key, exists, value))
                }
            }
            if (entries.map { it.key }.toSet() != PERSISTENT_KEYS.toSet()) {
                return BackupDataResult.Failure("备份数据清单不完整。")
            }
            val capsules = entries.first { it.key == AppStorageKeys.Capsules.CATALOG }
                .encodedValue?.let(AppStorageKeys.Capsules.CATALOG.codec::decode)
                ?: CapsuleCatalog.EMPTY
            val places = entries.first { it.key == AppStorageKeys.Places.CATALOG }
                .encodedValue?.let(AppStorageKeys.Places.CATALOG.codec::decode)
            val favorites = entries.first { it.key == AppStorageKeys.Favorites.PLACE_IDS }
                .encodedValue?.let(AppStorageKeys.Favorites.PLACE_IDS.codec::decode)
            BackupDataResult.Success(
                BackupPreview(
                    sessionId = selection.sessionId,
                    fileName = selection.fileName,
                    profileCount = if (
                        entries.first { it.key == AppStorageKeys.Profile.LOCAL_PROFILE }.exists
                    ) 1 else 0,
                    placeCount = places?.places?.size ?: 0,
                    favoriteCount = favorites?.placeIds?.size ?: 0,
                    capsuleCount = capsules.capsules.size,
                    photoCount = capsules.capsules.flatMap { it.imagePaths }.distinct().size,
                    entries = entries
                )
            )
        } catch (_: Throwable) {
            BackupDataResult.Failure("这不是有效的 CityCapsule 备份。")
        }
    }

    private fun writeEntries(
        entries: List<BackupEntry>,
        index: Int,
        callback: (BackupDataResult<Unit>) -> Unit
    ) {
        if (index >= entries.size) {
            clearDrafts(callback)
            return
        }
        val entry = entries[index]
        val done: (StorageResult<Unit>) -> Unit = { result ->
            if (result is StorageResult.Success || result === StorageResult.Missing) {
                writeEntries(entries, index + 1, callback)
            } else {
                callback(BackupDataResult.Failure("${entry.key.wireKey} 写入失败。"))
            }
        }
        if (!entry.exists) {
            storage.remove(entry.key, done)
        } else {
            val decoded = decodeUntyped(entry.key, entry.encodedValue.orEmpty())
                ?: return callback(BackupDataResult.Failure("${entry.key.wireKey} 无法解码。"))
            putUntyped(storage, entry.key, decoded, done)
        }
    }

    private fun encodePayload(entries: List<BackupEntry>): String = JSONObject().apply {
        put("app", "CityCapsule")
        put("backupVersion", BACKUP_VERSION)
        put("schemaVersion", 1)
        put("entries", JSONArray().apply {
            entries.forEach { entry ->
                put(JSONObject().apply {
                    put("store", entry.key.store.wireValue)
                    put("key", entry.key.wireKey)
                    put("type", entry.key.codec.valueType.wireValue)
                    put("exists", entry.exists)
                    entry.encodedValue?.let { put("value", it) }
                })
            }
        })
    }.toString()

    private companion object {
        const val BACKUP_VERSION = 1
        val PERSISTENT_KEYS = listOf(
            AppStorageKeys.Settings.THEME_MODE,
            AppStorageKeys.Profile.LOCAL_PROFILE,
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            AppStorageKeys.Places.CATALOG,
            AppStorageKeys.Favorites.PLACE_IDS,
            AppStorageKeys.Capsules.CATALOG
        )
    }
}

@Suppress("UNCHECKED_CAST")
private fun encodeUntyped(key: StorageKey<*>, value: Any?): String? = runCatching {
    (key as StorageKey<Any?>).codec.encode(value)
}.getOrNull()

@Suppress("UNCHECKED_CAST")
private fun decodeUntyped(key: StorageKey<*>, encoded: String): Any? =
    (key as StorageKey<Any?>).codec.decode(encoded)

@Suppress("UNCHECKED_CAST")
private fun putUntyped(
    storage: KeyValueStore,
    key: StorageKey<*>,
    value: Any?,
    callback: (StorageResult<Unit>) -> Unit
) {
    storage.put(key as StorageKey<Any?>, value, callback)
}
