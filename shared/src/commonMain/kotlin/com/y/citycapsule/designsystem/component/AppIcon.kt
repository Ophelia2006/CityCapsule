package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.role
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.stateDescription
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.designsystem.theme.AppTheme

/** Small, deliberately finite icon vocabulary used by the first product flows. */
enum class AppIconName(val glyph: String) {
    BACK("‹"), SEARCH("⌕"), EXPLORE("◇"), RECORD("□"), ROAM("⌁"), PROFILE("○"),
    FAVORITE("♡"), FAVORITE_FILLED("♥"), LOCATION("⌖"), ADD("＋"),
    MORE("…"), DRAG("≡"), CLOSE("×"), FORWARD("›"), PHOTO("▧"), RETRY("↻"), CHECK("✓"),
    SETTINGS("⚙️")
}

@Composable
fun AppIcon(
    name: AppIconName,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    size: Dp = AppTheme.dimensions.iconLg
) {
    Box(
        modifier = modifier.size(size).semantics {
            this.contentDescription = contentDescription
        },
        contentAlignment = Alignment.Center
    ) {
        Text(text = name.glyph, color = tint, style = AppTheme.typography.body)
    }
}

@Composable
fun AppIconButton(
    icon: AppIconName,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    compactVisual: Boolean = false
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppTheme.dimensions.minTouchTarget)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
                if (selected) stateDescription = "已选中"
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compactVisual) AppTheme.dimensions.iconXl else AppTheme.dimensions.minTouchTarget)
                .clip(RoundedCornerShape(if (compactVisual) AppTheme.dimensions.radiusXl else AppTheme.dimensions.radiusMd))
                .background(if (selected) colors.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            val iconTint = when {
                !enabled -> colors.disabledContent
                selected -> colors.primary
                else -> colors.textPrimary
            }
            if (compactVisual && icon == AppIconName.CLOSE) {
                AppCloseMark(iconTint)
            } else {
                AppIcon(
                    name = icon,
                    contentDescription = "",
                    size = if (compactVisual) AppTheme.dimensions.iconSm else AppTheme.dimensions.iconLg,
                    tint = iconTint
                )
            }
        }
    }
}

@Composable
private fun AppCloseMark(tint: Color) {
    val dimensions = AppTheme.dimensions
    Canvas(Modifier.size(dimensions.iconSm)) {
        val inset = size.minDimension / 4f
        val strokeWidth = (dimensions.strokeThin * 2).toPx()
        drawLine(
            color = tint,
            start = Offset(inset, inset),
            end = Offset(size.width - inset, size.height - inset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(size.width - inset, inset),
            end = Offset(inset, size.height - inset),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
