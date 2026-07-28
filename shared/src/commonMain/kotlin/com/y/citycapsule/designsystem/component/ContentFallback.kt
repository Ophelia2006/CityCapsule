package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.designsystem.theme.AppTheme

enum class PlaceFallbackKind(val mark: String) {
    LANDMARK("△"), CULTURE("◎"), FOOD("◒"), NATURE("⌁"), SHOPPING("◇"), OTHER("·")
}

/** Code-native category fallback; it is not photography and never claims to be one. */
@Composable
fun PlaceMediaFallback(kind: PlaceFallbackKind, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(AppTheme.colors.surfaceVariant), contentAlignment = Alignment.Center) {
        Text(kind.mark, color = AppTheme.colors.primary, style = AppTheme.typography.display)
    }
}
