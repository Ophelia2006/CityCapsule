package com.y.citycapsule.core.place

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PlaceCatalogCodecTest {
    @Test
    fun roundTripUsesStableOrderAndWireValues() {
        val encoded = PlaceCatalogCodec.encode(
            PlaceCatalog(
                places = listOf(
                    placeFixture(id = "place_b", name = "B"),
                    placeFixture(id = "place_a", name = "A")
                )
            )
        )
        val decoded = PlaceCatalogCodec.decode(encoded)

        assertEquals(listOf("place_a", "place_b"), decoded?.places?.map(Place::id))
        assertEquals(
            PlaceCategory.CULTURE,
            decoded?.places?.first()?.category
        )
        val json = JSONObject(encoded)
        assertEquals(PlaceContract.SCHEMA_VERSION, json.optInt("schemaVersion"))
        assertEquals(PlaceContract.CURRENT_SEED_VERSION, json.optInt("seedVersion"))
    }

    @Test
    fun unknownFieldsAreIgnored() {
        val decoded = PlaceCatalogCodec.decode(
            """
            {
              "schemaVersion": 1,
              "seedVersion": 1,
              "futureCatalogField": true,
              "places": [{
                "schemaVersion": 1,
                "id": "place_1",
                "name": "西岸美术馆",
                "city": "上海",
                "category": "culture",
                "tags": [],
                "createdAtEpochMs": 10,
                "updatedAtEpochMs": 20,
                "futurePlaceField": "ignored"
              }]
            }
            """.trimIndent()
        )

        assertEquals("西岸美术馆", decoded?.places?.single()?.name)
    }

    @Test
    fun malformedSchemaUnknownCategoryMissingTagsAndDuplicateIdsAreRejected() {
        assertNull(
            PlaceCatalogCodec.decode(
                """{"schemaVersion":2,"seedVersion":1,"places":[]}"""
            )
        )
        assertNull(
            PlaceCatalogCodec.decode(
                """
                {
                  "schemaVersion":1,
                  "seedVersion":1,
                  "places":[{
                    "schemaVersion":1,
                    "id":"place_1",
                    "name":"A",
                    "city":"上海",
                    "category":"unknown",
                    "tags":[],
                    "createdAtEpochMs":0,
                    "updatedAtEpochMs":0
                  }]
                }
                """.trimIndent()
            )
        )
        assertNull(
            PlaceCatalogCodec.decode(
                """
                {
                  "schemaVersion":1,
                  "seedVersion":1,
                  "places":[{
                    "schemaVersion":1,
                    "id":"place_1",
                    "name":"A",
                    "city":"上海",
                    "category":"culture",
                    "createdAtEpochMs":0,
                    "updatedAtEpochMs":0
                  }]
                }
                """.trimIndent()
            )
        )
        val duplicate = placeFixture(id = "same")
        assertFailsWith<IllegalArgumentException> {
            PlaceCatalogCodec.encode(PlaceCatalog(places = listOf(duplicate, duplicate)))
        }
    }
}
