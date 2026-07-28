package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalogSource
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceFilter
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceSearchEngine
import com.y.citycapsule.core.storage.StorageResult

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
        get() = filter.categories.isNotEmpty() ||
            !filter.city.isNullOrBlank() ||
            !filter.district.isNullOrBlank() ||
            (filter.favoritesOnly && mode == PlaceListMode.ALL)

    val contentState: PlaceListContentState
        get() = when {
            status == PlaceListUiStatus.LOADING -> PlaceListContentState.LOADING
            catalogSource == PlaceCatalogSource.RECOVERY_READ_ONLY ->
                PlaceListContentState.STORAGE_ERROR
            visiblePlaces.isNotEmpty() -> PlaceListContentState.RESULTS
            catalogPlaces.isEmpty() -> PlaceListContentState.EMPTY_CATALOG
            mode == PlaceListMode.FAVORITES &&
                query.isBlank() &&
                !hasActiveFilters -> PlaceListContentState.EMPTY_FAVORITES
            else -> PlaceListContentState.NO_MATCHES
        }
}

class PlaceListStateHolder(
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val mode: PlaceListMode = PlaceListMode.ALL,
    private val onDataChanged: () -> Unit = {},
    private val onStateChanged: (PlaceListUiState) -> Unit = {}
) {
    var state: PlaceListUiState = initialState(mode)
        private set

    private var loadGeneration = 0

    fun load() {
        val generation = ++loadGeneration
        update(state.copy(status = PlaceListUiStatus.LOADING, busyFavoriteId = null))
        placeRepository.getCatalogSnapshot { snapshot ->
            if (generation != loadGeneration) {
                return@getCatalogSnapshot
            }
            val sourceNotice = when (snapshot.source) {
                PlaceCatalogSource.PERSISTED -> null
                PlaceCatalogSource.INITIALIZED -> PlaceFeatureNotice(
                    "已准备 8 个离线示例地点，可继续新建自己的地点。",
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
            favoriteRepository.getFavoriteIds { favoriteResult ->
                if (generation != loadGeneration) {
                    return@getFavoriteIds
                }
                val favorites = when (favoriteResult) {
                    is StorageResult.Success -> favoriteResult.value
                    StorageResult.Missing -> FavoritePlaceIds.EMPTY
                    is StorageResult.Failure -> FavoritePlaceIds.EMPTY
                }
                val favoriteNotice = if (favoriteResult is StorageResult.Failure) {
                    PlaceFeatureNotice(
                        "想去状态暂不可用，地点目录仍可浏览。",
                        PlaceNoticeTone.WARNING
                    )
                } else {
                    null
                }
                update(
                    state.copy(
                        status = PlaceListUiStatus.READY,
                        catalogPlaces = snapshot.catalog.places,
                        favoriteIds = favorites.placeIds,
                        catalogSource = snapshot.source,
                        readOnly = snapshot.source == PlaceCatalogSource.RECOVERY_READ_ONLY,
                        notice = favoriteNotice ?: sourceNotice
                    ).withSearchResults()
                )
            }
        }
    }

    fun updateQuery(query: String) {
        if (state.status == PlaceListUiStatus.READY) {
            update(state.copy(query = query).withSearchResults())
        }
    }

    fun toggleCategory(category: PlaceCategory) {
        if (state.status != PlaceListUiStatus.READY) {
            return
        }
        val categories = state.filter.categories.toMutableSet().apply {
            if (!add(category)) {
                remove(category)
            }
        }
        update(
            state.copy(
                filter = state.filter.copy(categories = categories)
            ).withSearchResults()
        )
    }

    fun updateCity(city: String) {
        updateFilter { copy(city = city) }
    }

    fun updateDistrict(district: String) {
        updateFilter { copy(district = district) }
    }

    fun toggleFavoritesOnly() {
        if (mode == PlaceListMode.ALL) {
            updateFilter { copy(favoritesOnly = !favoritesOnly) }
        }
    }

    fun clearFilters() {
        if (state.status == PlaceListUiStatus.READY) {
            update(
                state.copy(
                    filter = forcedFilter(mode)
                ).withSearchResults()
            )
        }
    }

    fun toggleFavorite(placeId: String) {
        if (state.status != PlaceListUiStatus.READY ||
            state.readOnly ||
            state.busyFavoriteId != null
        ) {
            return
        }
        update(state.copy(busyFavoriteId = placeId, notice = null))
        favoriteRepository.toggleFavorite(placeId) { result ->
            when (result) {
                is StorageResult.Success -> {
                    val ids = if (result.value) {
                        state.favoriteIds + placeId
                    } else {
                        state.favoriteIds - placeId
                    }
                    update(
                        state.copy(
                            favoriteIds = ids,
                            busyFavoriteId = null,
                            notice = PlaceFeatureNotice(
                                if (result.value) "已加入想去。" else "已移出想去。",
                                PlaceNoticeTone.SUCCESS
                            )
                        ).withSearchResults()
                    )
                    onDataChanged()
                }
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        busyFavoriteId = null,
                        notice = PlaceFeatureNotice(
                            "想去操作失败，页面状态已保持不变。",
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    private fun updateFilter(transform: PlaceFilter.() -> PlaceFilter) {
        if (state.status != PlaceListUiStatus.READY) {
            return
        }
        val transformed = state.filter.transform()
        update(
            state.copy(
                filter = if (mode == PlaceListMode.FAVORITES) {
                    transformed.copy(favoritesOnly = true)
                } else {
                    transformed
                }
            ).withSearchResults()
        )
    }

    private fun PlaceListUiState.withSearchResults(): PlaceListUiState {
        val result = PlaceSearchEngine.search(
            places = catalogPlaces,
            favoriteIds = favoriteIds,
            query = query,
            filter = filter
        )
        return copy(
            visiblePlaces = result.places,
            filter = result.appliedFilter
        )
    }

    private fun update(nextState: PlaceListUiState) {
        state = nextState
        onStateChanged(nextState)
    }

    private companion object {
        fun initialState(mode: PlaceListMode): PlaceListUiState =
            PlaceListUiState(mode = mode, filter = forcedFilter(mode))

        fun forcedFilter(mode: PlaceListMode): PlaceFilter =
            PlaceFilter(favoritesOnly = mode == PlaceListMode.FAVORITES)
    }
}
