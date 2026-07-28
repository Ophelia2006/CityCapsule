package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
private fun AppStateContent(icon: AppIconName, title: String, message: String, modifier: Modifier, actionLabel: String?, onAction: (() -> Unit)?) {
    Column(modifier.fillMaxWidth().padding(vertical = AppTheme.dimensions.spacingXxl), horizontalAlignment = Alignment.CenterHorizontally) {
        AppIcon(icon, title, tint = AppTheme.colors.primary, size = AppTheme.dimensions.iconXl)
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppSectionTitle(title)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(message)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
            AppButton(actionLabel, onAction, variant = AppButtonVariant.SECONDARY)
        }
    }
}

@Composable fun EmptyState(title: String, message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
    AppStateContent(AppIconName.PHOTO, title, message, modifier, actionLabel, onAction)

@Composable fun LoadingState(message: String = "正在加载…", modifier: Modifier = Modifier) =
    AppStateContent(AppIconName.MORE, "请稍候", message, modifier, null, null)

@Composable fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) =
    AppStateContent(AppIconName.RETRY, "暂时无法加载", message, modifier, if (onRetry != null) "重试" else null, onRetry)
