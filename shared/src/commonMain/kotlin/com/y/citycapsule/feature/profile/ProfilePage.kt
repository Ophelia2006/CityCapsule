package com.y.citycapsule.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppAvatarPicker
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppProfileAvatar
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.onboarding.FeatureNoticeTone

@Page(AppRouteTable.PAGE_PROFILE, supportInLocal = true)
internal class ProfilePager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val profileRepository = LocalProfileRepository(storage)
        val onboardingRepository = OnboardingRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            ProfileScreen(
                navigator,
                profileRepository,
                onboardingRepository,
                themeHost
            )
        }
    }
}

@Composable
private fun ProfileScreen(
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    onboardingRepository: OnboardingRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember { mutableStateOf(ProfileUiState()) }
    val holder = remember(profileRepository, onboardingRepository) {
        ProfileStateHolder(
            profileRepository,
            onboardingRepository
        ) { uiState = it }
    }

    LaunchedEffect(holder) {
        holder.load()
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = "本地档案",
                subtitle = "昵称、头像、城市和简介均只保存在当前设备。"
            )
            uiState.notice?.let { notice ->
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppStatusMessage(
                    message = notice.message,
                    tone = notice.tone.toAppStatusTone()
                )
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (uiState.editing) {
                ProfileEditForm(uiState, holder)
            } else {
                ProfileSummary(uiState)
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (uiState.editing) {
                AppButton(
                    text = "保存档案",
                    onClick = holder::save,
                    enabled = !uiState.isBusy,
                    loading = uiState.status == ProfileUiStatus.SAVING,
                    loadingText = "正在保存…"
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "取消修改",
                    onClick = holder::cancelEditing,
                    variant = AppButtonVariant.TEXT,
                    enabled = !uiState.isBusy
                )
            } else {
                AppButton(
                    text = "编辑档案",
                    onClick = holder::beginEditing,
                    enabled = !uiState.isBusy
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "清除本地档案",
                    onClick = holder::requestClear,
                    variant = AppButtonVariant.DANGER,
                    enabled = !uiState.isBusy,
                    loading = uiState.status == ProfileUiStatus.CLEARING
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "返回上一页",
                    onClick = navigator::back,
                    variant = AppButtonVariant.TEXT,
                    enabled = !uiState.isBusy
                )
            }
        }

        if (uiState.showClearConfirmation) {
            AppConfirmDialog(
                title = "清除本地档案？",
                message = "昵称、头像、城市、简介和首次引导状态都会被清除。此操作无法撤销。",
                confirmText = "确认清除",
                onConfirm = {
                    holder.clear {
                        navigator.replace(AppRoute.Onboarding)
                    }
                },
                onDismiss = holder::dismissClear
            )
        }
    }
}

@Composable
private fun ProfileSummary(state: ProfileUiState) {
    AppSection(
        title = "档案预览",
        description = "这是后续城市记录页面使用的本地身份。"
    ) {
        AppProfileAvatar(preset = state.profile.avatarPreset)
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppBodyText(text = state.profile.displayName)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(
            text = state.profile.homeCity?.let { "常驻城市：$it" } ?: "未填写常驻城市"
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(text = state.profile.bio ?: "未填写个人简介")
    }
}

@Composable
private fun ProfileEditForm(
    state: ProfileUiState,
    holder: ProfileStateHolder
) {
    AppSection(
        title = "编辑档案",
        description = "保存失败时修改会保留在当前页面。"
    ) {
        AppTextField(
            value = state.profile.displayName,
            onValueChange = holder::updateDisplayName,
            label = "昵称",
            placeholder = "例如：城市漫游者",
            errorMessage = state.validationMessage,
            maxLength = LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppAvatarPicker(
            selected = state.profile.avatarPreset,
            onSelected = holder::updateAvatar,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.profile.homeCity.orEmpty(),
            onValueChange = holder::updateHomeCity,
            label = "常驻城市",
            placeholder = "例如：上海",
            maxLength = LocalProfileValidator.HOME_CITY_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.profile.bio.orEmpty(),
            onValueChange = holder::updateBio,
            label = "个人简介",
            placeholder = "写下一句关于你的城市观察",
            errorMessage = state.validationMessage,
            maxLength = LocalProfileValidator.BIO_MAX_LENGTH,
            maxLines = 4,
            enabled = !state.isBusy
        )
    }
}

private fun FeatureNoticeTone.toAppStatusTone(): AppStatusTone = when (this) {
    FeatureNoticeTone.NEUTRAL -> AppStatusTone.NEUTRAL
    FeatureNoticeTone.SUCCESS -> AppStatusTone.SUCCESS
    FeatureNoticeTone.WARNING -> AppStatusTone.WARNING
    FeatureNoticeTone.ERROR -> AppStatusTone.ERROR
}
