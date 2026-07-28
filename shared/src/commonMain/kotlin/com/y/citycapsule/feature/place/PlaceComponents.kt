package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCard
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
internal fun PlaceSummaryCard(
    place: Place,
    favorite: Boolean,
    favoriteBusy: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AppCard {
        AppSectionTitle(text = place.name)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(
            text = listOfNotNull(
                place.city,
                place.district,
                place.category.displayName()
            ).joinToString(" · ")
        )
        place.address?.let {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppBodyText(text = it)
        }
        if (place.tags.isNotEmpty()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(text = place.tags.joinToString("  #", prefix = "#"))
        }
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppButton(
            text = "查看详情",
            onClick = onOpen,
            variant = AppButtonVariant.SECONDARY
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppButton(
            text = if (favorite) "移出想去" else "想去",
            onClick = onToggleFavorite,
            variant = AppButtonVariant.TEXT,
            enabled = !favoriteBusy,
            loading = favoriteBusy,
            loadingText = "正在更新…"
        )
    }
}

internal fun PlaceCategory.displayName(): String = when (this) {
    PlaceCategory.LANDMARK -> "城市地标"
    PlaceCategory.CULTURE -> "文化场馆"
    PlaceCategory.FOOD -> "餐饮美食"
    PlaceCategory.NATURE -> "自然户外"
    PlaceCategory.SHOPPING -> "购物街区"
    PlaceCategory.OTHER -> "其他"
}

internal fun PlaceNoticeTone.toAppStatusTone() =
    when (this) {
        PlaceNoticeTone.NEUTRAL ->
            com.y.citycapsule.designsystem.component.AppStatusTone.NEUTRAL
        PlaceNoticeTone.SUCCESS ->
            com.y.citycapsule.designsystem.component.AppStatusTone.SUCCESS
        PlaceNoticeTone.WARNING ->
            com.y.citycapsule.designsystem.component.AppStatusTone.WARNING
        PlaceNoticeTone.ERROR ->
            com.y.citycapsule.designsystem.component.AppStatusTone.ERROR
    }
