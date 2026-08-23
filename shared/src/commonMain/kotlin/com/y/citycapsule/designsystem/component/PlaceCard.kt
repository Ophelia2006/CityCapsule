package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.y.citycapsule.designsystem.theme.AppTheme

enum class PlaceCardVariant { HERO, COMPACT }

data class PlaceCardModel(
    val name: String,
    val metadata: String,
    val supportingText: String? = null,
    val favorite: Boolean = false,
    val fallbackKind: PlaceFallbackKind = PlaceFallbackKind.OTHER
)

@Composable
fun PlaceCard(
    model: PlaceCardModel,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PlaceCardVariant = PlaceCardVariant.HERO,
    favoriteEnabled: Boolean = true,
    showFavoriteAction: Boolean = true,
    media: (@Composable () -> Unit)? = null
) {
    if (variant == PlaceCardVariant.HERO) {
        Column(modifier.fillMaxWidth().clip(RoundedCornerShape(AppTheme.dimensions.radiusLg)).background(AppTheme.colors.surface).clickable(onClick = onOpen)) {
            Box(Modifier.fillMaxWidth().height(AppTheme.dimensions.placeHeroHeight)) {
                if (media != null) media() else PlaceMediaFallback(model.fallbackKind)
            }
            PlaceCardText(model, onToggleFavorite, favoriteEnabled, showFavoriteAction, Modifier.padding(AppTheme.dimensions.spacingMd))
        }
    } else {
        Row(modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = AppTheme.dimensions.spacingXs), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(AppTheme.dimensions.placeCompactMediaSize).clip(RoundedCornerShape(AppTheme.dimensions.radiusMd))) {
                if (media != null) media() else PlaceMediaFallback(model.fallbackKind)
            }
            PlaceCardText(model, onToggleFavorite, favoriteEnabled, showFavoriteAction, Modifier.weight(1f).padding(start = AppTheme.dimensions.spacingSm))
        }
    }
}

@Composable
private fun PlaceCardText(model: PlaceCardModel, onToggleFavorite: () -> Unit, favoriteEnabled: Boolean, showFavoriteAction: Boolean, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            AppSectionTitle(model.name)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(model.metadata)
            if (!model.supportingText.isNullOrBlank()) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                AppCaptionText(model.supportingText)
            }
        }
        if (showFavoriteAction) {
            AppIconButton(
                icon = if (model.favorite) AppIconName.FAVORITE_FILLED else AppIconName.FAVORITE,
                contentDescription = if (model.favorite) "移出想去" else "加入想去",
                onClick = onToggleFavorite,
                selected = model.favorite,
                enabled = favoriteEnabled
            )
        }
    }
}
