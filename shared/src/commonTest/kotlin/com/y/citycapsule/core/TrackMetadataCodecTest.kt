package com.y.citycapsule.core.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackMetadataCodecTest {
    @Test fun metadataContainsOnlyIndexAndStatus() {
        val value = TrackMetadata(10L, listOf("file:///tracks/10/chunk_0.json"), 1, TrackStatus.INTERRUPTED, "定位中断", 11L)
        assertEquals(value, TrackMetadataCodec.decode(TrackMetadataCodec.encode(value)))
    }
}
