package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun <T> PhotoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        items.chunked(safeColumns).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEachIndexed { index, item ->
                    val cellModifier = Modifier.weight(1f).padding(
                        end = if (index < rowItems.lastIndex) AppTheme.dimensions.spacingXxs else AppTheme.dimensions.spacingNone
                    )
                    if (itemKey == null) {
                        Box(
                            cellModifier
                        ) { itemContent(item) }
                    } else key(itemKey(item)) {
                        Box(cellModifier) { itemContent(item) }
                    }
                }
                repeat(safeColumns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        }
    }
}

/** 3 columns on compact widths and 4 columns on medium/expanded widths. */
@Composable
fun <T> AdaptivePhotoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (item: T, tileSize: Dp) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= AppTheme.dimensions.adaptiveGridBreakpoint) 4 else 3
        val gap = AppTheme.dimensions.spacingXxs
        val tileSize = (maxWidth - gap * (columns - 1)) / columns
        PhotoGrid(items = items, columns = columns, itemKey = itemKey) { item ->
            itemContent(item, tileSize)
        }
    }
}

/**
 * A photo mosaic without trailing empty cells. It keeps rows at no more than
 * [maxColumns] and redistributes orphan cells into a balanced final pair.
 */
@Composable
fun <T> BalancedPhotoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    maxColumns: Int = 3,
    itemKey: ((T) -> Any)? = null,
    itemContent: @Composable (item: T, tileSize: Dp) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = AppTheme.dimensions.spacingXxs
        val availableWidth = maxWidth
        val rowSizes = balancedPhotoRowSizes(items.size, maxColumns)
        val tileSizes = rowSizes.flatMap { rowSize ->
            val tileSize = (availableWidth - gap * (rowSize - 1)) / rowSize
            List(rowSize) { tileSize }
        }
        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                items.forEachIndexed { index, item ->
                    if (itemKey == null) {
                        itemContent(item, tileSizes[index])
                    } else key(itemKey(item)) {
                        itemContent(item, tileSizes[index])
                    }
                }
            }
        ) { measurables, constraints ->
            val gapPx = gap.roundToPx()
            val placeables = measurables.mapIndexed { index, measurable ->
                val sizePx = tileSizes[index].roundToPx()
                measurable.measure(Constraints.fixed(sizePx, sizePx))
            }
            val rowHeights = rowSizes.mapIndexed { rowIndex, rowSize ->
                val firstItemIndex = rowSizes.take(rowIndex).sum()
                placeables.subList(firstItemIndex, firstItemIndex + rowSize)
                    .maxOfOrNull { it.height }
                    ?: 0
            }
            val totalHeight = rowHeights.sum() + gapPx * (rowSizes.size - 1).coerceAtLeast(0)
            layout(constraints.maxWidth, totalHeight) {
                var itemIndex = 0
                var y = 0
                rowSizes.forEachIndexed { rowIndex, rowSize ->
                    var x = 0
                    repeat(rowSize) {
                        val placeable = placeables[itemIndex++]
                        placeable.placeRelative(x, y)
                        x += placeable.width + gapPx
                    }
                    y += rowHeights[rowIndex] + gapPx
                }
            }
        }
    }
}

internal fun balancedPhotoRowSizes(itemCount: Int, maxColumns: Int = 3): List<Int> {
    if (itemCount <= 0) return emptyList()
    val columns = maxColumns.coerceAtLeast(1)
    val fullRows = itemCount / columns
    val remainder = itemCount % columns
    if (remainder != 1 || fullRows == 0 || columns == 1) {
        return List(fullRows) { columns } + listOfNotNull(remainder.takeIf { it > 0 })
    }
    val leadingFullRows = (fullRows - 1).coerceAtLeast(0)
    val balancedTailFirst = (columns + 1) / 2
    val balancedTailSecond = columns + 1 - balancedTailFirst
    return List(leadingFullRows) { columns } + listOf(balancedTailFirst, balancedTailSecond)
}
