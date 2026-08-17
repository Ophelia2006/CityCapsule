package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceSource
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceMediaCleanup
import com.y.citycapsule.core.place.PlaceRemoteDataSource
import com.y.citycapsule.core.place.RemotePlaceResult
import com.y.citycapsule.core.place.PlacePhotoCacheEntry
import com.y.citycapsule.core.place.PlacePhotoCacheContract
import com.y.citycapsule.core.place.PlacePhotoCacheRepository
import com.y.citycapsule.core.place.SystemPlaceClock
import com.y.citycapsule.core.place.PlaceVisualType
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule

enum class PlaceDetailUiStatus {
    LOADING,
    READY,
    NOT_FOUND,
    DELETING
}

data class PlaceDetailUiState(
    val status: PlaceDetailUiStatus = PlaceDetailUiStatus.LOADING,
    val place: Place? = null,
    val favorite: Boolean = false,
    val memoryCount: Int = 0,
    val recentMemories: List<CityCapsule> = emptyList(),
    val remotePhoto: PlacePhotoCacheEntry? = null,
    val togglingFavorite: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val notice: PlaceFeatureNotice? = null
) {
    val isBusy: Boolean
        get() = status == PlaceDetailUiStatus.LOADING ||
            status == PlaceDetailUiStatus.DELETING ||
            togglingFavorite
}

