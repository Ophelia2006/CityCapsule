package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.city.CityRegistry
import com.y.citycapsule.core.city.ExploreCityRepository
import com.y.citycapsule.core.city.ExploreCityRuntime
import com.y.citycapsule.core.city.ExploreCitySelection
import com.y.citycapsule.core.city.ReverseGeocodeCapability
import com.y.citycapsule.core.city.ReverseGeocodeResult
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.location.GeoDistance
import com.y.citycapsule.core.location.LocationCapability
import com.y.citycapsule.core.location.LocationResult
import com.y.citycapsule.core.location.CurrentLocationRuntime
import com.y.citycapsule.core.map.ExploreMapViewState
import com.y.citycapsule.core.map.MapAvailability
import com.y.citycapsule.core.map.MapCameraModel
import com.y.citycapsule.core.map.MapMarkerModel
import com.y.citycapsule.core.map.MapViewEvent
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalogSnapshot
import com.y.citycapsule.core.place.PlaceCatalogSource
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceFilter
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.PlacePhotoCacheEntry
import com.y.citycapsule.core.place.PlacePhotoCacheRepository
import com.y.citycapsule.core.place.PlacePhotoHydrator
import com.y.citycapsule.core.place.RemotePlace
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.place.PlaceSearchEngine
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

enum class PlaceLocationStatus {
    IDLE, REQUESTING, AVAILABLE, PERMISSION_DENIED,
    PERMISSION_PERMANENTLY_DENIED, SERVICE_DISABLED, UNAVAILABLE, FAILURE
}

enum class PlaceDirectoryViewMode { LIST, MAP }
enum class OnlinePlaceStatus { IDLE, LOADING, RESULTS, EMPTY, ERROR, UNAVAILABLE }

