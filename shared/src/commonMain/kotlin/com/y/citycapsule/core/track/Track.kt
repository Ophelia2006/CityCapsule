package com.y.citycapsule.core.track

data class TrackPoint(val latitude: Double, val longitude: Double, val accuracyMeters: Double?, val recordedAtEpochMs: Long)
enum class TrackStatus(val wireValue: String) { RECORDING("recording"), INTERRUPTED("interrupted"), COMPLETED("completed"); companion object { fun fromWireValue(value: String) = entries.firstOrNull { it.wireValue == value } } }
data class TrackMetadata(
    val sessionStartedAtEpochMs: Long,
    val chunkPaths: List<String> = emptyList(),
    val pointCount: Int = 0,
    val status: TrackStatus = TrackStatus.RECORDING,
    val interruptionReason: String? = null,
    val lastPointAtEpochMs: Long? = null
)

object TrackContract { const val SCHEMA_VERSION = 1; const val MAX_CHUNKS = 2_000 }
