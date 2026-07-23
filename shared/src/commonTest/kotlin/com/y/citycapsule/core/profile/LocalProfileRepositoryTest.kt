package com.y.citycapsule.core.profile

import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class LocalProfileRepositoryTest {
    @Test
    fun missingProfileUsesFrozenBusinessDefault() {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        var snapshot: LocalProfileSnapshot? = null

        repository.getProfileSnapshot { snapshot = it }

        assertEquals(LocalProfile.DEFAULT, assertNotNull(snapshot).profile)
        assertEquals(LocalProfileSource.DEFAULT_MISSING, snapshot?.source)
    }

    @Test
    fun saveNormalizesAndRoundTripsBehindRepository() {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        var write: StorageResult<Unit>? = null
        var read: StorageResult<LocalProfile>? = null

        repository.saveProfile(
            LocalProfile(
                displayName = "  Traveler  ",
                avatarPreset = AvatarPreset.SUNSET,
                homeCity = "  苏州 "
            )
        ) { write = it }
        repository.getProfile { read = it }

        assertIs<StorageResult.Success<Unit>>(write)
        assertEquals(
            LocalProfile(
                displayName = "Traveler",
                avatarPreset = AvatarPreset.SUNSET,
                homeCity = "苏州"
            ),
            assertIs<StorageResult.Success<LocalProfile>>(read).value
        )
    }

    @Test
    fun invalidProfileIsRejectedBeforeStorageWrite() {
        val repository = LocalProfileRepository(InMemoryKeyValueStore())
        var result: StorageResult<Unit>? = null

        repository.saveProfile(
            LocalProfile(displayName = " ", avatarPreset = AvatarPreset.SKY)
        ) { result = it }

        val failure = assertIs<StorageResult.Failure>(result)
        assertEquals(StorageErrorCode.INVALID_REQUEST, failure.error.code)
    }

    @Test
    fun corruptStoredProfileFallsBackAndPreservesWarning() {
        val storage = InMemoryKeyValueStore().apply {
            seedRaw(
                AppStorageKeys.Profile.LOCAL_PROFILE,
                encodedValue = """{"schemaVersion":1,"displayName":"","avatarPreset":"sky"}"""
            )
        }
        val repository = LocalProfileRepository(storage)
        var snapshot: LocalProfileSnapshot? = null

        repository.getProfileSnapshot { snapshot = it }

        assertEquals(LocalProfile.DEFAULT, assertNotNull(snapshot).profile)
        assertEquals(LocalProfileSource.DEFAULT_RECOVERY, snapshot?.source)
        assertEquals(StorageErrorCode.DECODE_FAILED, snapshot?.warning?.code)
    }
}
