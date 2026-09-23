package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.y.citycapsule.TextField
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索地点、街区…",
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val dimensions = AppTheme.dimensions
    val contentColor = if (enabled) AppTheme.colors.textPrimary else AppTheme.colors.disabledContent
    val shape = RoundedCornerShape(dimensions.radiusMd)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .shadow(
                AppTheme.elevation.raised,
                shape,
                clip = false,
                ambientColor = AppTheme.colors.scrim.copy(alpha = 0.05f),
                spotColor = AppTheme.colors.scrim.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(if (enabled) AppTheme.colors.surface else AppTheme.colors.disabledSurface)
            .border(
                dimensions.strokeThin,
                if (focused) AppTheme.colors.primary else AppTheme.colors.divider,
                shape
            )
            .padding(horizontal = dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(
            name = AppIconName.SEARCH,
            contentDescription = "搜索",
            tint = AppTheme.colors.textSecondary
        )
        TextField(
            value = value,
            placeholder = placeholder,
            autoFocus = false,
            onFocus = { focused = true },
            onBlur = { focused = false },
            textStyle = AppTheme.typography.body.copy(color = contentColor),
            placeholderColor = AppTheme.colors.textSecondary,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = dimensions.spacingXs,
                    top = dimensions.spacingSm,
                    bottom = dimensions.spacingSm
                ),
            onValueChange = { candidate ->
                if (enabled) onValueChange(candidate.replace("\n", ""))
            }
        )
    }
}
