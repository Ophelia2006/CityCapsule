package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalogSnapshot
import com.y.citycapsule.core.place.PlaceCatalogSource
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceFilter
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceSearchEngine
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.profile.LocalProfileSnapshot
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

enum class PlaceListMode {
    ALL,
    FAVORITES
}

enum class PlaceListUiStatus {
    LOADING,
    READY
}

enum class PlaceListContentState {
    LOADING,
    RESULTS,
    EMPTY_CATALOG,
    EMPTY_FAVORITES,
    NO_MATCHES,
    STORAGE_ERROR
}

data class PlaceListUiState(
    val status: PlaceListUiStatus = PlaceListUiStatus.LOADING,
    val mode: PlaceListMode = PlaceListMode.ALL,
    val homeCity: String? = null,
    val catalogPlaces: List<Place> = emptyList(),
    val visiblePlaces: List<Place> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val query: String = "",
    val filter: PlaceFilter = PlaceFilter(),
    val catalogSource: PlaceCatalogSource? = null,
    val readOnly: Boolean = false,
    val busyFavoriteId: String? = null,
    val notice: PlaceFeatureNotice? = null
) {
    val hasActiveFilters: Boolean
        get() = filter.categories.isNotEmpty() || activeAdvancedFilterCount > 0

    val activeAdvancedFilterCount: Int
        get() = listOf(
            !filter.city.isNullOrBlank(),
            !filter.district.isNullOrBlank(),
            filter.favoritesOnly && mode == PlaceListMode.ALL
        ).count { it }

    val directoryContext: String
        get() = homeCity?.takeIf(String::isNotBlank)?.let { "$it 优先 · 本地点目录" }
            ?: "本地点目录"

    val contentState: PlaceListContentState
        get() = when {
            status == PlaceListUiStatus.LOADING -> PlaceListContentState.LOADING
            catalogSource == PlaceCatalogSource.RECOVERY_READ_ONLY ->
                PlaceListContentState.STORAGE_ERROR
            visiblePlaces.isNotEmpty() -> PlaceListContentState.RESULTS
            catalogPlaces.isEmpty() -> PlaceListContentState.EMPTY_CATALOG
            mode == PlaceListMode.FAVORITES && query.isBlank() && !hasActiveFilters ->
                PlaceListContentState.EMPTY_FAVORITES
            else -> PlaceListContentState.NO_MATCHES
        }
}

sealed interface PlaceListIntent {
    data object Load : PlaceListIntent
    data object Retry : PlaceListIntent
    data class QueryChanged(val query: String) : PlaceListIntent
    data class CategoryToggled(val category: PlaceCategory) : PlaceListIntent
    data object CategoriesCleared : PlaceListIntent
    data class CityChanged(val city: String) : PlaceListIntent
    data class DistrictChanged(val district: String) : PlaceListIntent
    data object FavoritesOnlyToggled : PlaceListIntent
    data object ClearAdvancedFilters : PlaceListIntent
    data object ClearAllFilters : PlaceListIntent
    data class FavoriteToggled(val placeId: String) : PlaceListIntent
    data class PlaceClicked(val placeId: String) : PlaceListIntent
    data object CreatePlaceClicked : PlaceListIntent
    data object BackClicked : PlaceListIntent
    data object ExploreClicked : PlaceListIntent
}

sealed interface PlaceListEffect {
    data class NavigateToDetail(val placeId: String) : PlaceListEffect
    data object NavigateToEditor : PlaceListEffect
    data object NavigateBack : PlaceListEffect
    data object BackToExplore : PlaceListEffect
    data object FavoritesChanged : PlaceListEffect
}

