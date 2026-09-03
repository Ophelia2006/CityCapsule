package com.y.citycapsule.app.navigation

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxScope
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.y.citycapsule.designsystem.component.AppBottomNavigation
import com.y.citycapsule.designsystem.component.AppBottomNavigationItem
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.theme.AppTheme

private val rootNavigationItems = listOf(
    AppBottomNavigationItem(AppRootTab.EXPLORE.id, "探索", AppIconName.EXPLORE),
    AppBottomNavigationItem(AppRootTab.RECORD.id, "记录", AppIconName.RECORD),
    AppBottomNavigationItem(AppRootTab.ROAM.id, "漫游", AppIconName.ROAM),
    AppBottomNavigationItem(AppRootTab.PROFILE.id, "我的", AppIconName.PROFILE)
)

/** The single structural shell shared by all four product roots. */
@Composable
fun AppRootScaffold(
    selectedTab: AppRootTab,
    onTabSelected: (AppRootTab) -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppTheme.dimensions.adaptiveContentMaxWidth)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize(), content = content)
            AppBottomNavigation(
                items = rootNavigationItems,
                selectedId = selectedTab.id,
                onSelected = { selectedId ->
                    AppRootTab.fromId(selectedId)?.let(onTabSelected)
                }
            )
        }
    }
}
