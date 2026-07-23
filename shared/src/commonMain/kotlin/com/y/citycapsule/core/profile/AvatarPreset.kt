package com.y.citycapsule.core.profile

/**
 * Stable identifiers for avatars that can be rendered entirely from shared resources.
 *
 * Binary images and platform file paths are intentionally outside the local-profile v1
 * contract.
 */
enum class AvatarPreset(val wireValue: String) {
    SKY("sky"),
    FOREST("forest"),
    SUNSET("sunset"),
    NIGHT("night");

    companion object {
        fun fromWireValue(value: String): AvatarPreset? = entries.firstOrNull {
            it.wireValue == value
        }
    }
}
