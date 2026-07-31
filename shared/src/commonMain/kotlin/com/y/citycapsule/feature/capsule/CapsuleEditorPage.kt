package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
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
        AppFixedHeaderScaffold(
            statusBarHeight = statusBarHeight,
            contentMaxWidth = AppTheme.dimensions.readableContentMaxWidth,
            header = {
                AppActionTopBar(
                title = if (capsuleId == null) "这一刻" else "编辑城市碎片",
                onLeadingClick = { holder.requestClose(navigator::back) },
                leadingIcon = AppIconName.CLOSE,
                leadingDescription = "关闭编辑器",
                actionLabel = if (state.status == CapsuleUiStatus.SAVING) "保存中…" else "完成",
                onActionClick = {
                    holder.publish { published ->
                        completeCapsuleEditorNavigation(capsuleId, published.id, navigator)
                    }
                },
                actionEnabled = state.status == CapsuleUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            }
        ) {
            when (state.status) {
                CapsuleUiStatus.LOADING -> LoadingState("正在准备这一刻…")
                CapsuleUiStatus.NOT_FOUND -> EmptyState(
                    title = "没有找到这段记录",
                    message = "地点或城市碎片可能已经被删除。"
                )
                CapsuleUiStatus.ERROR -> ErrorState(
                    message = state.notice ?: "暂时无法打开编辑器。"
                )
                CapsuleUiStatus.READY, CapsuleUiStatus.SAVING -> CapsuleEditorContent(
                    state = state,
                    holder = holder,
                    photoPicker = photoPicker
                )
            }
        }
        AppBottomSheet(
            visible = state.showDiscardConfirmation,
            title = "要离开这一刻吗？",
            onDismiss = holder::dismissDiscard,
            dismissLabel = null,
            footer = {
                AppButton(
                    text = "保存草稿并退出",
                    onClick = { holder.saveDraftAndClose(navigator::back) },
                    enabled = state.status == CapsuleUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "继续编辑",
                    onClick = holder::dismissDiscard,
                    variant = AppButtonVariant.SECONDARY,
                    enabled = state.status == CapsuleUiStatus.READY
                )
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton(
                    text = "放弃修改",
                    onClick = { holder.discard(navigator::back) },
                    variant = AppButtonVariant.DANGER,
                    enabled = state.status == CapsuleUiStatus.READY
                )
            }
        ) {
            AppSecondaryText("你可以先把当前内容保存在这台设备上，下次从同一地点继续。")
            state.notice?.takeIf { it.contains("草稿") }?.let { notice ->
                Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                AppStatusMessage(notice, tone = AppStatusTone.ERROR)
            }
        }
    }
}

