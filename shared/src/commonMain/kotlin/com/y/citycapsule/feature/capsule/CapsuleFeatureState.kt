package com.y.citycapsule.feature.capsule

import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleContract
import com.y.citycapsule.core.capsule.CapsuleDateFormatter
import com.y.citycapsule.core.capsule.CapsuleMediaCleanup
import com.y.citycapsule.core.capsule.CapsuleMediaCleanupResult
import com.y.citycapsule.core.capsule.CapsuleMood
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CapsuleValidator
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.capsule.UtcCapsuleDateFormatter
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.media.PhotoPickerResult

enum class CapsuleUiStatus { LOADING, READY, SAVING, NOT_FOUND, ERROR }

data class CapsuleEditorState(
    val status: CapsuleUiStatus = CapsuleUiStatus.LOADING,
    val draft: CapsuleDraft = CapsuleDraft.EMPTY,
    val place: Place? = null,
    val validationMessage: String? = null,
    val notice: String? = null,
    val pickingImages: Boolean = false,
    val showDiscardConfirmation: Boolean = false
)

class CapsuleEditorStateHolder(
    private val capsuleId: String?,
    private val placeId: String?,
    private val capsuleRepository: CapsuleRepository,
    private val placeRepository: PlaceRepository,
    private val mediaCleanup: CapsuleMediaCleanup = CapsuleMediaCleanup.NO_OP,
    private val onStateChanged: (CapsuleEditorState) -> Unit = {}
) {
    var state = CapsuleEditorState()
        private set
    private var initialDraft = CapsuleDraft.EMPTY

    fun load() {
        update(state.copy(status = CapsuleUiStatus.LOADING, notice = null))
        if (capsuleId != null) {
            capsuleRepository.getDraft { draftResult ->
                val saved = (draftResult as? StorageResult.Success)?.value
                    ?.takeIf { it.capsuleId == capsuleId }
                if (saved != null) {
                    loadPlace(saved)
                } else {
                    capsuleRepository.getById(capsuleId) { result ->
                        when (result) {
                            is StorageResult.Success -> loadPlace(
                                CapsuleDraft(
                                    capsuleId = result.value.id,
                                    content = result.value.content,
                                    mood = result.value.mood,
                                    tags = result.value.tags,
                                    placeId = result.value.placeId,
                                    imagePaths = result.value.imagePaths
                                )
                            )
                            else -> update(state.copy(status = CapsuleUiStatus.NOT_FOUND))
                        }
                    }
                }
            }
        } else {
            capsuleRepository.getDraft { result ->
                val saved = (result as? StorageResult.Success)?.value?.takeIf {
                    it.capsuleId == null && it.placeId == placeId
                }
                val draft = if (saved != null) saved
                    else CapsuleDraft(placeId = placeId)
                loadPlace(draft)
            }
        }
    }

    private fun loadPlace(draft: CapsuleDraft) {
        val id = draft.placeId
        if (id.isNullOrBlank()) {
            update(state.copy(status = CapsuleUiStatus.ERROR, notice = "请先从地点详情开始记录。"))
            return
        }
        placeRepository.getPlace(id) { result ->
            if (result is StorageResult.Success) {
                initialDraft = draft
                update(CapsuleEditorState(CapsuleUiStatus.READY, draft, result.value))
            } else update(state.copy(status = CapsuleUiStatus.NOT_FOUND))
        }
    }

    fun updateContent(value: String) = change { copy(content = value) }
    fun updateMood(value: CapsuleMood?) = change { copy(mood = value) }
    fun addImages(paths: List<String>) = change {
        copy(imagePaths = (imagePaths + paths).distinct().take(CapsuleContract.IMAGE_MAX_COUNT))
    }
    fun removeImage(path: String) {
        if (path !in state.draft.imagePaths) return
        change { copy(imagePaths = imagePaths - path) }
        cleanupMedia(setOf(path))
    }

    fun pickImages(photoPicker: PhotoPickerCapability) {
        if (state.status != CapsuleUiStatus.READY || state.pickingImages) return
        val available = CapsuleContract.IMAGE_MAX_COUNT - state.draft.imagePaths.size
        if (available <= 0) {
            update(state.copy(notice = "每条城市碎片最多添加 ${CapsuleContract.IMAGE_MAX_COUNT} 张照片。"))
            return
        }
        update(state.copy(pickingImages = true, notice = null))
        photoPicker.pickImages(available) { result ->
            when (result) {
                is PhotoPickerResult.Success -> {
                    val accepted = (state.draft.imagePaths + result.paths)
                        .distinct()
                        .take(CapsuleContract.IMAGE_MAX_COUNT)
                    val rejected = result.paths.filterNot(accepted::contains)
                    update(
                        state.copy(
                            draft = state.draft.copy(imagePaths = accepted),
                            pickingImages = false,
                            notice = null
                        )
                    )
                    cleanupMedia(rejected)
                }
                PhotoPickerResult.Cancelled -> update(state.copy(pickingImages = false))
                is PhotoPickerResult.Failure -> update(
                    state.copy(pickingImages = false, notice = result.message)
                )
                PhotoPickerResult.Unsupported -> update(
                    state.copy(pickingImages = false, notice = "当前设备暂不支持照片选择，可以继续保存文字记录。")
                )
            }
        }
    }
    fun updateTags(value: String) = change {
        copy(tags = value.split(',', '，').map(String::trim).filter(String::isNotEmpty))
    }
    fun removeTag(tag: String) = change { copy(tags = tags - tag) }
    fun tagsText(): String = state.draft.tags.joinToString("，")
    fun isDirty(): Boolean = state.draft != initialDraft

    fun saveDraft(onSaved: () -> Unit = {}) {
        persistDraft(closeAfterSave = false, onSaved = onSaved)
    }

    fun saveDraftAndClose(onClose: () -> Unit) {
        persistDraft(closeAfterSave = true, onSaved = onClose)
    }

    private fun persistDraft(closeAfterSave: Boolean, onSaved: () -> Unit) {
        if (state.status != CapsuleUiStatus.READY) return
        val draftToSave = state.draft
        val removedPaths = initialDraft.imagePaths - draftToSave.imagePaths.toSet()
        capsuleRepository.saveDraft(draftToSave) { result ->
            if (result is StorageResult.Success) {
                initialDraft = draftToSave
                update(
                    state.copy(
                        notice = if (closeAfterSave) null else "草稿已保存在当前设备。",
                        showDiscardConfirmation = false
                    )
                )
                mediaCleanup.cleanupCandidates(removedPaths) { cleanupResult ->
                    showCleanupWarning(cleanupResult)
                    onSaved()
                }
            } else update(state.copy(notice = "草稿暂时无法保存，当前内容仍保留在页面中。"))
        }
    }

    fun publish(onPublished: (CityCapsule) -> Unit) {
        if (state.status != CapsuleUiStatus.READY) return
        val normalized = CapsuleValidator.normalizeDraft(state.draft)
        if (normalized == null) {
            update(state.copy(validationMessage = "请写下这一刻；正文最多 2000 字，标签最多 8 个。"))
            return
        }
        update(state.copy(status = CapsuleUiStatus.SAVING, validationMessage = null, notice = "正在保存到城市记忆…"))
        val removedPaths = initialDraft.imagePaths - normalized.imagePaths.toSet()
        capsuleRepository.publish(normalized) { result ->
            if (result is StorageResult.Success) {
                initialDraft = normalized
                mediaCleanup.cleanupCandidates(removedPaths) {
                    CapsuleFeatureRuntime.invalidate()
                    onPublished(result.value)
                }
            } else update(state.copy(status = CapsuleUiStatus.READY, notice = "保存失败，内容仍保留在页面中。"))
        }
    }

    fun requestClose(onClose: () -> Unit) {
        if (state.status != CapsuleUiStatus.READY) return
        if (isDirty()) update(state.copy(showDiscardConfirmation = true)) else onClose()
    }
    fun dismissDiscard() = update(state.copy(showDiscardConfirmation = false))
    fun discard(onClose: () -> Unit) {
        val discardedPaths = state.draft.imagePaths
        capsuleRepository.getDraft { result ->
            val saved = (result as? StorageResult.Success)?.value
            val matches = if (capsuleId != null) {
                saved?.capsuleId == capsuleId
            } else {
                saved?.capsuleId == null && saved?.placeId == placeId
            }
            if (matches) {
                capsuleRepository.clearDraft {
                    mediaCleanup.cleanupCandidates(discardedPaths) { finishDiscard(onClose) }
                }
            } else {
                mediaCleanup.cleanupCandidates(discardedPaths) { finishDiscard(onClose) }
            }
        }
    }

    private fun finishDiscard(onClose: () -> Unit) {
        update(state.copy(showDiscardConfirmation = false))
        onClose()
    }

    private fun change(transform: CapsuleDraft.() -> CapsuleDraft) {
        if (state.status == CapsuleUiStatus.READY) update(
            state.copy(draft = state.draft.transform(), validationMessage = null, notice = null)
        )
    }
    private fun cleanupMedia(paths: Collection<String>) {
        mediaCleanup.cleanupCandidates(paths, ::showCleanupWarning)
    }
    private fun showCleanupWarning(result: CapsuleMediaCleanupResult) {
        if (result is CapsuleMediaCleanupResult.Deferred &&
            state.status == CapsuleUiStatus.READY
        ) {
            update(state.copy(notice = "照片已从记录中移除，但文件暂未清理，将在稍后重试。"))
        }
    }
    private fun update(next: CapsuleEditorState) { state = next; onStateChanged(next) }
}

