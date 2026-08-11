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
                    placeFixture(id = "place_a", name = "A").copy(
                        geoPoint = GeoPoint(31.2304, 121.4737),
                        visualRef = PlaceVisualRef(
                            PlaceVisualType.BUNDLED_ASSET,
                            "places/shanghai_museum.webp"
                        )
                    )
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
        assertEquals(PlaceSource.USER, decoded?.places?.first()?.source)
        assertEquals(31.2304, decoded?.places?.first()?.geoPoint?.latitude)
        assertEquals(
            PlaceVisualType.BUNDLED_ASSET,
            decoded?.places?.first()?.visualRef?.type
        )
    }

    @Test
    fun v1CatalogMigratesSourceByExactSeedIdAndPreservesIds() {
        val decoded = PlaceCatalogCodec.decode(
            """
            {
              "schemaVersion":1,
              "seedVersion":1,
              "places":[
                {
                  "schemaVersion":1,
                  "id":"seed_shanghai_museum",
                  "name":"Seed",
                  "city":"Shanghai",
                  "category":"culture",
                  "tags":[],
                  "createdAtEpochMs":0,
                  "updatedAtEpochMs":0
                },
                {
                  "schemaVersion":1,
                  "id":"seed_not_bundled",
                  "name":"User",
                  "city":"Shanghai",
                  "category":"other",
                  "tags":[],
                  "createdAtEpochMs":1,
                  "updatedAtEpochMs":1
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(PlaceContract.SCHEMA_VERSION, decoded?.schemaVersion)
        assertEquals(
            listOf("seed_shanghai_museum", "seed_not_bundled"),
            decoded?.places?.map(Place::id)
        )
        assertEquals(PlaceSource.SEED, decoded?.places?.first()?.source)
        assertEquals(PlaceSource.USER, decoded?.places?.last()?.source)
        assertEquals(PlaceContract.CURRENT_SEED_VERSION, decoded?.seedVersion)
        assertEquals(
            PlaceSeedData.BY_ID["seed_shanghai_museum"]?.geoPoint,
            decoded?.places?.first()?.geoPoint
        )
        assertNull(decoded?.places?.first()?.visualRef)
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
                """{"schemaVersion":3,"seedVersion":1,"places":[]}"""
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
