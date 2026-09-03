package com.y.citycapsule.feature.route

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LocalRouteReorderTest {
    @Test
    fun reorderMovesItemToAnyValidPosition() {
        val ids = listOf("a", "b", "c", "d")

        assertEquals(listOf("c", "a", "b", "d"), reorderPlaceIds(ids, 2, 0))
        assertEquals(listOf("a", "c", "d", "b"), reorderPlaceIds(ids, 1, 3))
    }

    @Test
    fun invalidOrSamePositionKeepsOriginalInstance() {
        val ids = listOf("a", "b")

        assertSame(ids, reorderPlaceIds(ids, -1, 0))
        assertSame(ids, reorderPlaceIds(ids, 0, 2))
        assertSame(ids, reorderPlaceIds(ids, 1, 1))
    }

    @Test
    fun cityComparisonNormalizationAcceptsMunicipalitySuffix() {
        assertEquals("杭州", normalizeCity(" 杭州市 "))
        assertEquals("杭州", normalizeCity("杭州"))
    }
}
