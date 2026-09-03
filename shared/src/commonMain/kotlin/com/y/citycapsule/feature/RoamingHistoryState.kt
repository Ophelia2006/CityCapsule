package com.y.citycapsule.feature.roaming

import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.roaming.RoamingHistoryRepository
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.track.TrackFileCapability
import com.y.citycapsule.core.track.TrackPoint
import com.y.citycapsule.core.track.TrackReadResult
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

enum class RoamingHistoryStatus { LOADING, READY, ERROR }

data class RoamingHistoryUiState(
    val status: RoamingHistoryStatus = RoamingHistoryStatus.LOADING,
    val records: List<RoamingRecord> = emptyList(),
    val selectedRecord: RoamingRecord? = null,
    val capsules: List<CityCapsule> = emptyList(),
    val trackPoints: List<TrackPoint> = emptyList(),
    val message: String? = null
)

sealed interface RoamingHistoryIntent {
    data object Load : RoamingHistoryIntent
    data object Back : RoamingHistoryIntent
    data class OpenRecord(val id: String) : RoamingHistoryIntent
    data class OpenCapsule(val id: String) : RoamingHistoryIntent
    data class AddCapsule(val placeId: String, val sessionId: String) : RoamingHistoryIntent
}

sealed interface RoamingHistoryEffect {
    data object Back : RoamingHistoryEffect
    data class OpenRecord(val id: String) : RoamingHistoryEffect
    data class OpenCapsule(val id: String) : RoamingHistoryEffect
    data class AddCapsule(val placeId: String, val sessionId: String) : RoamingHistoryEffect
}

internal sealed interface RoamingHistoryMutation {
    data object Loading : RoamingHistoryMutation
    data class Loaded(val records: List<RoamingRecord>, val capsules: List<CityCapsule>, val selected: RoamingRecord?) : RoamingHistoryMutation
    data class TrackLoaded(val points: List<TrackPoint>) : RoamingHistoryMutation
    data class Failed(val message: String) : RoamingHistoryMutation
}

internal object RoamingHistoryReducer {
    fun reduce(state: RoamingHistoryUiState, mutation: RoamingHistoryMutation): RoamingHistoryUiState = when (mutation) {
        RoamingHistoryMutation.Loading -> state.copy(status = RoamingHistoryStatus.LOADING, message = null)
        is RoamingHistoryMutation.Loaded -> state.copy(status = RoamingHistoryStatus.READY, records = mutation.records, capsules = mutation.capsules, selectedRecord = mutation.selected, message = null)
        is RoamingHistoryMutation.TrackLoaded -> state.copy(trackPoints = mutation.points)
        is RoamingHistoryMutation.Failed -> state.copy(status = RoamingHistoryStatus.ERROR, message = mutation.message)
    }
}

class RoamingHistoryStore(
    private val history: RoamingHistoryRepository,
    private val capsules: CapsuleRepository,
    private val trackFiles: TrackFileCapability,
    private val recordId: String?,
    parentScope: CoroutineScope
) : MviStore<RoamingHistoryIntent, RoamingHistoryUiState, RoamingHistoryEffect> {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val intents = Channel<RoamingHistoryIntent>(Channel.UNLIMITED)
    private val mutations = Channel<RoamingHistoryMutation>(Channel.UNLIMITED)
    private val effectsChannel = Channel<RoamingHistoryEffect>(Channel.UNLIMITED)
    private val mutable = MutableStateFlow(RoamingHistoryUiState())
    private var disposed = false
    override val state: StateFlow<RoamingHistoryUiState> = mutable.asStateFlow()
    override val effects: Flow<RoamingHistoryEffect> = effectsChannel.receiveAsFlow()

    init {
        scope.launch { for (mutation in mutations) mutable.value = RoamingHistoryReducer.reduce(mutable.value, mutation) }
        scope.launch { for (intent in intents) handle(intent) }
    }
    override fun dispatch(intent: RoamingHistoryIntent) { if (!disposed) intents.trySend(intent) }
    override fun dispose() { if (!disposed) { disposed = true; intents.close(); mutations.close(); effectsChannel.close(); scope.cancel() } }

    private suspend fun handle(intent: RoamingHistoryIntent) = when (intent) {
        RoamingHistoryIntent.Load -> load()
        RoamingHistoryIntent.Back -> effectsChannel.send(RoamingHistoryEffect.Back)
        is RoamingHistoryIntent.OpenRecord -> effectsChannel.send(RoamingHistoryEffect.OpenRecord(intent.id))
        is RoamingHistoryIntent.OpenCapsule -> effectsChannel.send(RoamingHistoryEffect.OpenCapsule(intent.id))
        is RoamingHistoryIntent.AddCapsule -> effectsChannel.send(RoamingHistoryEffect.AddCapsule(intent.placeId, intent.sessionId))
    }

    private fun load() {
        mutations.trySend(RoamingHistoryMutation.Loading)
        history.getCatalog { historyResult ->
            if (historyResult !is StorageResult.Success) {
                mutations.trySend(RoamingHistoryMutation.Failed("漫游回顾读取失败，请重试。"))
                return@getCatalog
            }
            capsules.getPublished { capsuleResult ->
                if (capsuleResult !is StorageResult.Success) {
                    mutations.trySend(RoamingHistoryMutation.Failed("城市碎片读取失败，请重试。"))
                    return@getPublished
                }
                val records = historyResult.value.records.sortedByDescending(RoamingRecord::startedAtEpochMs)
                val selected = records.firstOrNull { it.id == recordId }
                mutations.trySend(RoamingHistoryMutation.Loaded(records, capsuleResult.value, selected))
                if (selected != null && selected.trackChunkPaths.isNotEmpty()) {
                    trackFiles.readChunks(selected.trackChunkPaths) { result ->
                        mutations.trySend(RoamingHistoryMutation.TrackLoaded((result as? TrackReadResult.Success)?.points.orEmpty()))
                    }
                } else {
                    mutations.trySend(RoamingHistoryMutation.TrackLoaded(emptyList()))
                }
            }
        }
    }
}
