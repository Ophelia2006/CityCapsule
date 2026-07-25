package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.y.citycapsule.designsystem.theme.AppTheme

/** Full-width, accessible selection control used by filter and category groups. */
@Composable
fun AppChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val dimensions = AppTheme.dimensions
    val background = when {
        !enabled -> colors.disabledSurface
        selected -> colors.primaryContainer
        else -> colors.surfaceVariant
    }
    val content = when {
        !enabled -> colors.disabledContent
        selected -> colors.onPrimaryContainer
        else -> colors.textSecondary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .clip(RoundedCornerShape(dimensions.radiusMd))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensions.spacingSm,
                vertical = dimensions.spacingXs
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            style = AppTheme.typography.bodySecondary
        )
    }
}
