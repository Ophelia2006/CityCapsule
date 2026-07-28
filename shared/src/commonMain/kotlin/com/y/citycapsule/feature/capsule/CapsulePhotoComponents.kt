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
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
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
    compact: Boolean = false
) {
    val dimensions = AppTheme.dimensions
    val height = if (compact) dimensions.mediaThumbnailSize else dimensions.mediaPreviewHeight
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