internal sealed interface PlaceListMutation {
    data object LoadStarted : PlaceListMutation
    data class ProfileLoaded(
        val homeCity: String?,
        val recoveredWithWarning: Boolean
    ) : PlaceListMutation
    data class CatalogLoaded(val snapshot: PlaceCatalogSnapshot) : PlaceListMutation
    data class FavoritesLoaded(
        val result: StorageResult<FavoritePlaceIds>
    ) : PlaceListMutation
    data class QueryChanged(val query: String) : PlaceListMutation
    data class CategoryToggled(val category: PlaceCategory) : PlaceListMutation
    data object CategoriesCleared : PlaceListMutation
    data class CityChanged(val city: String) : PlaceListMutation
    data class DistrictChanged(val district: String) : PlaceListMutation
    data object FavoritesOnlyToggled : PlaceListMutation
    data object ClearAdvancedFilters : PlaceListMutation
    data object ClearAllFilters : PlaceListMutation
    data class FavoriteToggleStarted(val placeId: String) : PlaceListMutation
    data class FavoriteToggleSucceeded(
        val placeId: String,
        val favorite: Boolean
    ) : PlaceListMutation
    data object FavoriteToggleFailed : PlaceListMutation
}

internal object PlaceListReducer {
    fun reduce(
        state: PlaceListUiState,
        mutation: PlaceListMutation
    ): PlaceListUiState = when (mutation) {
        PlaceListMutation.LoadStarted -> state.copy(
            status = PlaceListUiStatus.LOADING,
            busyFavoriteId = null,
            notice = null
        )
        is PlaceListMutation.ProfileLoaded -> state.copy(
            homeCity = mutation.homeCity?.trim()?.takeIf(String::isNotEmpty),
            notice = if (mutation.recoveredWithWarning) {
                PlaceFeatureNotice(
                    "档案城市暂时无法读取，当前仍可浏览本地点目录。",
                    PlaceNoticeTone.WARNING
                )
            } else {
                state.notice
            }
        ).withSearchResults()
        is PlaceListMutation.CatalogLoaded -> state.copy(
            catalogPlaces = mutation.snapshot.catalog.places,
            catalogSource = mutation.snapshot.source,
            readOnly = mutation.snapshot.source == PlaceCatalogSource.RECOVERY_READ_ONLY,
            notice = sourceNotice(mutation.snapshot.source) ?: state.notice
        ).withSearchResults()
        is PlaceListMutation.FavoritesLoaded -> {
            val favorites = when (val result = mutation.result) {
                is StorageResult.Success -> result.value
                StorageResult.Missing,
                is StorageResult.Failure -> FavoritePlaceIds.EMPTY
            }
            state.copy(
                status = PlaceListUiStatus.READY,
                favoriteIds = favorites.placeIds,
                notice = if (mutation.result is StorageResult.Failure) {
                    PlaceFeatureNotice(
                        "想去状态暂不可用，地点目录仍可浏览。",
                        PlaceNoticeTone.WARNING
                    )
                } else {
                    state.notice
                }
            ).withSearchResults()
        }
        is PlaceListMutation.QueryChanged -> ifReady(state) {
            copy(query = mutation.query).withSearchResults()
        }
        is PlaceListMutation.CategoryToggled -> ifReady(state) {
            val categories = filter.categories.toMutableSet().apply {
                if (!add(mutation.category)) remove(mutation.category)
            }
            copy(filter = filter.copy(categories = categories)).withSearchResults()
        }
        PlaceListMutation.CategoriesCleared -> ifReady(state) {
            copy(filter = filter.copy(categories = emptySet())).withSearchResults()
        }
        is PlaceListMutation.CityChanged -> updateFilter(state) {
            copy(city = mutation.city)
        }
        is PlaceListMutation.DistrictChanged -> updateFilter(state) {
            copy(district = mutation.district)
        }
        PlaceListMutation.FavoritesOnlyToggled -> if (
            state.mode == PlaceListMode.ALL
        ) {
            updateFilter(state) { copy(favoritesOnly = !favoritesOnly) }
        } else {
            state
        }
        PlaceListMutation.ClearAdvancedFilters -> ifReady(state) {
            copy(
                filter = forcedFilter(mode).copy(categories = filter.categories)
            ).withSearchResults()
        }
        PlaceListMutation.ClearAllFilters -> ifReady(state) {
            copy(filter = forcedFilter(mode)).withSearchResults()
        }
        is PlaceListMutation.FavoriteToggleStarted -> state.copy(
            busyFavoriteId = mutation.placeId
        )
        is PlaceListMutation.FavoriteToggleSucceeded -> {
            val favoriteIds = if (mutation.favorite) {
                state.favoriteIds + mutation.placeId
            } else {
                state.favoriteIds - mutation.placeId
            }
            state.copy(
                favoriteIds = favoriteIds,
                busyFavoriteId = null,
                notice = state.notice?.takeUnless {
                    it.message == FAVORITE_FAILURE_NOTICE
                }
            ).withSearchResults()
        }
        PlaceListMutation.FavoriteToggleFailed -> state.copy(
            busyFavoriteId = null,
            notice = PlaceFeatureNotice(
                FAVORITE_FAILURE_NOTICE,
                PlaceNoticeTone.ERROR
            )
        )
    }

