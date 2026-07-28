package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun <T> PhotoGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    itemContent: @Composable (T) -> Unit
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(modifier.fillMaxWidth()) {
        items.chunked(safeColumns).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEachIndexed { index, item ->
                    Box(
                        Modifier.weight(1f).padding(end = if (index < rowItems.lastIndex) AppTheme.dimensions.spacingXxs else AppTheme.dimensions.spacingNone)
                    ) { itemContent(item) }
                }
                repeat(safeColumns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        }
    }
}
