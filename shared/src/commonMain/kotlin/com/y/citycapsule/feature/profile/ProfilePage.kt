package com.y.citycapsule.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyListState
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.y.citycapsule.app.theme.KuiklyAppThemeHost
import com.y.citycapsule.app.theme.AppThemeHost
import com.y.citycapsule.app.theme.RuntimeAppTheme
import com.y.citycapsule.base.BasePager
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.media.AvatarImageCapability
import com.y.citycapsule.core.media.KuiklyAvatarImages
import com.y.citycapsule.core.media.KuiklyPhotoPicker
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteTable
import com.y.citycapsule.core.navigation.KuiklyAppNavigator
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.KuiklyKeyValueStore
import com.y.citycapsule.designsystem.component.AppActionTopBar
import com.y.citycapsule.designsystem.component.AppAvatarPicker
import com.y.citycapsule.designsystem.component.AppBodyText
import com.y.citycapsule.designsystem.component.AppButton
import com.y.citycapsule.designsystem.component.AppButtonVariant
import com.y.citycapsule.designsystem.component.AppCaptionText
import com.y.citycapsule.designsystem.component.AppConfirmDialog
import com.y.citycapsule.designsystem.component.AppDivider
import com.y.citycapsule.designsystem.component.AppIcon
import com.y.citycapsule.designsystem.component.AppIconButton
import com.y.citycapsule.designsystem.component.AppIconName
import com.y.citycapsule.designsystem.component.AppPageTitle
import com.y.citycapsule.designsystem.component.AppProfileAvatar
import com.y.citycapsule.designsystem.component.AppScaffold
import com.y.citycapsule.designsystem.component.AppSecondaryText
import com.y.citycapsule.designsystem.component.AppSectionTitle
import com.y.citycapsule.designsystem.component.AppStatusMessage
import com.y.citycapsule.designsystem.component.AppStatusTone
import com.y.citycapsule.designsystem.component.AppTextField
import com.y.citycapsule.designsystem.component.LoadingState
import com.y.citycapsule.designsystem.component.PlaceCard
import com.y.citycapsule.designsystem.component.PlaceCardModel
import com.y.citycapsule.designsystem.component.PlaceCardVariant
import com.y.citycapsule.designsystem.theme.AppTheme
import com.y.citycapsule.feature.capsule.CapsuleFeatureRuntime
import com.y.citycapsule.feature.place.PlaceFeatureRuntime
import com.y.citycapsule.feature.place.displayName
import com.y.citycapsule.feature.place.toFallbackKind
import kotlinx.coroutines.flow.collect

