package com.y.citycapsule.core.roaming

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoamingSessionCodecTest {
    @Test fun roundTripPreservesSession() {
        val value = RoamingSession("route-1", 10L, status = RoamingStatus.PAUSED)
        assertEquals(value, RoamingSessionCodec.decode(RoamingSessionCodec.encode(value)))
    }

    @Test fun activeSessionCannotHaveEndedAt() {
        assertNull(RoamingSessionValidator.normalizeOrNull(RoamingSession(null, 10L, 11L, RoamingStatus.ACTIVE)))
    }
}
