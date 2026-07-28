package com.y.citycapsule.feature.place

import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.capsule.CapsuleRepository

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

    fun toggleFavorite() {
        if (state.status != PlaceDetailUiStatus.READY || state.togglingFavorite) {
            return
        }
        update(state.copy(togglingFavorite = true, notice = null))
        favoriteRepository.toggleFavorite(placeId) { result ->
            when (result) {
                is StorageResult.Success -> {
                    update(
                        state.copy(
                            favorite = result.value,
                            togglingFavorite = false,
                            notice = PlaceFeatureNotice(
                                if (result.value) "已加入想去。" else "已移出想去。",
                                PlaceNoticeTone.SUCCESS
                            )
                        )
                    )
                    onDataChanged()
                }
                StorageResult.Missing,
                is StorageResult.Failure -> update(
                    state.copy(
                        togglingFavorite = false,
                        notice = PlaceFeatureNotice(
                            "想去状态更新失败，请稍后重试。",
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    fun requestDelete() {
        if (!state.isBusy && state.place != null) {
            if (state.memoryCount > 0) {
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
                else -> placeRepository.deletePlace(placeId) { result ->
                    when (result) {
                        is StorageResult.Success,
                        StorageResult.Missing -> {
                            onDataChanged()
                            onDeleted()
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

    private fun update(nextState: PlaceDetailUiState) {
        state = nextState
        onStateChanged(nextState)
    }
}
