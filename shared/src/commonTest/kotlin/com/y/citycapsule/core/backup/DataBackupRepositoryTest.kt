package com.y.citycapsule.core.backup

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.core.capsule.CapsuleCatalog
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.place.PlaceContract
import com.y.citycapsule.core.place.PlaceSeedData
import com.y.citycapsule.core.place.PlaceSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DataBackupRepositoryTest {
    @Test
    fun currentBackupRequiresV9ReaderSoLegacyReadersRejectIt() {
        val payload = JSONObject(awaitSnapshot(DataBackupRepository(InMemoryKeyValueStore())).payload)

        assertEquals(9, payload.optInt("backupVersion"))
        assertEquals(9, payload.optInt("schemaVersion"))
        assertEquals(9, payload.optInt("minReaderVersion"))
        assertTrue(payload.optInt("backupVersion") != 1)
    }

    @Test
    fun snapshotRoundTripRestoresPersistentValuesAndExcludesDraftCache() {
        val storage = InMemoryKeyValueStore()
        put(storage, AppStorageKeys.Settings.THEME_MODE, ThemeMode.DARK)
        put(storage, AppStorageKeys.Onboarding.COMPLETED_VERSION, 3L)
        put(storage, AppStorageKeys.Onboarding.DRAFT, AppStorageKeys.Onboarding.DRAFT.defaultValue)
        val repository = DataBackupRepository(storage)

        val snapshot = awaitSnapshot(repository)
        val preview = awaitPreview(
            repository,
            ImportSelection("test-session", snapshot.payload, "backup.zip")
        )

        put(storage, AppStorageKeys.Settings.THEME_MODE, ThemeMode.LIGHT)
        val restore = awaitRestore(repository, preview, emptyMap())

        assertIs<BackupDataResult.Success<Unit>>(restore)
        assertEquals(ThemeMode.DARK, get(storage, AppStorageKeys.Settings.THEME_MODE))
        assertEquals(3L, get(storage, AppStorageKeys.Onboarding.COMPLETED_VERSION))
        assertTrue(read(storage, AppStorageKeys.Onboarding.DRAFT) === StorageResult.Missing)
        assertTrue(!snapshot.payload.contains(AppStorageKeys.Onboarding.DRAFT.wireKey))
    }

    @Test
    fun previewRejectsUnknownBackupVersionWithoutWriting() {
        val storage = InMemoryKeyValueStore()
        put(storage, AppStorageKeys.Settings.THEME_MODE, ThemeMode.DARK)
        val repository = DataBackupRepository(storage)
        val payload = JSONObject(awaitSnapshot(repository).payload).apply {
            put("backupVersion", 99)
        }.toString()

        val result = awaitPreviewResult(
            repository,
            ImportSelection("test-session", payload, "future.zip")
        )

        assertIs<BackupDataResult.Failure>(result)
        assertTrue(result.message.contains("版本"))
        assertEquals(ThemeMode.DARK, get(storage, AppStorageKeys.Settings.THEME_MODE))
    }

    @Test
    fun previewRejectsBackupThatRequiresANewerReader() {
        val repository = DataBackupRepository(InMemoryKeyValueStore())
        val payload = JSONObject(awaitSnapshot(repository).payload).apply {
            put("minReaderVersion", 10)
        }.toString()

        val result = awaitPreviewResult(
            repository,
            ImportSelection("test-session", payload, "newer-reader.zip")
        )

        assertIs<BackupDataResult.Failure>(result)
        assertTrue(result.message.contains("更高版本"))
    }

    @Test
    fun previewRejectsUnknownEnvelopeSchema() {
        val repository = DataBackupRepository(InMemoryKeyValueStore())
        val payload = JSONObject(awaitSnapshot(repository).payload).apply {
            put("schemaVersion", 99)
        }.toString()

        val result = awaitPreviewResult(
            repository,
            ImportSelection("test-session", payload, "future-schema.zip")
        )

        assertIs<BackupDataResult.Failure>(result)
        assertTrue(result.message.contains("结构版本"))
    }

    @Test
    fun snapshotIncludesPublishedCameraOriginalAndNeverDraftOrThumbnailFiles() {
        val storage = InMemoryKeyValueStore()
        val cameraOriginal = "file:///sandbox/images/original/camera_1720000000000.jpg"
        val thumbnail = "file:///sandbox/images/thumbnail/camera_1720000000000.jpg.jpg"
        val draftOriginal = "file:///sandbox/images/original/draft_only.jpg"
        put(
            storage,
            AppStorageKeys.Capsules.CATALOG,
            CapsuleCatalog(capsules = listOf(
                CityCapsule(
                    id = "capsule-camera",
                    content = "Camera photo",
                    placeId = "seed_shanghai_museum",
                    imagePaths = listOf(cameraOriginal),
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 1
                )
            ))
        )
        put(
            storage,
            AppStorageKeys.Capsules.DRAFT,
            AppStorageKeys.Capsules.DRAFT.defaultValue.copy(
                imagePaths = listOf(draftOriginal, thumbnail),
                updatedAtEpochMs = 2
            )
        )

        val snapshot = awaitSnapshot(DataBackupRepository(storage))

        assertEquals(listOf(cameraOriginal), snapshot.mediaPaths)
        assertTrue(thumbnail !in snapshot.mediaPaths)
        assertTrue(draftOriginal !in snapshot.mediaPaths)
    }

    @Test
    fun oldBackupWithPlaceCatalogV1PreviewsAndRestoresAsV2() {
        val storage = InMemoryKeyValueStore()
        val repository = DataBackupRepository(storage)
        val root = JSONObject(awaitSnapshot(repository).payload)
        root.put("backupVersion", 1)
        root.put("schemaVersion", 1)
        root.put("minReaderVersion", 1)
        val entries = requireNotNull(root.optJSONArray("entries"))
        for (index in 0 until entries.length()) {
            val entry = requireNotNull(entries.optJSONObject(index))
            if (entry.optString("key") == AppStorageKeys.Places.CATALOG.wireKey) {
                entry.put("exists", true)
                entry.put(
                    "value",
                    """
                    {
                      "schemaVersion":1,
                      "seedVersion":1,
                      "places":[{
                        "schemaVersion":1,
                        "id":"seed_shanghai_museum",
                        "name":"Seed",
                        "city":"Shanghai",
                        "category":"culture",
                        "tags":[],
                        "createdAtEpochMs":10,
                        "updatedAtEpochMs":20
                      },{
                        "schemaVersion":1,
                        "id":"local_legacy",
                        "name":"User",
                        "city":"Shanghai",
                        "category":"other",
                        "tags":[],
                        "createdAtEpochMs":30,
                        "updatedAtEpochMs":40
                      }]
                    }
                    """.trimIndent()
                )
            }
        }

        val preview = awaitPreview(
            repository,
            ImportSelection("legacy-session", root.toString(), "legacy.zip")
        )
        val restore = awaitRestore(repository, preview, emptyMap())
        val catalog = get(storage, AppStorageKeys.Places.CATALOG)

        assertIs<BackupDataResult.Success<Unit>>(restore)
        assertEquals(PlaceSeedData.CATALOG.places.size + 1, preview.placeCount)
        assertEquals(PlaceContract.SCHEMA_VERSION, catalog.schemaVersion)
        assertEquals(
            PlaceSource.SEED,
            catalog.places.first { it.id == "seed_shanghai_museum" }.source
        )
        assertEquals(
            PlaceSource.USER,
            catalog.places.first { it.id == "local_legacy" }.source
        )
    }

    private fun awaitSnapshot(repository: DataBackupRepository): LocalStorageSnapshot {
        var result: BackupDataResult<LocalStorageSnapshot>? = null
        repository.snapshot { result = it }
        return (result as BackupDataResult.Success).value
    }

    private fun awaitPreview(
        repository: DataBackupRepository,
        selection: ImportSelection
    ): BackupPreview = (awaitPreviewResult(repository, selection) as BackupDataResult.Success).value

    private fun awaitPreviewResult(
        repository: DataBackupRepository,
        selection: ImportSelection
    ): BackupDataResult<BackupPreview> {
        var result: BackupDataResult<BackupPreview>? = null
        repository.preview(selection) { result = it }
        return requireNotNull(result)
    }

    private fun awaitRestore(
        repository: DataBackupRepository,
        preview: BackupPreview,
        mapping: Map<String, String>
    ): BackupDataResult<Unit> {
        var result: BackupDataResult<Unit>? = null
        repository.restore(preview, mapping) { result = it }
        return requireNotNull(result)
    }

    private fun <T> put(
        storage: InMemoryKeyValueStore,
        key: com.y.citycapsule.core.storage.StorageKey<T>,
        value: T
    ) {
        var result: StorageResult<Unit>? = null
        storage.put(key, value) { result = it }
        assertIs<StorageResult.Success<Unit>>(result)
    }

    private fun <T> get(
        storage: InMemoryKeyValueStore,
        key: com.y.citycapsule.core.storage.StorageKey<T>
    ): T = (read(storage, key) as StorageResult.Success).value

    private fun <T> read(
        storage: InMemoryKeyValueStore,
        key: com.y.citycapsule.core.storage.StorageKey<T>
    ): StorageResult<T> {
        var result: StorageResult<T>? = null
        storage.get(key) { result = it }
        return requireNotNull(result)
    }
}
