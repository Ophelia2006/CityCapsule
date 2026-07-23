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
import com.y.citycapsule.core.navigation.AppRouteKey
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.storage.ThemeModeSnapshot
import com.y.citycapsule.core.storage.ThemeModeSource
import com.y.citycapsule.core.theme.ThemeMode
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppSettingsRow
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppThemeSelector
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_SETTINGS, supportInLocal = true)
internal class SettingsPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val settingsRepository = SettingsRepository(KuiklyKeyValueStore(this))
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            SettingsScreen(navigator, settingsRepository, themeHost)
        }
    }
}

@Composable
private fun SettingsScreen(
    navigator: AppNavigator,
    settingsRepository: SettingsRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var isSaving by remember { mutableStateOf(false) }
    var storageStatus by remember {
        mutableStateOf(
            SettingsStatus("正在读取主题偏好…", AppStatusTone.NEUTRAL)
        )
    }

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
            AppTopBar(
                title = "设置",
                subtitle = "主题偏好由 shared 设计系统统一渲染并通过 MMKV 持久化。"
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
                title = "路由验收",
                description = "以下入口继续使用统一 AppNavigator。"
            ) {
                AppButton(
                    text = "返回首页",
                    variant = AppButtonVariant.SECONDARY,
                    onClick = { navigator.backTo(AppRouteKey.HOME) }
                )
                Spacer(Modifier.height(dimensions.spacingSm))
                AppButton(
                    text = "Push another Settings",
                    variant = AppButtonVariant.SECONDARY,
                    onClick = { navigator.navigate(AppRoute.Settings) }
                )
                Spacer(Modifier.height(dimensions.spacingSm))
                AppButton(
                    text = "返回上一页",
                    variant = AppButtonVariant.TEXT,
                    onClick = navigator::back
                )
            }
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
