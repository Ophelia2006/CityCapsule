package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmVariant: AppButtonVariant = AppButtonVariant.DANGER,
    dismissText: String = "取消"
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            scrimColor = AppTheme.colors.scrim
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppTheme.dimensions.screenHorizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            AppCard(
                modifier = Modifier
                    .widthIn(max = AppTheme.dimensions.contentMaxWidth)
                    .fillMaxWidth()
            ) {
                AppSectionTitle(text = title)
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppSecondaryText(text = message)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppButton(
                    text = confirmText,
                    variant = confirmVariant,
                    onClick = onConfirm
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = dismissText,
                    variant = AppButtonVariant.TEXT,
                    onClick = onDismiss
                )
            }
        }
    }
}
