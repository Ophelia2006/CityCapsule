package com.y.citycapsule.feature.profile

import com.y.citycapsule.core.onboarding.OnboardingContract
import com.y.citycapsule.core.onboarding.OnboardingDraft
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.feature.onboarding.FeatureNoticeTone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileStateHolderTest {
    @Test
    fun editSaveNormalizesAndPersistsProfile() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(AppStorageKeys.Profile.LOCAL_PROFILE, LocalProfile.DEFAULT)
        val holder = stateHolder(storage)
        holder.load()

        holder.beginEditing()
        holder.updateDisplayName("  Ophelia ")
        holder.updateAvatar(AvatarPreset.NIGHT)
        holder.updateHomeCity("  上海 ")
        holder.save()

        assertFalse(holder.state.editing)
        assertEquals(ProfileUiStatus.READY, holder.state.status)
        assertEquals("Ophelia", holder.state.profile.displayName)
        assertEquals("上海", holder.state.profile.homeCity)
        assertEquals(FeatureNoticeTone.SUCCESS, holder.state.notice?.tone)
        assertEquals(
            holder.state.profile,
            storage.getNow(AppStorageKeys.Profile.LOCAL_PROFILE).successValue()
        )
    }

    @Test
    fun cancelRestoresLastSavedProfile() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(AppStorageKeys.Profile.LOCAL_PROFILE, LocalProfile.DEFAULT)
        val holder = stateHolder(storage)
        holder.load()

        holder.beginEditing()
        holder.updateDisplayName("Temporary")
        holder.cancelEditing()

        assertFalse(holder.state.editing)
        assertEquals(LocalProfile.DEFAULT, holder.state.profile)
    }

    @Test
    fun invalidEditRemainsEditableAndIsNotPersisted() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(AppStorageKeys.Profile.LOCAL_PROFILE, LocalProfile.DEFAULT)
        val holder = stateHolder(storage)
        holder.load()

        holder.beginEditing()
        holder.updateDisplayName(" ")
        holder.save()

        assertTrue(holder.state.editing)
        assertEquals(LocalProfile.DEFAULT, storage.getNow(
            AppStorageKeys.Profile.LOCAL_PROFILE
        ).successValue())
    }

    @Test
    fun clearRemovesCompletionProfileAndDraftBeforeCallback() {
        val storage = InMemoryKeyValueStore()
        storage.putNow(
            AppStorageKeys.Onboarding.COMPLETED_VERSION,
            OnboardingContract.CURRENT_COMPLETED_VERSION
        )
        storage.putNow(AppStorageKeys.Profile.LOCAL_PROFILE, LocalProfile.DEFAULT)
        storage.putNow(AppStorageKeys.Onboarding.DRAFT, OnboardingDraft())
        val holder = stateHolder(storage)
        holder.load()
        var cleared = false

        holder.requestClear()
        holder.clear { cleared = true }

        assertTrue(cleared)
        assertIs<StorageResult.Missing>(
            storage.getNow(AppStorageKeys.Onboarding.COMPLETED_VERSION)
        )
        assertIs<StorageResult.Missing>(
            storage.getNow(AppStorageKeys.Profile.LOCAL_PROFILE)
        )
        assertIs<StorageResult.Missing>(
            storage.getNow(AppStorageKeys.Onboarding.DRAFT)
        )
    }

    private fun stateHolder(storage: KeyValueStore): ProfileStateHolder =
        ProfileStateHolder(
            LocalProfileRepository(storage),
            OnboardingRepository(storage)
        )
}

private fun <T> KeyValueStore.putNow(
    key: StorageKey<T>,
    value: T
): StorageResult<Unit> {
    var captured: StorageResult<Unit>? = null
    put(key, value) { captured = it }
    return requireNotNull(captured)
}

private fun <T> KeyValueStore.getNow(key: StorageKey<T>): StorageResult<T> {
    var captured: StorageResult<T>? = null
    get(key) { captured = it }
    return requireNotNull(captured)
}

private fun <T> StorageResult<T>.successValue(): T =
    assertIs<StorageResult.Success<T>>(this).value
