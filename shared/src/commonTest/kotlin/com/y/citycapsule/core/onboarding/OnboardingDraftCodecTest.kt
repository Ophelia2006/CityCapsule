package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OnboardingDraftCodecTest {
    @Test
    fun earlyStepMayPersistWithoutACompletedDisplayName() {
        val draft = OnboardingDraft(
            currentStep = OnboardingStep.IDENTITY,
            displayName = "",
            avatarPreset = AvatarPreset.FOREST
        )

        assertEquals(draft, OnboardingDraftCodec.decode(OnboardingDraftCodec.encode(draft)))
        assertNull(draft.toLocalProfileOrNull())
    }

    @Test
    fun completedDraftNormalizesIntoProfile() {
        val draft = OnboardingDraft(
            currentStep = OnboardingStep.REVIEW,
            displayName = "  Traveler ",
            avatarPreset = AvatarPreset.SUNSET,
            homeCity = "  杭州 ",
            bio = "  城市观察员 "
        )

        assertEquals(
            LocalProfile(
                displayName = "Traveler",
                avatarPreset = AvatarPreset.SUNSET,
                homeCity = "杭州",
                bio = "城市观察员"
            ),
            draft.toLocalProfileOrNull()
        )
    }

    @Test
    fun unknownStepAndOversizedTextAreRejected() {
        assertNull(
            OnboardingDraftCodec.decode(
                """
                {"schemaVersion":1,"currentStep":"remote","displayName":"",
                 "avatarPreset":"sky"}
                """.trimIndent()
            )
        )
        assertNull(
            OnboardingDraftCodec.decode(
                """
                {"schemaVersion":1,"currentStep":"identity",
                 "displayName":"${"A".repeat(LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH + 1)}",
                 "avatarPreset":"sky"}
                """.trimIndent()
            )
        )
    }
}
