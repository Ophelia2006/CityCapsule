package com.y.citycapsule.core.onboarding

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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OnboardingRepositoryTest {
    @Test
    fun completionWritesProfileThenMarkerAndFinallyRemovesDraft() {
        val storage = RecordingStorage().apply {
            seed(AppStorageKeys.Onboarding.DRAFT, OnboardingDraft(currentStep = OnboardingStep.REVIEW))
        }
        val repository = OnboardingRepository(storage)
        var result: StorageResult<OnboardingCompletion>? = null

        repository.complete(LocalProfile.DEFAULT) { result = it }

        val completion = assertIs<StorageResult.Success<OnboardingCompletion>>(result).value
        assertEquals(OnboardingContract.CURRENT_COMPLETED_VERSION, completion.completedVersion)
        assertNull(completion.cleanupWarning)
        assertEquals(
            listOf(
                "put:profile.local_profile",
                "put:onboarding.completed_version",
                "remove:onboarding.draft"
            ),
            storage.operations
        )
        assertEquals(
            LocalProfile.DEFAULT,
            storage.read(AppStorageKeys.Profile.LOCAL_PROFILE).successValue()
        )
        assertEquals(
            OnboardingContract.CURRENT_COMPLETED_VERSION,
            storage.read(AppStorageKeys.Onboarding.COMPLETED_VERSION).successValue()
        )
        assertIs<StorageResult.Missing>(storage.read(AppStorageKeys.Onboarding.DRAFT))
    }

    @Test
    fun profileWriteFailureNeverCreatesCompletionMarker() {
        val storage = RecordingStorage().apply {
            failPutWireKey = AppStorageKeys.Profile.LOCAL_PROFILE.wireKey
        }
        val repository = OnboardingRepository(storage)
        var result: StorageResult<OnboardingCompletion>? = null

        repository.complete(LocalProfile.DEFAULT) { result = it }

        assertIs<StorageResult.Failure>(result)
        assertEquals(listOf("put:profile.local_profile"), storage.operations)
        assertIs<StorageResult.Missing>(
            storage.read(AppStorageKeys.Onboarding.COMPLETED_VERSION)
        )
    }

    @Test
    fun markerWriteFailureLeavesProfileAndDraftForSafeResume() {
        val draft = OnboardingDraft(currentStep = OnboardingStep.REVIEW)
        val storage = RecordingStorage().apply {
            seed(AppStorageKeys.Onboarding.DRAFT, draft)
            failPutWireKey = AppStorageKeys.Onboarding.COMPLETED_VERSION.wireKey
        }
        val repository = OnboardingRepository(storage)
        var result: StorageResult<OnboardingCompletion>? = null

        repository.complete(LocalProfile.DEFAULT) { result = it }

        assertIs<StorageResult.Failure>(result)
        assertEquals(
            listOf(
                "put:profile.local_profile",
                "put:onboarding.completed_version"
            ),
            storage.operations
        )
        assertEquals(
            LocalProfile.DEFAULT,
            storage.read(AppStorageKeys.Profile.LOCAL_PROFILE).successValue()
        )
        assertEquals(draft, storage.read(AppStorageKeys.Onboarding.DRAFT).successValue())
    }

    @Test
    fun draftCleanupFailureCannotRollBackDurableCompletion() {
        val storage = RecordingStorage().apply {
            seed(AppStorageKeys.Onboarding.DRAFT, OnboardingDraft(currentStep = OnboardingStep.REVIEW))
            failRemoveWireKey = AppStorageKeys.Onboarding.DRAFT.wireKey
        }
        val repository = OnboardingRepository(storage)
        var result: StorageResult<OnboardingCompletion>? = null

        repository.complete(LocalProfile.DEFAULT) { result = it }

        val completion = assertIs<StorageResult.Success<OnboardingCompletion>>(result).value
        assertEquals(StorageErrorCode.NATIVE_FAILURE, completion.cleanupWarning?.code)
        assertEquals(
            OnboardingContract.CURRENT_COMPLETED_VERSION,
            storage.read(AppStorageKeys.Onboarding.COMPLETED_VERSION).successValue()
        )
    }

    @Test
    fun startupRepairsCompletionMarkerWhenProfileIsMissing() {
        val storage = RecordingStorage().apply {
            seed(
                AppStorageKeys.Onboarding.COMPLETED_VERSION,
                OnboardingContract.CURRENT_COMPLETED_VERSION
            )
        }
        val repository = OnboardingRepository(storage)
        var decision: StartupDecision? = null

        repository.getStartupDecision { decision = it }

        val captured = assertNotNull(decision)
        assertEquals(StartupDestination.ONBOARDING, captured.destination)
        assertEquals(StartupReason.PROFILE_MISSING, captured.reason)
        assertEquals(
            listOf("getMany", "remove:onboarding.completed_version"),
            storage.operations
        )
        assertIs<StorageResult.Missing>(
            storage.read(AppStorageKeys.Onboarding.COMPLETED_VERSION)
        )
    }

    @Test
    fun batchFailureProducesDegradedOnboardingWithoutDestructiveRepair() {
        val storage = RecordingStorage().apply { failGetMany = true }
        val repository = OnboardingRepository(storage)
        var decision: StartupDecision? = null

        repository.getStartupDecision { decision = it }

        val captured = assertNotNull(decision)
        assertEquals(StartupDestination.ONBOARDING, captured.destination)
        assertEquals(StartupReason.STORAGE_UNAVAILABLE, captured.reason)
        assertEquals(StorageErrorCode.NATIVE_FAILURE, captured.warnings.single().code)
        assertEquals(listOf("getMany"), storage.operations)
    }

    @Test
    fun resetAttemptsEveryRemovalInSafetyOrderAndReportsFirstFailure() {
        val storage = RecordingStorage().apply {
            seed(
                AppStorageKeys.Onboarding.COMPLETED_VERSION,
                OnboardingContract.CURRENT_COMPLETED_VERSION
            )
            seed(AppStorageKeys.Profile.LOCAL_PROFILE, LocalProfile.DEFAULT)
            seed(AppStorageKeys.Onboarding.DRAFT, OnboardingDraft())
            failRemoveWireKey = AppStorageKeys.Profile.LOCAL_PROFILE.wireKey
        }
        val repository = OnboardingRepository(storage)
        var result: StorageResult<Unit>? = null

        repository.resetLocalState { result = it }

        assertIs<StorageResult.Failure>(result)
        assertEquals(
            listOf(
                "remove:onboarding.completed_version",
                "remove:profile.local_profile",
                "remove:onboarding.draft"
            ),
            storage.operations
        )
        assertIs<StorageResult.Missing>(
            storage.read(AppStorageKeys.Onboarding.COMPLETED_VERSION)
        )
        assertEquals(
            LocalProfile.DEFAULT,
            storage.read(AppStorageKeys.Profile.LOCAL_PROFILE).successValue()
        )
        assertIs<StorageResult.Missing>(storage.read(AppStorageKeys.Onboarding.DRAFT))
    }
}

