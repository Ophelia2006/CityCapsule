package com.y.citycapsule.feature.onboarding

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
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.onboarding.OnboardingStep
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppAvatarPicker
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppProfileAvatar
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppStepIndicator
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_ONBOARDING, supportInLocal = true)
internal class OnboardingPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val repository = OnboardingRepository(KuiklyKeyValueStore(this))
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            OnboardingScreen(navigator, repository, themeHost)
        }
    }
}

@Composable
private fun OnboardingScreen(
    navigator: AppNavigator,
    repository: OnboardingRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember { mutableStateOf(OnboardingUiState()) }
    val holder = remember(repository) {
        OnboardingStateHolder(repository) { uiState = it }
    }

    LaunchedEffect(holder) {
        holder.load()
    }

    fun finishOnboarding() {
        navigator.backTo(AppRouteKey.HOME)
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = "首次引导",
                subtitle = "档案只保存在当前设备，你可以稍后继续修改。"
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            AppStepIndicator(
                currentStep = uiState.draft.currentStep.ordinal + 1,
                totalSteps = OnboardingStep.entries.size
            )
            uiState.notice?.let { notice ->
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppStatusMessage(
                    message = notice.message,
                    tone = notice.tone.toAppStatusTone()
                )
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            when (uiState.draft.currentStep) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    state = uiState,
                    onStart = holder::next,
                    onUseDefault = {
                        holder.useDefaultProfile { finishOnboarding() }
                    },
                    onBack = navigator::back
                )
                OnboardingStep.IDENTITY -> IdentityStep(
                    state = uiState,
                    holder = holder,
                    onBack = {
                        if (!holder.previous()) {
                            navigator.back()
                        }
                    }
                )
                OnboardingStep.DETAILS -> DetailsStep(
                    state = uiState,
                    holder = holder
                )
                OnboardingStep.REVIEW -> ReviewStep(
                    state = uiState,
                    holder = holder,
                    onComplete = {
                        holder.complete { finishOnboarding() }
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    state: OnboardingUiState,
    onStart: () -> Unit,
    onUseDefault: () -> Unit,
    onBack: () -> Unit
) {
    AppSection(
        title = "欢迎来到城市胶囊",
        description = "先创建一份轻量本地档案，之后记录城市故事时会更有归属感。"
    ) {
        AppBodyText(text = "本阶段不会创建账号，也不会上传昵称、城市或简介。")
        Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
        AppSecondaryText(text = "预设头像由 shared 直接渲染，不访问相册和文件。")
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppButton(
        text = "开始设置档案",
        onClick = onStart,
        enabled = !state.isBusy,
        loading = state.status == OnboardingUiStatus.SAVING_DRAFT
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    AppButton(
        text = "使用默认档案并开始",
        onClick = onUseDefault,
        variant = AppButtonVariant.SECONDARY,
        enabled = !state.isBusy,
        loading = state.status == OnboardingUiStatus.SUBMITTING
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    AppButton(
        text = "返回上一页",
        onClick = onBack,
        variant = AppButtonVariant.TEXT,
        enabled = !state.isBusy
    )
}

@Composable
private fun IdentityStep(
    state: OnboardingUiState,
    holder: OnboardingStateHolder,
    onBack: () -> Unit
) {
    AppSection(
        title = "怎么称呼你",
        description = "昵称必填，头像从四种本地预设中选择。"
    ) {
        AppTextField(
            value = state.draft.displayName,
            onValueChange = holder::updateDisplayName,
            label = "昵称",
            placeholder = "例如：城市漫游者",
            errorMessage = state.validationMessage,
            maxLength = LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppAvatarPicker(
            selected = state.draft.avatarPreset,
            onSelected = holder::updateAvatar,
            enabled = !state.isBusy
        )
    }
    StepButtons(
        state = state,
        nextText = "下一步",
        onNext = holder::next,
        onBack = onBack
    )
}

@Composable
private fun DetailsStep(
    state: OnboardingUiState,
    holder: OnboardingStateHolder
) {
    AppSection(
        title = "补充一点介绍",
        description = "城市和简介都是可选项，可以留空后继续。"
    ) {
        AppTextField(
            value = state.draft.homeCity.orEmpty(),
            onValueChange = holder::updateHomeCity,
            label = "常驻城市",
            placeholder = "例如：上海",
            maxLength = LocalProfileValidator.HOME_CITY_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.draft.bio.orEmpty(),
            onValueChange = holder::updateBio,
            label = "个人简介",
            placeholder = "写下一句关于你的城市观察",
            errorMessage = state.validationMessage,
            maxLength = LocalProfileValidator.BIO_MAX_LENGTH,
            maxLines = 4,
            enabled = !state.isBusy
        )
    }
    StepButtons(
        state = state,
        nextText = "预览档案",
        onNext = holder::next,
        onBack = { holder.previous() }
    )
}

@Composable
private fun ReviewStep(
    state: OnboardingUiState,
    holder: OnboardingStateHolder,
    onComplete: () -> Unit
) {
    AppSection(
        title = "确认本地档案",
        description = "完成后仍可从首页或设置进入档案页修改。"
    ) {
        AppProfileAvatar(preset = state.draft.avatarPreset)
        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
        AppBodyText(text = state.draft.displayName)
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(
            text = state.draft.homeCity?.let { "常驻城市：$it" } ?: "未填写常驻城市"
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppSecondaryText(
            text = state.draft.bio ?: "未填写个人简介"
        )
        state.validationMessage?.let {
            Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
            AppStatusMessage(message = it, tone = AppStatusTone.ERROR)
        }
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppButton(
        text = "保存并进入首页",
        onClick = onComplete,
        enabled = !state.isBusy,
        loading = state.status == OnboardingUiStatus.SUBMITTING,
        loadingText = "正在保存档案…"
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    AppButton(
        text = "返回修改",
        onClick = { holder.previous() },
        variant = AppButtonVariant.TEXT,
        enabled = !state.isBusy
    )
}

@Composable
private fun StepButtons(
    state: OnboardingUiState,
    nextText: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppButton(
        text = nextText,
        onClick = onNext,
        enabled = !state.isBusy,
        loading = state.status == OnboardingUiStatus.SAVING_DRAFT,
        loadingText = "正在保存进度…"
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    AppButton(
        text = "上一步",
        onClick = onBack,
        variant = AppButtonVariant.TEXT,
        enabled = !state.isBusy
    )
}

private fun FeatureNoticeTone.toAppStatusTone(): AppStatusTone = when (this) {
    FeatureNoticeTone.NEUTRAL -> AppStatusTone.NEUTRAL
    FeatureNoticeTone.SUCCESS -> AppStatusTone.SUCCESS
    FeatureNoticeTone.WARNING -> AppStatusTone.WARNING
    FeatureNoticeTone.ERROR -> AppStatusTone.ERROR
}
