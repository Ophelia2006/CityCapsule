package com.y.citycapsule.feature.profile

import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.media.AvatarImageCapability
import com.y.citycapsule.core.media.AvatarImageResult
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.media.PhotoPickerResult
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalogSnapshot
import com.y.citycapsule.core.place.PlaceCatalogSource
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.profile.AvatarPreset
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.profile.LocalProfileSnapshot
import com.y.citycapsule.core.profile.LocalProfileSource
import com.y.citycapsule.core.profile.LocalProfileValidator
import com.y.citycapsule.core.storage.StorageResult
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

enum class ProfileOverviewStatus { LOADING, READY }

enum class ProfileNoticeTone { NEUTRAL, WARNING, ERROR }

data class ProfileNotice(
    val message: String,
    val tone: ProfileNoticeTone
)

data class ProfileCityFootprint(
    val city: String,
    val placeCount: Int,
    val memoryCount: Int
)

data class ProfileOverviewUiState(
    val status: ProfileOverviewStatus = ProfileOverviewStatus.LOADING,
    val profile: LocalProfile = LocalProfile.DEFAULT,
    val catalogPlaces: List<Place> = emptyList(),
    val favoriteIds: Set<String>? = null,
    val capsules: List<CityCapsule>? = null,
    val catalogReadOnly: Boolean = false,
    val busyFavoriteId: String? = null,
    val notice: ProfileNotice? = null
) {
    val memoryCount: Int?
        get() = capsules?.size

    val visitedPlaceCount: Int?
        get() = capsules?.map(CityCapsule::placeId)?.toSet()?.size

    val wantToCount: Int?
        get() = favoriteIds?.size

    val wantToPlaces: List<Place>
        get() {
            val ids = favoriteIds ?: return emptyList()
            return catalogPlaces.filter { it.id in ids }.take(PROFILE_WANT_TO_PREVIEW_LIMIT)
        }

    val cityFootprints: List<ProfileCityFootprint>
        get() = buildCityFootprints(capsules.orEmpty(), catalogPlaces)

    val unresolvedVisitedPlaceCount: Int
        get() {
            val knownIds = catalogPlaces.mapTo(mutableSetOf(), Place::id)
            return capsules.orEmpty()
                .map(CityCapsule::placeId)
                .filterNot(knownIds::contains)
                .toSet()
                .size
        }
}

sealed interface ProfileOverviewIntent {
    data object Load : ProfileOverviewIntent
    data object Retry : ProfileOverviewIntent
    data object EditProfileClicked : ProfileOverviewIntent
    data object SettingsClicked : ProfileOverviewIntent
    data object WantToClicked : ProfileOverviewIntent
    data class PlaceClicked(val placeId: String) : ProfileOverviewIntent
    data class FavoriteToggled(val placeId: String) : ProfileOverviewIntent
}

sealed interface ProfileOverviewEffect {
    data object NavigateToEdit : ProfileOverviewEffect
    data object NavigateToSettings : ProfileOverviewEffect
    data object NavigateToWantTo : ProfileOverviewEffect
    data class NavigateToPlace(val placeId: String) : ProfileOverviewEffect
    data object FavoritesChanged : ProfileOverviewEffect
}

internal sealed interface ProfileOverviewMutation {
    data object LoadStarted : ProfileOverviewMutation
    data class ProfileLoaded(val snapshot: LocalProfileSnapshot) : ProfileOverviewMutation
    data class CatalogLoaded(val snapshot: PlaceCatalogSnapshot) : ProfileOverviewMutation
    data class FavoritesLoaded(
        val result: StorageResult<FavoritePlaceIds>
    ) : ProfileOverviewMutation
    data class CapsulesLoaded(
        val result: StorageResult<List<CityCapsule>>
    ) : ProfileOverviewMutation
    data class FavoriteToggleStarted(val placeId: String) : ProfileOverviewMutation
    data class FavoriteToggleSucceeded(
        val placeId: String,
        val favorite: Boolean
    ) : ProfileOverviewMutation
    data object FavoriteToggleFailed : ProfileOverviewMutation
}

