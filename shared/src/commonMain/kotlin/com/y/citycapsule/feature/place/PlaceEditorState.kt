package com.y.citycapsule.feature.place

import com.y.citycapsule.core.media.CameraCapability
import com.y.citycapsule.core.media.CameraCaptureResult
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.media.PhotoPickerResult
import com.y.citycapsule.core.location.LocationCapability
import com.y.citycapsule.core.location.LocationResult
import com.y.citycapsule.core.location.CurrentLocationRuntime
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceDraft
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.place.PlaceVisualRef
import com.y.citycapsule.core.place.PlaceVisualType
import com.y.citycapsule.core.place.PlaceMediaCleanup
import com.y.citycapsule.core.storage.StorageResult

enum class PlaceEditorMode {
    CREATE,
    EDIT
}

enum class PlaceEditorUiStatus {
    LOADING,
    READY,
    SAVING,
    NOT_FOUND
}

data class PlaceEditorUiState(
    val status: PlaceEditorUiStatus = PlaceEditorUiStatus.LOADING,
    val mode: PlaceEditorMode = PlaceEditorMode.CREATE,
    val draft: PlaceDraft = EMPTY_DRAFT,
    val latitudeText: String = "",
    val longitudeText: String = "",
    val validationMessage: String? = null,
    val showDiscardConfirmation: Boolean = false,
    val notice: PlaceFeatureNotice? = null
) {
    val isBusy: Boolean
        get() = status == PlaceEditorUiStatus.LOADING ||
            status == PlaceEditorUiStatus.SAVING

    companion object {
        val EMPTY_DRAFT = PlaceDraft(
            name = "",
            city = "",
            category = PlaceCategory.OTHER
        )
    }
}

