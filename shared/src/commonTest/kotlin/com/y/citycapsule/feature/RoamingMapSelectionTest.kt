package com.y.citycapsule.feature

import com.y.citycapsule.feature.roaming.RoamingSessionMutation
import com.y.citycapsule.feature.roaming.RoamingSessionReducer
import com.y.citycapsule.feature.roaming.RoamingSessionUiState
import kotlin.test.Test
import kotlin.test.assertEquals

class RoamingMapSelectionTest {
    @Test
    fun selectedMarkerBecomesRecoverableUiState() {
        val next = RoamingSessionReducer.reduce(
            RoamingSessionUiState(),
            RoamingSessionMutation.SelectMapPlace("place-2")
        )

        assertEquals("place-2", next.selectedMapPlaceId)
    }
}
