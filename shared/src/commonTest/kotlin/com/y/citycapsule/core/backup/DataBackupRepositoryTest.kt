package com.y.citycapsule.core.backup

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DataBackupRepositoryTest {
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