data class PlaceListUiState(
    val status: PlaceListUiStatus = PlaceListUiStatus.LOADING,
    val mode: PlaceListMode = PlaceListMode.ALL,
    val selectedCity: CityDefinition = CityRegistry.byId(CityRegistry.DEFAULT_CITY_ID)!!,
    val recentCityIds: List<String> = listOf(CityRegistry.DEFAULT_CITY_ID),
    val browseAllCities: Boolean = false,
    val detectedCity: CityDefinition? = null,
    val catalogPlaces: List<Place> = emptyList(),
    val visiblePlaces: List<Place> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val photoByPlaceId: Map<String, PlacePhotoCacheEntry> = emptyMap(),
    val query: String = "",
    val filter: PlaceFilter = PlaceFilter(),
    val catalogSource: PlaceCatalogSource? = null,
    val readOnly: Boolean = false,
    val busyFavoriteId: String? = null,
    val notice: PlaceFeatureNotice? = null,
    val locationStatus: PlaceLocationStatus = PlaceLocationStatus.IDLE,
    val currentLocation: GeoPoint? = null,
    val locationAccuracyMeters: Double? = null,
    val locationMessage: String? = null,
    val viewMode: PlaceDirectoryViewMode = PlaceDirectoryViewMode.LIST,
    val showMapPrivacyPrompt: Boolean = false,
    val mapPrivacyAccepted: Boolean = false,
    val selectedMapPlaceId: String? = null,
    val mapCamera: MapCameraModel? = null,
    val onlineStatus: OnlinePlaceStatus = OnlinePlaceStatus.IDLE,
    val onlinePlaces: List<RemotePlace> = emptyList(),
    val importingProviderId: String? = null
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
        get() = if (browseAllCities) "全部城市" else "${selectedCity.displayName} · 本地点目录"

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

    fun distanceLabel(place: Place): String? {
        val origin = currentLocation ?: return null
        val destination = place.geoPoint ?: return null
        return GeoDistance.label(GeoDistance.meters(origin, destination))
    }

    val mapViewState: ExploreMapViewState
        get() {
            val markers = visiblePlaces.mapNotNull { place ->
                place.geoPoint?.let { MapMarkerModel(place.id, place.name, it) }
            }
            val fallbackCenter = markers.firstOrNull()?.position ?: selectedCity.centerPoint
            return ExploreMapViewState(
                markers = markers,
                selectedPlaceId = selectedMapPlaceId,
                camera = mapCamera ?: fallbackCenter?.let { MapCameraModel(it, 12.0) },
                currentLocation = currentLocation,
                showCurrentLocation = currentLocation != null
            )
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
    data object CurrentLocationRequested : PlaceListIntent
    data class ExploreCitySelected(val cityId: String) : PlaceListIntent
    data object AllCitiesSelected : PlaceListIntent
    data object DetectedCityConfirmed : PlaceListIntent
    data object DetectedCityDismissed : PlaceListIntent
    data object ListViewSelected : PlaceListIntent
    data object MapViewSelected : PlaceListIntent
    data object MapPrivacyAccepted : PlaceListIntent
    data object MapPrivacyDeclined : PlaceListIntent
    data class MapEventReceived(val event: MapViewEvent) : PlaceListIntent
    data object OnlineSearchRequested : PlaceListIntent
    data object OnlineResultsDismissed : PlaceListIntent
    data class RemotePlaceImportRequested(val providerId: String) : PlaceListIntent
    data class CachedPhotoFailed(val placeId: String) : PlaceListIntent
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
    data class CityContextLoaded(val selection: ExploreCitySelection) : PlaceListMutation
    data class CityContextFailed(val message: String) : PlaceListMutation
    data class DetectedCity(val city: CityDefinition?) : PlaceListMutation
    data object AllCities : PlaceListMutation
    data object DetectionDismissed : PlaceListMutation
    data class CatalogLoaded(val snapshot: PlaceCatalogSnapshot) : PlaceListMutation
    data class FavoritesLoaded(
        val result: StorageResult<FavoritePlaceIds>
    ) : PlaceListMutation
    data class PhotoCacheLoaded(
        val result: StorageResult<Map<String, PlacePhotoCacheEntry>>
    ) : PlaceListMutation
    data class CachedPhotoRemoved(val placeId: String) : PlaceListMutation
    data class CachedPhotoAdded(val entry: PlacePhotoCacheEntry) : PlaceListMutation
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
    data object LocationStarted : PlaceListMutation
    data class LocationResolved(val result: LocationResult) : PlaceListMutation
    data object ListViewSelected : PlaceListMutation
    data object MapViewSelected : PlaceListMutation
    data object MapPrivacyPrompted : PlaceListMutation
    data object MapPrivacyAccepted : PlaceListMutation
    data object MapPrivacyDeclined : PlaceListMutation
    data class MapMarkerSelected(val placeId: String) : PlaceListMutation
    data class MapCameraChanged(val camera: MapCameraModel) : PlaceListMutation
    data class MapUnavailable(val reason: MapAvailability) : PlaceListMutation
    data object OnlineSearchStarted : PlaceListMutation
    data class OnlineSearchFinished(val result: RemotePlaceResult) : PlaceListMutation
    data object OnlineResultsDismissed : PlaceListMutation
    data class RemoteImportStarted(val providerId: String) : PlaceListMutation
    data class RemoteImportFinished(val place: Place?) : PlaceListMutation
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
        is PlaceListMutation.CityContextLoaded -> {
            val city = CityRegistry.byId(mutation.selection.selectedCityId) ?: state.selectedCity
            state.copy(
                selectedCity = city,
                recentCityIds = mutation.selection.recentCityIds,
                browseAllCities = false,
                detectedCity = null,
                filter = if (state.mode == PlaceListMode.ALL) {
                    state.filter.copy(city = city.displayName)
                } else {
                    state.filter
                }
            ).withSearchResults()
        }
        is PlaceListMutation.CityContextFailed -> state.copy(
            notice = PlaceFeatureNotice(mutation.message, PlaceNoticeTone.WARNING)
        )
        is PlaceListMutation.DetectedCity -> state.copy(
            detectedCity = mutation.city,
            locationMessage = if (mutation.city == null) {
                "当前位置不在已支持城市中，仍可手动选择城市。"
            } else {
                "已识别为${mutation.city.displayName}，确认后切换探索城市。"
            }
        )
        PlaceListMutation.AllCities -> state.copy(
            browseAllCities = true,
            detectedCity = null,
            filter = state.filter.copy(city = null)
        ).withSearchResults()
        PlaceListMutation.DetectionDismissed -> state.copy(detectedCity = null)
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
        is PlaceListMutation.PhotoCacheLoaded -> state.copy(
            photoByPlaceId = (mutation.result as? StorageResult.Success)?.value.orEmpty()
        )
        is PlaceListMutation.CachedPhotoRemoved -> state.copy(
            photoByPlaceId = state.photoByPlaceId - mutation.placeId
        )
        is PlaceListMutation.CachedPhotoAdded -> state.copy(
            photoByPlaceId = state.photoByPlaceId + (mutation.entry.placeId to mutation.entry)
        )
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
            copy(filter = forcedFilter(mode).copy(
                categories = filter.categories,
                city = selectedCity.displayName.takeIf { mode == PlaceListMode.ALL }
            ), browseAllCities = false).withSearchResults()
        }
        PlaceListMutation.ClearAllFilters -> ifReady(state) {
            copy(filter = forcedFilter(mode).copy(
                city = selectedCity.displayName.takeIf { mode == PlaceListMode.ALL }
            ), browseAllCities = false).withSearchResults()
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
        PlaceListMutation.LocationStarted -> state.copy(
            locationStatus = PlaceLocationStatus.REQUESTING,
            currentLocation = null,
            locationAccuracyMeters = null,
            locationMessage = null
        )
        is PlaceListMutation.LocationResolved -> when (val result = mutation.result) {
            is LocationResult.Success -> state.copy(
                locationStatus = PlaceLocationStatus.AVAILABLE,
                currentLocation = result.point,
                locationAccuracyMeters = result.accuracyMeters,
                locationMessage = "已按当前位置显示直线距离。",
                mapCamera = MapCameraModel(result.point, 14.0)
            )
            LocationResult.PermissionDenied -> state.locationFailure(
                PlaceLocationStatus.PERMISSION_DENIED, "未获得定位权限，地点目录仍可浏览。"
            )
            LocationResult.PermissionPermanentlyDenied -> state.locationFailure(
                PlaceLocationStatus.PERMISSION_PERMANENTLY_DENIED,
                "定位权限已被长期拒绝，可在系统设置中重新开启。"
            )
            LocationResult.ServiceDisabled -> state.locationFailure(
                PlaceLocationStatus.SERVICE_DISABLED, "系统定位服务已关闭。"
            )
            LocationResult.Unavailable -> state.locationFailure(
                PlaceLocationStatus.UNAVAILABLE, "当前设备暂不支持定位。"
            )
            is LocationResult.Failure -> state.locationFailure(
                PlaceLocationStatus.FAILURE, result.message
            )
        }
        PlaceListMutation.ListViewSelected -> state.copy(
            viewMode = PlaceDirectoryViewMode.LIST,
            showMapPrivacyPrompt = false
        )
        PlaceListMutation.MapViewSelected -> state.copy(
            viewMode = PlaceDirectoryViewMode.MAP,
            showMapPrivacyPrompt = false
        )
        PlaceListMutation.MapPrivacyPrompted -> state.copy(showMapPrivacyPrompt = true)
        PlaceListMutation.MapPrivacyAccepted -> state.copy(
            mapPrivacyAccepted = true,
            showMapPrivacyPrompt = false,
            viewMode = PlaceDirectoryViewMode.MAP
        )
        PlaceListMutation.MapPrivacyDeclined -> state.copy(
            showMapPrivacyPrompt = false,
            viewMode = PlaceDirectoryViewMode.LIST
        )
        is PlaceListMutation.MapMarkerSelected -> state.copy(
            selectedMapPlaceId = mutation.placeId
        )
        is PlaceListMutation.MapCameraChanged -> state.copy(mapCamera = mutation.camera)
        is PlaceListMutation.MapUnavailable -> state.copy(
            viewMode = PlaceDirectoryViewMode.LIST,
            selectedMapPlaceId = null,
            notice = PlaceFeatureNotice(
                when (mutation.reason) {
                    MapAvailability.MissingConfiguration -> "地图尚未配置，已返回地点列表。"
                    MapAvailability.Offline -> "当前网络不可用，已返回地点列表。"
                    MapAvailability.Unsupported -> "当前设备不支持地图，已返回地点列表。"
                    is MapAvailability.Failure -> mutation.reason.message
                    MapAvailability.Ready -> "地图暂时不可用，已返回地点列表。"
                },
                PlaceNoticeTone.WARNING
            )
        )
        PlaceListMutation.OnlineSearchStarted -> state.copy(
            onlineStatus = OnlinePlaceStatus.LOADING,
            onlinePlaces = emptyList(),
            notice = null
        )
        is PlaceListMutation.OnlineSearchFinished -> when (val result = mutation.result) {
            is RemotePlaceResult.Success -> state.copy(
                onlineStatus = if (result.places.isEmpty()) OnlinePlaceStatus.EMPTY else OnlinePlaceStatus.RESULTS,
                onlinePlaces = result.places
            )
            is RemotePlaceResult.Failure -> state.copy(
                onlineStatus = OnlinePlaceStatus.ERROR,
                notice = PlaceFeatureNotice(result.message, PlaceNoticeTone.WARNING)
            )
            RemotePlaceResult.Unavailable -> state.copy(
                onlineStatus = OnlinePlaceStatus.UNAVAILABLE,
                notice = PlaceFeatureNotice("在线地点服务当前不可用，本地点仍可浏览。", PlaceNoticeTone.WARNING)
            )
        }
        PlaceListMutation.OnlineResultsDismissed -> state.copy(
            onlineStatus = OnlinePlaceStatus.IDLE,
            onlinePlaces = emptyList(),
            importingProviderId = null
        )
        is PlaceListMutation.RemoteImportStarted -> state.copy(importingProviderId = mutation.providerId)
        is PlaceListMutation.RemoteImportFinished -> if (mutation.place != null) state.copy(
            catalogPlaces = (state.catalogPlaces + mutation.place).distinctBy(Place::id),
            importingProviderId = null,
            onlinePlaces = state.onlinePlaces.filterNot {
                mutation.place.contentSource?.endsWith(it.providerId) == true
            },
            notice = PlaceFeatureNotice("地点已保存到本地。", PlaceNoticeTone.SUCCESS)
        ).withSearchResults() else state.copy(
            importingProviderId = null,
            notice = PlaceFeatureNotice("地点保存失败，请重试。", PlaceNoticeTone.ERROR)
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
    private val cityRepository: ExploreCityRepository,
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val photoCacheRepository: PlacePhotoCacheRepository = PlacePhotoCacheRepository.NONE,
    private val locationCapability: LocationCapability = LocationCapability {
        it(LocationResult.Unavailable)
    },
    private val reverseGeocodeCapability: ReverseGeocodeCapability = ReverseGeocodeCapability {
        _, callback -> callback(ReverseGeocodeResult.UnsupportedCity())
    },
    private val remoteDataSource: PlaceRemoteDataSource? = null,
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
        data class LocationResultEvent(
            val operation: Long,
            val result: LocationResult
        ) : Event
        data class CitySelectionResult(val result: StorageResult<ExploreCitySelection>) : Event
        data class ReverseGeocodeResultEvent(val result: ReverseGeocodeResult) : Event
        data class OnlineResultEvent(val result: RemotePlaceResult) : Event
        data class ImportResultEvent(val result: StorageResult<Place>) : Event
        data class PhotoResolvedEvent(
            val generation: Long,
            val entry: PlacePhotoCacheEntry
        ) : Event
    }

    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val effectChannel = Channel<PlaceListEffect>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(initialState(mode, initialCategory))
    private var loadGeneration = 0L
    private var favoriteOperation = 0L
    private var locationOperation = 0L
    private var disposed = false
    private val photoHydrator = remoteDataSource?.let {
        PlacePhotoHydrator(it, photoCacheRepository)
    }

    override val state: StateFlow<PlaceListUiState> = mutableState.asStateFlow()
    override val effects: Flow<PlaceListEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch {
            for (event in events) {
                when (event) {
                    is Event.Intent -> handleIntent(event.value)
                    is Event.Mutation -> handleMutation(event)
                    is Event.FavoriteResult -> handleFavoriteResult(event)
                    is Event.LocationResultEvent -> handleLocationResult(event)
                    is Event.CitySelectionResult -> handleCitySelectionResult(event.result)
                    is Event.ReverseGeocodeResultEvent -> handleReverseGeocodeResult(event.result)
                    is Event.OnlineResultEvent -> reduce(PlaceListMutation.OnlineSearchFinished(event.result))
                    is Event.ImportResultEvent -> handleImportResult(event.result)
                    is Event.PhotoResolvedEvent -> if (event.generation == loadGeneration) {
                        reduce(PlaceListMutation.CachedPhotoAdded(event.entry))
                    }
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
        photoHydrator?.dispose()
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
            PlaceListIntent.CurrentLocationRequested -> startLocationRequest()
            is PlaceListIntent.ExploreCitySelected -> selectCity(intent.cityId)
            PlaceListIntent.AllCitiesSelected -> reduce(PlaceListMutation.AllCities)
            PlaceListIntent.DetectedCityConfirmed -> mutableState.value.detectedCity?.let { selectCity(it.id) }
            PlaceListIntent.DetectedCityDismissed -> reduce(PlaceListMutation.DetectionDismissed)
            PlaceListIntent.ListViewSelected -> reduce(PlaceListMutation.ListViewSelected)
            PlaceListIntent.MapViewSelected -> reduce(
                if (mutableState.value.mapPrivacyAccepted) {
                    PlaceListMutation.MapViewSelected
                } else {
                    PlaceListMutation.MapPrivacyPrompted
                }
            )
            PlaceListIntent.MapPrivacyAccepted -> reduce(PlaceListMutation.MapPrivacyAccepted)
            PlaceListIntent.MapPrivacyDeclined -> reduce(PlaceListMutation.MapPrivacyDeclined)
            is PlaceListIntent.MapEventReceived -> when (val event = intent.event) {
                is MapViewEvent.Ready -> event.camera?.let {
                    reduce(PlaceListMutation.MapCameraChanged(it))
                }
                is MapViewEvent.MarkerSelected -> {
                    if (mutableState.value.visiblePlaces.any { it.id == event.placeId }) {
                        reduce(PlaceListMutation.MapMarkerSelected(event.placeId))
                    }
                }
                is MapViewEvent.CameraChanged -> reduce(
                    PlaceListMutation.MapCameraChanged(event.camera)
                )
                is MapViewEvent.Unavailable -> reduce(
                    PlaceListMutation.MapUnavailable(event.reason)
                )
            }
            PlaceListIntent.OnlineSearchRequested -> startOnlineSearch()
            PlaceListIntent.OnlineResultsDismissed -> reduce(PlaceListMutation.OnlineResultsDismissed)
            is PlaceListIntent.RemotePlaceImportRequested -> startRemoteImport(intent.providerId)
            is PlaceListIntent.CachedPhotoFailed -> {
                reduce(PlaceListMutation.CachedPhotoRemoved(intent.placeId))
                photoCacheRepository.remove(intent.placeId)
            }
        }
    }

    private fun startOnlineSearch() {
        val remote = remoteDataSource ?: run {
            reduce(PlaceListMutation.OnlineSearchFinished(RemotePlaceResult.Unavailable))
            return
        }
        val current = mutableState.value
        if (current.onlineStatus == OnlinePlaceStatus.LOADING) return
        reduce(PlaceListMutation.OnlineSearchStarted)
        remote.search(
            query = current.query,
            city = current.selectedCity.displayName,
            near = current.currentLocation.takeIf { current.query.isBlank() }
        ) { result -> if (!disposed) events.trySend(Event.OnlineResultEvent(result)) }
    }

    private fun startRemoteImport(providerId: String) {
        val current = mutableState.value
        if (current.importingProviderId != null || current.readOnly) return
        val remote = current.onlinePlaces.firstOrNull { it.providerId == providerId } ?: return
        val existing = current.catalogPlaces.firstOrNull {
            it.contentSource?.endsWith(providerId) == true ||
                (it.name == remote.name && it.city == remote.city && it.address == remote.address)
        }
        if (existing != null) {
            reduce(PlaceListMutation.RemoteImportFinished(existing))
            return
        }
        reduce(PlaceListMutation.RemoteImportStarted(providerId))
        placeRepository.createPlace(remote.toImportedDraft()) { result ->
            if (!disposed) events.trySend(Event.ImportResultEvent(result))
        }
    }

    private fun handleImportResult(result: StorageResult<Place>) {
        reduce(PlaceListMutation.RemoteImportFinished((result as? StorageResult.Success)?.value))
        if (result is StorageResult.Success) PlaceFeatureRuntime.invalidate()
    }

    private fun startLoad() {
        val generation = ++loadGeneration
        reduce(PlaceListMutation.LoadStarted)
        cityRepository.get { result ->
            enqueue(
                generation,
                if (result is StorageResult.Success) PlaceListMutation.CityContextLoaded(result.value)
                else PlaceListMutation.CityContextFailed("探索城市暂时无法读取，当前使用上海目录。")
            )
        }
    }

    private suspend fun handleMutation(event: Event.Mutation) {
        if (event.generation != null && event.generation != loadGeneration) return
        reduce(event.value)
        when (event.value) {
            is PlaceListMutation.CityContextLoaded,
            is PlaceListMutation.CityContextFailed -> {
                val generation = event.generation ?: return
                placeRepository.getCatalogSnapshot { snapshot ->
                    enqueue(generation, PlaceListMutation.CatalogLoaded(snapshot))
                }
            }
            is PlaceListMutation.CatalogLoaded -> {
                val generation = event.generation ?: return
                photoCacheRepository.getValid { result ->
                    enqueue(generation, PlaceListMutation.PhotoCacheLoaded(result))
                }
            }
            is PlaceListMutation.PhotoCacheLoaded -> {
                val generation = event.generation ?: return
                favoriteRepository.getFavoriteIds { result ->
                    enqueue(generation, PlaceListMutation.FavoritesLoaded(result))
                }
            }
            is PlaceListMutation.FavoritesLoaded -> {
                val generation = event.generation ?: return
                val current = mutableState.value
                photoHydrator?.request(
                    current.visiblePlaces,
                    current.photoByPlaceId.keys
                ) { entry ->
                    if (!disposed) events.trySend(Event.PhotoResolvedEvent(generation, entry))
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

    private fun startLocationRequest() {
        if (mutableState.value.locationStatus == PlaceLocationStatus.REQUESTING) return
        val operation = ++locationOperation
        reduce(PlaceListMutation.LocationStarted)
        locationCapability.getCurrentLocation { result ->
            if (!disposed) events.trySend(Event.LocationResultEvent(operation, result))
        }
    }

    private fun handleLocationResult(event: Event.LocationResultEvent) {
        if (event.operation != locationOperation) return
        reduce(PlaceListMutation.LocationResolved(event.result))
        val success = event.result as? LocationResult.Success ?: return
        CurrentLocationRuntime.update(success.point)
        reverseGeocodeCapability.resolve(success.point) { result ->
            if (!disposed) events.trySend(Event.ReverseGeocodeResultEvent(result))
        }
    }

    private fun selectCity(cityId: String) {
        cityRepository.select(cityId) { result ->
            if (!disposed) events.trySend(Event.CitySelectionResult(result))
        }
    }

    private fun handleCitySelectionResult(result: StorageResult<ExploreCitySelection>) {
        if (result is StorageResult.Success) {
            reduce(PlaceListMutation.CityContextLoaded(result.value))
            ExploreCityRuntime.invalidate()
        } else {
            reduce(PlaceListMutation.CityContextFailed("城市切换失败，请重试。"))
        }
    }

    private fun handleReverseGeocodeResult(result: ReverseGeocodeResult) {
        reduce(
            PlaceListMutation.DetectedCity(
                when (result) {
                    is ReverseGeocodeResult.SupportedCity -> result.city
                    is ReverseGeocodeResult.UnsupportedCity -> result.city
                    is ReverseGeocodeResult.Failure -> null
                }
            )
        )
    }

    private fun enqueue(generation: Long, mutation: PlaceListMutation) {
        if (!disposed) events.trySend(Event.Mutation(generation, mutation))
    }

    private fun reduce(mutation: PlaceListMutation) {
        mutableState.value = PlaceListReducer.reduce(mutableState.value, mutation)
    }

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
    return copy(visiblePlaces = result.places, filter = result.appliedFilter)
}

private fun sourceNotice(source: PlaceCatalogSource): PlaceFeatureNotice? = when (source) {
    PlaceCatalogSource.PERSISTED -> null
    PlaceCatalogSource.INITIALIZED -> PlaceFeatureNotice(
        "已准备版本化离线城市地点，也可以添加自己的地点。",
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

private fun PlaceListUiState.locationFailure(
    status: PlaceLocationStatus,
    message: String
) = copy(
    locationStatus = status,
    currentLocation = null,
    locationAccuracyMeters = null,
    locationMessage = message
)
