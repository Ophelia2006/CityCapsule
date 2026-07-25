package com.y.citycapsule.feature.place

enum class PlaceNoticeTone {
    NEUTRAL,
    SUCCESS,
    WARNING,
    ERROR
}

data class PlaceFeatureNotice(
    val message: String,
    val tone: PlaceNoticeTone
)
