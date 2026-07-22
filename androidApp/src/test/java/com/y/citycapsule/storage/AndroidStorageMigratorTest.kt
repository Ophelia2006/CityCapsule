package com.y.citycapsule.storage

import com.y.citycapsule.core.storage.StorageMigrationContract
import com.y.citycapsule.core.storage.StorageMigrationState
import com.y.citycapsule.core.storage.StorageStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidStorageMigratorTest {
    @Test
    fun validLegacyThemeMigratesAndCleansSource() {
        val provider = MigrationFakeProvider()
        val source = MigrationFakeLegacySource(" DARK ")

        val result = AndroidStorageMigrator(provider, source).migrate()

        assertEquals(AndroidMigrationOutcome.COMPLETED, result.outcome)
        assertTrue(result.migratedTheme)
        assertTrue(source.cleared)
        assertEquals("dark", provider.preferences.read(StorageMigrationContract.TARGET_THEME_MODE))
        assertEquals(
            "string",
            provider.preferences.read(
                StorageMigrationContract.TYPE_METADATA_PREFIX + StorageMigrationContract.TARGET_THEME_MODE
            )
        )
        assertCompleted(provider.meta)
    }

    @Test
    fun existingTargetWinsOverLegacyValue() {
        val provider = MigrationFakeProvider()
        provider.preferences.write(StorageMigrationContract.TARGET_THEME_MODE, "light")
        val source = MigrationFakeLegacySource("dark")

        val result = AndroidStorageMigrator(provider, source).migrate()

        assertEquals(AndroidMigrationOutcome.COMPLETED, result.outcome)
        assertFalse(result.migratedTheme)
        assertEquals("light", provider.preferences.read(StorageMigrationContract.TARGET_THEME_MODE))
        assertTrue(source.cleared)
    }

    @Test
    fun invalidLegacyValueFallsBackWithoutPoisoningTarget() {
        val provider = MigrationFakeProvider()
        val source = MigrationFakeLegacySource("amoled")

        val result = AndroidStorageMigrator(provider, source).migrate()

        assertEquals(AndroidMigrationOutcome.COMPLETED, result.outcome)
        assertNull(provider.preferences.read(StorageMigrationContract.TARGET_THEME_MODE))
        assertTrue(source.cleared)
        assertCompleted(provider.meta)
    }

    @Test
    fun failedWriteIsRecordedAndNextLaunchCanRetry() {
        val provider = MigrationFakeProvider()
        provider.preferences.failKey = StorageMigrationContract.TARGET_THEME_MODE
        val source = MigrationFakeLegacySource("dark")
        val migrator = AndroidStorageMigrator(provider, source)

        val failed = migrator.migrate()
        val failedState = provider.meta.read(StorageMigrationContract.META_STATE)
        provider.preferences.failKey = null
        val retried = migrator.migrate()

        assertEquals(AndroidMigrationOutcome.FAILED, failed.outcome)
        assertEquals(StorageMigrationState.FAILED.wireValue, failedState)
        assertEquals(AndroidMigrationOutcome.COMPLETED, retried.outcome)
        assertEquals(2, retried.attempt)
        assertEquals("dark", provider.preferences.read(StorageMigrationContract.TARGET_THEME_MODE))
    }

    @Test
    fun exhaustedFailureDoesNotTouchLegacySource() {
        val provider = MigrationFakeProvider()
        provider.meta.write(StorageMigrationContract.META_STATE, StorageMigrationState.FAILED.wireValue)
        provider.meta.write(
            StorageMigrationContract.META_ATTEMPTS,
            StorageMigrationContract.MAX_ATTEMPTS.toString()
        )
        val source = MigrationFakeLegacySource("dark")

        val result = AndroidStorageMigrator(provider, source).migrate()

        assertEquals(AndroidMigrationOutcome.RETRY_EXHAUSTED, result.outcome)
        assertEquals(0, source.readCount)
        assertFalse(source.cleared)
    }

    private fun assertCompleted(meta: MigrationFakeStringStore) {
        assertEquals(
            StorageMigrationContract.CURRENT_SCHEMA_VERSION.toString(),
            meta.read(StorageMigrationContract.META_SCHEMA_VERSION)
        )
        assertEquals(
            StorageMigrationState.COMPLETED.wireValue,
            meta.read(StorageMigrationContract.META_STATE)
        )
    }
}

private class MigrationFakeLegacySource(value: String?) : AndroidLegacySettingsSource {
    private var current = AndroidLegacyThemeValue(true, value)
    var cleared: Boolean = false
    var readCount: Int = 0

    override fun readThemeMode(): AndroidLegacyThemeValue {
        readCount += 1
        return current
    }

    override fun clearThemeMode() {
        cleared = true
        current = AndroidLegacyThemeValue(false, null)
    }
}

private class MigrationFakeProvider : AndroidStorageProvider {
    val meta = MigrationFakeStringStore()
    val preferences = MigrationFakeStringStore()
    private val cache = MigrationFakeStringStore()

    override val isReady: Boolean = true

    override fun store(storeId: String): AndroidStringStore? = when (storeId) {
        StorageStore.META.wireValue -> meta
        StorageStore.PREFERENCES.wireValue -> preferences
        StorageStore.CACHE.wireValue -> cache
        else -> null
    }
}

private class MigrationFakeStringStore : AndroidStringStore {
    private val values = mutableMapOf<String, String>()
    var failKey: String? = null

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String): Boolean {
        if (key == failKey) {
            return false
        }
        values[key] = value
        return true
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
