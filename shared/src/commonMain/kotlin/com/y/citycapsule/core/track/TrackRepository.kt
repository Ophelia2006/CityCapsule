package com.y.citycapsule.core.track

import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

interface TrackRepository {
    fun get(callback: StorageCallback<TrackMetadata>)
    fun prepare(sessionStartedAt: Long, callback: StorageCallback<TrackMetadata>)
    fun append(point: TrackPoint, callback: StorageCallback<TrackMetadata>)
    fun interrupt(reason: String, callback: StorageCallback<TrackMetadata>)
    fun complete(callback: StorageCallback<TrackMetadata>)
}

class LocalTrackRepository(private val storage: KeyValueStore, private val files: TrackFileCapability) : TrackRepository {
    private val queue = mutableListOf<(() -> Unit) -> Unit>(); private var busy = false
    override fun get(callback: StorageCallback<TrackMetadata>) = storage.get(AppStorageKeys.Roaming.TRACK, callback)
    override fun prepare(sessionStartedAt: Long, callback: StorageCallback<TrackMetadata>) = enqueue { done ->
        get { result ->
            val existing = (result as? StorageResult.Success)?.value
            val value = if (existing?.sessionStartedAtEpochMs == sessionStartedAt) existing.copy(status = TrackStatus.RECORDING, interruptionReason = null) else TrackMetadata(sessionStartedAt)
            write(value, callback, done)
        }
    }
    override fun append(point: TrackPoint, callback: StorageCallback<TrackMetadata>) = enqueue { done -> get { result ->
        val metadata = result as? StorageResult.Success ?: return@get deliver(callback, invalid("Track metadata is missing."), done)
        if (metadata.value.chunkPaths.size >= TrackContract.MAX_CHUNKS) return@get deliver(callback, invalid("Track chunk limit reached."), done)
        files.writeChunk(metadata.value.sessionStartedAtEpochMs, metadata.value.chunkPaths.size, listOf(point)) { file ->
            if (file is TrackFileResult.Success) write(metadata.value.copy(chunkPaths = metadata.value.chunkPaths + file.path, pointCount = metadata.value.pointCount + 1, status = TrackStatus.RECORDING, interruptionReason = null, lastPointAtEpochMs = point.recordedAtEpochMs), callback, done)
            else deliver(callback, invalid("Track chunk write failed."), done)
        }
    } }
    override fun interrupt(reason: String, callback: StorageCallback<TrackMetadata>) = update(callback) { it.copy(status = TrackStatus.INTERRUPTED, interruptionReason = reason.take(120)) }
    override fun complete(callback: StorageCallback<TrackMetadata>) = update(callback) { it.copy(status = TrackStatus.COMPLETED) }
    private fun update(callback: StorageCallback<TrackMetadata>, transform: (TrackMetadata) -> TrackMetadata) = enqueue { done -> get { result -> if (result is StorageResult.Success) write(transform(result.value), callback, done) else deliver(callback, invalid("Track metadata is missing."), done) } }
    private fun write(value: TrackMetadata, callback: StorageCallback<TrackMetadata>, done: () -> Unit) = storage.put(AppStorageKeys.Roaming.TRACK, value) { result -> deliver(callback, if (result is StorageResult.Success) StorageResult.Success(value) else invalid("Track metadata write failed."), done) }
    private fun enqueue(task: (() -> Unit) -> Unit) { queue += task; drain() }; private fun drain() { if (busy || queue.isEmpty()) return; busy = true; val task = queue.removeAt(0); task { busy = false; drain() } }
    private fun <T> deliver(callback: StorageCallback<T>, result: StorageResult<T>, done: () -> Unit) { try { callback(result) } finally { done() } }
    private fun invalid(message: String) = StorageResult.Failure(StorageError(StorageErrorCode.INVALID_REQUEST, message))
}
