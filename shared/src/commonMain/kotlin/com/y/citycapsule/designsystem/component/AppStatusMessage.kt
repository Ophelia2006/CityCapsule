package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.designsystem.tokens.AppColorScheme

enum class AppStatusTone {
    NEUTRAL,
    SUCCESS,
    WARNING,
    ERROR
}

data class AppStatusPalette(
    val background: Color,
    val content: Color
)

fun resolveAppStatusPalette(
    colors: AppColorScheme,
    tone: AppStatusTone
): AppStatusPalette = when (tone) {
    AppStatusTone.NEUTRAL -> AppStatusPalette(colors.surfaceVariant, colors.textSecondary)
    AppStatusTone.SUCCESS -> AppStatusPalette(
        colors.successContainer,
        colors.onSuccessContainer
    )
    AppStatusTone.WARNING -> AppStatusPalette(
        colors.warningContainer,
        colors.onWarningContainer
    )
    AppStatusTone.ERROR -> AppStatusPalette(colors.errorContainer, colors.onErrorContainer)
}

@Composable
fun AppStatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    tone: AppStatusTone = AppStatusTone.NEUTRAL
) {
    val palette = resolveAppStatusPalette(AppTheme.colors, tone)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
            .background(palette.background)
            .padding(AppTheme.dimensions.spacingSm)
    ) {
        Text(
            text = message,
            color = palette.content,
            style = AppTheme.typography.bodySecondary
        )
    }
}