    private fun ifReady(
        state: PlaceListUiState,
        transform: PlaceListUiState.() -> PlaceListUiState
    ): PlaceListUiState = if (state.status == PlaceListUiStatus.READY) {
        state.transform()
    } else {
        state
    }

    private fun updateFilter(
        state: PlaceListUiState,
        transform: PlaceFilter.() -> PlaceFilter
    ): PlaceListUiState = ifReady(state) {
        val transformed = filter.transform()
        copy(
            filter = if (mode == PlaceListMode.FAVORITES) {
                transformed.copy(favoritesOnly = true)
            } else {
                transformed
            }
        ).withSearchResults()
    }
}

class PlaceListStore(
    private val profileRepository: LocalProfileRepository,
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    parentScope: CoroutineScope,
    mode: PlaceListMode = PlaceListMode.ALL,
    initialCategory: PlaceCategory? = null
) : MviStore<PlaceListIntent, PlaceListUiState, PlaceListEffect> {
    private sealed interface Event {
        data class Intent(val value: PlaceListIntent) : Event
        data class Mutation(
            val generation: Long?,
            val value: PlaceListMutation
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
    private val effectChannel = Channel<PlaceListEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(initialState(mode, initialCategory))
    private var loadGeneration = 0L
    private var favoriteOperation = 0L
    private var disposed = false

    override val state: StateFlow<PlaceListUiState> = mutableState.asStateFlow()
    override val effects: Flow<PlaceListEffect> = effectChannel.receiveAsFlow()

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

    override fun dispatch(intent: PlaceListIntent) {
        if (!disposed) events.trySend(Event.Intent(intent))
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        events.close()
        effectChannel.close()
        scope.cancel()
    }

    private suspend fun handleIntent(intent: PlaceListIntent) {
        when (intent) {
            PlaceListIntent.Load,
            PlaceListIntent.Retry -> startLoad()
            is PlaceListIntent.QueryChanged -> reduce(
                PlaceListMutation.QueryChanged(intent.query)
            )
            is PlaceListIntent.CategoryToggled -> reduce(
                PlaceListMutation.CategoryToggled(intent.category)
            )
            PlaceListIntent.CategoriesCleared -> reduce(
                PlaceListMutation.CategoriesCleared
            )
            is PlaceListIntent.CityChanged -> reduce(
                PlaceListMutation.CityChanged(intent.city)
            )
            is PlaceListIntent.DistrictChanged -> reduce(
                PlaceListMutation.DistrictChanged(intent.district)
            )
            PlaceListIntent.FavoritesOnlyToggled -> reduce(
                PlaceListMutation.FavoritesOnlyToggled
            )
            PlaceListIntent.ClearAdvancedFilters -> reduce(
                PlaceListMutation.ClearAdvancedFilters
            )
            PlaceListIntent.ClearAllFilters -> reduce(
                PlaceListMutation.ClearAllFilters
            )
            is PlaceListIntent.FavoriteToggled -> startFavoriteToggle(intent.placeId)
            is PlaceListIntent.PlaceClicked -> effectChannel.send(
                PlaceListEffect.NavigateToDetail(intent.placeId)
            )
            PlaceListIntent.CreatePlaceClicked -> effectChannel.send(
                PlaceListEffect.NavigateToEditor
            )
            PlaceListIntent.BackClicked -> effectChannel.send(
                PlaceListEffect.NavigateBack
            )
            PlaceListIntent.ExploreClicked -> effectChannel.send(
                PlaceListEffect.BackToExplore
            )
        }
    }

    private fun startLoad() {
        val generation = ++loadGeneration
        reduce(PlaceListMutation.LoadStarted)
        profileRepository.getProfileSnapshot { snapshot ->
            enqueue(generation, profileMutation(snapshot))
        }
    }

    private suspend fun handleMutation(event: Event.Mutation) {
        if (event.generation != null && event.generation != loadGeneration) return
        reduce(event.value)
        when (event.value) {
            is PlaceListMutation.ProfileLoaded -> {
                val generation = event.generation ?: return
                placeRepository.getCatalogSnapshot { snapshot ->
                    enqueue(generation, PlaceListMutation.CatalogLoaded(snapshot))
                }
            }
            is PlaceListMutation.CatalogLoaded -> {
                val generation = event.generation ?: return
                favoriteRepository.getFavoriteIds { result ->
                    enqueue(generation, PlaceListMutation.FavoritesLoaded(result))
                }
            }
            else -> Unit
        }
    }

    private fun startFavoriteToggle(placeId: String) {
        val current = mutableState.value
        if (
            current.status != PlaceListUiStatus.READY ||
            current.readOnly ||
            current.busyFavoriteId != null
        ) return
        val operation = ++favoriteOperation
        reduce(PlaceListMutation.FavoriteToggleStarted(placeId))
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
                    PlaceListMutation.FavoriteToggleSucceeded(
                        event.placeId,
                        result.value
                    )
                )
                effectChannel.send(PlaceListEffect.FavoritesChanged)
            }
            StorageResult.Missing,
            is StorageResult.Failure -> reduce(
                PlaceListMutation.FavoriteToggleFailed
            )
        }
    }

    private fun enqueue(generation: Long, mutation: PlaceListMutation) {
        if (!disposed) events.trySend(Event.Mutation(generation, mutation))
    }

    private fun reduce(mutation: PlaceListMutation) {
        mutableState.value = PlaceListReducer.reduce(mutableState.value, mutation)
    }

    private fun profileMutation(snapshot: LocalProfileSnapshot) =
        PlaceListMutation.ProfileLoaded(
            homeCity = snapshot.profile.homeCity,
            recoveredWithWarning = snapshot.warning != null
        )
}