internal object ProfileOverviewReducer {
    fun reduce(
        state: ProfileOverviewUiState,
        mutation: ProfileOverviewMutation
    ): ProfileOverviewUiState = when (mutation) {
        ProfileOverviewMutation.LoadStarted -> state.copy(
            status = ProfileOverviewStatus.LOADING,
            busyFavoriteId = null,
            notice = null
        )
        is ProfileOverviewMutation.ProfileLoaded -> state.copy(
            profile = mutation.snapshot.profile,
            notice = if (mutation.snapshot.source == LocalProfileSource.DEFAULT_RECOVERY) {
                partialNotice()
            } else {
                state.notice
            }
        )
        is ProfileOverviewMutation.CatalogLoaded -> state.copy(
            catalogPlaces = mutation.snapshot.catalog.places,
            catalogReadOnly = mutation.snapshot.source ==
                PlaceCatalogSource.RECOVERY_READ_ONLY,
            notice = if (
                mutation.snapshot.warning != null ||
                mutation.snapshot.source == PlaceCatalogSource.RECOVERY_READ_ONLY
            ) {
                partialNotice()
            } else {
                state.notice
            }
        )
        is ProfileOverviewMutation.FavoritesLoaded -> state.copy(
            favoriteIds = when (val result = mutation.result) {
                is StorageResult.Success -> result.value.placeIds
                StorageResult.Missing -> emptySet()
                is StorageResult.Failure -> null
            },
            notice = if (mutation.result is StorageResult.Failure) {
                partialNotice()
            } else {
                state.notice
            }
        )
        is ProfileOverviewMutation.CapsulesLoaded -> {
            val capsules = when (val result = mutation.result) {
                is StorageResult.Success -> result.value
                StorageResult.Missing -> emptyList()
                is StorageResult.Failure -> null
            }
            val next = state.copy(
                status = ProfileOverviewStatus.READY,
                capsules = capsules,
                notice = if (mutation.result is StorageResult.Failure) {
                    partialNotice()
                } else {
                    state.notice
                }
            )
            if (next.unresolvedVisitedPlaceCount > 0 && next.notice == null) {
                next.copy(
                    notice = ProfileNotice(
                        "部分历史地点已无法归入城市，记忆与去过地点统计仍保留。",
                        ProfileNoticeTone.NEUTRAL
                    )
                )
            } else {
                next
            }
        }
        is ProfileOverviewMutation.FavoriteToggleStarted -> state.copy(
            busyFavoriteId = mutation.placeId
        )
        is ProfileOverviewMutation.FavoriteToggleSucceeded -> {
            val ids = state.favoriteIds.orEmpty()
            state.copy(
                favoriteIds = if (mutation.favorite) {
                    ids + mutation.placeId
                } else {
                    ids - mutation.placeId
                },
                busyFavoriteId = null,
                notice = state.notice?.takeUnless {
                    it.message == PROFILE_FAVORITE_FAILURE_NOTICE
                }
            )
        }
        ProfileOverviewMutation.FavoriteToggleFailed -> state.copy(
            busyFavoriteId = null,
            notice = ProfileNotice(
                PROFILE_FAVORITE_FAILURE_NOTICE,
                ProfileNoticeTone.ERROR
            )
        )
    }

    private fun partialNotice() = ProfileNotice(
        "部分本地数据暂时无法读取，当前仅展示可确认的城市档案。",
        ProfileNoticeTone.WARNING
    )
}

