package com.y.citycapsule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.AppThemeRuntime
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.backup.DataBackupRepository
import com.y.citycapsule.core.backup.KuiklyDataArchiveCapability
import com.y.citycapsule.core.media.KuiklyManagedMediaFiles
import com.y.citycapsule.core.media.KuiklyMediaMaintenance
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.capsule.RepositoryMediaMaintenance
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppDivider
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppSettingsRow
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppThemeSelector
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.capsule.CapsuleFeatureRuntime
import com.y.citycapsule.feature.place.PlaceFeatureRuntime
import com.y.citycapsule.feature.profile.ProfileFeatureRuntime
import com.y.citycapsule.feature.settings.SettingsEffect
import com.y.citycapsule.feature.settings.SettingsIntent
import com.y.citycapsule.feature.settings.SettingsNoticeTone
import com.y.citycapsule.feature.settings.SettingsOperation
import com.y.citycapsule.feature.settings.SettingsStore
import com.y.citycapsule.feature.settings.SettingsUiState
import com.y.citycapsule.feature.settings.formatBytes
import kotlinx.coroutines.flow.collect

@Page(AppRouteTable.PAGE_SETTINGS, supportInLocal = true)
internal class SettingsPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            val scope = rememberCoroutineScope()
            val store = remember {
                val mediaMaintenance = KuiklyMediaMaintenance(this)
                SettingsStore(
                    settingsRepository = SettingsRepository(storage),
                    backupRepository = DataBackupRepository(storage),
                    archive = KuiklyDataArchiveCapability(this),
                    mediaFiles = KuiklyManagedMediaFiles(this),
                    mediaMaintenance = mediaMaintenance,
                    repositoryMediaMaintenance = RepositoryMediaMaintenance(
                        LocalCapsuleRepository(storage), mediaMaintenance
                    ),
                    parentScope = scope
                )
            }
            val state by store.state.collectAsState()
            DisposableEffect(store) { onDispose(store::dispose) }
            LaunchedEffect(store) {
                store.effects.collect { effect ->
                    when (effect) {
                        SettingsEffect.NavigateBack -> navigator.back()
                        SettingsEffect.NavigateOnboarding ->
                            navigator.navigate(AppRoute.Onboarding)
                        is SettingsEffect.PreviewTheme ->
                            AppThemeRuntime.previewMode(effect.mode)
                        is SettingsEffect.CommitTheme ->
                            AppThemeRuntime.applyPersistedMode(effect.mode)
                        is SettingsEffect.RollbackTheme ->
                            AppThemeRuntime.rollbackMode(effect.mode)
                        SettingsEffect.DataImported -> {
                            ProfileFeatureRuntime.invalidate()
                            PlaceFeatureRuntime.invalidate()
                            CapsuleFeatureRuntime.invalidate()
                        }
                    }
                }
            }
            LaunchedEffect(store) { store.dispatch(SettingsIntent.Load) }
            RuntimeAppTheme(themeHost = themeHost) {
                SettingsScreen(state, store::dispatch)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    dispatch: (SettingsIntent) -> Unit
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    val d = AppTheme.dimensions
    AppScaffold(statusBarHeight = statusBarHeight) {
        AppActionTopBar(
            title = "设置",
            subtitle = "管理这台设备上的显示与城市数据",
            onLeadingClick = { dispatch(SettingsIntent.BackClicked) }
        )
        Spacer(Modifier.height(d.spacingXl))
        AppSectionTitle("显示")
        Spacer(Modifier.height(d.spacingMd))
        AppSettingsRow(
            title = "主题",
            description = "跟随系统、浅色或深色"
        ) {
            AppThemeSelector(
                selectedMode = state.themeMode,
                enabled = !state.busy,
                onModeSelected = { dispatch(SettingsIntent.ThemeSelected(it)) }
            )
        }
        SectionDivider()

        AppSectionTitle("数据与存储")
        Spacer(Modifier.height(d.spacingMd))
        AppSettingsRow(
            title = "存储占用",
            description = "共约 ${formatBytes(state.totalBytesApprox)}\n" +
                "原图 ${state.mediaUsage.originalCount} 张 · ${formatBytes(state.mediaUsage.originalBytes)}\n" +
                "缩略图 ${state.mediaUsage.thumbnailCount} 张 · ${formatBytes(state.mediaUsage.thumbnailBytes)}\n" +
                "备份 ${state.platformUsage.recoveryCount} 个 · ${formatBytes(state.platformUsage.recoveryBytes)}\n" +
                "缓存 ${state.platformUsage.cacheCount} 个 · ${formatBytes(state.platformUsage.cacheBytes)}"
        )
        RowAction(
            title = "清理缩略图",
            description = "仅删除可按需重新生成的缩略图，不影响原图和城市记忆",
            enabled = !state.busy
        ) { dispatch(SettingsIntent.ClearThumbnailsClicked) }
        RowAction(
            title = "清理无引用媒体",
            description = "核对已发布记忆和草稿后，清理超过宽限时间且不再被引用的托管照片",
            enabled = !state.busy
        ) { dispatch(SettingsIntent.CleanupMediaClicked) }
        RowAction(
            title = "清理缓存",
            description = "删除未发布草稿与临时导入文件，不删除已发布的城市记忆",
            enabled = !state.busy
        ) { dispatch(SettingsIntent.ClearCacheClicked) }
        RowAction(
            title = "导出备份",
            description = "导出档案、地点、想去、城市碎片和已引用照片",
            enabled = !state.busy
        ) { dispatch(SettingsIntent.ExportClicked) }
        RowAction(
            title = "从备份导入",
            description = "校验并预览后才会写入；导入前自动保留恢复包",
            enabled = !state.busy
        ) { dispatch(SettingsIntent.ImportClicked) }
        if (state.operation != SettingsOperation.NONE) {
            Spacer(Modifier.height(d.spacingSm))
            AppStatusMessage(
                message = operationMessage(state.operation),
                tone = AppStatusTone.NEUTRAL
            )
        }
        state.notice?.let {
            Spacer(Modifier.height(d.spacingSm))
            AppStatusMessage(message = it.message, tone = it.tone.toAppTone())
        }
        SectionDivider()

        AppSectionTitle("了解 CityCapsule")
        Spacer(Modifier.height(d.spacingMd))
        RowAction(
            title = "隐私",
            description = "查看本地数据、照片与系统选择器的使用方式"
        ) { dispatch(SettingsIntent.PrivacyClicked) }
        RowAction(
            title = "关于",
            description = "产品定位、当前能力与版本信息"
        ) { dispatch(SettingsIntent.AboutClicked) }
        RowAction(
            title = "重新查看首次引导",
            description = "不会清除已经保存的城市数据"
        ) { dispatch(SettingsIntent.OnboardingClicked) }
        Spacer(Modifier.height(d.spacingXl))
    }

    if (state.confirmClearCache) {
        AppConfirmDialog(
            title = "清理临时缓存？",
            message = "未发布的首次引导草稿、城市碎片草稿和临时导入文件会被删除。已发布记录、想去地点和照片不会被删除。",
            confirmText = "清理缓存",
            onConfirm = { dispatch(SettingsIntent.ClearCacheConfirmed) },
            onDismiss = { dispatch(SettingsIntent.DismissConfirmation) }
        )
    }
    if (state.confirmImport && state.preview != null) {
        val preview = state.preview
        AppConfirmDialog(
            title = "导入前预览",
            message = "${preview.fileName}\n\n" +
                "个人档案 ${preview.profileCount} · 地点 ${preview.placeCount}\n" +
                "想去 ${preview.favoriteCount} · 城市碎片 ${preview.capsuleCount}\n" +
                "照片 ${preview.photoCount}\n\n" +
                "确认后会先创建导入前备份，再替换当前数据。",
            confirmText = "备份并导入",
            onConfirm = { dispatch(SettingsIntent.ImportConfirmed) },
            onDismiss = { dispatch(SettingsIntent.CancelImport) }
        )
    }
    if (state.showPrivacy) {
        AppConfirmDialog(
            title = "隐私",
            message = "CityCapsule 当前不提供账号、云同步或社区功能。档案、地点、想去和城市碎片保存在本机；选择的照片会复制到应用沙箱。\n\n" +
                "应用只在你主动操作时打开系统照片或文件选择器。导出文件由你选择保存位置；卸载或清除应用数据可能删除未导出的本地内容。",
            confirmText = "知道了",
            onConfirm = { dispatch(SettingsIntent.CloseInfo) },
            onDismiss = { dispatch(SettingsIntent.CloseInfo) }
        )
    }
    if (state.showAbout) {
        AppConfirmDialog(
            title = "关于 CityCapsule",
            message = "城市胶囊是一款“城市探索 + 个人城市记录”工具，核心体验是发现、探索、记录与回忆。\n\n" +
                "当前版本：1.0.0\n数据方式：本地优先\n支持平台：Android / HarmonyOS",
            confirmText = "完成",
            onConfirm = { dispatch(SettingsIntent.CloseInfo) },
            onDismiss = { dispatch(SettingsIntent.CloseInfo) }
        )
    }
}

