package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.RowScope
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppProfileAvatar(
    preset: AvatarPreset,
    managedPath: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = AppTheme.dimensions.minTouchTarget
) {
    var imageFailed by remember(managedPath) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusXl))
            .background(AppTheme.colors.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (managedPath == null || imageFailed) {
            Text(
                text = preset.symbol(),
                color = AppTheme.colors.onPrimaryContainer,
                style = AppTheme.typography.sectionTitle
            )
        } else {
            Image(
                painter = rememberAsyncImagePainter(managedPath, onError = { imageFailed = true }),
                contentDescription = "用户头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun AppAvatarPicker(
    selected: AvatarPreset,
    onSelected: (AvatarPreset) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppBodyText(text = "预设头像")
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppTheme.dimensions.radiusLg))
                .background(AppTheme.colors.surfaceVariant)
                .padding(AppTheme.dimensions.spacingXxs)
        ) {
            AvatarPreset.entries.forEach { preset ->
                AvatarOption(
                    preset = preset,
                    selected = preset == selected,
                    enabled = enabled,
                    onSelected = onSelected
                )
            }
        }
    }
}

@Composable
private fun RowScope.AvatarOption(
    preset: AvatarPreset,
    selected: Boolean,
    enabled: Boolean,
    onSelected: (AvatarPreset) -> Unit
) {
    val background = if (selected) {
        AppTheme.colors.primaryContainer
    } else {
        Color.Transparent
    }
    val content = when {
        !enabled -> AppTheme.colors.disabledContent
        selected -> AppTheme.colors.onPrimaryContainer
        else -> AppTheme.colors.textSecondary
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))
            .background(background)
            .clickable(
                enabled = enabled && !selected,
                onClick = { onSelected(preset) }
            )
            .padding(
                horizontal = AppTheme.dimensions.spacingXxs,
                vertical = AppTheme.dimensions.spacingXs
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = preset.symbol(),
            color = content,
            style = AppTheme.typography.sectionTitle
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        Text(
            text = preset.displayName(),
            color = content,
            style = AppTheme.typography.caption
        )
    }
}

private fun AvatarPreset.symbol(): String = when (this) {
    AvatarPreset.SKY -> "晴"
    AvatarPreset.FOREST -> "森"
    AvatarPreset.SUNSET -> "霞"
    AvatarPreset.NIGHT -> "夜"
}

private fun AvatarPreset.displayName(): String = when (this) {
    AvatarPreset.SKY -> "天空"
    AvatarPreset.FOREST -> "森林"
    AvatarPreset.SUNSET -> "晚霞"
    AvatarPreset.NIGHT -> "夜色"
}
