package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxWithConstraints
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.semantics.paneTitle
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.y.citycapsule.designsystem.theme.AppTheme

/**
 * Shared adaptive list/detail surface. Compact windows show only [primary] and
 * keep route navigation as the detail experience. Map/info can reuse the same primitive.
 */
@Composable
fun AdaptivePane(
    primaryTitle: String,
    secondaryTitle: String,
    modifier: Modifier = Modifier,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit
) {
    val dimensions = AppTheme.dimensions
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth < dimensions.adaptiveGridBreakpoint) {
            Box(Modifier.fillMaxSize().semantics { paneTitle = primaryTitle }) {
                primary()
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier.width(dimensions.adaptivePrimaryPaneWidth)
                        .fillMaxSize()
                        .semantics { paneTitle = primaryTitle }
                ) {
                    primary()
                }
                Spacer(Modifier.width(dimensions.adaptivePaneGap))
                Box(
                    Modifier.weight(1f)
                        .fillMaxSize()
                        .semantics { paneTitle = secondaryTitle }
                ) {
                    secondary()
                }
            }
        }
    }
}
