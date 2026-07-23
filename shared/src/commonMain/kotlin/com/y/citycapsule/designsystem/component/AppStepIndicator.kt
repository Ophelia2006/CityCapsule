package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppStepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    val safeTotal = totalSteps.coerceAtLeast(1)
    val safeCurrent = currentStep.coerceIn(1, safeTotal)

    Column(modifier = modifier.fillMaxWidth()) {
        AppCaptionText(text = "步骤 $safeCurrent / $safeTotal")
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(safeTotal) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppTheme.dimensions.spacingXxs)
                        .height(AppTheme.dimensions.spacingXxs)
                        .clip(RoundedCornerShape(AppTheme.dimensions.radiusSm))
                        .background(
                            if (index < safeCurrent) {
                                AppTheme.colors.primary
                            } else {
                                AppTheme.colors.divider
                            }
                        )
                )
            }
        }
    }
}