class ProfileOverviewStore(
    private val profileRepository: LocalProfileRepository,
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val capsuleRepository: CapsuleRepository,
    parentScope: CoroutineScope
) : MviStore<ProfileOverviewIntent, ProfileOverviewUiState, ProfileOverviewEffect> {
    private sealed interface Event {
        data class Intent(val value: ProfileOverviewIntent) : Event
        data class Mutation(
            val generation: Long?,
            val value: ProfileOverviewMutation
        ) : Event
        data class FavoriteResult(
            val operation: Long,
            val placeId: String,
            val result: StorageResult<Boolean>
        ) : Event
    }

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val effectChannel = Channel<ProfileOverviewEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(ProfileOverviewUiState())
    private var loadGeneration = 0L
    private var favoriteOperation = 0L
    private var disposed = false

    override val state: StateFlow<ProfileOverviewUiState> = mutableState.asStateFlow()
    override val effects: Flow<ProfileOverviewEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch {
            for (event in events) {
                when (event) {
                    is Event.Intent -> handleIntent(event.value)
                    is Event.Mutation -> handleMutation(event)
                    is Event.FavoriteResult -> handleFavoriteResult(event)
                }
            }
        }
    }

    override fun dispatch(intent: ProfileOverviewIntent) {
        if (!disposed) events.trySend(Event.Intent(intent))
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        events.close()
        effectChannel.close()
        scope.cancel()
    }

    private suspend fun handleIntent(intent: ProfileOverviewIntent) {
        when (intent) {
            ProfileOverviewIntent.Load,
            ProfileOverviewIntent.Retry -> startLoad()
            ProfileOverviewIntent.EditProfileClicked ->
                effectChannel.send(ProfileOverviewEffect.NavigateToEdit)
            ProfileOverviewIntent.SettingsClicked ->
                effectChannel.send(ProfileOverviewEffect.NavigateToSettings)
            ProfileOverviewIntent.WantToClicked ->
                effectChannel.send(ProfileOverviewEffect.NavigateToWantTo)
            is ProfileOverviewIntent.PlaceClicked ->
                effectChannel.send(ProfileOverviewEffect.NavigateToPlace(intent.placeId))
            is ProfileOverviewIntent.FavoriteToggled -> startFavoriteToggle(intent.placeId)
        }
    }

    private fun startLoad() {
        val generation = ++loadGeneration
        reduce(ProfileOverviewMutation.LoadStarted)
        profileRepository.getProfileSnapshot { snapshot ->
            enqueue(generation, ProfileOverviewMutation.ProfileLoaded(snapshot))
        }
    }

    private suspend fun handleMutation(event: Event.Mutation) {
        if (event.generation != null && event.generation != loadGeneration) return
        reduce(event.value)
        when (event.value) {
            is ProfileOverviewMutation.ProfileLoaded -> {
                val generation = event.generation ?: return
                placeRepository.getCatalogSnapshot { snapshot ->
                    enqueue(generation, ProfileOverviewMutation.CatalogLoaded(snapshot))
                }
            }
            is ProfileOverviewMutation.CatalogLoaded -> {
                val generation = event.generation ?: return
                favoriteRepository.getFavoriteIds { result ->
                    enqueue(generation, ProfileOverviewMutation.FavoritesLoaded(result))
                }
            }
            is ProfileOverviewMutation.FavoritesLoaded -> {
                val generation = event.generation ?: return
                capsuleRepository.getPublished { result ->
                    enqueue(generation, ProfileOverviewMutation.CapsulesLoaded(result))
                }
            }
            else -> Unit
        }
    }

    private fun startFavoriteToggle(placeId: String) {
        val current = mutableState.value
        if (
            current.status != ProfileOverviewStatus.READY ||
            current.catalogReadOnly ||
            current.favoriteIds == null ||
            current.busyFavoriteId != null ||
            placeId !in current.favoriteIds
        ) {
            return
        }
        val operation = ++favoriteOperation
        reduce(ProfileOverviewMutation.FavoriteToggleStarted(placeId))
        favoriteRepository.toggleFavorite(placeId) { result ->
            if (!disposed) {
                events.trySend(Event.FavoriteResult(operation, placeId, result))
            }
        }
    }

    private suspend fun handleFavoriteResult(event: Event.FavoriteResult) {
        if (event.operation != favoriteOperation) return
        when (val result = event.result) {
            is StorageResult.Success -> {
                reduce(
                    ProfileOverviewMutation.FavoriteToggleSucceeded(
                        event.placeId,
                        result.value
                    )
                )
                effectChannel.send(ProfileOverviewEffect.FavoritesChanged)
            }
            StorageResult.Missing,
            is StorageResult.Failure -> reduce(
                ProfileOverviewMutation.FavoriteToggleFailed
            )
        }
    }

    private fun enqueue(generation: Long, mutation: ProfileOverviewMutation) {
        if (!disposed) events.trySend(Event.Mutation(generation, mutation))
    }

    private fun reduce(mutation: ProfileOverviewMutation) {
        mutableState.value = ProfileOverviewReducer.reduce(mutableState.value, mutation)
    }
}

