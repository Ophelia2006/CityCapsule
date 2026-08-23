package com.y.citycapsule.feature.home

import com.y.citycapsule.core.city.CityDefinition
import com.y.citycapsule.core.city.CityRegistry
import com.y.citycapsule.core.city.ExploreCityRepository
import com.y.citycapsule.core.city.ExploreCityRuntime
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.favorite.FavoritePlaceIds
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.location.CurrentLocationRuntime
import com.y.citycapsule.core.location.GeoDistance
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCatalogSource
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlacePhotoCacheEntry
import com.y.citycapsule.core.place.PlacePhotoCacheRepository
import com.y.citycapsule.core.place.PlacePhotoHydrator
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.RemotePlace
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.place.normalizePlaceCityName
import com.y.citycapsule.core.place.loadCityPlaceRecommendations
import com.y.citycapsule.core.profile.LocalProfile
import com.y.citycapsule.core.profile.LocalProfileRepository
import com.y.citycapsule.core.storage.StorageResult

enum class HomeUiStatus { LOADING, READY }
enum class HomeOnlineStatus { IDLE, LOADING, RESULTS, EMPTY, ERROR }
enum class HomeCityLookupStatus { IDLE, LOADING, ERROR }

data class HomeRecentMemory(val capsule: CityCapsule, val place: Place?, val dateLabel: String)

data class HomeSupportingSection(val title: String, val placeIds: List<String>)

data class HomeUiState(
    val status: HomeUiStatus = HomeUiStatus.LOADING,
    val profile: LocalProfile = LocalProfile.DEFAULT,
    val selectedCity: CityDefinition = requireNotNull(CityRegistry.byId(CityRegistry.DEFAULT_CITY_ID)),
    val rankedPlaces: List<Place> = emptyList(),
    val supportingPlaceIds: List<String> = emptyList(),
    val supportingTitle: String = "换一种逛法",
    val favoriteIds: Set<String> = emptySet(),
    val recordedPlaceIds: Set<String> = emptySet(),
    val recentMemories: List<HomeRecentMemory> = emptyList(),
    val photoByPlaceId: Map<String, PlacePhotoCacheEntry> = emptyMap(),
    val catalogReadOnly: Boolean = false,
    val notice: String? = null,
    val busyFavoriteId: String? = null,
    val onlineStatus: HomeOnlineStatus = HomeOnlineStatus.IDLE,
    val onlineRecommendations: List<RemotePlace> = emptyList(),
    val cityLookupStatus: HomeCityLookupStatus = HomeCityLookupStatus.IDLE,
    val cityLookupMessage: String? = null
) {
    val featuredPlaceWithMedia: Place? get() = rankedPlaces.firstOrNull {
        it.visualRef != null || it.id in photoByPlaceId
    }
    val featuredPlace: Place? get() = featuredPlaceWithMedia ?: rankedPlaces.firstOrNull()
    val categories: List<PlaceCategory>
        get() = PlaceCategory.entries.filter { category -> rankedPlaces.any { it.category == category } }
    val supportingPlaces: List<Place>
        get() {
            val placeById = rankedPlaces.associateBy(Place::id)
            val featuredId = featuredPlace?.id
            return supportingPlaceIds.mapNotNull(placeById::get).filterNot { it.id == featuredId }
        }
}

/** Pure, explainable local ranking. No location, network, or personalization is implied. */
object HomeRecommendationPolicy {
    fun rank(
        places: List<Place>,
        currentCity: String?,
        favoriteIds: Set<String>,
        recordedPlaceIds: Set<String>,
        currentLocation: com.y.citycapsule.core.place.GeoPoint? = null
    ): List<Place> {
        val normalizedCity = currentCity?.trim().orEmpty()
        val tiers = places.groupBy { place ->
            HomePlaceTier(
                cityRank = if (normalizedCity.isNotEmpty() && normalizePlaceCityName(place.city) == normalizePlaceCityName(normalizedCity)) 0 else 1,
                discoveryRank = when {
                    place.id in favoriteIds -> 0
                    place.id !in recordedPlaceIds -> 1
                    else -> 2
                },
                coverRank = if (place.visualRef != null) 0 else 1
            )
        }
        return tiers.keys
            .sortedWith(compareBy<HomePlaceTier> { it.cityRank }.thenBy { it.discoveryRank }.thenBy { it.coverRank })
            .flatMap { tier -> diversify(tiers[tier].orEmpty(), currentLocation) }
    }

