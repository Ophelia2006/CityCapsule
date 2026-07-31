package com.y.citycapsule.feature.settings

import com.y.citycapsule.core.backup.ArchiveResult
import com.y.citycapsule.core.backup.BackupDataResult
import com.y.citycapsule.core.backup.BackupPreview
import com.y.citycapsule.core.backup.DataArchiveCapability
import com.y.citycapsule.core.backup.DataBackupRepository
import com.y.citycapsule.core.backup.ImportSelection
import com.y.citycapsule.core.backup.LocalStorageSnapshot
import com.y.citycapsule.core.backup.PlatformStorageUsage
import com.y.citycapsule.core.media.ManagedMediaDeleteResult
import com.y.citycapsule.core.media.ManagedMediaFileCapability
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.storage.SettingsRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.storage.ThemeModeSnapshot
import com.y.citycapsule.core.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class SettingsOperation { NONE, LOADING, SAVING_THEME, CLEARING_CACHE, EXPORTING, SELECTING_IMPORT, IMPORTING }
enum class SettingsNoticeTone { NEUTRAL, SUCCESS, WARNING, ERROR }

data class SettingsNotice(val message: String, val tone: SettingsNoticeTone)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val operation: SettingsOperation = SettingsOperation.LOADING,
    val structuredBytesApprox: Long = 0,
    val platformUsage: PlatformStorageUsage = PlatformStorageUsage(0, 0, 0),
    val preview: BackupPreview? = null,
    val notice: SettingsNotice? = null,
    val showPrivacy: Boolean = false,
    val showAbout: Boolean = false,
    val confirmClearCache: Boolean = false,
    val confirmImport: Boolean = false
) {
    val busy: Boolean get() = operation != SettingsOperation.NONE
    val totalBytesApprox: Long
        get() = structuredBytesApprox + platformUsage.mediaBytes +
            platformUsage.cacheBytes + platformUsage.recoveryBytes
}

sealed interface SettingsIntent {
    data object Load : SettingsIntent
    data class ThemeSelected(val mode: ThemeMode) : SettingsIntent
    data object PrivacyClicked : SettingsIntent
    data object AboutClicked : SettingsIntent
    data object CloseInfo : SettingsIntent
    data object ClearCacheClicked : SettingsIntent
    data object ClearCacheConfirmed : SettingsIntent
    data object DismissConfirmation : SettingsIntent
    data object ExportClicked : SettingsIntent
    data object ImportClicked : SettingsIntent
    data object ImportConfirmed : SettingsIntent
    data object CancelImport : SettingsIntent
    data object OnboardingClicked : SettingsIntent
    data object BackClicked : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object NavigateOnboarding : SettingsEffect
    data class PreviewTheme(val mode: ThemeMode) : SettingsEffect
    data class CommitTheme(val mode: ThemeMode) : SettingsEffect
    data class RollbackTheme(val mode: ThemeMode) : SettingsEffect
    data object DataImported : SettingsEffect
}

