package com.y.citycapsule.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageMigrationContractTest {
    @Test
    fun migrationMetadataIsFrozen() {
        assertEquals(1, StorageMigrationContract.CURRENT_SCHEMA_VERSION)
        assertEquals(3, StorageMigrationContract.MAX_ATTEMPTS)
        assertEquals("storage.schema_version", StorageMigrationContract.META_SCHEMA_VERSION)
        assertEquals("storage.migration_state", StorageMigrationContract.META_STATE)
        assertEquals("settings.theme_mode", StorageMigrationContract.TARGET_THEME_MODE)
    }

    @Test
    fun legacyThemeNormalizationAcceptsOnlyKnownValues() {
        assertEquals("dark", StorageMigrationContract.normalizeLegacyThemeMode(" DARK "))
        assertEquals("system", StorageMigrationContract.normalizeLegacyThemeMode("system"))
        assertNull(StorageMigrationContract.normalizeLegacyThemeMode("amoled"))
        assertNull(StorageMigrationContract.normalizeLegacyThemeMode(null))
    }

    @Test
    fun unknownStateSafelyReturnsNotStarted() {
        assertEquals(
            StorageMigrationState.NOT_STARTED,
            StorageMigrationState.fromWireValue("future_state")
        )
    }
}
