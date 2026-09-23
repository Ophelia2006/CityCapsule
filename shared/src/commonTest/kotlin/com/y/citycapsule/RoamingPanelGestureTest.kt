package com.y.citycapsule

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoamingPanelGestureTest {
    @Test
    fun upwardDragExpandsPanel() {
        assertTrue(roamingPanelExpandedAfterDrag(false, dragDistancePx = -30f, thresholdPx = 24f))
    }

    @Test
    fun downwardDragCollapsesPanel() {
        assertFalse(roamingPanelExpandedAfterDrag(true, dragDistancePx = 30f, thresholdPx = 24f))
    }

    @Test
    fun shortDragKeepsCurrentState() {
        assertTrue(roamingPanelExpandedAfterDrag(true, dragDistancePx = 10f, thresholdPx = 24f))
        assertFalse(roamingPanelExpandedAfterDrag(false, dragDistancePx = -10f, thresholdPx = 24f))
    }
}
