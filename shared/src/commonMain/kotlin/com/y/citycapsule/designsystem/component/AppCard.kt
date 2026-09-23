package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AppTheme.colors.surface,
    elevation: Dp = AppTheme.elevation.raised,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTheme.dimensions.radiusLg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                ambientColor = AppTheme.colors.scrim.copy(alpha = 0.08f),
                spotColor = AppTheme.colors.scrim.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(containerColor)
            .border(AppTheme.dimensions.strokeThin, AppTheme.colors.divider, shape)
            .padding(AppTheme.dimensions.spacingMd),
        content = content
    )
}

@Composable
fun AppSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AppSectionTitle(text = title)
        if (!description.isNullOrBlank()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(text = description)
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppCard(content = content)
    }
}

@Composable
fun AppDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTheme.dimensions.strokeThin)
            .background(AppTheme.colors.divider)
    )
}
