package com.y.citycapsule.core.favorite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FavoritePlaceIdsCodecTest {
    @Test
    fun roundTripSortsAndDeduplicatesIds() {
        val decoded = FavoritePlaceIdsCodec.decode(
            """
            {
              "schemaVersion": 1,
              "placeIds": ["place_b", "place_a", "place_b"]
            }
            """.trimIndent()
        )
        val reencoded = FavoritePlaceIdsCodec.encode(requireNotNull(decoded))

        assertEquals(setOf("place_a", "place_b"), decoded.placeIds)
        assertEquals(
            FavoritePlaceIds(placeIds = setOf("place_a", "place_b")),
            FavoritePlaceIdsCodec.decode(reencoded)
        )
    }

    @Test
    fun unsupportedSchemaMalformedPayloadAndInvalidIdsAreRejected() {
        assertNull(
            FavoritePlaceIdsCodec.decode(
                """{"schemaVersion":2,"placeIds":[]}"""
            )
        )
        assertNull(
            FavoritePlaceIdsCodec.decode(
                """{"schemaVersion":1}"""
            )
        )
        assertNull(
            FavoritePlaceIdsCodec.decode(
                """{"schemaVersion":1,"placeIds":["bad id"]}"""
            )
        )
        assertFailsWith<IllegalArgumentException> {
            FavoritePlaceIdsCodec.encode(FavoritePlaceIds(placeIds = setOf("bad id")))
        }
    }
}