@Composable
internal fun ProfileRootContent(
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    placeRepository: PlaceRepository,
    favoriteRepository: FavoriteRepository,
    capsuleRepository: CapsuleRepository,
    active: Boolean,
    statusBarHeight: Float,
    listState: LazyListState
) {
    val storeScope = rememberCoroutineScope()
    val store = remember(
        profileRepository,
        placeRepository,
        favoriteRepository,
        capsuleRepository
    ) {
        ProfileOverviewStore(
            profileRepository = profileRepository,
            placeRepository = placeRepository,
            favoriteRepository = favoriteRepository,
            capsuleRepository = capsuleRepository,
            parentScope = storeScope
        )
    }
    val uiState by store.state.collectAsState()
    val placeInvalidationOwner = remember { PlaceFeatureRuntime.newOwnerToken() }
    val profileRevision = ProfileFeatureRuntime.revision
    val placeRevision = PlaceFeatureRuntime.revision
    val initialPlaceRevision = remember { placeRevision }
    val capsuleRevision = CapsuleFeatureRuntime.revision

    DisposableEffect(store) {
        onDispose(store::dispose)
    }

    LaunchedEffect(store, active, profileRevision, capsuleRevision) {
        if (active) store.dispatch(ProfileOverviewIntent.Load)
    }

    LaunchedEffect(store, placeRevision) {
        if (
            active &&
            placeRevision != initialPlaceRevision &&
            PlaceFeatureRuntime.shouldReload(placeInvalidationOwner)
        ) store.dispatch(ProfileOverviewIntent.Load)
    }

    LaunchedEffect(store, navigator) {
        store.effects.collect { effect ->
            when (effect) {
                ProfileOverviewEffect.NavigateToEdit ->
                    navigator.navigate(AppRoute.ProfileEdit)
                ProfileOverviewEffect.NavigateToSettings ->
                    navigator.navigate(AppRoute.Settings)
                ProfileOverviewEffect.NavigateToWantTo ->
                    navigator.navigate(AppRoute.Favorites)
                is ProfileOverviewEffect.NavigateToPlace ->
                    navigator.navigate(AppRoute.PlaceDetail(effect.placeId))
                ProfileOverviewEffect.FavoritesChanged ->
                    PlaceFeatureRuntime.invalidateFrom(placeInvalidationOwner)
            }
        }
    }

    val dimensions = AppTheme.dimensions
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = dimensions.screenHorizontalPadding,
            top = statusBarHeight.dp + dimensions.spacingXxl,
            end = dimensions.screenHorizontalPadding,
            bottom = dimensions.spacingXl
        )
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileOverviewHeader(
                    onSettings = {
                        store.dispatch(ProfileOverviewIntent.SettingsClicked)
                    }
                )
                uiState.notice?.let { notice ->
                    Spacer(Modifier.height(dimensions.spacingMd))
                    AppStatusMessage(
                        message = notice.message,
                        tone = notice.tone.toAppStatusTone()
                    )
                }
                Spacer(Modifier.height(dimensions.spacingXl))
                if (uiState.status == ProfileOverviewStatus.LOADING) {
                    LoadingState("正在整理你的城市档案…")
                } else {
                    ProfileIdentity(uiState)
                    Spacer(Modifier.height(dimensions.spacingXl))
                    ProfileStats(uiState)
                    Spacer(Modifier.height(dimensions.spacingXl))
                    CityFootprints(uiState)
                    Spacer(Modifier.height(dimensions.spacingXl))
                    WantToPreview(uiState, store::dispatch)
                    Spacer(Modifier.height(dimensions.spacingXl))
                    ProfileNavigation(
                        onEdit = {
                            store.dispatch(ProfileOverviewIntent.EditProfileClicked)
                        },
                        onSettings = {
                            store.dispatch(ProfileOverviewIntent.SettingsClicked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOverviewHeader(onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppPageTitle("我的")
            Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
            AppSecondaryText("一份只属于你的城市档案。")
        }
        AppIconButton(
            icon = AppIconName.SETTINGS,
            contentDescription = "数据与设置",
            onClick = onSettings
        )
    }
}

@Composable
private fun ProfileIdentity(state: ProfileOverviewUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppProfileAvatar(
            preset = state.profile.avatarPreset,
            managedPath = state.profile.avatarManagedPath,
            size = AppTheme.dimensions.profileAvatarSize
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppTheme.dimensions.spacingMd)
        ) {
            AppSectionTitle(state.profile.displayName)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppSecondaryText(
                state.profile.homeCity?.let { "$it · 本地档案" } ?: "未设置常驻城市 · 本地档案"
            )
            state.profile.bio?.takeIf(String::isNotBlank)?.let { bio ->
                Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
                AppBodyText(bio)
            }
        }
    }
}

@Composable
private fun ProfileStats(state: ProfileOverviewUiState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ProfileStat(
            value = state.memoryCount?.toString() ?: "—",
            label = "城市碎片",
            modifier = Modifier.weight(1f)
        )
        ProfileStat(
            value = state.visitedPlaceCount?.toString() ?: "—",
            label = "去过地点",
            modifier = Modifier.weight(1f)
        )
        ProfileStat(
            value = state.wantToCount?.toString() ?: "—",
            label = "想去",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = AppTheme.colors.textPrimary,
            style = AppTheme.typography.pageTitle
        )
        Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
        AppCaptionText(label)
    }
}

@Composable
private fun CityFootprints(state: ProfileOverviewUiState) {
    AppSectionTitle("我的城市足迹")
    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    if (state.cityFootprints.isEmpty()) {
        AppSecondaryText(
            if (state.capsules == null) {
                "城市足迹暂时无法读取。"
            } else {
                "留下第一条城市碎片后，这里会按真实地点整理你的城市。"
            }
        )
    } else {
        state.cityFootprints.forEachIndexed { index, footprint ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.dimensions.spacingXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBodyText(footprint.city, modifier = Modifier.weight(1f))
                AppSecondaryText(
                    "${footprint.placeCount} 个地点 · ${footprint.memoryCount} 条记忆"
                )
            }
            if (index < state.cityFootprints.lastIndex) AppDivider()
        }
    }
}

