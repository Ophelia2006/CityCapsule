package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.designsystem.theme.AppTheme

/** Shared page surface with safe-area padding, scrolling, and a bounded content column. */
@Composable
fun AppScaffold(
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val dimensions = AppTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = dimensions.contentMaxWidth)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = dimensions.screenHorizontalPadding,
                top = statusBarHeight.dp + dimensions.spacingXxl,
                end = dimensions.screenHorizontalPadding,
                bottom = dimensions.spacingXl
            )
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}