    fun supportingSection(
        rankedPlaces: List<Place>,
        currentCity: String?,
        favoriteIds: Set<String>
    ): HomeSupportingSection {
        val remaining = rankedPlaces.drop(1)
        val favorites = remaining.filter { it.id in favoriteIds }
        val normalizedCity = currentCity?.trim().orEmpty()
        val sameCity = remaining.filter {
            normalizedCity.isNotEmpty() && normalizePlaceCityName(it.city) == normalizePlaceCityName(normalizedCity)
        }
        val places = when {
            favorites.isNotEmpty() -> favorites
            sameCity.isNotEmpty() -> sameCity
            else -> remaining
        }.take(HOME_SUPPORTING_PLACE_LIMIT)
        val title = when {
            favorites.isNotEmpty() -> "想去的地方"
            normalizedCity.isNotEmpty() -> "这座城里"
            else -> "换一种逛法"
        }
        return HomeSupportingSection(title, places.map(Place::id))
    }

    private fun diversify(places: List<Place>, currentLocation: com.y.citycapsule.core.place.GeoPoint?): List<Place> {
        val buckets = PlaceCategory.entries.map { category ->
            places.filter { it.category == category }.sortedWith(
                compareBy<Place> { place ->
                    if (currentLocation == null || place.geoPoint == null) Double.MAX_VALUE
                    else GeoDistance.meters(currentLocation, place.geoPoint)
                }.thenBy(Place::id)
            ).toMutableList()
        }
        val result = mutableListOf<Place>()
        while (buckets.any { it.isNotEmpty() }) {
            buckets.forEach { bucket -> if (bucket.isNotEmpty()) result += bucket.removeAt(0) }
        }
        return result
    }

    private data class HomePlaceTier(val cityRank: Int, val discoveryRank: Int, val coverRank: Int)
}

