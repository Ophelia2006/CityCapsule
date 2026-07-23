package com.y.citycapsule.core.profile

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalProfileCodecTest {
    @Test
    fun roundTripNormalizesTextAndKeepsStableWireValues() {
        val encoded = LocalProfileCodec.encode(
            LocalProfile(
                displayName = "  Ophelia  ",
                avatarPreset = AvatarPreset.FOREST,
                homeCity = "  上海 ",
                bio = "  收集城市里的小瞬间  "
            )
        )

        val decoded = LocalProfileCodec.decode(encoded)

        assertEquals(
            LocalProfile(
                displayName = "Ophelia",
                avatarPreset = AvatarPreset.FOREST,
                homeCity = "上海",
                bio = "收集城市里的小瞬间"
            ),
            decoded
        )
        val wireJson = JSONObject(encoded)
        assertEquals(1, wireJson.optInt(LocalProfileContract.FIELD_SCHEMA_VERSION))
        assertEquals(
            "forest",
            wireJson.optString(LocalProfileContract.FIELD_AVATAR_PRESET)
        )
    }

    @Test
    fun unknownFieldsAreIgnoredForForwardCompatibility() {
        val decoded = LocalProfileCodec.decode(
            """
            {
              "schemaVersion": 1,
              "displayName": "Traveler",
              "avatarPreset": "sky",
              "futureField": "ignored"
            }
            """.trimIndent()
        )

        assertEquals(
            LocalProfile(displayName = "Traveler", avatarPreset = AvatarPreset.SKY),
            decoded
        )
    }

    @Test
    fun unsupportedSchemaUnknownAvatarAndInvalidNameAreRejected() {
        assertNull(
            LocalProfileCodec.decode(
                """{"schemaVersion":2,"displayName":"A","avatarPreset":"sky"}"""
            )
        )
        assertNull(
            LocalProfileCodec.decode(
                """{"schemaVersion":1,"displayName":"A","avatarPreset":"remote_url"}"""
            )
        )
        assertNull(
            LocalProfileCodec.decode(
                """{"schemaVersion":1,"displayName":"  ","avatarPreset":"sky"}"""
            )
        )
    }

    @Test
    fun validatorEnforcesFrozenLengthLimits() {
        val valid = LocalProfileValidator.validate(
            LocalProfile(
                displayName = "A".repeat(LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH),
                avatarPreset = AvatarPreset.NIGHT,
                homeCity = "B".repeat(LocalProfileValidator.HOME_CITY_MAX_LENGTH),
                bio = "C".repeat(LocalProfileValidator.BIO_MAX_LENGTH)
            )
        )
        val invalid = LocalProfileValidator.validate(
            LocalProfile(
                displayName = "A".repeat(LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH + 1),
                avatarPreset = AvatarPreset.NIGHT
            )
        )

        assertTrue(valid.isValid)
        assertEquals(
            listOf(LocalProfileValidationError.DISPLAY_NAME_TOO_LONG),
            invalid.errors
        )
    }
}
