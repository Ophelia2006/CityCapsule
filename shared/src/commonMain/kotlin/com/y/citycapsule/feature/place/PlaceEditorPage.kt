package com.y.citycapsule.feature.place

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
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppChoiceChip
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSection
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.AppTopBar
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_PLACE_EDITOR, supportInLocal = true)
internal class PlaceEditorPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val placeId = pageData.params
            .optString(AppRouteTable.PARAM_PLACE_ID)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        val navigator = KuiklyAppNavigator(this)
        val placeRepository = LocalPlaceRepository(KuiklyKeyValueStore(this))
        val themeHost = KuiklyAppThemeHost(this)
        setContent {
            PlaceEditorScreen(placeId, navigator, placeRepository, themeHost)
        }
    }
}

@Composable
private fun PlaceEditorScreen(
    placeId: String?,
    navigator: AppNavigator,
    placeRepository: LocalPlaceRepository,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var uiState by remember {
        mutableStateOf(
            PlaceEditorUiState(
                mode = if (placeId == null) PlaceEditorMode.CREATE else PlaceEditorMode.EDIT
            )
        )
    }
    val holder = remember(placeId, placeRepository) {
        PlaceEditorStateHolder(
            placeId = placeId,
            placeRepository = placeRepository,
            onDataChanged = PlaceFeatureRuntime::invalidate,
            onStateChanged = { uiState = it }
        )
    }

    LaunchedEffect(holder) {
        holder.load()
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar(
                title = if (uiState.mode == PlaceEditorMode.CREATE) "新建地点" else "编辑地点",
                subtitle = "保存失败时当前输入会继续保留在页面中。"
            )
            uiState.notice?.let {
                Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                AppStatusMessage(it.message, tone = it.tone.toAppStatusTone())
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (uiState.status == PlaceEditorUiStatus.NOT_FOUND) {
                AppSecondaryText("待编辑地点不存在或暂时无法读取。")
            } else {
                PlaceEditorForm(uiState, holder)
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                AppButton(
                    text = "保存地点",
                    onClick = {
                        holder.save { place, created ->
                            if (created) {
                                navigator.replace(AppRoute.PlaceDetail(place.id))
                            } else {
                                navigator.back()
                            }
                        }
                    },
                    enabled = !uiState.isBusy,
                    loading = uiState.status == PlaceEditorUiStatus.SAVING,
                    loadingText = "正在保存…"
                )
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton(
                text = "放弃并返回",
                onClick = { holder.requestDiscard(navigator::back) },
                variant = AppButtonVariant.TEXT,
                enabled = !uiState.isBusy
            )
        }

        if (uiState.showDiscardConfirmation) {
            AppConfirmDialog(
                title = "放弃未保存修改？",
                message = "当前页面中的修改不会写入本地地点目录。",
                confirmText = "确认放弃",
                onConfirm = { holder.confirmDiscard(navigator::back) },
                onDismiss = holder::dismissDiscard
            )
        }
    }
}

@Composable
private fun PlaceEditorForm(
    state: PlaceEditorUiState,
    holder: PlaceEditorStateHolder
) {
    AppSection(title = "基本信息") {
        AppTextField(
            value = state.draft.name,
            onValueChange = holder::updateName,
            label = "地点名称",
            placeholder = "例如：城市博物馆",
            errorMessage = state.validationMessage,
            maxLength = PlaceValidator.NAME_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.draft.city,
            onValueChange = holder::updateCity,
            label = "城市",
            placeholder = "例如：上海",
            errorMessage = state.validationMessage,
            maxLength = PlaceValidator.CITY_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.draft.district.orEmpty(),
            onValueChange = holder::updateDistrict,
            label = "区域",
            placeholder = "例如：黄浦区",
            maxLength = PlaceValidator.DISTRICT_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.draft.address.orEmpty(),
            onValueChange = holder::updateAddress,
            label = "地址",
            maxLength = PlaceValidator.ADDRESS_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = holder.tagsText(),
            onValueChange = holder::updateTags,
            label = "标签",
            supportingText = "使用中文或英文逗号分隔，最多 8 个。",
            maxLength = TAGS_INPUT_MAX_LENGTH,
            enabled = !state.isBusy
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppTextField(
            value = state.draft.note.orEmpty(),
            onValueChange = holder::updateNote,
            label = "备注",
            maxLength = PlaceValidator.NOTE_MAX_LENGTH,
            maxLines = NOTE_MAX_LINES,
            enabled = !state.isBusy
        )
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
    AppSection(title = "地点分类") {
        PlaceCategory.entries.forEach { category ->
            AppChoiceChip(
                text = category.displayName(),
                selected = state.draft.category == category,
                onClick = { holder.updateCategory(category) },
                enabled = !state.isBusy
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        }
    }
}

private const val TAGS_INPUT_MAX_LENGTH = 135
private const val NOTE_MAX_LINES = 6
