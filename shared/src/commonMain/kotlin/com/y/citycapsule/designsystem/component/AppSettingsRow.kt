package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.RowScope
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppBodyText(text = title)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(text = description)
        }
        if (content != null) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            content()
        }
    }
}

@Composable
fun AppThemeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    systemLabel: String = "跟随系统",
    lightLabel: String = "浅色",
    darkLabel: String = "深色"
) {
    val options = listOf(
        ThemeMode.SYSTEM to systemLabel,
        ThemeMode.LIGHT to lightLabel,
        ThemeMode.DARK to darkLabel
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusLg))
            .background(AppTheme.colors.surfaceVariant)
            .padding(AppTheme.dimensions.spacingXxs)
    ) {
        options.forEach { (mode, label) ->
            ThemeOption(
                mode = mode,
                label = label,
                selected = mode == selectedMode,
                enabled = enabled,
                onModeSelected = onModeSelected
            )
        }
    }
}

@Composable
private fun RowScope.ThemeOption(
    mode: ThemeMode,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onModeSelected: (ThemeMode) -> Unit
) {
    val background = if (selected) AppTheme.colors.primaryContainer else Color.Transparent
    val contentColor = when {
        !enabled -> AppTheme.colors.disabledContent
        selected -> AppTheme.colors.onPrimaryContainer
        else -> AppTheme.colors.textSecondary
    }

    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
            .background(background)
            .clickable(
                enabled = enabled && !selected,
                onClick = { onModeSelected(mode) }
            )
            .padding(
                horizontal = AppTheme.dimensions.spacingXxs,
                vertical = AppTheme.dimensions.spacingSm
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            style = AppTheme.typography.bodySecondary
        )
    }
}
