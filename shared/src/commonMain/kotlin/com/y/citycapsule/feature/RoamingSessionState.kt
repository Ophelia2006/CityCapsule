package com.y.citycapsule.feature.roaming

import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.roaming.RoamingSession
import com.y.citycapsule.core.roaming.RoamingSessionRepository
import com.y.citycapsule.core.roaming.RoamingStatus
import com.y.citycapsule.core.route.LocalRouteRepository
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.location.LocationCapability
import com.y.citycapsule.core.location.LocationResult
import com.y.citycapsule.core.track.TrackMetadata
import com.y.citycapsule.core.track.TrackPoint
import com.y.citycapsule.core.track.TrackRepository
import com.y.citycapsule.core.checkin.*
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.track.TrackFileCapability
import com.y.citycapsule.core.track.TrackReadResult
import com.y.citycapsule.core.location.GeoDistance
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

data class RoamingSessionUiState(
    val loading: Boolean = true,
    val requestedRouteId: String? = null,
    val requestedRouteName: String? = null,
    val session: RoamingSession? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val track: TrackMetadata? = null,
    val sampling: Boolean = false,
    val routePlaces: List<Place> = emptyList(), val nearbyPlaceId: String? = null,
    val checkIns: List<CheckIn> = emptyList(), val distanceMeters: Double? = null,
    val relatedCapsules: List<CityCapsule> = emptyList()
)

sealed interface RoamingSessionIntent {
    data object Load : RoamingSessionIntent
    data object Back : RoamingSessionIntent
    data object Start : RoamingSessionIntent
    data object Pause : RoamingSessionIntent
    data object Resume : RoamingSessionIntent
    data object End : RoamingSessionIntent
    data object SampleLocation : RoamingSessionIntent
    data class ConfirmCheckIn(val placeId: String) : RoamingSessionIntent
    data class ManualCheckIn(val placeId: String) : RoamingSessionIntent
}

sealed interface RoamingSessionEffect { data object Back : RoamingSessionEffect }

internal sealed interface RoamingSessionMutation {
    data object Loading : RoamingSessionMutation
    data class Loaded(val session: RoamingSession?, val routeName: String?) : RoamingSessionMutation
    data object OperationStarted : RoamingSessionMutation
    data class OperationSucceeded(val session: RoamingSession) : RoamingSessionMutation
    data class Failed(val message: String) : RoamingSessionMutation
    data class TrackLoaded(val track: TrackMetadata?) : RoamingSessionMutation
    data object SamplingStarted : RoamingSessionMutation
    data class TrackUpdated(val track: TrackMetadata) : RoamingSessionMutation
    data class Nearby(val placeId: String?) : RoamingSessionMutation
    data class CheckInsLoaded(val values: List<CheckIn>) : RoamingSessionMutation
    data class Summary(val distance: Double?, val capsules: List<CityCapsule>) : RoamingSessionMutation
}

internal object RoamingSessionReducer {
    fun reduce(state: RoamingSessionUiState, mutation: RoamingSessionMutation): RoamingSessionUiState = when (mutation) {
        RoamingSessionMutation.Loading -> state.copy(loading = true, message = null)
        is RoamingSessionMutation.Loaded -> state.copy(loading = false, session = mutation.session, requestedRouteName = mutation.routeName)
        RoamingSessionMutation.OperationStarted -> state.copy(busy = true, message = null)
        is RoamingSessionMutation.OperationSucceeded -> state.copy(loading = false, busy = false, session = mutation.session)
        is RoamingSessionMutation.Failed -> state.copy(loading = false, busy = false, message = mutation.message)
        is RoamingSessionMutation.TrackLoaded -> state.copy(track = mutation.track)
        RoamingSessionMutation.SamplingStarted -> state.copy(sampling = true)
        is RoamingSessionMutation.TrackUpdated -> state.copy(track = mutation.track, sampling = false)
        is RoamingSessionMutation.Nearby -> state.copy(nearbyPlaceId = mutation.placeId)
        is RoamingSessionMutation.CheckInsLoaded -> state.copy(checkIns = mutation.values)
        is RoamingSessionMutation.Summary -> state.copy(distanceMeters = mutation.distance, relatedCapsules = mutation.capsules)
    }
}

