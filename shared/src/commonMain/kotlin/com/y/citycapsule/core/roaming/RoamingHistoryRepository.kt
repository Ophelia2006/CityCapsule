package com.y.citycapsule.core.roaming

import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

interface RoamingHistoryRepository {
    fun getCatalog(callback: StorageCallback<RoamingHistoryCatalog>)
    fun get(recordId: String, callback: StorageCallback<RoamingRecord>)
    fun archive(record: RoamingRecord, callback: StorageCallback<RoamingRecord>)
}

class LocalRoamingHistoryRepository(private val storage: KeyValueStore) : RoamingHistoryRepository {
    private val queue = mutableListOf<(() -> Unit) -> Unit>()
    private var busy = false

    override fun getCatalog(callback: StorageCallback<RoamingHistoryCatalog>) = storage.get(AppStorageKeys.Roaming.HISTORY) {
        callback(if (it === StorageResult.Missing) StorageResult.Success(RoamingHistoryCatalog.EMPTY) else it)
    }

    override fun get(recordId: String, callback: StorageCallback<RoamingRecord>) = getCatalog { result ->
        if (result is StorageResult.Success) callback(result.value.records.firstOrNull { it.id == recordId }?.let { StorageResult.Success(it) } ?: StorageResult.Missing)
        else callback(failure("Roaming history read failed."))
    }

    override fun archive(record: RoamingRecord, callback: StorageCallback<RoamingRecord>) {
        val normalized = RoamingHistoryValidator.normalizeOrNull(record) ?: return callback(failure("Roaming record is invalid."))
        enqueue { done -> getCatalog { result ->
            if (result !is StorageResult.Success) { callback(failure("Roaming history read failed.")); done(); return@getCatalog }
            val merged = (listOf(normalized) + result.value.records.filterNot { it.id == normalized.id })
                .sortedByDescending(RoamingRecord::startedAtEpochMs)
                .take(RoamingHistoryContract.MAX_RECORDS)
            storage.put(AppStorageKeys.Roaming.HISTORY, RoamingHistoryCatalog(records = merged)) { saved ->
                callback(if (saved is StorageResult.Success) StorageResult.Success(normalized) else failure("Roaming history write failed.")); done()
            }
        } }
    }

    private fun enqueue(task: (() -> Unit) -> Unit) { queue += task; drain() }
    private fun drain() { if (busy || queue.isEmpty()) return; busy = true; val task = queue.removeAt(0); task { busy = false; drain() } }
    private fun failure(message: String) = StorageResult.Failure(StorageError(StorageErrorCode.INVALID_REQUEST, message))
}