@Composable
private fun CapsuleEditorContent(
    state: CapsuleEditorState,
    holder: CapsuleEditorStateHolder,
    photoPicker: PhotoPickerCapability
) {
    val dimensions = AppTheme.dimensions
    state.notice?.let {
        AppStatusMessage(it, tone = AppStatusTone.NEUTRAL)
        Spacer(Modifier.height(dimensions.spacingMd))
    }

    AppSectionTitle("照片")
    Spacer(Modifier.height(dimensions.spacingXxs))
    AppSecondaryText(
        if (state.draft.imagePaths.isEmpty()) {
            "没有照片也可以保存这一刻。"
        } else {
            "${state.draft.imagePaths.size}/${CapsuleContract.IMAGE_MAX_COUNT} 张 · 点击 × 移除"
        }
    )
    if (state.draft.imagePaths.isNotEmpty()) {
        Spacer(Modifier.height(dimensions.spacingSm))
        CapsuleEditablePhotoGrid(state.draft.imagePaths, holder::removeImage)
    }
    Spacer(Modifier.height(dimensions.spacingSm))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = state.status == CapsuleUiStatus.READY &&
                    !state.pickingImages &&
                    state.draft.imagePaths.size < CapsuleContract.IMAGE_MAX_COUNT
            ) { holder.pickImages(photoPicker) }
            .padding(vertical = dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(AppIconName.PHOTO, "选择照片", tint = AppTheme.colors.primary)
        Spacer(Modifier.width(dimensions.spacingXs))
        AppBodyText(
            if (state.pickingImages) "正在打开相册…"
            else if (state.draft.imagePaths.isEmpty()) "从相册选择照片"
            else "继续添加照片"
        )
        Spacer(Modifier.weight(1f))
        AppCaptionText("${state.draft.imagePaths.size}/${CapsuleContract.IMAGE_MAX_COUNT}")
    }

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("写下这一刻")
    Spacer(Modifier.height(dimensions.spacingSm))
    AppTextField(
        value = state.draft.content,
        onValueChange = holder::updateContent,
        label = null,
        placeholder = "此刻的光、声音，或你想记住的一句话……",
        maxLength = CapsuleContract.CONTENT_MAX_LENGTH,
        maxLines = 10,
        errorMessage = state.validationMessage,
        enabled = state.status == CapsuleUiStatus.READY
    )

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("今天感觉怎么样？")
    Spacer(Modifier.height(dimensions.spacingSm))
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
                            end = if (index == 0) dimensions.spacingXxs
                            else dimensions.spacingNone
                        ),
                    enabled = state.status == CapsuleUiStatus.READY
                )
            }
            if (moods.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(dimensions.spacingXxs))
    }

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("地点")
    Spacer(Modifier.height(dimensions.spacingSm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(AppIconName.LOCATION, "记录地点", tint = AppTheme.colors.primary)
        Spacer(Modifier.width(dimensions.spacingXs))
        AppBodyText(state.place?.name ?: "地点信息暂不可用")
    }
    state.place?.let { place ->
        Spacer(Modifier.height(dimensions.spacingXxs))
        AppSecondaryText(
            listOfNotNull(place.city, place.district).joinToString(" · ")
        )
    }

    Spacer(Modifier.height(dimensions.spacingXl))
    AppSectionTitle("标签")
    if (state.draft.tags.isNotEmpty()) {
        Spacer(Modifier.height(dimensions.spacingSm))
        state.draft.tags.chunked(2).forEach { tags ->
            Row(Modifier.fillMaxWidth()) {
                tags.forEachIndexed { index, tag ->
                    AppChoiceChip(
                        text = "#$tag ×",
                        selected = true,
                        onClick = { holder.removeTag(tag) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                end = if (index == 0) dimensions.spacingXxs
                                else dimensions.spacingNone
                            ),
                        enabled = state.status == CapsuleUiStatus.READY
                    )
                }
                if (tags.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(dimensions.spacingXxs))
        }
    }
    Spacer(Modifier.height(dimensions.spacingSm))
    AppTextField(
        value = holder.tagsText(),
        onValueChange = holder::updateTags,
        label = null,
        placeholder = "添加标签，例如：咖啡，散步，夜景",
        supportingText = "用逗号分隔，最多 8 个。",
        enabled = state.status == CapsuleUiStatus.READY
    )
    Spacer(Modifier.height(dimensions.spacingXxl))
    AppCaptionText("点击右上角“完成”，保存到你的城市记忆。")
}

internal fun CapsuleMood.displayName(): String = when (this) {
    CapsuleMood.HAPPY -> "开心"; CapsuleMood.CALM -> "平静"; CapsuleMood.SURPRISED -> "惊喜"
    CapsuleMood.MELANCHOLY -> "低落"; CapsuleMood.ENERGETIC -> "充满活力"
}

internal fun completeCapsuleEditorNavigation(
    editingCapsuleId: String?,
    publishedCapsuleId: String,
    navigator: AppNavigator
) {
    if (editingCapsuleId == null) {
        navigator.replace(AppRoute.CapsuleDetail(publishedCapsuleId))
    } else {
        navigator.back()
    }
}
