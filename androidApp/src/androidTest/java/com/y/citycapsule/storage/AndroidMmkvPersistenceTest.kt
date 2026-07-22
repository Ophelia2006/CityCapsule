package com.y.citycapsule.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tencent.mmkv.MMKV
import com.y.citycapsule.core.storage.StorageMigrationContract
import com.y.citycapsule.core.storage.StorageMigrationState
import com.y.citycapsule.core.storage.StorageStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMmkvPersistenceTest {
    @Test
    fun valueSurvivesMemoryCacheReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MMKV.initialize(context)
        val mmapId = "cc_android_device_test"
        val key = "diagnostics.persistence_probe"
        val first = MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE)
        first.removeValueForKey(key)

        assertTrue(first.encode(key, "android-mmkv-ok"))
        first.clearMemoryCache()
        val reopened = MMKV.mmkvWithID(mmapId, MMKV.SINGLE_PROCESS_MODE)

        assertEquals("android-mmkv-ok", reopened.decodeString(key))
        reopened.removeValueForKey(key)
    }

    @Test
    fun sharedPreferencesThemeMigratesIntoMmkv() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val initialization = AndroidMmkvStorage.initialize(context)
        assertTrue(initialization.success)
        val meta = requireNotNull(AndroidMmkvStorage.store(StorageStore.META.wireValue))
        val target = requireNotNull(AndroidMmkvStorage.store(StorageStore.PREFERENCES.wireValue))
        val typeKey = StorageMigrationContract.TYPE_METADATA_PREFIX +
            StorageMigrationContract.TARGET_THEME_MODE
        listOf(
            StorageMigrationContract.META_SCHEMA_VERSION,
            StorageMigrationContract.META_STATE,
            StorageMigrationContract.META_ATTEMPTS,
            StorageMigrationContract.META_LAST_ERROR
        ).forEach(meta::remove)
        target.remove(StorageMigrationContract.TARGET_THEME_MODE)
        target.remove(typeKey)
        val legacy = context.getSharedPreferences(
            StorageMigrationContract.LEGACY_SETTINGS_STORE,
            Context.MODE_PRIVATE
        )
        legacy.edit()
            .putString(StorageMigrationContract.LEGACY_THEME_MODE, "dark")
            .commit()

        val result = AndroidStorageMigrator(
            AndroidMmkvStorage,
            AndroidSharedPreferencesLegacySettingsSource(context)
        ).migrate()

        assertEquals(AndroidMigrationOutcome.COMPLETED, result.outcome)
        assertEquals("dark", target.read(StorageMigrationContract.TARGET_THEME_MODE))
        assertEquals("string", target.read(typeKey))
        assertEquals(
            StorageMigrationState.COMPLETED.wireValue,
            meta.read(StorageMigrationContract.META_STATE)
        )
        assertTrue(!legacy.contains(StorageMigrationContract.LEGACY_THEME_MODE))

        target.remove(StorageMigrationContract.TARGET_THEME_MODE)
        target.remove(typeKey)
        listOf(
            StorageMigrationContract.META_SCHEMA_VERSION,
            StorageMigrationContract.META_STATE,
            StorageMigrationContract.META_ATTEMPTS,
            StorageMigrationContract.META_LAST_ERROR
        ).forEach(meta::remove)
    }
}
