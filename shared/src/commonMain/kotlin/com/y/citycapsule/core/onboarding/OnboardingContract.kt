package com.y.citycapsule.core.onboarding

object OnboardingContract {
    const val SCHEMA_VERSION = 1
    const val CURRENT_COMPLETED_VERSION = 1L

    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_CURRENT_STEP = "currentStep"
    const val FIELD_DISPLAY_NAME = "displayName"
    const val FIELD_AVATAR_PRESET = "avatarPreset"
    const val FIELD_HOME_CITY = "homeCity"
    const val FIELD_BIO = "bio"
}

enum class OnboardingStep(val wireValue: String) {
    WELCOME("welcome"),
    IDENTITY("identity"),
    DETAILS("details"),
    REVIEW("review");

    companion object {
        fun fromWireValue(value: String): OnboardingStep? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}