enum class ProfileEditorStatus { LOADING, READY, SAVING }

data class ProfileEditorUiState(
    val status: ProfileEditorStatus = ProfileEditorStatus.LOADING,
    val profile: LocalProfile = LocalProfile.DEFAULT,
    val savedProfile: LocalProfile = LocalProfile.DEFAULT,
    val validationMessage: String? = null,
    val notice: ProfileNotice? = null,
    val showDiscardConfirmation: Boolean = false,
    val avatarBusy: Boolean = false
) {
    val isBusy: Boolean
        get() = status != ProfileEditorStatus.READY || avatarBusy

    val isDirty: Boolean
        get() = profile != savedProfile
}

sealed interface ProfileEditorIntent {
    data object Load : ProfileEditorIntent
    data class DisplayNameChanged(val value: String) : ProfileEditorIntent
    data class AvatarChanged(val value: AvatarPreset) : ProfileEditorIntent
    data object PickAvatarPhoto : ProfileEditorIntent
    data class HomeCityChanged(val value: String) : ProfileEditorIntent
    data class BioChanged(val value: String) : ProfileEditorIntent
    data object SaveClicked : ProfileEditorIntent
    data object BackClicked : ProfileEditorIntent
    data object DismissDiscard : ProfileEditorIntent
    data object DiscardConfirmed : ProfileEditorIntent
}

sealed interface ProfileEditorEffect {
    data object NavigateBack : ProfileEditorEffect
    data object SavedAndNavigateBack : ProfileEditorEffect
}

internal sealed interface ProfileEditorMutation {
    data object LoadStarted : ProfileEditorMutation
    data class Loaded(val snapshot: LocalProfileSnapshot) : ProfileEditorMutation
    data class DisplayNameChanged(val value: String) : ProfileEditorMutation
    data class AvatarChanged(val value: AvatarPreset) : ProfileEditorMutation
    data object AvatarWorkStarted : ProfileEditorMutation
    data class ManagedAvatarReady(val path: String) : ProfileEditorMutation
    data class AvatarWorkFailed(val message: String?) : ProfileEditorMutation
    data class HomeCityChanged(val value: String) : ProfileEditorMutation
    data class BioChanged(val value: String) : ProfileEditorMutation
    data object ValidationFailed : ProfileEditorMutation
    data object SaveStarted : ProfileEditorMutation
    data class SaveSucceeded(val profile: LocalProfile) : ProfileEditorMutation
    data object SaveFailed : ProfileEditorMutation
    data object ShowDiscard : ProfileEditorMutation
    data object HideDiscard : ProfileEditorMutation
}

