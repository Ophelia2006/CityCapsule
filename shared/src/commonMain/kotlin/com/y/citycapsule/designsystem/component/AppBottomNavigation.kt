package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.role
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.semantics.stateDescription
import com.y.citycapsule.designsystem.theme.AppTheme

data class AppBottomNavigationItem(
    val id: String,
    val label: String,
    val icon: AppIconName
)

@Composable
fun AppBottomNavigation(
    items: List<AppBottomNavigationItem>,
    selectedId: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().background(AppTheme.colors.surface)) {
        AppDivider()
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = AppTheme.dimensions.bottomNavigationHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = item.id == selectedId
                Column(
                    modifier = Modifier.weight(1f)
                        .heightIn(min = AppTheme.dimensions.minTouchTarget)
                        .semantics {
                            contentDescription = item.label
                            role = Role.Tab
                            stateDescription = if (selected) "已选择" else "未选择"
                        }
                        .clickable { onSelected(item.id) }
                        .padding(vertical = AppTheme.dimensions.spacingXs),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIcon(item.icon, "", tint = if (selected) AppTheme.colors.primary else AppTheme.colors.textSecondary)
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                    Text(item.label, color = if (selected) AppTheme.colors.primary else AppTheme.colors.textSecondary, style = AppTheme.typography.caption)
                }
            }
        }
    }
}
