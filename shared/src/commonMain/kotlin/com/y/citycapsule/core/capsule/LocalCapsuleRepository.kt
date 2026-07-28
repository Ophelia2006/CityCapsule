package com.y.citycapsule.core.capsule

import com.tencent.kuikly.core.datetime.DateTime
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

class LocalCapsuleRepository(
    private val storage: KeyValueStore,
    private val clock: CapsuleClock = CapsuleClock { DateTime.currentTimestamp() },
    private val idGenerator: CapsuleIdGenerator = TimestampCapsuleIdGenerator(clock)
) : CapsuleRepository {
    private val queue = mutableListOf<(() -> Unit) -> Unit>()
    private var mutationInFlight = false

    override fun getPublished(callback: StorageCallback<List<CityCapsule>>) {
        getCatalog { result ->
            callback(when (result) {
                is StorageResult.Success -> StorageResult.Success(
                    result.value.capsules.sortedByDescending(CityCapsule::createdAtEpochMs)
                )
                StorageResult.Missing -> StorageResult.Success(emptyList())
                is StorageResult.Failure -> result
            })
        }
    }

    override fun getPublishedForPlace(
        placeId: String,
        callback: StorageCallback<List<CityCapsule>>
    ) {
        val id = placeId.trim()
        if (id.isEmpty()) {
            callback(invalid("Place id is invalid."))
            return
        }
        getPublished { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> StorageResult.Success(
                        result.value.filter { it.placeId == id }
                    )
                    StorageResult.Missing -> StorageResult.Success(emptyList())
                    is StorageResult.Failure -> result
                }
            )
        }
    }

    override fun getById(capsuleId: String, callback: StorageCallback<CityCapsule>) {
        val id = capsuleId.trim()
        if (id.isEmpty()) return callback(invalid("Capsule id is invalid."))
        getCatalog { result ->
            callback(when (result) {
                is StorageResult.Success -> result.value.capsules.firstOrNull { it.id == id }
                    ?.let { StorageResult.Success(it) } ?: StorageResult.Missing
                StorageResult.Missing -> StorageResult.Missing
                is StorageResult.Failure -> result
            })
        }
    }

    override fun publish(draft: CapsuleDraft, callback: StorageCallback<CityCapsule>) {
        val normalized = CapsuleValidator.normalizeDraft(draft)
            ?: return callback(invalid("Capsule draft is invalid."))
        enqueue { complete ->
            getCatalog { result ->
                val catalog = when (result) {
                    is StorageResult.Success -> result.value
                    StorageResult.Missing -> CapsuleCatalog.EMPTY
                    is StorageResult.Failure -> return@getCatalog deliver(callback, result, complete)
                }
                val existingIndex = normalized.capsuleId?.let { id ->
                    catalog.capsules.indexOfFirst { it.id == id }
                } ?: -1
                if (normalized.capsuleId != null && existingIndex < 0) {
                    return@getCatalog deliver(callback, StorageResult.Missing, complete)
                }
                if (existingIndex < 0 && catalog.capsules.size >= CapsuleContract.MAX_CATALOG_SIZE) {
                    return@getCatalog deliver(callback, invalid("Capsule catalog is full."), complete)
                }
                val now = clock.nowEpochMs()
                val existing = catalog.capsules.getOrNull(existingIndex)
                val id = existing?.id ?: uniqueId(catalog)
                    ?: return@getCatalog deliver(callback, invalid("Capsule id could not be generated."), complete)
                val capsule = CapsuleValidator.normalize(
                    CityCapsule(
                        id = id,
                        content = normalized.content,
                        mood = normalized.mood,
                        tags = normalized.tags,
                        placeId = requireNotNull(normalized.placeId),
                        imagePaths = normalized.imagePaths,
                        createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                        updatedAtEpochMs = maxOf(now, existing?.createdAtEpochMs ?: now)
                    )
                ) ?: return@getCatalog deliver(callback, invalid("Capsule is invalid."), complete)
                val updated = catalog.capsules.toMutableList().apply {
                    if (existingIndex >= 0) this[existingIndex] = capsule else add(capsule)
                }
                storage.put(AppStorageKeys.Capsules.CATALOG, catalog.copy(capsules = updated)) { write ->
                    when (write) {
                        is StorageResult.Success -> clearDraftIfMatches(normalized) {
                            deliver(callback, StorageResult.Success(capsule), complete)
                        }
                        StorageResult.Missing -> deliver(callback, nativeFailure(), complete)
                        is StorageResult.Failure -> deliver(callback, write, complete)
                    }
                }
            }
        }
    }

    override fun delete(capsuleId: String, callback: StorageCallback<Unit>) {
        val id = capsuleId.trim()
        if (id.isEmpty()) return callback(invalid("Capsule id is invalid."))
        enqueue { complete ->
            getCatalog { result ->
                when (result) {
                    is StorageResult.Success -> {
                        if (result.value.capsules.none { it.id == id }) {
                            deliver(callback, StorageResult.Missing, complete)
                        } else storage.put(
                            AppStorageKeys.Capsules.CATALOG,
                            result.value.copy(capsules = result.value.capsules.filterNot { it.id == id })
                        ) { write ->
                            when (write) {
                                is StorageResult.Success -> clearDraftForCapsule(id) {
                                    deliver(callback, StorageResult.Success(Unit), complete)
                                }
                                StorageResult.Missing -> deliver(callback, nativeFailure(), complete)
                                is StorageResult.Failure -> deliver(callback, write, complete)
                            }
                        }
                    }
                    StorageResult.Missing -> deliver(callback, StorageResult.Missing, complete)
                    is StorageResult.Failure -> deliver(callback, result, complete)
                }
            }
        }
    }

    override fun getDraft(callback: StorageCallback<CapsuleDraft>) {
        storage.get(AppStorageKeys.Capsules.DRAFT) { result ->
            callback(if (result is StorageResult.Missing) StorageResult.Success(CapsuleDraft.EMPTY) else result)
        }
    }

    override fun saveDraft(draft: CapsuleDraft, callback: StorageCallback<Unit>) {
        val safe = draft.copy(
            capsuleId = draft.capsuleId?.trim()?.takeIf(String::isNotEmpty),
            content = draft.content.take(CapsuleContract.CONTENT_MAX_LENGTH),
            tags = draft.tags
                .map(String::trim)
                .filter(String::isNotEmpty)
                .map { it.take(CapsuleContract.TAG_MAX_LENGTH) }
                .distinct()
                .take(CapsuleContract.TAG_MAX_COUNT),
            placeId = draft.placeId?.trim()?.takeIf(String::isNotEmpty),
            imagePaths = draft.imagePaths
                .map(String::trim)
                .filter { it.isNotEmpty() && it.length <= CapsuleContract.IMAGE_PATH_MAX_LENGTH }
                .distinct()
                .take(CapsuleContract.IMAGE_MAX_COUNT),
            updatedAtEpochMs = clock.nowEpochMs()
        )
        storage.put(AppStorageKeys.Capsules.DRAFT, safe, callback)
    }

    override fun clearDraft(callback: StorageCallback<Unit>) {
        storage.remove(AppStorageKeys.Capsules.DRAFT, callback)
    }

    private fun getCatalog(callback: StorageCallback<CapsuleCatalog>) {
        storage.get(AppStorageKeys.Capsules.CATALOG, callback)
    }

    private fun clearDraftIfMatches(
        published: CapsuleDraft,
        onComplete: () -> Unit
    ) {
        getDraft { result ->
            val saved = (result as? StorageResult.Success)?.value
            val matches = if (published.capsuleId != null) {
                saved?.capsuleId == published.capsuleId
            } else {
                saved?.capsuleId == null && saved?.placeId == published.placeId
            }
            if (matches) clearDraft { onComplete() } else onComplete()
        }
    }

    private fun clearDraftForCapsule(capsuleId: String, onComplete: () -> Unit) {
        getDraft { result ->
            val saved = (result as? StorageResult.Success)?.value
            if (saved?.capsuleId == capsuleId) clearDraft { onComplete() } else onComplete()
        }
    }

    private fun uniqueId(catalog: CapsuleCatalog): String? {
        repeat(10) {
            val candidate = idGenerator.newId().trim()
            if (candidate.isNotEmpty() && catalog.capsules.none { it.id == candidate }) return candidate
        }
        return null
    }

    private fun enqueue(operation: (() -> Unit) -> Unit) {
        queue += operation
        if (!mutationInFlight) next()
    }

    private fun next() {
        if (queue.isEmpty()) { mutationInFlight = false; return }
        mutationInFlight = true
        val operation = queue.removeAt(0)
        operation {
            mutationInFlight = false
            next()
        }
    }

    private fun <T> deliver(callback: StorageCallback<T>, result: StorageResult<T>, complete: () -> Unit) {
        try { callback(result) } finally { complete() }
    }
}

private class TimestampCapsuleIdGenerator(private val clock: CapsuleClock) : CapsuleIdGenerator {
    private var sequence = 0
    override fun newId(): String = "capsule_${clock.nowEpochMs()}_${sequence++}"
}

private fun invalid(message: String) = StorageResult.Failure(
    StorageError(StorageErrorCode.INVALID_REQUEST, message)
)

private fun nativeFailure() = StorageResult.Failure(
    StorageError(StorageErrorCode.NATIVE_FAILURE, "Capsule write was not confirmed.")
)
