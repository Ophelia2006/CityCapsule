package com.y.citycapsule.feature.onboarding

import com.y.citycapsule.core.onboarding.OnboardingContract
import com.y.citycapsule.core.onboarding.OnboardingDraft
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.onboarding.OnboardingStep
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageBatchResult
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageKey
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnboardingStateHolderTest {
    @Test
    fun loadResumesPersistedDraftAtItsInternalStep() {
        val storage = InMemoryKeyValueStore()
        val draft = OnboardingDraft(
            currentStep = OnboardingStep.DETAILS,
            displayName = "Traveler",
            avatarPreset = AvatarPreset.FOREST
        )
        storage.putNow(AppStorageKeys.Onboarding.DRAFT, draft)
        val holder = OnboardingStateHolder(OnboardingRepository(storage))

        holder.load()

        assertEquals(OnboardingUiStatus.READY, holder.state.status)
        assertEquals(draft, holder.state.draft)
    }

    @Test
    fun identityStepRejectsBlankDisplayNameWithoutAdvancing() {
        val holder = OnboardingStateHolder(
            OnboardingRepository(InMemoryKeyValueStore())
        )
        holder.load()

        assertTrue(holder.next())
        assertEquals(OnboardingStep.IDENTITY, holder.state.draft.currentStep)

        assertFalse(holder.next())
        assertEquals(OnboardingStep.IDENTITY, holder.state.draft.currentStep)
        assertNotNull(holder.state.validationMessage)
    }

    @Test
    fun completeFlowPersistsProfileAndCompletionMarker() {
        val storage = InMemoryKeyValueStore()
        val holder = OnboardingStateHolder(OnboardingRepository(storage))
        var completedProfile: LocalProfile? = null
        holder.load()

        holder.next()
        holder.updateDisplayName("  Traveler  ")
        holder.updateAvatar(AvatarPreset.SUNSET)
        holder.next()
        holder.updateHomeCity("  杭州 ")
        holder.next()
        holder.complete { completedProfile = it }

        assertEquals(
            LocalProfile(
                displayName = "Traveler",
                avatarPreset = AvatarPreset.SUNSET,
                homeCity = "杭州"
            ),
            completedProfile
        )
        assertEquals(OnboardingUiStatus.COMPLETED, holder.state.status)
        assertEquals(
            OnboardingContract.CURRENT_COMPLETED_VERSION,
            storage.getNow(AppStorageKeys.Onboarding.COMPLETED_VERSION).successValue()
        )
        assertEquals(
            completedProfile,
            storage.getNow(AppStorageKeys.Profile.LOCAL_PROFILE).successValue()
        )
        assertIs<StorageResult.Missing>(
            storage.getNow(AppStorageKeys.Onboarding.DRAFT)
        )
    }

    @Test
    fun storageFailureKeepsFlowUsableAndInputInMemory() {
        val holder = OnboardingStateHolder(
            OnboardingRepository(AlwaysFailingStorage())
        )

        holder.load()
        holder.next()
        holder.updateDisplayName("Traveler")

        assertEquals(OnboardingUiStatus.READY, holder.state.status)
        assertEquals(OnboardingStep.IDENTITY, holder.state.draft.currentStep)
        assertEquals("Traveler", holder.state.draft.displayName)
        assertEquals(FeatureNoticeTone.WARNING, holder.state.notice?.tone)
    }
}

private class AlwaysFailingStorage : KeyValueStore {
    private val failure = StorageResult.Failure(
        StorageError(StorageErrorCode.NATIVE_FAILURE, "Injected state-holder failure.")
    )

    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        callback(failure)
    }

    override fun <T> put(
        key: StorageKey<T>,
        value: T,
        callback: StorageCallback<Unit>
    ) {
        callback(failure)
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        callback(failure)
    }

    override fun contains(
        key: StorageKey<*>,
        callback: StorageCallback<Boolean>
    ) {
        callback(failure)
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        callback(failure)
    }
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
