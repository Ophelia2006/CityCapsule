package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
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
import com.tencent.kuikly.compose.material3.Text
import com.y.citycapsule.designsystem.theme.AppTheme

data class AppMenuItem(
    val id: String,
    val label: String,
    val destructive: Boolean = false,
    val enabled: Boolean = true
)

@Composable
fun AppOverflowMenu(
    expanded: Boolean,
    items: List<AppMenuItem>,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!expanded) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, scrimColor = AppTheme.colors.scrim)) {
        Box(Modifier.fillMaxSize().padding(AppTheme.dimensions.screenHorizontalPadding), contentAlignment = Alignment.CenterEnd) {
            AppCard(Modifier.widthIn(max = AppTheme.dimensions.contentMaxWidth)) {
                items.forEachIndexed { index, item ->
                    Box(Modifier.fillMaxWidth().clickable(enabled = item.enabled) { onSelected(item.id) }.padding(AppTheme.dimensions.spacingMd)) {
                        Text(
                            item.label,
                            color = if (item.destructive) AppTheme.colors.error else if (item.enabled) AppTheme.colors.textPrimary else AppTheme.colors.disabledContent,
                            style = AppTheme.typography.body
                        )
                    }
                    if (index < items.lastIndex) AppDivider()
                }
            }
        }
    }
}

@Composable
fun AppBottomSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, scrimColor = AppTheme.colors.scrim)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AppCard(Modifier.fillMaxWidth().widthIn(max = AppTheme.dimensions.contentMaxWidth)) {
                AppSectionTitle(title)
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                Column { content() }
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton("关闭", onDismiss, variant = AppButtonVariant.TEXT)
            }
        }
    }
}
