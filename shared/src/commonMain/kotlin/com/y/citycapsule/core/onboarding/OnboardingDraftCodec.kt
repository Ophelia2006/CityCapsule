package com.y.citycapsule.core.onboarding

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.optionalString
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object OnboardingDraftCodec : StorageCodec<OnboardingDraft> {
    override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

    override fun encode(value: OnboardingDraft): String {
        val draft = requireNotNull(value.normalizedOrNull()) {
            "Onboarding draft does not satisfy schema v1 validation."
        }
        return JSONObject().apply {
            put(OnboardingContract.FIELD_SCHEMA_VERSION, draft.schemaVersion)
            put(OnboardingContract.FIELD_CURRENT_STEP, draft.currentStep.wireValue)
            put(OnboardingContract.FIELD_DISPLAY_NAME, draft.displayName)
            put(OnboardingContract.FIELD_AVATAR_PRESET, draft.avatarPreset.wireValue)
            draft.homeCity?.let { put(OnboardingContract.FIELD_HOME_CITY, it) }
            draft.bio?.let { put(OnboardingContract.FIELD_BIO, it) }
        }.toString()
    }

    override fun decode(encoded: String): OnboardingDraft? = try {
        val json = JSONObject(encoded)
        val step = OnboardingStep.fromWireValue(
            json.optString(OnboardingContract.FIELD_CURRENT_STEP)
        ) ?: return null
        val avatarPreset = AvatarPreset.fromWireValue(
            json.optString(OnboardingContract.FIELD_AVATAR_PRESET)
        ) ?: return null
        OnboardingDraft(
            schemaVersion = json.optInt(
                OnboardingContract.FIELD_SCHEMA_VERSION,
                UNSUPPORTED_SCHEMA
            ),
            currentStep = step,
            displayName = json.optString(OnboardingContract.FIELD_DISPLAY_NAME),
            avatarPreset = avatarPreset,
            homeCity = json.optionalString(OnboardingContract.FIELD_HOME_CITY),
            bio = json.optionalString(OnboardingContract.FIELD_BIO)
        ).normalizedOrNull()
    } catch (_: Throwable) {
        null
    }

    private const val UNSUPPORTED_SCHEMA = -1
}
