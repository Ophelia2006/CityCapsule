package com.y.citycapsule.core.place

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaceValidatorTest {
    @Test
    fun placeNormalizationTrimsFieldsAndDeduplicatesTags() {
        val result = PlaceValidator.validate(
            placeFixture(
                id = "  place_1 ",
                name = "  上海博物馆 ",
                city = " 上海 ",
                district = "  ",
                address = " 人民大道 201 号 ",
                tags = listOf(" 博物馆 ", "博物馆", "MUSEUM", "museum"),
                note = " 适合雨天参观 "
            )
        )

        assertTrue(result.isValid)
        assertEquals("place_1", result.place?.id)
        assertEquals("上海博物馆", result.place?.name)
        assertEquals("上海", result.place?.city)
        assertNull(result.place?.district)
        assertEquals(listOf("博物馆", "MUSEUM"), result.place?.tags)
        assertEquals("适合雨天参观", result.place?.note)
    }

    @Test
    fun validatorRejectsIdentityRequiredFieldsLengthsAndTimestamps() {
        val result = PlaceValidator.validate(
            placeFixture(
                id = "bad id",
                name = " ",
                city = "C".repeat(PlaceValidator.CITY_MAX_LENGTH + 1),
                tags = List(PlaceValidator.TAG_MAX_COUNT + 1) { "tag_$it" },
                createdAtEpochMs = 20L,
                updatedAtEpochMs = 10L
            )
        )

        assertFalse(result.isValid)
        assertTrue(PlaceValidationError.INVALID_ID in result.errors)
        assertTrue(PlaceValidationError.NAME_REQUIRED in result.errors)
        assertTrue(PlaceValidationError.CITY_TOO_LONG in result.errors)
        assertTrue(PlaceValidationError.TOO_MANY_TAGS in result.errors)
        assertTrue(PlaceValidationError.INVALID_UPDATED_AT in result.errors)
    }

    @Test
    fun validatorRejectsOutOfRangeCoordinatesAndEmptyVisualReference() {
        val result = PlaceValidator.validate(
            placeFixture().copy(
                geoPoint = GeoPoint(latitude = 91.0, longitude = 181.0),
                visualRef = PlaceVisualRef(PlaceVisualType.MANAGED_FILE, " ")
            )
        )

        assertFalse(result.isValid)
        assertTrue(PlaceValidationError.INVALID_GEO_POINT in result.errors)
        assertTrue(PlaceValidationError.INVALID_VISUAL_REF in result.errors)
    }

    @Test
    fun draftValidationDoesNotRequireGeneratedIdentityOrTimestamps() {
        val result = PlaceValidator.validateDraft(
            PlaceDraft(
                name = "  武康大楼 ",
                city = " 上海 ",
                category = PlaceCategory.LANDMARK,
                tags = listOf("建筑", " 建筑 ")
            )
        )

        assertTrue(result.isValid)
        assertEquals("武康大楼", result.draft?.name)
        assertEquals(listOf("建筑"), result.draft?.tags)
    }

    @Test
    fun catalogRejectsDuplicateIdsInvalidRecordsAndOversizedLists() {
        val duplicate = PlaceCatalogValidator.validate(
            PlaceCatalog(
                places = listOf(
                    placeFixture(id = "same"),
                    placeFixture(id = "same", name = "另一个地点")
                )
            )
        )
        val invalid = PlaceCatalogValidator.validate(
            PlaceCatalog(places = listOf(placeFixture(id = "bad id")))
        )
        val oversized = PlaceCatalogValidator.validate(
            PlaceCatalog(
                places = List(PlaceContract.MAX_CATALOG_SIZE + 1) { index ->
                    placeFixture(id = "place_$index")
                }
            )
        )

        assertEquals(
            listOf(PlaceCatalogValidationError.DUPLICATE_PLACE_ID),
            duplicate.errors
        )
        assertEquals(listOf(PlaceCatalogValidationError.INVALID_PLACE), invalid.errors)
        assertEquals(
            listOf(PlaceCatalogValidationError.TOO_MANY_PLACES),
            oversized.errors
        )
    }
}
