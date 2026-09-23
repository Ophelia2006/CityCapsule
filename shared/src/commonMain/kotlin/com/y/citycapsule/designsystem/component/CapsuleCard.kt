package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.y.citycapsule.designsystem.theme.AppTheme

enum class CapsuleCardVariant { TIMELINE, RECENT }

data class CapsuleCardModel(
    val dateLabel: String,
    val placeLabel: String,
    val excerpt: String,
    val metadata: String? = null
)

@Composable
fun CapsuleCard(
    model: CapsuleCardModel,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CapsuleCardVariant = CapsuleCardVariant.TIMELINE,
    media: (@Composable () -> Unit)? = null
) {
    val containerModifier = modifier
        .fillMaxWidth()
        .let { base ->
            if (variant == CapsuleCardVariant.RECENT) {
                val shape = RoundedCornerShape(AppTheme.dimensions.radiusLg)
                base.shadow(
                    AppTheme.elevation.raised,
                    shape,
                    clip = false,
                    ambientColor = AppTheme.colors.scrim.copy(alpha = 0.06f),
                    spotColor = AppTheme.colors.scrim.copy(alpha = 0.10f)
                ).clip(shape)
                    .background(AppTheme.colors.surface)
            } else {
                base
            }
        }
        .clickable(onClick = onOpen)
        .padding(
            horizontal = if (variant == CapsuleCardVariant.TIMELINE) {
                AppTheme.dimensions.spacingXxs
            } else {
                AppTheme.dimensions.spacingMd
            },
            vertical = AppTheme.dimensions.spacingMd
        )
    Column(
        containerModifier
    ) {
        AppCaptionText(model.dateLabel)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        if (variant == CapsuleCardVariant.TIMELINE) AppSectionTitle(model.placeLabel) else AppBodyText(model.placeLabel)
        if (media != null) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))) { media() }
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppBodyText(model.excerpt)
        if (!model.metadata.isNullOrBlank()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppSecondaryText(model.metadata)
        }
    }
}