class SettingsStore(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: DataBackupRepository,
    private val archive: DataArchiveCapability,
    private val mediaFiles: ManagedMediaFileCapability,
    parentScope: CoroutineScope
) : MviStore<SettingsIntent, SettingsUiState, SettingsEffect> {
    private sealed interface Event {
        data class Intent(val value: SettingsIntent) : Event
        data class Loaded(
            val snapshot: LocalStorageSnapshot?,
            val usage: PlatformStorageUsage?,
            val message: String?
        ) : Event
        data class ThemeLoaded(val snapshot: ThemeModeSnapshot) : Event
        data class ThemeSaved(val previous: ThemeMode, val target: ThemeMode, val ok: Boolean) : Event
        data class CacheCleared(val bytes: Long, val warning: String?) : Event
        data class ExportSnapshot(val result: BackupDataResult<LocalStorageSnapshot>) : Event
        data class Exported(val result: ArchiveResult<String>) : Event
        data class ImportSelected(val result: ArchiveResult<ImportSelection>) : Event
        data class PreviewDecoded(val result: BackupDataResult<BackupPreview>) : Event
        data class ImportSnapshot(val result: BackupDataResult<LocalStorageSnapshot>) : Event
        data class RecoveryCreated(
            val old: LocalStorageSnapshot,
            val result: ArchiveResult<String>
        ) : Event
        data class MediaCommitted(
            val old: LocalStorageSnapshot,
            val result: ArchiveResult<com.y.citycapsule.core.backup.ImportedMedia>
        ) : Event
        data class DataWritten(
            val old: LocalStorageSnapshot,
            val createdPaths: List<String>,
            val result: BackupDataResult<Unit>
        ) : Event
        data class RollbackFinished(val deleted: Boolean, val restored: Boolean) : Event
    }

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val effectChannel = Channel<SettingsEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(SettingsUiState())
    private var disposed = false

    override val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()
    override val effects: Flow<SettingsEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch { for (event in events) handle(event) }
    }

    override fun dispatch(intent: SettingsIntent) {
        if (!disposed) events.trySend(Event.Intent(intent))
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        events.close()
        effectChannel.close()
        scope.cancel()
    }

    private suspend fun handle(event: Event) {
        when (event) {
            is Event.Intent -> handleIntent(event.value)
            is Event.ThemeLoaded -> {
                mutableState.value = mutableState.value.copy(themeMode = event.snapshot.mode)
                effectChannel.send(SettingsEffect.CommitTheme(event.snapshot.mode))
            }
            is Event.Loaded -> mutableState.value = mutableState.value.copy(
                operation = SettingsOperation.NONE,
                structuredBytesApprox = event.snapshot?.structuredBytesApprox ?: 0,
                platformUsage = event.usage ?: PlatformStorageUsage(0, 0, 0),
                notice = event.message?.let { SettingsNotice(it, SettingsNoticeTone.WARNING) }
            )
            is Event.ThemeSaved -> {
                mutableState.value = mutableState.value.copy(
                    operation = SettingsOperation.NONE,
                    themeMode = if (event.ok) event.target else event.previous,
                    notice = SettingsNotice(
                        if (event.ok) "主题设置已保存。" else "主题保存失败，已恢复原设置。",
                        if (event.ok) SettingsNoticeTone.SUCCESS else SettingsNoticeTone.ERROR
                    )
                )
                effectChannel.send(
                    if (event.ok) SettingsEffect.CommitTheme(event.target)
                    else SettingsEffect.RollbackTheme(event.previous)
                )
            }
            is Event.CacheCleared -> {
                mutableState.value = mutableState.value.copy(
                    operation = SettingsOperation.NONE,
                    confirmClearCache = false,
                    notice = SettingsNotice(
                        event.warning ?: "已清理 ${formatBytes(event.bytes)} 临时缓存。",
                        if (event.warning == null) SettingsNoticeTone.SUCCESS else SettingsNoticeTone.WARNING
                    )
                )
                load()
            }
            is Event.ExportSnapshot -> when (val result = event.result) {
                is BackupDataResult.Success -> archive.export(
                    result.value.payload, result.value.mediaPaths
                ) { enqueue(Event.Exported(it)) }
                is BackupDataResult.Failure -> finishFailure(result.message)
            }
            is Event.Exported -> when (val result = event.result) {
                is ArchiveResult.Success -> finishSuccess("备份已导出到所选位置。")
                ArchiveResult.Cancelled -> finishNeutral("已取消导出。")
                is ArchiveResult.Failure -> finishFailure(result.message)
                ArchiveResult.Unsupported -> finishFailure("当前平台暂不支持导出。")
            }
            is Event.ImportSelected -> handleImportSelection(event.result)
            is Event.PreviewDecoded -> when (val result = event.result) {
                is BackupDataResult.Success -> mutableState.value = mutableState.value.copy(
                    operation = SettingsOperation.NONE,
                    preview = result.value,
                    confirmImport = true,
                    notice = null
                )
                is BackupDataResult.Failure -> finishFailure(result.message)
            }
            is Event.ImportSnapshot -> when (val result = event.result) {
                is BackupDataResult.Success -> archive.createRecovery(
                    result.value.payload, result.value.mediaPaths
                ) { enqueue(Event.RecoveryCreated(result.value, it)) }
                is BackupDataResult.Failure -> finishFailure(result.message)
            }
            is Event.RecoveryCreated -> when (val result = event.result) {
                is ArchiveResult.Success -> archive.commitImportedMedia(
                    mutableState.value.preview?.sessionId.orEmpty()
                ) { enqueue(Event.MediaCommitted(event.old, it)) }
                is ArchiveResult.Failure -> finishFailure(result.message)
                else -> finishFailure("无法创建导入前备份，导入已停止。")
            }
            is Event.MediaCommitted -> handleCommittedMedia(event)
            is Event.DataWritten -> handleDataWritten(event)
            is Event.RollbackFinished -> {
                mutableState.value = mutableState.value.copy(
                    operation = SettingsOperation.NONE,
                    preview = null,
                    confirmImport = false,
                    notice = SettingsNotice(
                        if (event.restored && event.deleted) {
                            "导入失败，原数据已恢复，导入照片已清理。"
                        } else {
                            "导入失败且自动恢复不完整；导入前备份仍保留在应用内，请停止继续修改数据。"
                        },
                        SettingsNoticeTone.ERROR
                    )
                )
            }
        }
    }

    private suspend fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.Load -> load()
            is SettingsIntent.ThemeSelected -> saveTheme(intent.mode)
            SettingsIntent.PrivacyClicked -> mutableState.value =
                mutableState.value.copy(showPrivacy = true)
            SettingsIntent.AboutClicked -> mutableState.value =
                mutableState.value.copy(showAbout = true)
            SettingsIntent.CloseInfo -> mutableState.value =
                mutableState.value.copy(showPrivacy = false, showAbout = false)
            SettingsIntent.ClearCacheClicked -> mutableState.value =
                mutableState.value.copy(confirmClearCache = true)
            SettingsIntent.ClearCacheConfirmed -> clearCache()
            SettingsIntent.DismissConfirmation -> mutableState.value =
                mutableState.value.copy(confirmClearCache = false, confirmImport = false)
            SettingsIntent.ExportClicked -> {
                if (!mutableState.value.busy) {
                    mutableState.value = mutableState.value.copy(operation = SettingsOperation.EXPORTING)
                    backupRepository.snapshot { enqueue(Event.ExportSnapshot(it)) }
                }
            }
            SettingsIntent.ImportClicked -> {
                if (!mutableState.value.busy) {
                    mutableState.value = mutableState.value.copy(operation = SettingsOperation.SELECTING_IMPORT)
                    archive.selectImport { enqueue(Event.ImportSelected(it)) }
                }
            }
            SettingsIntent.ImportConfirmed -> {
                if (!mutableState.value.busy && mutableState.value.preview != null) {
                    mutableState.value = mutableState.value.copy(
                        operation = SettingsOperation.IMPORTING, confirmImport = false
                    )
                    backupRepository.snapshot { enqueue(Event.ImportSnapshot(it)) }
                }
            }
            SettingsIntent.CancelImport -> cancelImport()
            SettingsIntent.OnboardingClicked -> effectChannel.send(SettingsEffect.NavigateOnboarding)
            SettingsIntent.BackClicked -> effectChannel.send(SettingsEffect.NavigateBack)
        }
    }

    private fun load() {
        mutableState.value = mutableState.value.copy(operation = SettingsOperation.LOADING)
        settingsRepository.getThemeModeSnapshot { enqueue(Event.ThemeLoaded(it)) }
        var snapshot: LocalStorageSnapshot? = null
        var usage: PlatformStorageUsage? = null
        var snapshotDone = false
        var usageDone = false
        var warning: String? = null
        fun complete() {
            if (snapshotDone && usageDone) enqueue(Event.Loaded(snapshot, usage, warning))
        }
        backupRepository.snapshot {
            snapshotDone = true
            when (it) {
                is BackupDataResult.Success -> snapshot = it.value
                is BackupDataResult.Failure -> warning = it.message
            }
            complete()
        }
        archive.storageUsage {
            usageDone = true
            when (it) {
                is ArchiveResult.Success -> usage = it.value
                else -> warning = warning ?: "部分存储占用暂时无法读取。"
            }
            complete()
        }
    }

    private suspend fun saveTheme(target: ThemeMode) {
        val previous = mutableState.value.themeMode
        if (mutableState.value.busy || target == previous) return
        mutableState.value = mutableState.value.copy(
            themeMode = target, operation = SettingsOperation.SAVING_THEME
        )
        effectChannel.send(SettingsEffect.PreviewTheme(target))
        settingsRepository.setThemeMode(target) {
            enqueue(Event.ThemeSaved(previous, target, it is StorageResult.Success))
        }
    }

    private fun clearCache() {
        mutableState.value = mutableState.value.copy(operation = SettingsOperation.CLEARING_CACHE)
        backupRepository.clearDrafts { dataResult ->
            archive.clearTemporaryFiles { fileResult ->
                val bytes = (fileResult as? ArchiveResult.Success)?.value ?: 0
                val warning = when {
                    dataResult is BackupDataResult.Failure -> dataResult.message
                    fileResult is ArchiveResult.Failure -> fileResult.message
                    fileResult is ArchiveResult.Unsupported -> "结构化草稿已清理，文件缓存不受当前平台支持。"
                    else -> null
                }
                enqueue(Event.CacheCleared(bytes, warning))
            }
        }
    }

    private fun handleImportSelection(result: ArchiveResult<ImportSelection>) {
        when (result) {
            is ArchiveResult.Success -> backupRepository.preview(result.value) {
                enqueue(Event.PreviewDecoded(it))
            }
            ArchiveResult.Cancelled -> finishNeutral("已取消导入。")
            is ArchiveResult.Failure -> finishFailure(result.message)
            ArchiveResult.Unsupported -> finishFailure("当前平台暂不支持导入。")
        }
    }

    private fun handleCommittedMedia(event: Event.MediaCommitted) {
        when (val result = event.result) {
            is ArchiveResult.Success -> {
                val preview = mutableState.value.preview
                    ?: return finishFailure("导入预览已失效。")
                backupRepository.restore(preview, result.value.pathMapping) {
                    enqueue(Event.DataWritten(event.old, result.value.createdPaths, it))
                }
            }
            is ArchiveResult.Failure -> finishFailure(result.message)
            else -> finishFailure("导入照片未能恢复。")
        }
    }

    private suspend fun handleDataWritten(event: Event.DataWritten) {
        when (event.result) {
            is BackupDataResult.Success -> {
                mutableState.value = mutableState.value.copy(
                    operation = SettingsOperation.NONE,
                    preview = null,
                    confirmImport = false,
                    notice = SettingsNotice("导入完成，数据已恢复。", SettingsNoticeTone.SUCCESS)
                )
                effectChannel.send(SettingsEffect.DataImported)
                settingsRepository.getThemeModeSnapshot { enqueue(Event.ThemeLoaded(it)) }
                load()
            }
            is BackupDataResult.Failure -> {
                val selection = ImportSelection("rollback", event.old.payload, "rollback")
                backupRepository.preview(selection) { previewResult ->
                    if (previewResult is BackupDataResult.Success) {
                        backupRepository.restore(
                            previewResult.value,
                            event.old.mediaPaths.associateWith { it }
                        ) { restoreResult ->
                            mediaFiles.deleteManagedImages(event.createdPaths) { deleteResult ->
                                enqueue(Event.RollbackFinished(
                                    deleted = deleteResult is ManagedMediaDeleteResult.Success,
                                    restored = restoreResult is BackupDataResult.Success
                                ))
                            }
                        }
                    } else {
                        enqueue(Event.RollbackFinished(deleted = false, restored = false))
                    }
                }
            }
        }
    }

    private fun cancelImport() {
        val sessionId = mutableState.value.preview?.sessionId
        mutableState.value = mutableState.value.copy(preview = null, confirmImport = false)
        if (sessionId != null) archive.discardImport(sessionId) {}
    }

    private fun finishSuccess(message: String) {
        mutableState.value = mutableState.value.copy(
            operation = SettingsOperation.NONE,
            notice = SettingsNotice(message, SettingsNoticeTone.SUCCESS)
        )
    }

    private fun finishNeutral(message: String) {
        mutableState.value = mutableState.value.copy(
            operation = SettingsOperation.NONE,
            notice = SettingsNotice(message, SettingsNoticeTone.NEUTRAL)
        )
    }

    private fun finishFailure(message: String) {
        mutableState.value = mutableState.value.copy(
            operation = SettingsOperation.NONE,
            notice = SettingsNotice(message, SettingsNoticeTone.ERROR)
        )
    }

    private fun enqueue(event: Event) {
        if (!disposed) events.trySend(event)
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
