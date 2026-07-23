package com.y.citycapsule.designsystem.tokens

/** Durations are milliseconds and are shared by all components. */
data class AppMotion(
    val feedbackDurationMillis: Int,
    val transitionDurationMillis: Int,
    val emphasizedDurationMillis: Int
)

val DefaultAppMotion = AppMotion(
    feedbackDurationMillis = 120,
    transitionDurationMillis = 220,
    emphasizedDurationMillis = 300
)
