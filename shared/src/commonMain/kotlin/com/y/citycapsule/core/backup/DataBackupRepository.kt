package com.y.citycapsule.core.backup

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.capsule.CapsuleCatalog
import com.y.citycapsule.core.place.PlaceVisualRef
import com.y.citycapsule.core.place.PlaceVisualType
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
    val routeCount: Int,
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
    val capsuleCount: Int,
    val routeCount: Int
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
            } else if (entry.key == AppStorageKeys.Places.CATALOG && entry.exists) {
                val catalog = entry.encodedValue
                    ?.let(AppStorageKeys.Places.CATALOG.codec::decode)
                    ?: return callback(BackupDataResult.Failure("地点数据无法解码。"))
                val updated = catalog.copy(
                    places = catalog.places.map { place ->
                        val visual = place.visualRef
                        if (visual?.type == PlaceVisualType.MANAGED_FILE) {
                            place.copy(
                                visualRef = pathMapping[visual.value]?.let {
                                    PlaceVisualRef(PlaceVisualType.MANAGED_FILE, it)
                                }
                            )
                        } else place
                    }
                )
                entry.copy(encodedValue = AppStorageKeys.Places.CATALOG.codec.encode(updated))
            } else if (entry.key == AppStorageKeys.Profile.LOCAL_PROFILE && entry.exists) {
                val profile = entry.encodedValue
                    ?.let(AppStorageKeys.Profile.LOCAL_PROFILE.codec::decode)
                    ?: return callback(BackupDataResult.Failure("个人档案数据无法解码。"))
                val updated = profile.copy(
                    avatarManagedPath = profile.avatarManagedPath?.let(pathMapping::get)
                )
                entry.copy(
                    encodedValue = AppStorageKeys.Profile.LOCAL_PROFILE.codec.encode(updated)
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
        val profile = entries.first { it.key == AppStorageKeys.Profile.LOCAL_PROFILE }
            .encodedValue?.let(AppStorageKeys.Profile.LOCAL_PROFILE.codec::decode)
        val places = entries.first { it.key == AppStorageKeys.Places.CATALOG }
            .encodedValue?.let(AppStorageKeys.Places.CATALOG.codec::decode)
        val favorites = entries.first { it.key == AppStorageKeys.Favorites.PLACE_IDS }
            .encodedValue?.let(AppStorageKeys.Favorites.PLACE_IDS.codec::decode)
        val routes = entries.first { it.key == AppStorageKeys.Routes.CATALOG }
            .encodedValue?.let(AppStorageKeys.Routes.CATALOG.codec::decode)
        return BackupDataResult.Success(
            LocalStorageSnapshot(
                payload = payload,
                structuredBytesApprox = payload.length.toLong(),
                mediaPaths = (
                    capsules.capsules.flatMap { it.imagePaths } +
                        places?.places.orEmpty().mapNotNull { place ->
                            place.visualRef?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }?.value
                        } + listOfNotNull(profile?.avatarManagedPath)
                    ).distinct(),
                profileCount = if (profile != null) 1 else 0,
                placeCount = places?.places?.size ?: 0,
                favoriteCount = favorites?.placeIds?.size ?: 0,
                capsuleCount = capsules.capsules.size,
                routeCount = routes?.routes?.size ?: 0
            )
        )
    }

    private fun decodePreview(selection: ImportSelection): BackupDataResult<BackupPreview> {
        return try {
            val root = JSONObject(selection.payload)
            if (root.optString("app") != APP_ID) {
                return BackupDataResult.Failure("这不是有效的 CityCapsule 备份。")
            }
            val backupVersion = root.optInt("backupVersion", -1)
            if (backupVersion !in MIN_SUPPORTED_BACKUP_VERSION..BACKUP_VERSION) {
                return BackupDataResult.Failure("备份版本不受支持，未读取任何数据。")
            }
            val schemaVersion = root.optInt("schemaVersion", backupVersion)
            if (schemaVersion !in MIN_SUPPORTED_BACKUP_VERSION..BACKUP_VERSION) {
                return BackupDataResult.Failure("备份结构版本不受支持，未读取任何数据。")
            }
            val minimumReaderVersion = root.optInt(
                "minReaderVersion",
                MIN_SUPPORTED_BACKUP_VERSION
            )
            if (minimumReaderVersion > BACKUP_VERSION) {
                return BackupDataResult.Failure("备份需要更高版本的 CityCapsule，未读取任何数据。")
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
            var normalizedEntries = entries
            if (backupVersion < 3 && normalizedEntries.none { it.key == AppStorageKeys.Routes.CATALOG }) {
                normalizedEntries = normalizedEntries + BackupEntry(AppStorageKeys.Routes.CATALOG, false, null)
            }
            if (backupVersion < 4 && normalizedEntries.none { it.key == AppStorageKeys.Roaming.SESSION }) {
                normalizedEntries = normalizedEntries + BackupEntry(AppStorageKeys.Roaming.SESSION, false, null)
            }
            if (backupVersion < 5 && normalizedEntries.none { it.key == AppStorageKeys.Roaming.TRACK }) normalizedEntries = normalizedEntries + BackupEntry(AppStorageKeys.Roaming.TRACK, false, null)
            if (backupVersion < 6 && normalizedEntries.none { it.key == AppStorageKeys.Roaming.CHECK_INS }) normalizedEntries = normalizedEntries + BackupEntry(AppStorageKeys.Roaming.CHECK_INS, false, null)
            if (backupVersion < 8 && normalizedEntries.none { it.key == AppStorageKeys.Explore.CITY_SELECTION }) {
                normalizedEntries = normalizedEntries + BackupEntry(AppStorageKeys.Explore.CITY_SELECTION, false, null)
            }
            if (normalizedEntries.map { it.key }.toSet() != PERSISTENT_KEYS.toSet()) {
                return BackupDataResult.Failure("备份数据清单不完整。")
            }
            val capsules = normalizedEntries.first { it.key == AppStorageKeys.Capsules.CATALOG }
                .encodedValue?.let(AppStorageKeys.Capsules.CATALOG.codec::decode)
                ?: CapsuleCatalog.EMPTY
            val places = normalizedEntries.first { it.key == AppStorageKeys.Places.CATALOG }
                .encodedValue?.let(AppStorageKeys.Places.CATALOG.codec::decode)
            val favorites = normalizedEntries.first { it.key == AppStorageKeys.Favorites.PLACE_IDS }
                .encodedValue?.let(AppStorageKeys.Favorites.PLACE_IDS.codec::decode)
            val routes = normalizedEntries.first { it.key == AppStorageKeys.Routes.CATALOG }
                .encodedValue?.let(AppStorageKeys.Routes.CATALOG.codec::decode)
            val profile = normalizedEntries.first { it.key == AppStorageKeys.Profile.LOCAL_PROFILE }
                .encodedValue?.let(AppStorageKeys.Profile.LOCAL_PROFILE.codec::decode)
            BackupDataResult.Success(
                BackupPreview(
                    sessionId = selection.sessionId,
                    fileName = selection.fileName,
                    profileCount = if (
                        normalizedEntries.first { it.key == AppStorageKeys.Profile.LOCAL_PROFILE }.exists
                    ) 1 else 0,
                    placeCount = places?.places?.size ?: 0,
                    favoriteCount = favorites?.placeIds?.size ?: 0,
                    capsuleCount = capsules.capsules.size,
                    routeCount = routes?.routes?.size ?: 0,
                    photoCount = (
                        capsules.capsules.flatMap { it.imagePaths } +
                            places?.places.orEmpty().mapNotNull { place ->
                                place.visualRef?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }?.value
                            } + listOfNotNull(profile?.avatarManagedPath)
                        ).distinct().size,
                    entries = normalizedEntries
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
        put("app", APP_ID)
        put("backupVersion", BACKUP_VERSION)
        put("minReaderVersion", BACKUP_VERSION)
        put("schemaVersion", BACKUP_VERSION)
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
        const val APP_ID = "CityCapsule"
        const val MIN_SUPPORTED_BACKUP_VERSION = 1
        const val BACKUP_VERSION = 9
        val PERSISTENT_KEYS = listOf(
            AppStorageKeys.Settings.THEME_MODE,
            AppStorageKeys.Profile.LOCAL_PROFILE,
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            AppStorageKeys.Places.CATALOG,
            AppStorageKeys.Explore.CITY_SELECTION,
            AppStorageKeys.Favorites.PLACE_IDS,
            AppStorageKeys.Capsules.CATALOG,
            AppStorageKeys.Routes.CATALOG,
            AppStorageKeys.Roaming.SESSION,
            AppStorageKeys.Roaming.TRACK,
            AppStorageKeys.Roaming.CHECK_INS
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
