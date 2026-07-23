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
import com.tencent.kuikly.compose.ui.graphics.Color
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.designsystem.tokens.AppColorScheme

enum class AppButtonVariant {
    PRIMARY,
    SECONDARY,
    TEXT,
    DANGER
}

data class AppButtonPalette(
    val background: Color,
    val content: Color
)

fun resolveAppButtonPalette(
    colors: AppColorScheme,
    variant: AppButtonVariant,
    enabled: Boolean
): AppButtonPalette {
    if (!enabled) {
        return AppButtonPalette(
            background = if (variant == AppButtonVariant.TEXT) {
                Color.Transparent
            } else {
                colors.disabledSurface
            },
            content = colors.disabledContent
        )
    }

    return when (variant) {
        AppButtonVariant.PRIMARY -> AppButtonPalette(colors.primary, colors.onPrimary)
        AppButtonVariant.SECONDARY -> AppButtonPalette(
            colors.primaryContainer,
            colors.onPrimaryContainer
        )
        AppButtonVariant.TEXT -> AppButtonPalette(Color.Transparent, colors.primary)
        AppButtonVariant.DANGER -> AppButtonPalette(colors.error, colors.onError)
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = "处理中…"
) {
    val dimensions = AppTheme.dimensions
    val canClick = enabled && !loading
    val palette = resolveAppButtonPalette(AppTheme.colors, variant, canClick)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .clip(RoundedCornerShape(dimensions.radiusLg))
            .background(palette.background)
            .clickable(enabled = canClick, onClick = onClick)
            .padding(
                horizontal = dimensions.spacingMd,
                vertical = dimensions.spacingSm
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (loading) loadingText else text,
            color = palette.content,
            style = AppTheme.typography.button
        )
    }
}
