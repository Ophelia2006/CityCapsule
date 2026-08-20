package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
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
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppPageTitle(text = title)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            AppSecondaryText(text = subtitle)
        }
    }
}

/** Compact action bar used by detail and editor pages outside the root app shell. */
@Composable
fun AppActionTopBar(
    title: String,
    onLeadingClick: () -> Unit,
    leadingIcon: AppIconName = AppIconName.BACK,
    leadingDescription: String = "返回",
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionIcon: AppIconName? = null,
    actionDescription: String = actionLabel.orEmpty(),
    actionEnabled: Boolean = true,
    onTitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(
            icon = leadingIcon,
            contentDescription = leadingDescription,
            onClick = onLeadingClick
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = AppTheme.dimensions.spacingXs)
        ) {
            if (onTitleClick == null) {
                AppSectionTitle(title)
            } else {
                Box(
                    modifier = Modifier
                        .heightIn(min = AppTheme.dimensions.minTouchTarget)
                        .clickable(onClick = onTitleClick),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AppSectionTitle(title)
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                AppCaptionText(subtitle)
            }
        }
        when {
            actionIcon != null && onActionClick != null -> AppIconButton(
                icon = actionIcon,
                contentDescription = actionDescription,
                onClick = onActionClick,
                enabled = actionEnabled
            )
            !actionLabel.isNullOrBlank() && onActionClick != null -> Box(
                modifier = Modifier
                    .heightIn(min = AppTheme.dimensions.minTouchTarget)
                    .clickable(enabled = actionEnabled, onClick = onActionClick)
                    .padding(horizontal = AppTheme.dimensions.spacingXs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = if (actionEnabled) {
                        AppTheme.colors.primary
                    } else {
                        AppTheme.colors.disabledContent
                    },
                    style = AppTheme.typography.button
                )
            }
            else -> Spacer(Modifier.height(AppTheme.dimensions.minTouchTarget))
        }
    }
}