internal fun initialState(
    mode: PlaceListMode,
    initialCategory: PlaceCategory?
): PlaceListUiState = PlaceListUiState(
    mode = mode,
    filter = forcedFilter(mode).copy(
        categories = initialCategory?.let(::setOf).orEmpty()
    )
)

internal fun forcedFilter(mode: PlaceListMode): PlaceFilter = PlaceFilter(
    favoritesOnly = mode == PlaceListMode.FAVORITES
)

internal fun PlaceListUiState.withSearchResults(): PlaceListUiState {
    val result = PlaceSearchEngine.search(
        places = catalogPlaces,
        favoriteIds = favoriteIds,
        query = query,
        filter = filter
    )
    val currentCity = homeCity?.trim().orEmpty()
    val places = if (
        query.isBlank() &&
        filter.city.isNullOrBlank() &&
        currentCity.isNotEmpty()
    ) {
        result.places.sortedBy { place ->
            if (place.city.equals(currentCity, ignoreCase = true)) 0 else 1
        }
    } else {
        result.places
    }
    return copy(visiblePlaces = places, filter = result.appliedFilter)
}

private fun sourceNotice(source: PlaceCatalogSource): PlaceFeatureNotice? = when (source) {
    PlaceCatalogSource.PERSISTED -> null
    PlaceCatalogSource.INITIALIZED -> PlaceFeatureNotice(
        "已准备 8 个离线示例地点，也可以添加自己的地点。",
        PlaceNoticeTone.NEUTRAL
    )
    PlaceCatalogSource.MEMORY_FALLBACK -> PlaceFeatureNotice(
        "本地存储暂不可用，当前显示内存中的示例地点。",
        PlaceNoticeTone.WARNING
    )
    PlaceCatalogSource.RECOVERY_READ_ONLY -> PlaceFeatureNotice(
        "地点数据无法安全解码，已进入只读恢复状态。",
        PlaceNoticeTone.ERROR
    )
}

private const val FAVORITE_FAILURE_NOTICE = "想去操作失败，页面状态已保持不变。"
