package com.y.citycapsule.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tencent.mmkv.MMKV
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoritePlaceIdsCodec
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalog
import com.y.citycapsule.core.place.PlaceCatalogCodec
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.storage.AppStorageKeys
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
    fun placeCatalogAndFavoritesSurviveMemoryCacheReload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MMKV.initialize(context)
        val catalogKey = AppStorageKeys.Places.CATALOG
        val favoritesKey = AppStorageKeys.Favorites.PLACE_IDS
        val catalog = PlaceCatalog(
            places = listOf(
                Place(
                    id = "device_place",
                    name = "设备持久化地点",
                    city = "上海",
                    category = PlaceCategory.CULTURE,
                    tags = listOf("设备测试"),
                    createdAtEpochMs = 1_000L,
                    updatedAtEpochMs = 1_000L
                )
            )
        )
        val favorites = FavoritePlaceIds(placeIds = setOf("device_place"))
        val encodedCatalog = PlaceCatalogCodec.encode(catalog)
        val encodedFavorites = FavoritePlaceIdsCodec.encode(favorites)
        val first = MMKV.mmkvWithID(
            StorageStore.PREFERENCES.wireValue,
            MMKV.SINGLE_PROCESS_MODE
        )
        val originalCatalog = snapshot(first, catalogKey.wireKey)
        val originalFavorites = snapshot(first, favoritesKey.wireKey)

        try {
            assertTrue(first.encode(catalogKey.wireKey, encodedCatalog))
            assertTrue(
                first.encode(
                    typeKey(catalogKey.wireKey),
                    catalogKey.codec.valueType.wireValue
                )
            )
            assertTrue(first.encode(favoritesKey.wireKey, encodedFavorites))
            assertTrue(
                first.encode(
                    typeKey(favoritesKey.wireKey),
                    favoritesKey.codec.valueType.wireValue
                )
            )

            first.clearMemoryCache()
            val reopened = MMKV.mmkvWithID(
                StorageStore.PREFERENCES.wireValue,
                MMKV.SINGLE_PROCESS_MODE
            )

            assertEquals(
                catalog,
                PlaceCatalogCodec.decode(
                    requireNotNull(reopened.decodeString(catalogKey.wireKey))
                )
            )
            assertEquals(
                favorites,
                FavoritePlaceIdsCodec.decode(
                    requireNotNull(reopened.decodeString(favoritesKey.wireKey))
                )
            )
            assertEquals(
                catalogKey.codec.valueType.wireValue,
                reopened.decodeString(typeKey(catalogKey.wireKey))
            )
            assertEquals(
                favoritesKey.codec.valueType.wireValue,
                reopened.decodeString(typeKey(favoritesKey.wireKey))
            )
            restore(reopened, catalogKey.wireKey, originalCatalog)
            restore(reopened, favoritesKey.wireKey, originalFavorites)
        } catch (error: Throwable) {
            val reopened = MMKV.mmkvWithID(
                StorageStore.PREFERENCES.wireValue,
                MMKV.SINGLE_PROCESS_MODE
            )
            restore(reopened, catalogKey.wireKey, originalCatalog)
            restore(reopened, favoritesKey.wireKey, originalFavorites)
            throw error
        }
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

    private fun snapshot(mmkv: MMKV, key: String): RawValue = RawValue(
        value = mmkv.decodeString(key),
        valueType = mmkv.decodeString(typeKey(key))
    )

    private fun restore(mmkv: MMKV, key: String, rawValue: RawValue) {
        mmkv.removeValueForKey(key)
        mmkv.removeValueForKey(typeKey(key))
        rawValue.value?.let { mmkv.encode(key, it) }
        rawValue.valueType?.let { mmkv.encode(typeKey(key), it) }
    }

    private fun typeKey(key: String): String = TYPE_KEY_PREFIX + key

    private data class RawValue(
        val value: String?,
        val valueType: String?
    )

    private companion object {
        const val TYPE_KEY_PREFIX = "__cc_type__."
    }
}
