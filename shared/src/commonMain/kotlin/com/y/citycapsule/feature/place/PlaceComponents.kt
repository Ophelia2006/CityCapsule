package com.y.citycapsule.feature.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlacePhotoCacheEntry
import com.y.citycapsule.core.media.ImageLoadPriority
import com.y.citycapsule.core.media.PlaceImageLoadRuntime
import com.y.citycapsule.designsystem.component.PlaceCard
import com.y.citycapsule.designsystem.component.PlaceCardModel
import com.y.citycapsule.designsystem.component.PlaceCardVariant
import com.y.citycapsule.designsystem.component.PlaceFallbackKind

@Composable
internal fun PlaceSummaryCard(
    place: Place,
    favorite: Boolean,
    favoriteEnabled: Boolean,
    distanceLabel: String? = null,
    photo: PlacePhotoCacheEntry? = null,
    imagePriority: ImageLoadPriority = ImageLoadPriority.PREFETCH,
    onCachedPhotoFailed: () -> Unit = {},
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    PlaceCard(
        model = PlaceCardModel(
            name = place.name,
            metadata = listOfNotNull(
                place.city, place.district, place.category.displayName(), distanceLabel
            ).joinToString(" · "),
            supportingText = place.address ?: place.tags.takeIf { it.isNotEmpty() }?.joinToString("  ") { "#$it" },
            favorite = favorite,
            fallbackKind = place.category.toFallbackKind()
        ),
        onOpen = onOpen,
        onToggleFavorite = onToggleFavorite,
        variant = PlaceCardVariant.COMPACT,
        favoriteEnabled = favoriteEnabled,
        media = {
            PlaceMedia(
                place = place,
                cachedPhoto = photo,
                imagePriority = imagePriority,
                onCachedPhotoFailed = onCachedPhotoFailed
            )
        }
    )
}

@Composable
internal fun PlaceMedia(
    place: Place,
    cachedPhoto: PlacePhotoCacheEntry? = null,
    imagePriority: ImageLoadPriority = ImageLoadPriority.VISIBLE,
    modifier: Modifier = Modifier,
    onCachedPhotoFailed: () -> Unit = {},
    onLoaded: () -> Unit = {}
) {
    val localUrl = place.visualRef?.value
    val displayUrl = localUrl ?: cachedPhoto?.url
    val coordinatedUrl = displayUrl?.takeIf { localUrl == null && it.startsWith("http") }
    var loadAllowed by remember(displayUrl) { mutableStateOf(coordinatedUrl == null) }
    var failureReported by remember(displayUrl) { mutableStateOf(false) }
    DisposableEffect(coordinatedUrl, imagePriority) {
        val lease = coordinatedUrl?.let { url ->
            PlaceImageLoadRuntime.coordinator.acquire(url, imagePriority) { allowed ->
                loadAllowed = allowed
            }
        }
        onDispose { lease?.release() }
    }
    Box(modifier.fillMaxSize()) {
        com.y.citycapsule.designsystem.component.PlaceMediaFallback(place.category.toFallbackKind())
        displayUrl?.takeIf { loadAllowed }?.let { url ->
            Image(
                painter = rememberAsyncImagePainter(
                    url,
                    onSuccess = {
                        coordinatedUrl?.let { PlaceImageLoadRuntime.coordinator.complete(it, true) }
                        failureReported = false
                        onLoaded()
                    },
                    onError = {
                        coordinatedUrl?.let { PlaceImageLoadRuntime.coordinator.complete(it, false) }
                        if (localUrl == null && !failureReported) {
                            failureReported = true
                            onCachedPhotoFailed()
                        }
                    }
                ),
                contentDescription = "${place.name}地点照片",
                modifier = Modifier.fillMaxWidth().fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

internal fun PlaceCategory.toFallbackKind(): PlaceFallbackKind = when (this) {
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