class PlaceEditorStateHolder(
    private val placeId: String?,
    private val placeRepository: PlaceRepository,
    private val mediaCleanup: PlaceMediaCleanup = PlaceMediaCleanup.NO_OP,
    private val onDataChanged: () -> Unit = {},
    private val onStateChanged: (PlaceEditorUiState) -> Unit = {}
) {
    var state: PlaceEditorUiState = PlaceEditorUiState(
        mode = if (placeId == null) PlaceEditorMode.CREATE else PlaceEditorMode.EDIT
    )
        private set

    private var originalPlace: Place? = null
    private var initialDraft: PlaceDraft = PlaceEditorUiState.EMPTY_DRAFT
    private var initialLatitudeText: String = ""
    private var initialLongitudeText: String = ""

    fun load() {
        if (placeId == null) {
            originalPlace = null
            initialDraft = PlaceEditorUiState.EMPTY_DRAFT
            initialLatitudeText = ""
            initialLongitudeText = ""
            update(
                PlaceEditorUiState(
                    status = PlaceEditorUiStatus.READY,
                    mode = PlaceEditorMode.CREATE,
                    draft = initialDraft
                )
            )
            return
        }
        update(state.copy(status = PlaceEditorUiStatus.LOADING, notice = null))
        placeRepository.getPlace(placeId) { result ->
            when (result) {
                is StorageResult.Success -> {
                    originalPlace = result.value
                    initialDraft = result.value.toDraft()
                    initialLatitudeText = initialDraft.geoPoint?.latitude?.toString().orEmpty()
                    initialLongitudeText = initialDraft.geoPoint?.longitude?.toString().orEmpty()
                    update(
                        PlaceEditorUiState(
                            status = PlaceEditorUiStatus.READY,
                            mode = PlaceEditorMode.EDIT,
                            draft = initialDraft,
                            latitudeText = initialDraft.geoPoint?.latitude?.toString().orEmpty(),
                            longitudeText = initialDraft.geoPoint?.longitude?.toString().orEmpty()
                        )
                    )
                }
                StorageResult.Missing -> update(
                    state.copy(status = PlaceEditorUiStatus.NOT_FOUND)
                )
                is StorageResult.Failure -> update(
                    state.copy(
                        status = PlaceEditorUiStatus.NOT_FOUND,
                        notice = PlaceFeatureNotice(
                            "暂时无法读取待编辑地点。",
                            PlaceNoticeTone.ERROR
                        )
                    )
                )
            }
        }
    }

    fun updateName(value: String) = updateDraft { copy(name = value) }

    fun updateCity(value: String) = updateDraft { copy(city = value) }

    fun updateDistrict(value: String) = updateDraft { copy(district = value) }

    fun updateCategory(value: PlaceCategory) = updateDraft { copy(category = value) }

    fun updateAddress(value: String) = updateDraft { copy(address = value) }

    fun updateTags(value: String) = updateDraft {
        copy(tags = value.split(',', '，').map(String::trim).filter(String::isNotEmpty))
    }

    fun updateNote(value: String) = updateDraft { copy(personalNote = value) }

    fun updateDescription(value: String) = updateDraft { copy(description = value) }

    fun updateLatitude(value: String) = updateCoordinates(value, state.longitudeText)

    fun updateLongitude(value: String) = updateCoordinates(state.latitudeText, value)

    fun useCurrentLocation(location: LocationCapability) {
        if (state.status != PlaceEditorUiStatus.READY) return
        location.getCurrentLocation { result ->
            when (result) {
                is LocationResult.Success -> {
                    CurrentLocationRuntime.update(result.point)
                    update(
                        state.copy(
                            draft = state.draft.copy(geoPoint = result.point),
                            latitudeText = result.point.latitude.toString(),
                            longitudeText = result.point.longitude.toString(),
                            notice = PlaceFeatureNotice("已使用当前位置。", PlaceNoticeTone.SUCCESS)
                        )
                    )
                }
                LocationResult.PermissionDenied -> coordinateNotice("未获得定位权限，可以手动填写坐标。")
                LocationResult.PermissionPermanentlyDenied -> coordinateNotice("定位权限已被长期拒绝，可以手动填写坐标。")
                LocationResult.ServiceDisabled -> coordinateNotice("系统定位服务已关闭，可以手动填写坐标。")
                LocationResult.Unavailable -> coordinateNotice("当前无法定位，可以手动填写坐标。")
                is LocationResult.Failure -> coordinateNotice(result.message)
            }
        }
    }

    private fun updateCoordinates(latitude: String, longitude: String) {
        if (state.status != PlaceEditorUiStatus.READY) return
        val lat = latitude.trim().toDoubleOrNull()
        val lon = longitude.trim().toDoubleOrNull()
        val point = when {
            latitude.isBlank() && longitude.isBlank() -> null
            lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0 -> GeoPoint(lat, lon)
            else -> state.draft.geoPoint
        }
        update(
            state.copy(
                latitudeText = latitude,
                longitudeText = longitude,
                draft = state.draft.copy(geoPoint = point),
                validationMessage = null,
                notice = null
            )
        )
    }

    private fun coordinateNotice(message: String) = update(
        state.copy(notice = PlaceFeatureNotice(message, PlaceNoticeTone.WARNING))
    )

    fun captureCover(camera: CameraCapability) {
        if (state.status != PlaceEditorUiStatus.READY) return
        camera.captureImage { result ->
            when (result) {
                is CameraCaptureResult.Success -> setCover(result.path)
                CameraCaptureResult.Cancelled -> Unit
                is CameraCaptureResult.Failure -> update(state.copy(notice = PlaceFeatureNotice(result.message, PlaceNoticeTone.ERROR)))
                CameraCaptureResult.Unsupported -> update(state.copy(notice = PlaceFeatureNotice("当前设备不支持拍照，可以从相册选择封面。", PlaceNoticeTone.WARNING)))
            }
        }
    }

    fun pickCover(photoPicker: PhotoPickerCapability) {
        if (state.status != PlaceEditorUiStatus.READY) return
        photoPicker.pickImages(1) { result ->
            when (result) {
                is PhotoPickerResult.Success -> result.paths.firstOrNull()?.let(::setCover)
                PhotoPickerResult.Cancelled -> Unit
                is PhotoPickerResult.Failure -> update(state.copy(notice = PlaceFeatureNotice(result.message, PlaceNoticeTone.ERROR)))
                PhotoPickerResult.Unsupported -> update(state.copy(notice = PlaceFeatureNotice("当前设备不支持相册选择。", PlaceNoticeTone.WARNING)))
            }
        }
    }

    fun removeCover() {
        val previous = state.draft.visualRef?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }?.value
        updateDraft { copy(visualRef = null) }
        if (previous != null && previous != originalPlace?.visualRef?.value) {
            mediaCleanup.cleanupCandidates(listOf(previous)) { }
        }
    }

    private fun setCover(path: String) {
        val previous = state.draft.visualRef?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }?.value
        updateDraft { copy(visualRef = PlaceVisualRef(PlaceVisualType.MANAGED_FILE, path)) }
        if (previous != null && previous != path && previous != originalPlace?.visualRef?.value) {
            mediaCleanup.cleanupCandidates(listOf(previous)) { }
        }
    }

    fun tagsText(): String = state.draft.tags.joinToString("，")

    fun isDirty(): Boolean = state.draft != initialDraft ||
        state.latitudeText != initialLatitudeText || state.longitudeText != initialLongitudeText

    fun requestDiscard(onDiscardImmediately: () -> Unit) {
        if (state.isBusy) {
            return
        }
        if (isDirty()) {
            update(state.copy(showDiscardConfirmation = true))
        } else {
            onDiscardImmediately()
        }
    }

    fun dismissDiscard() {
        if (!state.isBusy) {
            update(state.copy(showDiscardConfirmation = false))
        }
    }

    fun confirmDiscard(onDiscarded: () -> Unit) {
        if (!state.isBusy) {
            update(state.copy(showDiscardConfirmation = false))
            cleanupUncommittedCover(onDiscarded)
        }
    }

    fun save(onSaved: (place: Place, created: Boolean) -> Unit) {
        if (state.status != PlaceEditorUiStatus.READY) {
            return
        }
        if (!coordinatesAreValid()) {
            update(
                state.copy(
                    validationMessage = "请同时填写有效的纬度和经度，或将两项都留空。",
                    notice = PlaceFeatureNotice("地点坐标无效。", PlaceNoticeTone.ERROR)
                )
            )
            return
        }
        val normalized = PlaceValidator.normalizeDraftOrNull(state.draft)
        if (normalized == null) {
            update(
                state.copy(
                    validationMessage = validationMessage(state.draft),
                    notice = PlaceFeatureNotice(
                        "请修正地点信息后再保存。",
                        PlaceNoticeTone.ERROR
                    )
                )
            )
            return
        }
        update(
            state.copy(
                status = PlaceEditorUiStatus.SAVING,
                draft = normalized,
                validationMessage = null,
                notice = PlaceFeatureNotice("正在保存地点…", PlaceNoticeTone.NEUTRAL)
            )
        )
        if (state.mode == PlaceEditorMode.CREATE) {
            placeRepository.createPlace(normalized) { result ->
                handleSaveResult(result, created = true, onSaved)
            }
        } else {
            val original = originalPlace
            if (original == null) {
                update(state.copy(status = PlaceEditorUiStatus.NOT_FOUND))
                return
            }
            placeRepository.updatePlace(
                original.copy(
                    name = normalized.name,
                    city = normalized.city,
                    district = normalized.district,
                    category = normalized.category,
                    address = normalized.address,
                    tags = normalized.tags,
                    description = normalized.description,
                    personalNote = normalized.personalNote,
                    contentSource = normalized.contentSource,
                    geoPoint = normalized.geoPoint,
                    visualRef = normalized.visualRef
                )
            ) { result ->
                handleSaveResult(result, created = false, onSaved)
            }
        }
    }

    private fun handleSaveResult(
        result: StorageResult<Place>,
        created: Boolean,
        onSaved: (Place, Boolean) -> Unit
    ) {
        when (result) {
            is StorageResult.Success -> {
                val previousCover = originalPlace?.visualRef
                    ?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }
                    ?.value
                originalPlace = result.value
                initialDraft = result.value.toDraft()
                initialLatitudeText = initialDraft.geoPoint?.latitude?.toString().orEmpty()
                initialLongitudeText = initialDraft.geoPoint?.longitude?.toString().orEmpty()
                update(
                    state.copy(
                        status = PlaceEditorUiStatus.READY,
                        draft = initialDraft,
                        notice = PlaceFeatureNotice(
                            "地点已保存。",
                            PlaceNoticeTone.SUCCESS
                        )
                    )
                )
                onDataChanged()
                val currentCover = result.value.visualRef?.value
                previousCover?.takeIf { it != currentCover }?.let {
                    mediaCleanup.cleanupCandidates(listOf(it)) { }
                }
                onSaved(result.value, created)
            }
            StorageResult.Missing -> update(
                state.copy(
                    status = PlaceEditorUiStatus.NOT_FOUND,
                    notice = PlaceFeatureNotice(
                        "待编辑地点已不存在。",
                        PlaceNoticeTone.ERROR
                    )
                )
            )
            is StorageResult.Failure -> update(
                state.copy(
                    status = PlaceEditorUiStatus.READY,
                    notice = PlaceFeatureNotice(
                        "保存失败，当前输入仍保留在页面中。",
                        PlaceNoticeTone.ERROR
                    )
                )
            )
        }
    }

    private fun updateDraft(transform: PlaceDraft.() -> PlaceDraft) {
        if (state.status == PlaceEditorUiStatus.READY) {
            update(
                state.copy(
                    draft = state.draft.transform(),
                    validationMessage = null,
                    notice = null
                )
            )
        }
    }

    private fun coordinatesAreValid(): Boolean {
        if (state.latitudeText.isBlank() && state.longitudeText.isBlank()) return true
        val latitude = state.latitudeText.trim().toDoubleOrNull() ?: return false
        val longitude = state.longitudeText.trim().toDoubleOrNull() ?: return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun validationMessage(draft: PlaceDraft): String = when {
        draft.name.trim().isEmpty() -> "地点名称不能为空。"
        draft.city.trim().isEmpty() -> "城市不能为空。"
        draft.name.trim().length > PlaceValidator.NAME_MAX_LENGTH ->
            "地点名称不能超过 ${PlaceValidator.NAME_MAX_LENGTH} 个字符。"
        draft.city.trim().length > PlaceValidator.CITY_MAX_LENGTH ->
            "城市不能超过 ${PlaceValidator.CITY_MAX_LENGTH} 个字符。"
        draft.tags.distinct().size > PlaceValidator.TAG_MAX_COUNT ->
            "标签不能超过 ${PlaceValidator.TAG_MAX_COUNT} 个。"
        else -> "请检查区域、地址、标签和备注的长度。"
    }

    private fun Place.toDraft(): PlaceDraft = PlaceDraft(
        name = name,
        city = city,
        district = district,
        category = category,
        address = address,
        tags = tags,
        description = description,
        personalNote = personalNote,
        contentSource = contentSource,
        geoPoint = geoPoint,
        visualRef = visualRef
    )

    private fun update(nextState: PlaceEditorUiState) {
        state = nextState
        onStateChanged(nextState)
    }

    private fun cleanupUncommittedCover(onComplete: () -> Unit) {
        val current = state.draft.visualRef
            ?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }
            ?.value
        val original = originalPlace?.visualRef?.value
        if (current == null || current == original) onComplete()
        else mediaCleanup.cleanupCandidates(listOf(current)) { onComplete() }
    }
}