@Composable
private fun WantToPreview(
    state: ProfileOverviewUiState,
    dispatch: (ProfileOverviewIntent) -> Unit
) {
    ProfileSectionHeader(
        title = "想去的地方",
        actionLabel = "查看全部",
        onAction = { dispatch(ProfileOverviewIntent.WantToClicked) }
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingSm))
    when {
        state.favoriteIds == null -> AppSecondaryText("想去清单暂时无法读取。")
        state.wantToPlaces.isEmpty() -> AppSecondaryText(
            "还没有想去的地方，从探索中遇见下一站。"
        )
        else -> state.wantToPlaces.forEachIndexed { index, place ->
            ProfileWantToPlace(
                place = place,
                busy = state.busyFavoriteId == place.id,
                onOpen = {
                    dispatch(ProfileOverviewIntent.PlaceClicked(place.id))
                },
                onRemove = {
                    dispatch(ProfileOverviewIntent.FavoriteToggled(place.id))
                }
            )
            if (index < state.wantToPlaces.lastIndex) AppDivider()
        }
    }
}

@Composable
private fun ProfileWantToPlace(
    place: Place,
    busy: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    PlaceCard(
        model = PlaceCardModel(
            name = place.name,
            metadata = listOfNotNull(
                place.category.displayName(),
                place.district ?: place.city
            ).joinToString(" · "),
            favorite = true,
            fallbackKind = place.category.toFallbackKind()
        ),
        onOpen = onOpen,
        onToggleFavorite = onRemove,
        variant = PlaceCardVariant.COMPACT,
        favoriteEnabled = !busy
    )
}

@Composable
private fun ProfileSectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppSectionTitle(title, modifier = Modifier.weight(1f))
        AppCaptionText(
            text = "$actionLabel  ›",
            modifier = Modifier.clickable(onClick = onAction)
        )
    }
}

@Composable
private fun ProfileNavigation(
    onEdit: () -> Unit,
    onSettings: () -> Unit
) {
    AppSectionTitle("档案与数据")
    Spacer(Modifier.height(AppTheme.dimensions.spacingXs))
    ProfileNavigationRow(
        title = "编辑个人档案",
        description = "头像、昵称、城市和简介",
        onClick = onEdit
    )
    AppDivider()
    ProfileNavigationRow(
        title = "数据与设置",
        description = "主题、首次引导和危险操作",
        onClick = onSettings
    )
}

@Composable
private fun ProfileNavigationRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = AppTheme.dimensions.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AppBodyText(title)
            Spacer(Modifier.height(AppTheme.dimensions.spacingXxs))
            AppCaptionText(description)
        }
        AppIcon(
            name = AppIconName.FORWARD,
            contentDescription = "进入$title",
            tint = AppTheme.colors.textSecondary
        )
    }
}

@Page(AppRouteTable.PAGE_PROFILE_EDIT, supportInLocal = true)
internal class ProfileEditPager : BasePager() {
    override fun willInit() {
        super.willInit()
        val navigator = KuiklyAppNavigator(this)
        val storage = KuiklyKeyValueStore(this)
        setContent {
            ProfileEditScreen(
                navigator = navigator,
                profileRepository = LocalProfileRepository(storage),
                photoPicker = KuiklyPhotoPicker(this),
                avatarImages = KuiklyAvatarImages(this),
                themeHost = KuiklyAppThemeHost(this)
            )
        }
    }
}

