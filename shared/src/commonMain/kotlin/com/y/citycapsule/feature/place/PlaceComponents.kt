package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.designsystem.component.PlaceCard
import com.y.citycapsule.designsystem.component.PlaceCardModel
import com.y.citycapsule.designsystem.component.PlaceCardVariant
import com.y.citycapsule.designsystem.component.PlaceFallbackKind

@Composable
internal fun PlaceSummaryCard(
    place: Place,
    favorite: Boolean,
    favoriteBusy: Boolean,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    PlaceCard(
        model = PlaceCardModel(
            name = place.name,
            metadata = listOfNotNull(place.city, place.district, place.category.displayName()).joinToString(" · "),
            supportingText = place.address ?: place.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" },
            favorite = favorite,
            fallbackKind = place.category.toFallbackKind()
        ),
        onOpen = onOpen,
        onToggleFavorite = onToggleFavorite,
        variant = PlaceCardVariant.COMPACT,
        favoriteEnabled = !favoriteBusy
    )
}

private fun PlaceCategory.toFallbackKind(): PlaceFallbackKind = when (this) {
    PlaceCategory.LANDMARK -> PlaceFallbackKind.LANDMARK
    PlaceCategory.CULTURE -> PlaceFallbackKind.CULTURE
    PlaceCategory.FOOD -> PlaceFallbackKind.FOOD
    PlaceCategory.NATURE -> PlaceFallbackKind.NATURE
    PlaceCategory.SHOPPING -> PlaceFallbackKind.SHOPPING
    PlaceCategory.OTHER -> PlaceFallbackKind.OTHER
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