private class RecordingStorage : KeyValueStore {
    private val delegate = InMemoryKeyValueStore()
    val operations = mutableListOf<String>()
    var failPutWireKey: String? = null
    var failRemoveWireKey: String? = null
    var failGetMany: Boolean = false

    fun <T> seed(key: StorageKey<T>, value: T) {
        delegate.put(key, value) {}
    }

    fun <T> read(key: StorageKey<T>): StorageResult<T> {
        var captured: StorageResult<T>? = null
        delegate.get(key) { captured = it }
        return requireNotNull(captured)
    }

    override fun <T> get(key: StorageKey<T>, callback: StorageCallback<T>) {
        operations += "get:${key.wireKey}"
        delegate.get(key, callback)
    }

    override fun <T> put(
        key: StorageKey<T>,
        value: T,
        callback: StorageCallback<Unit>
    ) {
        operations += "put:${key.wireKey}"
        if (key.wireKey == failPutWireKey) {
            callback(testFailure())
        } else {
            delegate.put(key, value, callback)
        }
    }

    override fun remove(key: StorageKey<*>, callback: StorageCallback<Unit>) {
        operations += "remove:${key.wireKey}"
        if (key.wireKey == failRemoveWireKey) {
            callback(testFailure())
        } else {
            delegate.remove(key, callback)
        }
    }

    override fun contains(
        key: StorageKey<*>,
        callback: StorageCallback<Boolean>
    ) {
        delegate.contains(key, callback)
    }

    override fun getMany(
        keys: List<StorageKey<*>>,
        callback: StorageCallback<StorageBatchResult>
    ) {
        operations += "getMany"
        if (failGetMany) {
            callback(testFailure())
        } else {
            delegate.getMany(keys, callback)
        }
    }

    private fun testFailure(): StorageResult.Failure = StorageResult.Failure(
        StorageError(StorageErrorCode.NATIVE_FAILURE, "Injected test failure.")
    )
}

private fun <T> StorageResult<T>.successValue(): T =
    assertIs<StorageResult.Success<T>>(this).value
