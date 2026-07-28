package com.y.citycapsule.designsystem.tokens

import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/** Semantic elevation levels. Components decide whether the current renderer supports a shadow. */
data class AppElevation(
    val flat: Dp,
    val raised: Dp,
    val overlay: Dp
)

val DefaultAppElevation = AppElevation(
    flat = 0.dp,
    raised = 4.dp,
    overlay = 12.dp
)