internal object ProfileEditorReducer {
    fun reduce(
        state: ProfileEditorUiState,
        mutation: ProfileEditorMutation
    ): ProfileEditorUiState = when (mutation) {
        ProfileEditorMutation.LoadStarted -> state.copy(
            status = ProfileEditorStatus.LOADING,
            validationMessage = null,
            notice = null
        )
        is ProfileEditorMutation.Loaded -> ProfileEditorUiState(
            status = ProfileEditorStatus.READY,
            profile = mutation.snapshot.profile,
            savedProfile = mutation.snapshot.profile,
            notice = if (mutation.snapshot.source == LocalProfileSource.DEFAULT_RECOVERY) {
                ProfileNotice(
                    "本地档案暂时无法读取，当前显示默认档案。",
                    ProfileNoticeTone.WARNING
                )
            } else {
                null
            }
        )
        is ProfileEditorMutation.DisplayNameChanged -> updateProfile(state) {
            copy(displayName = mutation.value)
        }
        is ProfileEditorMutation.AvatarChanged -> updateProfile(state) {
            copy(avatarPreset = mutation.value, avatarManagedPath = null)
        }
        ProfileEditorMutation.AvatarWorkStarted -> state.copy(
            avatarBusy = true,
            notice = ProfileNotice("正在准备头像…", ProfileNoticeTone.NEUTRAL)
        )
        is ProfileEditorMutation.ManagedAvatarReady -> state.copy(
            avatarBusy = false,
            profile = state.profile.copy(avatarManagedPath = mutation.path),
            notice = null
        )
        is ProfileEditorMutation.AvatarWorkFailed -> state.copy(
            avatarBusy = false,
            notice = mutation.message?.let { ProfileNotice(it, ProfileNoticeTone.ERROR) }
        )
        is ProfileEditorMutation.HomeCityChanged -> updateProfile(state) {
            copy(homeCity = mutation.value)
        }
        is ProfileEditorMutation.BioChanged -> updateProfile(state) {
            copy(bio = mutation.value)
        }
        ProfileEditorMutation.ValidationFailed -> state.copy(
            validationMessage = "请检查昵称、城市和简介的长度。"
        )
        ProfileEditorMutation.SaveStarted -> state.copy(
            status = ProfileEditorStatus.SAVING,
            validationMessage = null,
            notice = ProfileNotice("正在保存本地档案…", ProfileNoticeTone.NEUTRAL)
        )
        is ProfileEditorMutation.SaveSucceeded -> state.copy(
            status = ProfileEditorStatus.READY,
            profile = mutation.profile,
            savedProfile = mutation.profile,
            notice = null
        )
        ProfileEditorMutation.SaveFailed -> state.copy(
            status = ProfileEditorStatus.READY,
            notice = ProfileNotice(
                "保存失败，修改仍保留在当前页面，请重试。",
                ProfileNoticeTone.ERROR
            )
        )
        ProfileEditorMutation.ShowDiscard -> state.copy(showDiscardConfirmation = true)
        ProfileEditorMutation.HideDiscard -> state.copy(showDiscardConfirmation = false)
    }

    private fun updateProfile(
        state: ProfileEditorUiState,
        transform: LocalProfile.() -> LocalProfile
    ): ProfileEditorUiState = if (state.status == ProfileEditorStatus.READY) {
        state.copy(
            profile = state.profile.transform(),
            validationMessage = null,
            notice = state.notice?.takeUnless { it.tone == ProfileNoticeTone.ERROR }
        )
    } else {
        state
    }
}

