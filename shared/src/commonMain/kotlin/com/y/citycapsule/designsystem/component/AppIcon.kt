package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
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
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.designsystem.theme.AppTheme

/** Small, deliberately finite icon vocabulary used by the first product flows. */
enum class AppIconName(val glyph: String) {
    BACK("‹"), SEARCH("⌕"), EXPLORE("◇"), RECORD("□"), PROFILE("○"),
    FAVORITE("♡"), FAVORITE_FILLED("♥"), LOCATION("⌖"), ADD("＋"),
    MORE("…"), CLOSE("×"), PHOTO("▧"), RETRY("↻"), CHECK("✓")
}

@Composable
fun AppIcon(
    name: AppIconName,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    size: Dp = AppTheme.dimensions.iconLg
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
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
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .size(AppTheme.dimensions.minTouchTarget)
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
            .background(if (selected) colors.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AppIcon(
            name = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> colors.disabledContent
                selected -> colors.primary
                else -> colors.textPrimary
            }
        )
    }
}
