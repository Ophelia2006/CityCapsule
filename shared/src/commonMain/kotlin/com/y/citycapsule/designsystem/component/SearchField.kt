package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索地点、街区…",
    enabled: Boolean = true
) {
    Box(modifier.fillMaxWidth()) {
        AppTextField(
            value = value,
            onValueChange = onValueChange,
            label = "搜索",
            placeholder = placeholder,
            enabled = enabled
        )
        AppIcon(
            name = AppIconName.SEARCH,
            contentDescription = "搜索",
            modifier = Modifier.padding(top = AppTheme.dimensions.spacingXl, start = AppTheme.dimensions.spacingSm),
            tint = AppTheme.colors.textSecondary
        )
    }
}
