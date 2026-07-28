package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    require(options.isNotEmpty())
    val safeSelectedIndex = selectedIndex.coerceIn(options.indices)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusLg))
            .background(AppTheme.colors.surfaceVariant)
            .padding(AppTheme.dimensions.spacingXxs)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == safeSelectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = AppTheme.dimensions.minTouchTarget)
                    .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
                    .background(if (selected) AppTheme.colors.surface else Color.Transparent)
                    .clickable(enabled = !selected) { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) {
                        AppTheme.colors.textPrimary
                    } else {
                        AppTheme.colors.textSecondary
                    },
                    style = AppTheme.typography.button
                )
            }
        }
    }
}
