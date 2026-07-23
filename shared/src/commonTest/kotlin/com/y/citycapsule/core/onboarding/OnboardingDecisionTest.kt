package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingDecisionTest {
    @Test
    fun currentCompletionAndValidProfileGoHomeAndCleanStaleDraft() {
        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Success(1L),
            profile = StorageResult.Success(LocalProfile.DEFAULT),
            draft = StorageResult.Success(OnboardingDraft(currentStep = OnboardingStep.REVIEW))
        )

        assertEquals(StartupDestination.HOME, decision.destination)
        assertEquals(StartupReason.COMPLETED, decision.reason)
        assertEquals(LocalProfile.DEFAULT, decision.profile)
        assertTrue(decision.repair.clearDraft)
    }

    @Test
    fun missingCompletionResumesDraftWithoutRepairingValidData() {
        val draft = OnboardingDraft(currentStep = OnboardingStep.DETAILS)

        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Missing,
            profile = StorageResult.Missing,
            draft = StorageResult.Success(draft)
        )

        assertEquals(StartupDestination.ONBOARDING, decision.destination)
        assertEquals(StartupReason.NOT_COMPLETED, decision.reason)
        assertEquals(draft, decision.draft)
        assertFalse(decision.repair.isRequired)
    }

    @Test
    fun completedMarkerWithoutProfileIsRepairedAndReturnsToOnboarding() {
        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Success(1L),
            profile = StorageResult.Missing,
            draft = StorageResult.Missing
        )

        assertEquals(StartupDestination.ONBOARDING, decision.destination)
        assertEquals(StartupReason.PROFILE_MISSING, decision.reason)
        assertTrue(decision.repair.clearCompletionVersion)
    }

    @Test
    fun corruptProfileAndDraftAreIsolatedAndScheduledForCleanup() {
        val corrupt = StorageResult.Failure(
            StorageError(StorageErrorCode.DECODE_FAILED, "Corrupt test value.")
        )

        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Success(1L),
            profile = corrupt,
            draft = corrupt
        )

        assertEquals(StartupReason.PROFILE_INVALID, decision.reason)
        assertTrue(decision.repair.clearCompletionVersion)
        assertTrue(decision.repair.clearProfile)
        assertTrue(decision.repair.clearDraft)
        assertEquals(2, decision.warnings.size)
    }

    @Test
    fun transientStorageFailureNeverDeletesDurableState() {
        val unavailable = StorageResult.Failure(
            StorageError(StorageErrorCode.NOT_INITIALIZED, "Unavailable in test.")
        )

        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Success(1L),
            profile = unavailable,
            draft = StorageResult.Missing
        )

        assertEquals(StartupReason.STORAGE_UNAVAILABLE, decision.reason)
        assertFalse(decision.repair.isRequired)
    }

    @Test
    fun zeroCompletionVersionIsNotCompleted() {
        val decision = OnboardingStartupDecider.decide(
            completedVersion = StorageResult.Success(0L),
            profile = StorageResult.Success(LocalProfile.DEFAULT),
            draft = StorageResult.Missing
        )

        assertEquals(StartupDestination.ONBOARDING, decision.destination)
        assertEquals(StartupReason.NOT_COMPLETED, decision.reason)
    }
}
