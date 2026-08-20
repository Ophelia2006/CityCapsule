package com.y.citycapsule.core.profile

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.y.citycapsule.core.storage.StorageCodec
import com.y.citycapsule.core.storage.StorageValueType

object LocalProfileCodec : StorageCodec<LocalProfile> {
    override val valueType: StorageValueType = StorageValueType.JSON_OBJECT

    override fun encode(value: LocalProfile): String {
        val profile = requireNotNull(LocalProfileValidator.normalizeOrNull(value)) {
            "Local profile does not satisfy schema v2 validation."
        }
        return JSONObject().apply {
            put(LocalProfileContract.FIELD_SCHEMA_VERSION, profile.schemaVersion)
            put(LocalProfileContract.FIELD_DISPLAY_NAME, profile.displayName)
            put(LocalProfileContract.FIELD_AVATAR_PRESET, profile.avatarPreset.wireValue)
            profile.avatarManagedPath?.let {
                put(LocalProfileContract.FIELD_AVATAR_MANAGED_PATH, it)
            }
            profile.homeCity?.let { put(LocalProfileContract.FIELD_HOME_CITY, it) }
            profile.bio?.let { put(LocalProfileContract.FIELD_BIO, it) }
        }.toString()
    }

    override fun decode(encoded: String): LocalProfile? = try {
        val json = JSONObject(encoded)
        val avatarPreset = AvatarPreset.fromWireValue(
            json.optString(LocalProfileContract.FIELD_AVATAR_PRESET)
        ) ?: return null
        val schemaVersion = json.optInt(
            LocalProfileContract.FIELD_SCHEMA_VERSION,
            UNSUPPORTED_SCHEMA
        )
        LocalProfileValidator.normalizeOrNull(
            LocalProfile(
                schemaVersion = if (schemaVersion == LocalProfileContract.LEGACY_SCHEMA_VERSION) {
                    LocalProfileContract.SCHEMA_VERSION
                } else {
                    schemaVersion
                },
                displayName = json.optString(LocalProfileContract.FIELD_DISPLAY_NAME),
                avatarPreset = avatarPreset,
                avatarManagedPath = if (schemaVersion == LocalProfileContract.SCHEMA_VERSION) {
                    json.optionalString(LocalProfileContract.FIELD_AVATAR_MANAGED_PATH)
                } else {
                    null
                },
                homeCity = json.optionalString(LocalProfileContract.FIELD_HOME_CITY),
                bio = json.optionalString(LocalProfileContract.FIELD_BIO)
            )
        )
    } catch (_: Throwable) {
        null
    }

    private const val UNSUPPORTED_SCHEMA = -1
}

internal fun JSONObject.optionalString(key: String): String? =
    if (has(key)) optString(key).normalizedOptionalText() else null
