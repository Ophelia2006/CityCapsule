package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.y.citycapsule.TextField
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    errorMessage: String? = null,
    maxLength: Int? = null,
    maxLines: Int = 1,
    enabled: Boolean = true
) {
    var focused by remember { mutableStateOf(false) }
    val colors = AppTheme.colors
    val dimensions = AppTheme.dimensions
    val borderColor = when {
        !errorMessage.isNullOrBlank() -> colors.error
        focused -> colors.primary
        else -> colors.divider
    }
    val contentColor = if (enabled) colors.textPrimary else colors.disabledContent

    Column(modifier = modifier.fillMaxWidth()) {
        AppBodyText(text = label)
        Spacer(Modifier.height(dimensions.spacingXs))
        TextField(
            value = value,
            placeholder = placeholder,
            autoFocus = false,
            onValueChange = { candidate ->
                val normalized = if (maxLines == 1) {
                    candidate.replace("\n", "")
                } else {
                    candidate
                }
                if (enabled && (maxLength == null || normalized.length <= maxLength)) {
                    onValueChange(normalized)
                }
            },
            onFocus = { focused = true },
            onBlur = { focused = false },
            textStyle = AppTheme.typography.body.copy(color = contentColor),
            placeholderColor = colors.textSecondary,
            cursorBrush = null,
            maxLines = maxLines,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensions.minTouchTarget)
                .clip(RoundedCornerShape(dimensions.radiusMd))
                .background(
                    if (enabled) colors.surfaceVariant else colors.disabledSurface
                )
                .border(
                    width = dimensions.strokeThin,
                    color = borderColor,
                    shape = RoundedCornerShape(dimensions.radiusMd)
                )
                .padding(
                    horizontal = dimensions.spacingSm,
                    vertical = dimensions.spacingSm
                )
        )
        val footer = when {
            !errorMessage.isNullOrBlank() -> errorMessage
            !supportingText.isNullOrBlank() -> supportingText
            else -> null
        }
        if (footer != null) {
            Spacer(Modifier.height(dimensions.spacingXxs))
            Text(
                text = footer,
                color = if (!errorMessage.isNullOrBlank()) {
                    colors.error
                } else {
                    colors.textSecondary
                },
                style = AppTheme.typography.caption
            )
        }
        if (maxLength != null) {
            Spacer(Modifier.height(dimensions.spacingXxs))
            AppCaptionText(text = "${value.length}/$maxLength")
        }
    }
}