@Composable
private fun RowAction(
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    AppSettingsRow(
        title = title,
        description = description,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = AppTheme.dimensions.spacingMd)
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppDivider()
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
}

private fun operationMessage(operation: SettingsOperation): String = when (operation) {
    SettingsOperation.NONE -> ""
    SettingsOperation.LOADING -> "正在读取本地数据…"
    SettingsOperation.SAVING_THEME -> "正在保存主题…"
    SettingsOperation.CLEARING_CACHE -> "正在清理缓存…"
    SettingsOperation.CLEARING_THUMBNAILS -> "正在清理缩略图…"
    SettingsOperation.CLEANING_MEDIA -> "正在核对并清理无引用媒体…"
    SettingsOperation.EXPORTING -> "正在创建备份…"
    SettingsOperation.SELECTING_IMPORT -> "正在读取并校验备份…"
    SettingsOperation.IMPORTING -> "正在备份当前数据并导入…"
}

private fun SettingsNoticeTone.toAppTone(): AppStatusTone = when (this) {
    SettingsNoticeTone.NEUTRAL -> AppStatusTone.NEUTRAL
    SettingsNoticeTone.SUCCESS -> AppStatusTone.SUCCESS
    SettingsNoticeTone.WARNING -> AppStatusTone.WARNING
    SettingsNoticeTone.ERROR -> AppStatusTone.ERROR
}