class HomeStateHolder(
    private val profileRepository: LocalProfileRepository,
    private val cityRepository: ExploreCityRepository,
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val capsuleRepository: CapsuleRepository,
    private val dateFormatter: CapsuleDateFormatter,
    private val photoCacheRepository: PlacePhotoCacheRepository = PlacePhotoCacheRepository.NONE,
    private val remoteDataSource: PlaceRemoteDataSource? = null,
    private val onDataChanged: () -> Unit = {},
    private val onStateChanged: (HomeUiState) -> Unit = {}
) {
    var state = HomeUiState()
        private set
    private var loadGeneration = 0
    private var cityLookupGeneration = 0
    private val photoHydrator = remoteDataSource?.let {
        PlacePhotoHydrator(it, photoCacheRepository)
    }

    fun load() {
        val generation = ++loadGeneration
        update(state.copy(status = HomeUiStatus.LOADING, busyFavoriteId = null))
        profileRepository.getProfileSnapshot { profileSnapshot ->
            if (generation != loadGeneration) return@getProfileSnapshot
            cityRepository.get city@{ cityResult ->
                if (generation != loadGeneration) return@city
                val selectedCity = (cityResult as? StorageResult.Success)
                    ?.value?.selectedCity
                    ?: requireNotNull(CityRegistry.byId(CityRegistry.DEFAULT_CITY_ID))
                placeRepository.getCatalogSnapshot { catalogSnapshot ->
                if (generation != loadGeneration) return@getCatalogSnapshot
                favoriteRepository.getFavoriteIds { favoriteResult ->
                    if (generation != loadGeneration) return@getFavoriteIds
                    capsuleRepository.getPublished { capsuleResult ->
                        if (generation != loadGeneration) return@getPublished
                        val favorites = (favoriteResult as? StorageResult.Success)?.value ?: FavoritePlaceIds.EMPTY
                        val capsules = (capsuleResult as? StorageResult.Success)?.value.orEmpty()
                        val allPlaces = catalogSnapshot.catalog.places
                        val places = allPlaces.filter {
                            normalizePlaceCityName(it.city) == normalizePlaceCityName(selectedCity.displayName)
                        }
                        val placeById = allPlaces.associateBy(Place::id)
                        val recordedPlaceIds = capsules.mapTo(mutableSetOf()) { it.placeId }
                        val rankedPlaces = HomeRecommendationPolicy.rank(
                            places,
                            selectedCity.displayName,
                            favorites.placeIds,
                            recordedPlaceIds,
                            CurrentLocationRuntime.point
                        )
                        val supportingSection = HomeRecommendationPolicy.supportingSection(
                            rankedPlaces,
                            selectedCity.displayName,
                            favorites.placeIds
                        )
                        photoCacheRepository.getValid { photoResult ->
                            if (generation != loadGeneration) return@getValid
                            update(
                                HomeUiState(
                                    status = HomeUiStatus.READY,
                                    profile = profileSnapshot.profile,
                                    selectedCity = selectedCity,
                                    rankedPlaces = rankedPlaces,
                                    supportingPlaceIds = supportingSection.placeIds,
                                    supportingTitle = supportingSection.title,
                                    favoriteIds = favorites.placeIds,
                                    recordedPlaceIds = recordedPlaceIds,
                                    catalogReadOnly = catalogSnapshot.source ==
                                        PlaceCatalogSource.RECOVERY_READ_ONLY,
                                    recentMemories = capsules
                                        .sortedWith(compareByDescending<CityCapsule> { it.createdAtEpochMs }.thenBy { it.id })
                                        .take(HOME_RECENT_MEMORY_LIMIT)
                                        .map { HomeRecentMemory(it, placeById[it.placeId], dateFormatter.format(it.createdAtEpochMs)) },
                                    photoByPlaceId = (photoResult as? StorageResult.Success)?.value.orEmpty(),
                                    notice = when {
                                        favoriteResult is StorageResult.Failure -> "想去状态暂时无法读取，仍可继续探索地点。"
                                        capsuleResult is StorageResult.Failure -> "最近的城市记忆暂时无法读取，地点仍可浏览。"
                                        catalogSnapshot.source == PlaceCatalogSource.RECOVERY_READ_ONLY ->
                                            "地点数据暂时无法安全读取，请重试。"
                                        cityResult is StorageResult.Failure -> "探索城市暂时无法读取，当前显示上海内容。"
                                        profileSnapshot.warning != null || catalogSnapshot.warning != null ->
                                            "部分本地数据暂时不可用，当前已显示可安全读取的内容。"
                                        else -> null
                                    }
                                )
                            )
                            if (state.featuredPlaceWithMedia == null && remoteDataSource != null) {
                                loadOnlineRecommendations(generation, selectedCity)
                            }
                            photoHydrator?.request(
                                rankedPlaces.take(HOME_PHOTO_LOOKUP_LIMIT),
                                state.photoByPlaceId.keys
                            ) { entry ->
                                if (generation == loadGeneration) {
                                    update(state.copy(photoByPlaceId = state.photoByPlaceId + (entry.placeId to entry)))
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    fun dispose() {
        loadGeneration++
        cityLookupGeneration++
        photoHydrator?.dispose()
    }

    fun selectCity(city: CityDefinition) {
        cityLookupGeneration++
        cityRepository.select(city) { result ->
            if (result is StorageResult.Success) {
                ExploreCityRuntime.invalidate()
                load()
            } else {
                update(state.copy(notice = "城市切换失败，请重试。"))
            }
        }
    }

    fun searchAndSelectCity(rawName: String) {
        val requestedName = rawName.trim().removeSuffix("市").trim()
        if (requestedName.length < 2) {
            update(state.copy(
                cityLookupStatus = HomeCityLookupStatus.ERROR,
                cityLookupMessage = "请输入完整城市名称。"
            ))
            return
        }
        CityRegistry.byDisplayName(requestedName)?.let {
            selectCity(it)
            return
        }
        val remote = remoteDataSource ?: run {
            update(state.copy(
                cityLookupStatus = HomeCityLookupStatus.ERROR,
                cityLookupMessage = "当前无法连接城市地点服务。"
            ))
            return
        }
        update(state.copy(cityLookupStatus = HomeCityLookupStatus.LOADING, cityLookupMessage = null))
        val generation = ++cityLookupGeneration
        remote.search("景点", requestedName, null) callback@{ result ->
            if (generation != cityLookupGeneration) return@callback
            val places = (result as? RemotePlaceResult.Success)?.places.orEmpty()
            val matched = places.firstOrNull {
                normalizePlaceCityName(it.city) == normalizePlaceCityName(requestedName)
            }
            if (matched == null) {
                update(state.copy(
                    cityLookupStatus = HomeCityLookupStatus.ERROR,
                    cityLookupMessage = "没有找到“$requestedName”，请检查城市名称或网络后重试。"
                ))
                return@callback
            }
            val cityName = matched.city.ifBlank { requestedName }
            val resolvedCity = CityDefinition(
                id = "remote-${cityName.hashCode().toUInt().toString(16)}",
                displayName = cityName,
                centerPoint = matched.geoPoint,
                supported = false,
                contentPackVersion = 0
            )
            cityRepository.select(resolvedCity) { selection ->
                if (generation != cityLookupGeneration) return@select
                if (selection is StorageResult.Success) {
                    ExploreCityRuntime.invalidate()
                    load()
                } else {
                    update(state.copy(
                        cityLookupStatus = HomeCityLookupStatus.ERROR,
                        cityLookupMessage = "城市切换失败，请重试。"
                    ))
                }
            }
        }
    }

    private fun loadOnlineRecommendations(generation: Int, city: CityDefinition) {
        val remote = remoteDataSource ?: return
        update(state.copy(onlineStatus = HomeOnlineStatus.LOADING, onlineRecommendations = emptyList()))
        loadCityPlaceRecommendations(remote, city.displayName, city.centerPoint, HOME_REMOTE_RECOMMENDATION_LIMIT) callback@{ result ->
            if (generation != loadGeneration) return@callback
            when (result) {
                is RemotePlaceResult.Success -> update(state.copy(
                    onlineStatus = if (result.places.isEmpty()) HomeOnlineStatus.EMPTY else HomeOnlineStatus.RESULTS,
                    onlineRecommendations = result.places.take(HOME_REMOTE_RECOMMENDATION_LIMIT)
                ))
                is RemotePlaceResult.Failure,
                RemotePlaceResult.Unavailable -> update(state.copy(
                    onlineStatus = HomeOnlineStatus.ERROR,
                    onlineRecommendations = emptyList()
                ))
            }
        }
    }

    fun invalidateCachedPhoto(placeId: String) {
        if (placeId !in state.photoByPlaceId) return
        update(state.copy(photoByPlaceId = state.photoByPlaceId - placeId))
        photoCacheRepository.remove(placeId)
    }

    fun toggleFavorite(placeId: String) {
        if (
            state.status != HomeUiStatus.READY ||
            state.catalogReadOnly ||
            state.busyFavoriteId != null
        ) return
        update(state.copy(busyFavoriteId = placeId))
        favoriteRepository.toggleFavorite(placeId) { result ->
            when (result) {
                is StorageResult.Success -> {
                    val ids = if (result.value) state.favoriteIds + placeId else state.favoriteIds - placeId
                    update(
                        state.copy(
                            favoriteIds = ids,
                            busyFavoriteId = null,
                            notice = state.notice?.takeUnless { it == HOME_FAVORITE_FAILURE_NOTICE }
                        )
                    )
                    onDataChanged()
                }
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(busyFavoriteId = null, notice = HOME_FAVORITE_FAILURE_NOTICE)
                )
            }
        }
    }

    private fun update(next: HomeUiState) {
        state = next
        onStateChanged(next)
    }
}

internal const val HOME_RECENT_MEMORY_LIMIT = 3
internal const val HOME_SUPPORTING_PLACE_LIMIT = 3
internal const val HOME_PHOTO_LOOKUP_LIMIT = 8
private const val HOME_FAVORITE_FAILURE_NOTICE = "想去操作失败，页面状态已保持不变。"
private const val HOME_REMOTE_RECOMMENDATION_LIMIT = 4