data class CapsuleTimelineItem(
    val capsule: CityCapsule,
    val place: Place?,
    val dateLabel: String
)
data class CapsuleTimelineState(
    val status: CapsuleUiStatus = CapsuleUiStatus.LOADING,
    val items: List<CapsuleTimelineItem> = emptyList(),
    val notice: String? = null
)

class CapsuleTimelineStateHolder(
    private val capsuleRepository: CapsuleRepository,
    private val placeRepository: PlaceRepository,
    private val dateFormatter: CapsuleDateFormatter = UtcCapsuleDateFormatter,
    private val onStateChanged: (CapsuleTimelineState) -> Unit = {}
) {
    var state = CapsuleTimelineState(); private set
    fun load() {
        update(state.copy(status = CapsuleUiStatus.LOADING))
        capsuleRepository.getPublished { result ->
            if (result !is StorageResult.Success) {
                update(CapsuleTimelineState(CapsuleUiStatus.ERROR, notice = "城市记忆暂时无法读取。"))
                return@getPublished
            }
            val capsules = result.value
            if (capsules.isEmpty()) {
                update(CapsuleTimelineState(CapsuleUiStatus.READY))
                return@getPublished
            }
            val items = MutableList<CapsuleTimelineItem?>(capsules.size) { null }
            var remaining = capsules.size
            capsules.forEachIndexed { index, capsule ->
                placeRepository.getPlace(capsule.placeId) { placeResult ->
                    items[index] = CapsuleTimelineItem(
                        capsule,
                        (placeResult as? StorageResult.Success)?.value,
                        dateFormatter.format(capsule.createdAtEpochMs)
                    )
                    remaining--
                    if (remaining == 0) update(
                        CapsuleTimelineState(CapsuleUiStatus.READY, items.filterNotNull())
                    )
                }
            }
        }
    }
    private fun update(next: CapsuleTimelineState) { state = next; onStateChanged(next) }
}