class PlaceDetailStateHolder(
    private val placeId: String,
    private val placeRepository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository,
    private val capsuleRepository: CapsuleRepository,
    private val mediaCleanup: PlaceMediaCleanup = PlaceMediaCleanup.NO_OP,
    private val remoteDataSource: PlaceRemoteDataSource? = null,
    private val photoCacheRepository: PlacePhotoCacheRepository? = null,
    private val onDataChanged: () -> Unit = {},
    private val onStateChanged: (PlaceDetailUiState) -> Unit = {}
) {
    var state: PlaceDetailUiState = PlaceDetailUiState()
        private set

    private var loadGeneration = 0

    fun load() {
        val generation = ++loadGeneration
        update(state.copy(status = PlaceDetailUiStatus.LOADING, notice = null))
        placeRepository.getPlace(placeId) { placeResult ->
            if (generation != loadGeneration) {
                return@getPlace
            }
            when (placeResult) {
                is StorageResult.Success -> favoriteRepository.isFavorite(placeId) {
                    favoriteResult ->
                    if (generation != loadGeneration) {
                        return@isFavorite
                    }
                    val baseState = PlaceDetailUiState(
                        status = PlaceDetailUiStatus.READY,
                        place = placeResult.value,
                        favorite = (favoriteResult as? StorageResult.Success)?.value ?: false,
                        notice = if (favoriteResult is StorageResult.Failure) {
                            PlaceFeatureNotice(
                                "想去状态暂不可用，地点内容仍可查看。",
                                PlaceNoticeTone.WARNING
                            )
                        } else null
                    )
                    capsuleRepository.getPublishedForPlace(placeId) { memories ->
                        if (generation != loadGeneration) return@getPublishedForPlace
                        update(
                            baseState.copy(
                                memoryCount = (memories as? StorageResult.Success)
                                    ?.value?.size ?: 0,
                                recentMemories = (memories as? StorageResult.Success)
                                    ?.value?.sortedByDescending(CityCapsule::createdAtEpochMs)
                                    ?.take(3).orEmpty(),
                                notice = if (memories is StorageResult.Failure) {
                                    PlaceFeatureNotice(
                                        "城市记忆状态暂时无法读取，地点删除已禁用。",
                                        PlaceNoticeTone.WARNING
                                    )
                                } else {
                                    baseState.notice
                                }
                            )
                        )
                        loadPhoto(placeResult.value, generation)
                    }
                }
                StorageResult.Missing -> update(
                    PlaceDetailUiState(status = PlaceDetailUiStatus.NOT_FOUND)
                )
                is StorageResult.Failure -> update(
                    PlaceDetailUiState(
                        status = PlaceDetailUiStatus.NOT_FOUND,
                        notice = PlaceFeatureNotice(
                            "暂时无法读取这个地点。",
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    private fun loadPhoto(place: Place, generation: Int) {
        if (place.visualRef != null) return
        val cache = photoCacheRepository
        if (cache == null) {
            loadRemotePhoto(place, generation)
            return
        }
        cache.getValid { result ->
            if (generation != loadGeneration) return@getValid
            val cached = (result as? StorageResult.Success)?.value?.get(place.id)
            if (cached != null) update(state.copy(remotePhoto = cached))
            else loadRemotePhoto(place, generation)
        }
    }

    private fun loadRemotePhoto(place: Place, generation: Int) {
        remoteDataSource?.search(place.name, place.city, place.geoPoint) { result ->
            if (generation != loadGeneration) return@search
            val photoUrl = (result as? RemotePlaceResult.Success)
                ?.places
                ?.asSequence()
                ?.filter { !it.photoUrl.isNullOrBlank() }
                ?.sortedBy { if (it.name.equals(place.name, ignoreCase = true)) 0 else 1 }
                ?.firstOrNull { candidate ->
                    candidate.name.equals(place.name, ignoreCase = true) ||
                        candidate.name.contains(place.name, ignoreCase = true) ||
                        place.name.contains(candidate.name, ignoreCase = true)
                }
                ?.photoUrl
            if (photoUrl != null) {
                val entry = PlacePhotoCacheEntry(
                    placeId = place.id,
                    url = photoUrl,
                    source = PlacePhotoCacheContract.SOURCE_AMAP_POI,
                    updatedAtEpochMs = SystemPlaceClock.nowEpochMs()
                )
                update(state.copy(remotePhoto = entry))
                photoCacheRepository?.put(place.id, photoUrl, entry.source) {}
            }
        }
    }

    fun invalidateCachedPhoto() {
        if (state.place?.visualRef != null || state.remotePhoto == null) return
        update(state.copy(remotePhoto = null))
        photoCacheRepository?.remove(placeId)
    }

    fun toggleFavorite() {
        if (state.status != PlaceDetailUiStatus.READY || state.togglingFavorite) {
            return
        }
        update(state.copy(togglingFavorite = true))
        favoriteRepository.toggleFavorite(placeId) { result ->
            when (result) {
                is StorageResult.Success -> {
                    update(
                        state.copy(
                            favorite = result.value,
                            togglingFavorite = false,
                            notice = state.notice?.takeUnless {
                                it.message == FAVORITE_UPDATE_FAILURE_NOTICE
                            }
                        )
                    )
                    onDataChanged()
                }
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        togglingFavorite = false,
                        notice = PlaceFeatureNotice(
                            FAVORITE_UPDATE_FAILURE_NOTICE,
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    fun requestDelete() {
        if (!state.isBusy && state.place != null) {
            if (state.place?.source == PlaceSource.SEED) {
                update(
                    state.copy(
                        notice = PlaceFeatureNotice(
                            "内置地点会随应用保留，不能删除。",
                            PlaceNoticeTone.WARNING
                        )
                    )
                )
            } else if (state.memoryCount > 0) {
                update(
                    state.copy(
                        notice = PlaceFeatureNotice(
                            "这个地点关联 ${state.memoryCount} 条城市记忆，请先处理这些记忆再删除地点。",
                            PlaceNoticeTone.WARNING
                        )
                    )
                )
            } else {
                update(state.copy(showDeleteConfirmation = true))
            }
        }
    }

    fun dismissDelete() {
        if (state.status != PlaceDetailUiStatus.DELETING) {
            update(state.copy(showDeleteConfirmation = false))
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (state.status != PlaceDetailUiStatus.READY || state.place == null) {
            return
        }
        update(
            state.copy(
                status = PlaceDetailUiStatus.DELETING,
                showDeleteConfirmation = false,
                notice = PlaceFeatureNotice("正在删除地点…", PlaceNoticeTone.NEUTRAL)
            )
        )
        capsuleRepository.getPublishedForPlace(placeId) { memories ->
            when {
                memories !is StorageResult.Success -> update(
                    state.copy(
                        status = PlaceDetailUiStatus.READY,
                        notice = PlaceFeatureNotice(
                            "无法确认地点是否仍有关联记忆，本次未删除地点。",
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
                memories.value.isNotEmpty() -> update(
                    state.copy(
                        status = PlaceDetailUiStatus.READY,
                        memoryCount = memories.value.size,
                        notice = PlaceFeatureNotice(
                            "这个地点关联 ${memories.value.size} 条城市记忆，请先处理这些记忆再删除地点。",
                            PlaceNoticeTone.WARNING
                        )
                    )
                )
                else -> {
                    val coverPath = state.place?.visualRef
                        ?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }
                        ?.value
                    placeRepository.deletePlace(placeId) { result ->
                    when (result) {
                        is StorageResult.Success,
                        StorageResult.Missing -> {
                            onDataChanged()
                            if (coverPath == null) onDeleted()
                            else mediaCleanup.cleanupCandidates(listOf(coverPath)) { onDeleted() }
                        }
                        is StorageResult.Failure -> update(
                            state.copy(
                                status = PlaceDetailUiStatus.READY,
                                notice = PlaceFeatureNotice(
                                    "删除失败，地点仍保留在本地。",
                                    PlaceNoticeTone.ERROR
                                )
                            )
                        )
                    }
                }
                }
            }
        }
    }

    private fun update(nextState: PlaceDetailUiState) {
        state = nextState
        onStateChanged(nextState)
    }

    private companion object {
        const val FAVORITE_UPDATE_FAILURE_NOTICE = "想去状态更新失败，请稍后重试。"
    }
}
