package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.Dp
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.BalancedPhotoGrid
import com.y.citycapsule.designsystem.component.AppIconButton
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
internal fun CapsulePhotoList(
    paths: List<String>,
    modifier: Modifier = Modifier,
    onRemove: ((String) -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        paths.forEachIndexed { index, path ->
            CapsulePhoto(path = path, description = "城市碎片照片 ${index + 1}")
            if (onRemove != null) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                AppButton(
                    text = "移除这张照片",
                    onClick = { onRemove(path) },
                    variant = AppButtonVariant.TEXT
                )
            }
            if (index < paths.lastIndex) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            }
        }
    }
}

@Composable
internal fun CapsulePhoto(
    path: String,
    description: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    heightOverride: Dp? = null
) {
    val dimensions = AppTheme.dimensions
    val height = heightOverride ?: if (compact) {
        dimensions.mediaThumbnailSize
    } else {
        dimensions.mediaPreviewHeight
    }
    var failed by remember(path) { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(
        path,
        onSuccess = { failed = false },
        onError = { failed = true }
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(dimensions.radiusLg))
            .background(AppTheme.colors.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = description,
            modifier = Modifier.fillMaxWidth().height(height),
            contentScale = ContentScale.Crop
        )
        if (path.isBlank() || failed) {
            AppSecondaryText(
                text = "照片暂时无法显示",
                modifier = Modifier.padding(dimensions.spacingMd)
            )
        }
    }
}

@Composable
internal fun CapsuleEditablePhotoGrid(
    paths: List<String>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BalancedPhotoGrid(
        items = paths,
        modifier = modifier,
        itemKey = { it }
    ) { path, tileSize ->
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            CapsulePhoto(
                path = path,
                description = "已选择的城市照片",
                compact = true,
                heightOverride = tileSize
            )
            AppIconButton(
                icon = AppIconName.CLOSE,
                contentDescription = "移除这张照片",
                onClick = { onRemove(path) },
                selected = true,
                compactVisual = true
            )
        }
    }
}

/** Photo-first detail layout: one 4:3-like hero followed by compact square tiles. */
@Composable
internal fun CapsuleDetailPhotoLayout(
    paths: List<String>,
    modifier: Modifier = Modifier
) {
    if (paths.isEmpty()) return
    if (capsuleDetailPhotoLayoutMode(paths.size) == CapsuleDetailPhotoLayoutMode.TWO_UP) {
        BalancedPhotoGrid(
            items = paths,
            modifier = modifier,
            maxColumns = 2,
            itemKey = { it }
        ) { path, tileSize ->
            CapsulePhoto(
                path = path,
                description = "城市记忆照片",
                compact = true,
                heightOverride = tileSize
            )
        }
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        CapsulePhoto(
            path = paths.first(),
            description = "城市记忆主照片"
        )
        val remaining = paths.drop(1)
        if (remaining.isNotEmpty()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            BalancedPhotoGrid(items = remaining, itemKey = { it }) { path, tileSize ->
                CapsulePhoto(
                    path = path,
                    description = "城市记忆照片",
                    compact = true,
                    heightOverride = tileSize
                )
            }
        }
    }
}

internal enum class CapsuleDetailPhotoLayoutMode { SINGLE_HERO, TWO_UP, HERO_WITH_GRID }

internal fun capsuleDetailPhotoLayoutMode(photoCount: Int): CapsuleDetailPhotoLayoutMode = when {
    photoCount <= 1 -> CapsuleDetailPhotoLayoutMode.SINGLE_HERO
    photoCount == 2 -> CapsuleDetailPhotoLayoutMode.TWO_UP
    else -> CapsuleDetailPhotoLayoutMode.HERO_WITH_GRID
}
