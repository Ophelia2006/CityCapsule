package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListScope
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.onGloballyPositioned
import com.tencent.kuikly.compose.ui.layout.positionInRoot
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.designsystem.theme.AppTheme

/** Shared page surface with safe-area padding, scrolling, and a bounded content column. */
@Composable
fun AppScaffold(
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp = AppTheme.dimensions.contentMaxWidth,
    bottomBar: (@Composable () -> Unit)? = null,
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
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
            bottomBar?.invoke()
        }
    }
}

/** Page surface whose header stays visible while only [content] scrolls. */
@Composable
fun AppFixedHeaderScaffold(
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp = AppTheme.dimensions.contentMaxWidth,
    bottomBar: (@Composable () -> Unit)? = null,
    contentListState: com.tencent.kuikly.compose.foundation.lazy.LazyListState =
        com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState(),
    onContentBoundsChanged: ((topPx: Float, bottomPx: Float) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val dimensions = AppTheme.dimensions

    Box(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions.screenHorizontalPadding,
                        top = statusBarHeight.dp + dimensions.spacingXxl,
                        end = dimensions.screenHorizontalPadding
                    ),
                content = header
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.positionInRoot().y
                        onContentBoundsChanged?.invoke(top, top + coordinates.size.height)
                    },
                state = contentListState,
                contentPadding = PaddingValues(
                    start = dimensions.screenHorizontalPadding,
                    end = dimensions.screenHorizontalPadding,
                    bottom = dimensions.spacingXl
                )
            ) {
                item {
                    Column(Modifier.fillMaxWidth(), content = content)
                }
            }
            bottomBar?.invoke()
        }
    }
}

/**
 * Fixed-header page surface whose body is expressed as real lazy items.
 *
 * Use this for long or incrementally loaded collections. Unlike
 * [AppFixedHeaderScaffold], it does not wrap the entire body in one giant lazy item,
 * so item keys can preserve identity and the list can anchor visible rows on append.
 */
@Composable
fun AppFixedHeaderLazyScaffold(
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    contentMaxWidth: Dp = AppTheme.dimensions.contentMaxWidth,
    bottomBar: (@Composable () -> Unit)? = null,
    contentListState: LazyListState = rememberLazyListState(),
    onContentBoundsChanged: ((topPx: Float, bottomPx: Float) -> Unit)? = null,
    header: @Composable ColumnScope.() -> Unit,
    content: LazyListScope.() -> Unit
) {
    val colors = AppTheme.colors
    val dimensions = AppTheme.dimensions

    Box(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensions.screenHorizontalPadding,
                        top = statusBarHeight.dp + dimensions.spacingXxl,
                        end = dimensions.screenHorizontalPadding
                    ),
                content = header
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        val top = coordinates.positionInRoot().y
                        onContentBoundsChanged?.invoke(top, top + coordinates.size.height)
                    },
                state = contentListState,
                contentPadding = PaddingValues(
                    start = dimensions.screenHorizontalPadding,
                    end = dimensions.screenHorizontalPadding,
                    bottom = dimensions.spacingXl
                ),
                content = content
            )
            bottomBar?.invoke()
        }
    }
}
