package com.y.citycapsule.designsystem.component

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.y.citycapsule.designsystem.theme.AppTheme

@Composable
fun AppDisplayText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.display,
        textAlign = textAlign
    )
}

@Composable
fun AppPageTitle(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.pageTitle,
        textAlign = textAlign
    )
}

@Composable
fun AppSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.sectionTitle
    )
}

@Composable
fun AppBodyText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textPrimary,
        style = AppTheme.typography.body,
        textAlign = textAlign
    )
}

@Composable
fun AppSecondaryText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textSecondary,
        style = AppTheme.typography.bodySecondary,
        textAlign = textAlign
    )
}

@Composable
fun AppCaptionText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        color = AppTheme.colors.textSecondary,
        style = AppTheme.typography.caption
    )
}