class RoamingSessionStore(
    private val repository: RoamingSessionRepository,
    private val routes: LocalRouteRepository,
    private val tracks: TrackRepository,
    private val location: LocationCapability,
    private val places: PlaceRepository, private val checkIns: CheckInRepository,
    private val capsules: CapsuleRepository, private val files: TrackFileCapability,
    requestedRouteId: String?,
    parentScope: CoroutineScope
) : MviStore<RoamingSessionIntent, RoamingSessionUiState, RoamingSessionEffect> {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val intents = Channel<RoamingSessionIntent>(Channel.UNLIMITED)
    private val mutations = Channel<RoamingSessionMutation>(Channel.UNLIMITED)
    private val effectChannel = Channel<RoamingSessionEffect>(Channel.UNLIMITED)
    private val mutable = MutableStateFlow(RoamingSessionUiState(requestedRouteId = requestedRouteId))
    private var disposed = false
    override val state: StateFlow<RoamingSessionUiState> = mutable.asStateFlow()
    override val effects: Flow<RoamingSessionEffect> = effectChannel.receiveAsFlow()

    init {
        scope.launch { for (mutation in mutations) mutable.value = RoamingSessionReducer.reduce(mutable.value, mutation) }
        scope.launch { for (intent in intents) handle(intent) }
    }
    override fun dispatch(intent: RoamingSessionIntent) { if (!disposed) intents.trySend(intent) }
    override fun dispose() { if (!disposed) { disposed = true; intents.close(); mutations.close(); effectChannel.close(); scope.cancel() } }

    private suspend fun handle(intent: RoamingSessionIntent) = when (intent) {
        RoamingSessionIntent.Load -> load()
        RoamingSessionIntent.Back -> effectChannel.send(RoamingSessionEffect.Back)
        RoamingSessionIntent.Start -> operate { repository.start(mutable.value.requestedRouteId, it) }
        RoamingSessionIntent.Pause -> operate(repository::pause)
        RoamingSessionIntent.Resume -> operate(repository::resume)
        RoamingSessionIntent.End -> operate(repository::end)
        RoamingSessionIntent.SampleLocation -> sample()
        is RoamingSessionIntent.ConfirmCheckIn -> addCheckIn(intent.placeId, CheckInMethod.GPS_CONFIRMED)
        is RoamingSessionIntent.ManualCheckIn -> addCheckIn(intent.placeId, CheckInMethod.MANUAL)
    }

    private fun load() {
        mutations.trySend(RoamingSessionMutation.Loading)
        routes.getCatalog { routeResult ->
            val name = (routeResult as? StorageResult.Success)?.value?.routes?.firstOrNull { it.id == mutable.value.requestedRouteId }?.name
            repository.get { sessionResult ->
                mutations.trySend(RoamingSessionMutation.Loaded((sessionResult as? StorageResult.Success)?.value, name))
                tracks.get { track -> mutations.trySend(RoamingSessionMutation.TrackLoaded((track as? StorageResult.Success)?.value)) }
                loadSummary((sessionResult as? StorageResult.Success)?.value)
            }
        }
    }

    private fun operate(operation: ((StorageResult<RoamingSession>) -> Unit) -> Unit) {
        mutations.trySend(RoamingSessionMutation.OperationStarted)
        operation { result -> if (result is StorageResult.Success) {
            mutations.trySend(RoamingSessionMutation.OperationSucceeded(result.value))
            when (result.value.status) {
                RoamingStatus.ACTIVE -> tracks.prepare(result.value.startedAtEpochMs) { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackUpdated(track.value)) }
                RoamingStatus.ENDED -> tracks.complete { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackUpdated(track.value)) }
                RoamingStatus.PAUSED -> Unit
            }
        } else mutations.trySend(RoamingSessionMutation.Failed("漫游状态更新失败，请重试。")) }
    }

    private fun sample() {
        if (mutable.value.session?.status != RoamingStatus.ACTIVE || mutable.value.sampling) return
        mutations.trySend(RoamingSessionMutation.SamplingStarted)
        location.getCurrentLocation { result -> when (result) {
            is LocationResult.Success -> { val nearby=mutable.value.routePlaces.filter{it.geoPoint!=null}.map{it to GeoDistance.meters(result.point,requireNotNull(it.geoPoint))}.filter{it.second<=150.0}.minByOrNull{it.second}; mutations.trySend(RoamingSessionMutation.Nearby(nearby?.first?.id)); tracks.append(TrackPoint(result.point.latitude, result.point.longitude, result.accuracyMeters, com.tencent.kuikly.core.datetime.DateTime.currentTimestamp())) { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackUpdated(track.value)) else interrupt("轨迹文件写入失败") } }
            else -> interrupt(when (result) { LocationResult.PermissionDenied, LocationResult.PermissionPermanentlyDenied -> "定位权限不可用"; LocationResult.ServiceDisabled -> "定位服务已关闭"; LocationResult.Unavailable -> "设备定位不可用"; is LocationResult.Failure -> result.message; else -> "定位暂时中断" })
        } }
    }
    private fun interrupt(reason: String) { tracks.interrupt(reason) { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackUpdated(track.value)) else mutations.trySend(RoamingSessionMutation.Failed(reason)) } }
    private fun addCheckIn(placeId:String,method:CheckInMethod){val s=mutable.value.session?:return;if(method==CheckInMethod.GPS_CONFIRMED&&mutable.value.nearbyPlaceId!=placeId)return;checkIns.add(s.startedAtEpochMs,placeId,method,null){r->if(r is StorageResult.Success)mutations.trySend(RoamingSessionMutation.CheckInsLoaded(r.value.checkIns))}}
    private fun loadSummary(session: RoamingSession?) {
        if (session == null) return
        places.getCatalog { placeResult ->
            val allPlaces = (placeResult as? StorageResult.Success)?.value?.places.orEmpty()
            routes.getCatalog { routeResult ->
                val ids = (routeResult as? StorageResult.Success)?.value?.routes
                    ?.firstOrNull { it.id == session.routeId }?.orderedPlaceIds.orEmpty()
                mutable.value = mutable.value.copy(routePlaces = allPlaces.filter { it.id in ids })
                checkIns.prepare(session.startedAtEpochMs) { result ->
                    if (result is StorageResult.Success) mutations.trySend(RoamingSessionMutation.CheckInsLoaded(result.value.checkIns))
                }
                capsules.getPublished { capsuleResult ->
                    val values = (capsuleResult as? StorageResult.Success)?.value.orEmpty().filter {
                        it.createdAtEpochMs >= session.startedAtEpochMs && (session.endedAtEpochMs == null || it.createdAtEpochMs <= session.endedAtEpochMs)
                    }
                    tracks.get { trackResult ->
                        val meta = (trackResult as? StorageResult.Success)?.value
                        if (meta == null) {
                            mutations.trySend(RoamingSessionMutation.Summary(null, values))
                        } else files.readChunks(meta.chunkPaths) { read ->
                            val points = (read as? TrackReadResult.Success)?.points.orEmpty()
                            val distance = points.zipWithNext().sumOf {
                                GeoDistance.meters(com.y.citycapsule.core.place.GeoPoint(it.first.latitude, it.first.longitude), com.y.citycapsule.core.place.GeoPoint(it.second.latitude, it.second.longitude))
                            }
                            mutations.trySend(RoamingSessionMutation.Summary(distance, values))
                        }
                    }
                }
            }
        }
    }
}
