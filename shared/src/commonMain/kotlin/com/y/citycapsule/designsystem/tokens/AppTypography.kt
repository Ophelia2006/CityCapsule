package com.y.citycapsule.designsystem.tokens

import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.unit.sp

/** Semantic type scale. Color is supplied separately by the active color scheme. */
data class AppTypography(
    val display: TextStyle,
    val cityTitle: TextStyle,
    val pageTitle: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val bodySecondary: TextStyle,
    val button: TextStyle,
    val caption: TextStyle
)

val DefaultAppTypography = AppTypography(
    display = TextStyle(
        fontSize = 34.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold
    ),
    cityTitle = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif
    ),
    pageTitle = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold
    ),
    sectionTitle = TextStyle(
        fontSize = 19.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Bold
    ),
    body = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySecondary = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    button = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    )
)
