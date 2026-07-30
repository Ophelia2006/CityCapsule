package com.y.citycapsule

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
import com.y.citycapsule.app.theme.AppThemeRuntime
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.onboarding.OnboardingRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.storage.ThemeModeSnapshot
import com.y.citycapsule.core.storage.ThemeModeSource
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppSettingsRow
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppThemeSelector
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.profile.ProfileFeatureRuntime

@Page(AppRouteTable.PAGE_SETTINGS, supportInLocal = true)
internal class SettingsPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val settingsRepository = SettingsRepository(storage)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            SettingsScreen(
                navigator,
                settingsRepository,
                OnboardingRepository(storage),
                themeHost
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    navigator: AppNavigator,
    settingsRepository: SettingsRepository,
    onboardingRepository: OnboardingRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var isSaving by remember { mutableStateOf(false) }
    var storageStatus by remember {
        mutableStateOf(
            SettingsStatus("正在读取主题偏好…", AppStatusTone.NEUTRAL)
        )
    }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var isClearingProfile by remember { mutableStateOf(false) }
    var clearStatus by remember { mutableStateOf<SettingsStatus?>(null) }

    fun applySnapshot(snapshot: ThemeModeSnapshot) {
        AppThemeRuntime.applyPersistedMode(snapshot.mode)
        storageStatus = when (snapshot.source) {
            ThemeModeSource.PERSISTED -> SettingsStatus(
                "当前偏好：${snapshot.mode.displayName()}",
                AppStatusTone.NEUTRAL
            )
            ThemeModeSource.DEFAULT_MISSING -> SettingsStatus(
                "尚未保存偏好，当前跟随系统",
                AppStatusTone.NEUTRAL
            )
            ThemeModeSource.DEFAULT_RECOVERY -> SettingsStatus(
                "存储暂不可用，已安全回退为跟随系统",
                AppStatusTone.WARNING
            )
        }
    }

    fun selectTheme(targetMode: ThemeMode) {
        if (isSaving || targetMode == AppThemeRuntime.themeMode) {
            return
        }

        val previousMode = AppThemeRuntime.themeMode
        AppThemeRuntime.previewMode(targetMode)
        isSaving = true
        storageStatus = SettingsStatus(
            "正在保存 ${targetMode.displayName()}…",
            AppStatusTone.NEUTRAL
        )
        settingsRepository.setThemeMode(targetMode) { writeResult ->
            isSaving = false
            when (writeResult) {
                is StorageResult.Success -> {
                    AppThemeRuntime.applyPersistedMode(targetMode)
                    storageStatus = SettingsStatus(
                        "已保存：${targetMode.displayName()}",
                        AppStatusTone.SUCCESS
                    )
                }
                StorageResult.Missing -> {
                    AppThemeRuntime.rollbackMode(previousMode)
                    storageStatus = SettingsStatus(
                        "保存结果未确认，已恢复 ${previousMode.displayName()}",
                        AppStatusTone.ERROR
                    )
                }
                is StorageResult.Failure -> {
                    AppThemeRuntime.rollbackMode(previousMode)
                    storageStatus = SettingsStatus(
                        "暂时无法保存，已恢复 ${previousMode.displayName()}",
                        AppStatusTone.ERROR
                    )
                }
            }
        }
    }

    LaunchedEffect(settingsRepository) {
        settingsRepository.getThemeModeSnapshot(::applySnapshot)
    }

    RuntimeAppTheme(themeHost = themeHost) {
        val dimensions = AppTheme.dimensions
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppActionTopBar(
                title = "设置",
                subtitle = "调整城市胶囊在当前设备上的使用体验。",
                onLeadingClick = navigator::back
            )
            Spacer(Modifier.height(dimensions.spacingLg))
            AppSection(
                title = "主题偏好",
                description = "选择后立即预览；保存失败会自动恢复原主题。"
            ) {
                AppSettingsRow(
                    title = "显示模式",
                    description = "跟随系统、浅色或深色"
                ) {
                    AppThemeSelector(
                        selectedMode = AppThemeRuntime.themeMode,
                        enabled = !isSaving,
                        onModeSelected = ::selectTheme
                    )
                }
                Spacer(Modifier.height(dimensions.spacingMd))
                AppStatusMessage(
                    message = storageStatus.message,
                    tone = storageStatus.tone
                )
            }
            Spacer(Modifier.height(dimensions.spacingLg))
            AppSection(
                title = "首次引导",
                description = "重新查看引导不会清除已有的本地档案。"
            ) {
                AppButton(
                    text = "重新查看首次引导",
                    variant = AppButtonVariant.SECONDARY,
                    onClick = { navigator.navigate(AppRoute.Onboarding) }
                )
            }
            Spacer(Modifier.height(dimensions.spacingLg))
            AppSection(
                title = "危险操作",
                description = "这里只清除个人档案和首次引导状态，不会删除地点、想去清单或城市记忆。"
            ) {
                AppButton(
                    text = "清除本地档案",
                    variant = AppButtonVariant.DANGER,
                    enabled = !isClearingProfile,
                    loading = isClearingProfile,
                    loadingText = "正在清除…",
                    onClick = { showClearConfirmation = true }
                )
                clearStatus?.let { status ->
                    Spacer(Modifier.height(dimensions.spacingMd))
                    AppStatusMessage(message = status.message, tone = status.tone)
                }
            }
        }

        if (showClearConfirmation) {
            AppConfirmDialog(
                title = "清除本地档案？",
                message = "昵称、头像、常驻城市、简介和首次引导状态会被清除；地点、想去清单和城市记忆会保留。此操作无法撤销。",
                confirmText = "确认清除",
                onConfirm = {
                    showClearConfirmation = false
                    isClearingProfile = true
                    clearStatus = SettingsStatus(
                        "正在清除本地档案…",
                        AppStatusTone.NEUTRAL
                    )
                    onboardingRepository.resetLocalState { result ->
                        isClearingProfile = false
                        when (result) {
                            is StorageResult.Success -> {
                                ProfileFeatureRuntime.invalidate()
                                navigator.replace(AppRoute.Onboarding)
                            }
                            StorageResult.Missing,
                            is StorageResult.Failure -> {
                                clearStatus = SettingsStatus(
                                    "未能完整清除本地档案，请重试。",
                                    AppStatusTone.ERROR
                                )
                            }
                        }
                    }
                },
                onDismiss = { showClearConfirmation = false }
            )
        }
    }
}

private data class SettingsStatus(
    val message: String,
    val tone: AppStatusTone
)

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}
