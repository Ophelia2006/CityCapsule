package com.y.citycapsule.feature.capsule

import androidx.compose.runtime.*
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.*
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.*
import com.y.citycapsule.core.navigation.*
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.*
import com.y.citycapsule.designsystem.theme.AppTheme

@Page(AppRouteTable.PAGE_CAPSULE_DETAIL, supportInLocal = true)
internal class CapsuleDetailPager : BasePager() {
    override fun willInit() {
        super.willInit(); val storage = KuiklyKeyValueStore(this)
        val id = pageData.params.optString("capsuleId").orEmpty()
        setContent { CapsuleDetailScreen(id, KuiklyAppNavigator(this), LocalCapsuleRepository(storage), LocalPlaceRepository(storage), KuiklyAppThemeHost(this)) }
    }
}

@Composable
private fun CapsuleDetailScreen(id: String, navigator: AppNavigator, capsules: CapsuleRepository, places: LocalPlaceRepository, themeHost: AppThemeHost) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var state by remember { mutableStateOf(CapsuleDetailState()) }
    val holder = remember(id) { CapsuleDetailStateHolder(id, capsules, places) { state = it } }
    LaunchedEffect(holder) { holder.load() }
    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppTopBar("城市碎片", state.place?.let { "留在 ${it.name} 的这一刻" })
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            state.notice?.let { AppStatusMessage(it, tone = AppStatusTone.ERROR); Spacer(Modifier.height(AppTheme.dimensions.spacingMd)) }
            val capsule = state.capsule
            if (capsule == null) AppSecondaryText(if (state.status == CapsuleUiStatus.LOADING) "正在读取记忆…" else "这条城市记忆已不存在。")
            else {
                AppSection("这一刻") {
                    AppCaptionText(formatCapsuleDate(capsule.createdAtEpochMs))
                    if (capsule.imagePaths.isNotEmpty()) {
                        Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                        CapsulePhotoList(capsule.imagePaths)
                    }
                    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
                    AppBodyText(capsule.content)
                    capsule.mood?.let { Spacer(Modifier.height(AppTheme.dimensions.spacingSm)); AppSecondaryText("心情 · ${it.displayName()}") }
                    if (capsule.tags.isNotEmpty()) { Spacer(Modifier.height(AppTheme.dimensions.spacingSm)); AppCaptionText(capsule.tags.joinToString("  ") { "#$it" }) }
                }
                Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
                state.place?.let { place ->
                    AppButton("回到 ${place.name}", { navigator.navigate(AppRoute.PlaceDetail(place.id)) }, variant = AppButtonVariant.SECONDARY)
                    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                }
                AppButton("编辑这条记忆", { navigator.navigate(AppRoute.CapsuleEditor(capsule.id, capsule.placeId)) }, variant = AppButtonVariant.SECONDARY)
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppButton("删除", holder::requestDelete, variant = AppButtonVariant.TEXT)
            }
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            AppButton("查看全部城市记忆", { navigator.navigate(AppRoute.Timeline) }, variant = AppButtonVariant.SECONDARY)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppButton("返回上一页", navigator::back, variant = AppButtonVariant.TEXT)
        }
        if (state.showDeleteConfirmation) AppConfirmDialog(
            "删除这条城市记忆？", "删除后无法恢复。", "确认删除",
            onConfirm = { holder.delete { navigator.replace(AppRoute.Timeline) } }, onDismiss = holder::dismissDelete
        )
    }
}
