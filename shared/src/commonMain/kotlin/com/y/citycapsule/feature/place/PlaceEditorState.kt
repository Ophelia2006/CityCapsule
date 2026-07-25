package com.y.citycapsule.feature.place

import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceCategory
import com.y.citycapsule.core.place.PlaceDraft
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceValidator
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
    private val onDataChanged: () -> Unit = {},
    private val onStateChanged: (PlaceEditorUiState) -> Unit = {}
) {
    var state: PlaceEditorUiState = PlaceEditorUiState(
        mode = if (placeId == null) PlaceEditorMode.CREATE else PlaceEditorMode.EDIT
    )
        private set

    private var originalPlace: Place? = null
    private var initialDraft: PlaceDraft = PlaceEditorUiState.EMPTY_DRAFT

    fun load() {
        if (placeId == null) {
            originalPlace = null
            initialDraft = PlaceEditorUiState.EMPTY_DRAFT
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
                    update(
                        PlaceEditorUiState(
                            status = PlaceEditorUiStatus.READY,
                            mode = PlaceEditorMode.EDIT,
                            draft = initialDraft
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

    fun updateNote(value: String) = updateDraft { copy(note = value) }

    fun tagsText(): String = state.draft.tags.joinToString("，")

    fun isDirty(): Boolean = state.draft != initialDraft

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
            onDiscarded()
        }
    }

    fun save(onSaved: (place: Place, created: Boolean) -> Unit) {
        if (state.status != PlaceEditorUiStatus.READY) {
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
                    note = normalized.note
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
                originalPlace = result.value
                initialDraft = result.value.toDraft()
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
        note = note
    )

    private fun update(nextState: PlaceEditorUiState) {
        state = nextState
        onStateChanged(nextState)
    }
}
