package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ColdStartLifecycleTest {
    @Test
    fun freshInstallStartsOnboarding() {
        val repository = OnboardingRepository(InMemoryKeyValueStore())

        assertEquals(
            StartupDestination.ONBOARDING,
            repository.startupDecision().destination
        )
    }

    @Test
    fun completedOnboardingMakesNextColdStartGoHome() {
        val repository = OnboardingRepository(InMemoryKeyValueStore())

        var completion: StorageResult<OnboardingCompletion>? = null
        repository.complete(LocalProfile.DEFAULT) { completion = it }

        assertIs<StorageResult.Success<OnboardingCompletion>>(completion)
        assertEquals(StartupDestination.HOME, repository.startupDecision().destination)
    }

    @Test
    fun clearingLocalStateMakesNextColdStartReturnToOnboarding() {
        val repository = OnboardingRepository(InMemoryKeyValueStore())
        repository.complete(LocalProfile.DEFAULT) {}

        var reset: StorageResult<Unit>? = null
        repository.resetLocalState { reset = it }

        assertIs<StorageResult.Success<Unit>>(reset)
        assertEquals(
            StartupDestination.ONBOARDING,
            repository.startupDecision().destination
        )
    }
}

private fun OnboardingRepository.startupDecision(): StartupDecision {
    var captured: StartupDecision? = null
    getStartupDecision { captured = it }
    return requireNotNull(captured)
}
