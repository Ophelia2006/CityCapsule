package com.y.citycapsule.core.onboarding

import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.profile.normalizedOptionalText

data class OnboardingDraft(
    val schemaVersion: Int = OnboardingContract.SCHEMA_VERSION,
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val displayName: String = "",
    val avatarPreset: AvatarPreset = AvatarPreset.SKY,
    val homeCity: String? = null,
    val bio: String? = null
) {
    fun normalizedOrNull(): OnboardingDraft? {
        val normalized = copy(
            displayName = displayName.trim(),
            homeCity = homeCity.normalizedOptionalText(),
            bio = bio.normalizedOptionalText()
        )
        return normalized.takeIf {
            it.schemaVersion == OnboardingContract.SCHEMA_VERSION &&
                it.displayName.length <= LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH &&
                (it.homeCity?.length ?: 0) <= LocalProfileValidator.HOME_CITY_MAX_LENGTH &&
                (it.bio?.length ?: 0) <= LocalProfileValidator.BIO_MAX_LENGTH
        }
    }

    fun toLocalProfileOrNull(): LocalProfile? = LocalProfileValidator.normalizeOrNull(
        LocalProfile(
            displayName = displayName,
            avatarPreset = avatarPreset,
            homeCity = homeCity,
            bio = bio
        )
    )

    companion object {
        val EMPTY = OnboardingDraft()
    }
}