data class CapsuleDetailState(
    val status: CapsuleUiStatus = CapsuleUiStatus.LOADING,
    val capsule: CityCapsule? = null,
    val place: Place? = null,
    val dateLabel: String = "",
    val showDeleteConfirmation: Boolean = false,
    val notice: String? = null
)

class CapsuleDetailStateHolder(
    private val capsuleId: String,
    private val capsuleRepository: CapsuleRepository,
    private val placeRepository: PlaceRepository,
    private val mediaCleanup: CapsuleMediaCleanup = CapsuleMediaCleanup.NO_OP,
    private val dateFormatter: CapsuleDateFormatter = UtcCapsuleDateFormatter,
    private val onStateChanged: (CapsuleDetailState) -> Unit = {}
) {
    var state = CapsuleDetailState(); private set
    fun load() {
        capsuleRepository.getById(capsuleId) { result ->
            if (result !is StorageResult.Success) return@getById update(CapsuleDetailState(CapsuleUiStatus.NOT_FOUND))
            placeRepository.getPlace(result.value.placeId) { place ->
                update(
                    CapsuleDetailState(
                        status = CapsuleUiStatus.READY,
                        capsule = result.value,
                        place = (place as? StorageResult.Success)?.value,
                        dateLabel = dateFormatter.format(result.value.createdAtEpochMs)
                    )
                )
            }
        }
    }
    fun requestDelete() = update(state.copy(showDeleteConfirmation = true))
    fun dismissDelete() = update(state.copy(showDeleteConfirmation = false))
    fun delete(onDeleted: () -> Unit) {
        val candidatePaths = state.capsule?.imagePaths.orEmpty()
        capsuleRepository.delete(capsuleId) { result ->
            if (result is StorageResult.Success || result is StorageResult.Missing) {
                mediaCleanup.cleanupCandidates(candidatePaths) {
                    CapsuleFeatureRuntime.invalidate()
                    onDeleted()
                }
            } else update(state.copy(showDeleteConfirmation = false, notice = "删除失败，城市记忆仍保留。"))
        }
    }
    private fun update(next: CapsuleDetailState) { state = next; onStateChanged(next) }
}
