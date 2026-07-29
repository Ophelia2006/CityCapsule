package com.y.citycapsule

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

/** Explore root content hosted inside the single AppShellPage. */
@Composable
internal fun HomeRootContent(
    navigator: AppNavigator,
    statusBarHeight: Float,
    listState: LazyListState
) {
    val dimensions = AppTheme.dimensions
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = dimensions.screenHorizontalPadding,
            top = statusBarHeight.dp + dimensions.spacingXxl,
            end = dimensions.screenHorizontalPadding,
            bottom = dimensions.spacingXl
        )
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppTopBar(
                    title = "探索城市",
                    subtitle = "从一个真实地点开始，留下属于你的城市记忆。"
                )
                Spacer(Modifier.height(dimensions.spacingXxl))
                AppSectionTitle(text = "今天想去哪里？")
                Spacer(Modifier.height(dimensions.spacingXs))
                AppBodyText(text = "浏览本地地点目录，找到下一处值得走走的地方。")
                Spacer(Modifier.height(dimensions.spacingLg))
                AppButton(
                    text = "探索地点",
                    onClick = { navigator.navigate(AppRoute.PlaceList) }
                )
                Spacer(Modifier.height(dimensions.spacingSm))
                AppButton(
                    text = "想去",
                    variant = AppButtonVariant.SECONDARY,
                    onClick = { navigator.navigate(AppRoute.Favorites) }
                )
            }
        }
    }
}