class ProfileEditorStore(
    private val profileRepository: LocalProfileRepository,
    private val photoPicker: PhotoPickerCapability,
    private val avatarImages: AvatarImageCapability,
    parentScope: CoroutineScope
) : MviStore<ProfileEditorIntent, ProfileEditorUiState, ProfileEditorEffect> {
    constructor(
        profileRepository: LocalProfileRepository,
        parentScope: CoroutineScope
    ) : this(
        profileRepository = profileRepository,
        photoPicker = PhotoPickerCapability { _, callback -> callback(PhotoPickerResult.Unsupported) },
        avatarImages = object : AvatarImageCapability {
            override fun prepareAvatar(sourcePath: String, callback: (AvatarImageResult) -> Unit) {
                callback(AvatarImageResult.Unsupported)
            }

            override fun deleteAvatar(
                path: String,
                callback: (com.y.citycapsule.core.media.ManagedMediaDeleteResult) -> Unit
            ) {
                callback(com.y.citycapsule.core.media.ManagedMediaDeleteResult.Unsupported)
            }
        },
        parentScope = parentScope
    )
    private sealed interface Event {
        data class Intent(val value: ProfileEditorIntent) : Event
        data class Loaded(val snapshot: LocalProfileSnapshot) : Event
        data class Mutation(val value: ProfileEditorMutation) : Event
        data class Saved(
            val normalized: LocalProfile,
            val result: StorageResult<Unit>
        ) : Event
    }

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val effectChannel = Channel<ProfileEditorEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(ProfileEditorUiState())
    private var disposed = false

    override val state: StateFlow<ProfileEditorUiState> = mutableState.asStateFlow()
    override val effects: Flow<ProfileEditorEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch {
            for (event in events) {
                when (event) {
                    is Event.Intent -> handleIntent(event.value)
                    is Event.Loaded -> reduce(ProfileEditorMutation.Loaded(event.snapshot))
                    is Event.Mutation -> reduce(event.value)
                    is Event.Saved -> handleSaved(event)
                }
            }
        }
    }

    override fun dispatch(intent: ProfileEditorIntent) {
        if (!disposed) events.trySend(Event.Intent(intent))
    }

    override fun dispose() {
        if (disposed) return
        cleanupUnsavedAvatar()
        disposed = true
        events.close()
        effectChannel.close()
        scope.cancel()
    }

    private suspend fun handleIntent(intent: ProfileEditorIntent) {
        when (intent) {
            ProfileEditorIntent.Load -> {
                reduce(ProfileEditorMutation.LoadStarted)
                profileRepository.getProfileSnapshot { snapshot ->
                    if (!disposed) events.trySend(Event.Loaded(snapshot))
                }
            }
            is ProfileEditorIntent.DisplayNameChanged -> reduce(
                ProfileEditorMutation.DisplayNameChanged(intent.value)
            )
            is ProfileEditorIntent.AvatarChanged -> {
                cleanupUnsavedAvatar()
                reduce(ProfileEditorMutation.AvatarChanged(intent.value))
            }
            ProfileEditorIntent.PickAvatarPhoto -> pickAvatar()
            is ProfileEditorIntent.HomeCityChanged -> reduce(
                ProfileEditorMutation.HomeCityChanged(intent.value)
            )
            is ProfileEditorIntent.BioChanged -> reduce(
                ProfileEditorMutation.BioChanged(intent.value)
            )
            ProfileEditorIntent.SaveClicked -> save()
            ProfileEditorIntent.BackClicked -> {
                if (mutableState.value.isDirty) {
                    reduce(ProfileEditorMutation.ShowDiscard)
                } else {
                    effectChannel.send(ProfileEditorEffect.NavigateBack)
                }
            }
            ProfileEditorIntent.DismissDiscard -> reduce(ProfileEditorMutation.HideDiscard)
            ProfileEditorIntent.DiscardConfirmed -> {
                reduce(ProfileEditorMutation.HideDiscard)
                cleanupUnsavedAvatar()
                effectChannel.send(ProfileEditorEffect.NavigateBack)
            }
        }
    }

    private fun save() {
        val current = mutableState.value
        if (current.status != ProfileEditorStatus.READY) return
        val normalized = LocalProfileValidator.normalizeOrNull(current.profile)
        if (normalized == null) {
            reduce(ProfileEditorMutation.ValidationFailed)
            return
        }
        reduce(ProfileEditorMutation.SaveStarted)
        profileRepository.saveProfile(normalized) { result ->
            if (!disposed) events.trySend(Event.Saved(normalized, result))
        }
    }

    private fun pickAvatar() {
        if (mutableState.value.isBusy) return
        reduce(ProfileEditorMutation.AvatarWorkStarted)
        photoPicker.pickImages(1) { result ->
            when (result) {
                is PhotoPickerResult.Success -> prepareAvatar(result.paths.first())
                PhotoPickerResult.Cancelled -> reduceAsync(ProfileEditorMutation.AvatarWorkFailed(null))
                PhotoPickerResult.Unsupported -> reduceAsync(ProfileEditorMutation.AvatarWorkFailed("当前设备不支持选择头像照片。"))
                is PhotoPickerResult.Failure -> reduceAsync(ProfileEditorMutation.AvatarWorkFailed(result.message))
            }
        }
    }

    private fun prepareAvatar(sourcePath: String) {
        avatarImages.prepareAvatar(sourcePath) { result ->
            when (result) {
                is AvatarImageResult.Success -> {
                    val previous = mutableState.value.profile.avatarManagedPath
                    if (previous != null && previous != mutableState.value.savedProfile.avatarManagedPath) {
                        avatarImages.deleteAvatar(previous) { }
                    }
                    reduceAsync(ProfileEditorMutation.ManagedAvatarReady(result.path))
                }
                AvatarImageResult.Unsupported -> reduceAsync(ProfileEditorMutation.AvatarWorkFailed("当前设备不支持头像裁剪。"))
                is AvatarImageResult.Failure -> reduceAsync(ProfileEditorMutation.AvatarWorkFailed(result.message))
            }
        }
    }

    private fun cleanupUnsavedAvatar() {
        val current = mutableState.value.profile.avatarManagedPath
        val saved = mutableState.value.savedProfile.avatarManagedPath
        if (current != null && current != saved) avatarImages.deleteAvatar(current) { }
    }

    private fun reduceAsync(mutation: ProfileEditorMutation) {
        if (!disposed) events.trySend(Event.Mutation(mutation))
    }

    private suspend fun handleSaved(event: Event.Saved) {
        when (event.result) {
            is StorageResult.Success -> {
                val oldAvatar = mutableState.value.savedProfile.avatarManagedPath
                reduce(ProfileEditorMutation.SaveSucceeded(event.normalized))
                if (oldAvatar != null && oldAvatar != event.normalized.avatarManagedPath) {
                    avatarImages.deleteAvatar(oldAvatar) { }
                }
                effectChannel.send(ProfileEditorEffect.SavedAndNavigateBack)
            }
            StorageResult.Missing,
            is StorageResult.Failure -> reduce(ProfileEditorMutation.SaveFailed)
        }
    }

    private fun reduce(mutation: ProfileEditorMutation) {
        mutableState.value = ProfileEditorReducer.reduce(mutableState.value, mutation)
    }
}

internal fun buildCityFootprints(
    capsules: List<CityCapsule>,
    places: List<Place>
): List<ProfileCityFootprint> {
    val placeById = places.associateBy(Place::id)
    return capsules
        .mapNotNull { capsule ->
            placeById[capsule.placeId]?.let { place -> place.city.trim() to capsule.placeId }
        }
        .filter { (city, _) -> city.isNotEmpty() }
        .groupBy(Pair<String, String>::first)
        .map { (city, entries) ->
            ProfileCityFootprint(
                city = city,
                placeCount = entries.map(Pair<String, String>::second).toSet().size,
                memoryCount = entries.size
            )
        }
        .sortedWith(
            compareByDescending<ProfileCityFootprint> { it.memoryCount }
                .thenByDescending { it.placeCount }
                .thenBy { it.city }
        )
}

private const val PROFILE_WANT_TO_PREVIEW_LIMIT = 3
private const val PROFILE_FAVORITE_FAILURE_NOTICE =
    "移出想去失败，页面状态已保持不变。"