@Composable
private fun ProfileEditScreen(
    navigator: AppNavigator,
    profileRepository: LocalProfileRepository,
    photoPicker: PhotoPickerCapability,
    avatarImages: AvatarImageCapability,
    themeHost: AppThemeHost
) {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    val storeScope = rememberCoroutineScope()
    val store = remember(profileRepository) {
        ProfileEditorStore(profileRepository, photoPicker, avatarImages, storeScope)
    }
    val uiState by store.state.collectAsState()

    DisposableEffect(store) {
        onDispose(store::dispose)
    }
    LaunchedEffect(store) {
        store.dispatch(ProfileEditorIntent.Load)
    }
    LaunchedEffect(store, navigator) {
        store.effects.collect { effect ->
            when (effect) {
                ProfileEditorEffect.NavigateBack -> navigator.back()
                ProfileEditorEffect.SavedAndNavigateBack -> {
                    ProfileFeatureRuntime.invalidate()
                    navigator.back()
                }
            }
        }
    }

    RuntimeAppTheme(themeHost = themeHost) {
        AppScaffold(statusBarHeight = statusBarHeight) {
            AppActionTopBar(
                title = "编辑个人档案",
                onLeadingClick = {
                    store.dispatch(ProfileEditorIntent.BackClicked)
                },
                actionLabel = "保存",
                onActionClick = {
                    store.dispatch(ProfileEditorIntent.SaveClicked)
                },
                actionEnabled = uiState.status == ProfileEditorStatus.READY
            )
            Spacer(Modifier.height(AppTheme.dimensions.spacingLg))
            if (uiState.status == ProfileEditorStatus.LOADING) {
                LoadingState("正在读取个人档案…")
            } else {
                uiState.notice?.let { notice ->
                    AppStatusMessage(
                        notice.message,
                        tone = notice.tone.toAppStatusTone()
                    )
                    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
                }
                ProfileEditForm(uiState, store::dispatch)
            }
        }

        if (uiState.showDiscardConfirmation) {
            AppConfirmDialog(
                title = "放弃这次修改？",
                message = "尚未保存的头像、昵称、城市和简介修改将丢失。",
                confirmText = "放弃修改",
                onConfirm = {
                    store.dispatch(ProfileEditorIntent.DiscardConfirmed)
                },
                onDismiss = {
                    store.dispatch(ProfileEditorIntent.DismissDiscard)
                }
            )
        }
    }
}

@Composable
private fun ProfileEditForm(
    state: ProfileEditorUiState,
    dispatch: (ProfileEditorIntent) -> Unit
) {
    AppTextField(
        value = state.profile.displayName,
        onValueChange = {
            dispatch(ProfileEditorIntent.DisplayNameChanged(it))
        },
        label = "昵称",
        placeholder = "例如：城市漫游者",
        errorMessage = state.validationMessage,
        maxLength = LocalProfileValidator.DISPLAY_NAME_MAX_LENGTH,
        enabled = !state.isBusy
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppProfileAvatar(
            preset = state.profile.avatarPreset,
            managedPath = state.profile.avatarManagedPath,
            size = AppTheme.dimensions.profileAvatarSize
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppTheme.dimensions.spacingMd)
        ) {
            AppButton(
                text = "从相册选择",
                onClick = { dispatch(ProfileEditorIntent.PickAvatarPhoto) },
                variant = AppButtonVariant.SECONDARY,
                enabled = !state.isBusy
            )
        }
    }
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    AppAvatarPicker(
        selected = state.profile.avatarPreset,
        onSelected = {
            dispatch(ProfileEditorIntent.AvatarChanged(it))
        },
        enabled = !state.isBusy
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    AppTextField(
        value = state.profile.homeCity.orEmpty(),
        onValueChange = {
            dispatch(ProfileEditorIntent.HomeCityChanged(it))
        },
        label = "常驻城市",
        placeholder = "例如：上海",
        maxLength = LocalProfileValidator.HOME_CITY_MAX_LENGTH,
        enabled = !state.isBusy
    )
    Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
    AppTextField(
        value = state.profile.bio.orEmpty(),
        onValueChange = {
            dispatch(ProfileEditorIntent.BioChanged(it))
        },
        label = "个人简介",
        placeholder = "写下一句关于你的城市观察",
        errorMessage = state.validationMessage,
        maxLength = LocalProfileValidator.BIO_MAX_LENGTH,
        maxLines = 4,
        enabled = !state.isBusy
    )
    if (state.status == ProfileEditorStatus.SAVING) {
        Spacer(Modifier.height(AppTheme.dimensions.spacingMd))
        AppSecondaryText("正在保存…", textAlign = TextAlign.Center)
    }
}

private fun ProfileNoticeTone.toAppStatusTone(): AppStatusTone = when (this) {
    ProfileNoticeTone.NEUTRAL -> AppStatusTone.NEUTRAL
    ProfileNoticeTone.WARNING -> AppStatusTone.WARNING
    ProfileNoticeTone.ERROR -> AppStatusTone.ERROR
}
