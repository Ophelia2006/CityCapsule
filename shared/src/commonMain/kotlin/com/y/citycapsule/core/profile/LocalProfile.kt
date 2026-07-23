package com.y.citycapsule.core.profile

object LocalProfileContract {
    const val SCHEMA_VERSION = 1

    const val FIELD_SCHEMA_VERSION = "schemaVersion"
    const val FIELD_DISPLAY_NAME = "displayName"
    const val FIELD_AVATAR_PRESET = "avatarPreset"
    const val FIELD_HOME_CITY = "homeCity"
    const val FIELD_BIO = "bio"
}

/**
 * The single local-only profile owned by this app installation.
 *
 * It carries no account id, network identity, token, image bytes, or platform file path.
 */
data class LocalProfile(
    val schemaVersion: Int = LocalProfileContract.SCHEMA_VERSION,
    val displayName: String,
    val avatarPreset: AvatarPreset,
    val homeCity: String? = null,
    val bio: String? = null
) {
    companion object {
        val DEFAULT = LocalProfile(
            displayName = "城市漫游者",
            avatarPreset = AvatarPreset.SKY
        )
    }
}
