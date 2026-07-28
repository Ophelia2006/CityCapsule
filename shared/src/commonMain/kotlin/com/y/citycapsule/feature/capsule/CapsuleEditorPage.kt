package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.*
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.*
import com.y.citycapsule.core.navigation.*
import com.y.citycapsule.core.media.KuiklyPhotoPicker
import com.y.citycapsule.core.media.KuiklyManagedMediaFiles
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.*
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_CAPSULE_EDITOR, supportInLocal = true)
internal class CapsuleEditorPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val storage = KuiklyKeyValueStore(this)
        val capsuleRepository = LocalCapsuleRepository(storage)
        val capsuleId = pageData.params.optString("capsuleId").takeIf(String::isNotBlank)
        val placeId = pageData.params.optString(AppRouteTable.PARAM_PLACE_ID).takeIf(String::isNotBlank)
        setContent {
            CapsuleEditorScreen(
                capsuleId, placeId, KuiklyAppNavigator(this),
                capsuleRepository, LocalPlaceRepository(storage),
                KuiklyPhotoPicker(this),
                RepositoryCapsuleMediaCleanup(
                    capsuleRepository,
                    KuiklyManagedMediaFiles(this)
                ),
                KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun CapsuleEditorScreen(
    capsuleId: String?, placeId: String?, navigator: AppNavigator,
    capsuleRepository: CapsuleRepository, placeRepository: LocalPlaceRepository,
    photoPicker: PhotoPickerCapability,
    mediaCleanup: CapsuleMediaCleanup,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleEditorState()) }
    val holder = remember(capsuleId, placeId) {
        CapsuleEditorStateHolder(
            capsuleId,
            placeId,
            capsuleRepository,
            placeRepository,
            mediaCleanup
        ) { state = it }
    }
    LaunchedEffect(holder) { holder.load() }
    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = if (capsuleId == null) "这一刻" else "编辑城市碎片",
                subtitle = state.place?.let { "留在 ${it.name} 的城市记忆" }
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (state.status == CapsuleUiStatus.LOADING) {
                AppSecondaryText("正在准备记录…")
            } else if (state.status == CapsuleUiStatus.NOT_FOUND || state.status == CapsuleUiStatus.ERROR) {
                AppStatusMessage(state.notice ?: "地点或城市碎片已经不存在。", tone = AppStatusTone.ERROR)
            } else {
                state.notice?.let { AppStatusMessage(it, tone = AppStatusTone.NEUTRAL); Spacer(Modifier.height(AppTheme.dimensions.spacingMd)) }
                AppSection(
                    title = "照片",
                    description = if (state.draft.imagePaths.isEmpty()) {
                        "选择这一刻的城市照片；没有照片也可以保存文字记忆。"
                    } else {
                        "已添加 ${state.draft.imagePaths.size}/${CapsuleContract.IMAGE_MAX_COUNT} 张"
                    }
                ) {
                    if (state.draft.imagePaths.isNotEmpty()) {
                        CapsulePhotoList(
                            paths = state.draft.imagePaths,
                            onRemove = holder::removeImage
                        )
                        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    }
                    AppButton(
                        text = if (state.draft.imagePaths.isEmpty()) "从相册选择照片" else "继续添加照片",
                        onClick = { holder.pickImages(photoPicker) },
                        variant = AppButtonVariant.SECONDARY,
                        enabled = state.status == CapsuleUiStatus.READY &&
                            state.draft.imagePaths.size < CapsuleContract.IMAGE_MAX_COUNT,
                        loading = state.pickingImages,
                        loadingText = "正在打开相册…"
                    )
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppTextField(
                    value = state.draft.content,
                    onValueChange = holder::updateContent,
                    label = "写下这一刻",
                    placeholder = "此刻的光、声音，或你想记住的一句话……",
                    maxLength = CapsuleContract.CONTENT_MAX_LENGTH,
                    maxLines = 8,
                    errorMessage = state.validationMessage,
                    enabled = state.status == CapsuleUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppSection(title = "今天感觉怎么样？") {
                    CapsuleMood.entries.chunked(2).forEach { moods ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            moods.forEachIndexed { index, mood ->
                                AppChoiceChip(
                                    text = mood.displayName(),
                                    selected = state.draft.mood == mood,
                                    onClick = {
                                        holder.updateMood(if (state.draft.mood == mood) null else mood)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(
                                            end = if (index == 0) {
                                                AppTheme.dimensions.spacingXxs
                                            } else {
                                                AppTheme.dimensions.spacingNone
                                            }
                                        ),
                                    enabled = state.status == CapsuleUiStatus.READY
                                )
                            }
                            if (moods.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
                    }
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppTextField(
                    value = holder.tagsText(), onValueChange = holder::updateTags,
                    label = "标签", placeholder = "咖啡，散步，夜景",
                    supportingText = "用逗号分隔，最多 8 个。",
                    enabled = state.status == CapsuleUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppButton(
                    text = "保存到城市记忆",
                    onClick = { holder.publish { navigator.replace(AppRoute.CapsuleDetail(it.id)) } },
                    enabled = state.status == CapsuleUiStatus.READY,
                    loading = state.status == CapsuleUiStatus.SAVING,
                    loadingText = "正在保存…"
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton("保存草稿", holder::saveDraft, variant = AppButtonVariant.SECONDARY, enabled = state.status == CapsuleUiStatus.READY)
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton("关闭", { holder.requestClose(navigator::back) }, variant = AppButtonVariant.TEXT, enabled = state.status == CapsuleUiStatus.READY)
        }
        if (state.showDiscardConfirmation) AppConfirmDialog(
            title = "离开这一刻？", message = "未保存的修改将丢失。你也可以先保存草稿。",
            confirmText = "放弃修改", onConfirm = { holder.discard(navigator::back) }, onDismiss = holder::dismissDiscard
        )
    }
}

internal fun CapsuleMood.displayName(): String = when (this) {
    CapsuleMood.HAPPY -> "开心"; CapsuleMood.CALM -> "平静"; CapsuleMood.SURPRISED -> "惊喜"
    CapsuleMood.MELANCHOLY -> "低落"; CapsuleMood.ENERGETIC -> "充满活力"
}
