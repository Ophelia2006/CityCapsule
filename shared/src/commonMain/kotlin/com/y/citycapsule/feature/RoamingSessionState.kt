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
import com.y.citycapsule.core.place.GeoPoint
import com.y.citycapsule.core.map.MapAvailability
import com.y.citycapsule.core.map.MapViewEvent
import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.track.TrackFileCapability
import com.y.citycapsule.core.track.TrackReadResult
import com.y.citycapsule.core.location.GeoDistance
import com.y.citycapsule.core.route.LocalRouteDraft
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.roaming.RoamingHistoryRepository
import com.y.citycapsule.core.roaming.RoamingMode
import com.y.citycapsule.core.roaming.RoamingPlaceSnapshot
import com.y.citycapsule.core.roaming.RoamingRecord
import com.y.citycapsule.core.roaming.RoamingVisit
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

data class NearbyRoamingPlace(val place: Place, val distanceMeters: Double, val isFavorite: Boolean)

data class RoamingSessionUiState(
    val loading: Boolean = true,
    val requestedRouteId: String? = null,
    val activeRouteId: String? = null,
    val requestedRouteName: String? = null,
    val session: RoamingSession? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val track: TrackMetadata? = null,
    val sampling: Boolean = false,
    val routePlaces: List<Place> = emptyList(),
    val plannedRoutePoints: List<GeoPoint> = emptyList(),
    val availablePlaces: List<Place> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val nearbyPlaces: List<NearbyRoamingPlace> = emptyList(),
    val checkIns: List<CheckIn> = emptyList(), val distanceMeters: Double? = null,
    val relatedCapsules: List<CityCapsule> = emptyList(),
    val previousMemories: List<CityCapsule> = emptyList(),
    val lastRemovedFavoriteId: String? = null,
    val currentLocation: GeoPoint? = null,
    val trackPoints: List<TrackPoint> = emptyList(),
    val mapPrivacyAccepted: Boolean = false,
    val showMapPrivacyPrompt: Boolean = false,
    val showCapsulePlacePicker: Boolean = false,
    val selectedCapsulePlaceId: String? = null,
    val mapMessage: String? = null
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
    data class RestoreWantTo(val placeId: String) : RoamingSessionIntent
    data object SaveAsRoute : RoamingSessionIntent
    data object MapRequested : RoamingSessionIntent
    data object MapPrivacyAccepted : RoamingSessionIntent
    data object MapPrivacyDismissed : RoamingSessionIntent
    data object OpenCapsulePlacePicker : RoamingSessionIntent
    data object DismissCapsulePlacePicker : RoamingSessionIntent
    data class CapsulePlaceSelected(val placeId: String) : RoamingSessionIntent
    data object CreateCapsule : RoamingSessionIntent
    data class OpenPreviousMemory(val capsuleId: String) : RoamingSessionIntent
    data class MapEventReceived(val event: MapViewEvent) : RoamingSessionIntent
}

sealed interface RoamingSessionEffect {
    data object Back : RoamingSessionEffect
    data class OpenCapsuleEditor(val placeId: String, val roamingSessionId: String) : RoamingSessionEffect
    data class OpenCapsule(val capsuleId: String) : RoamingSessionEffect
}

internal sealed interface RoamingSessionMutation {
    data object Loading : RoamingSessionMutation
    data class Loaded(val session: RoamingSession?, val activeRouteId: String?, val routeName: String?) : RoamingSessionMutation
    data object OperationStarted : RoamingSessionMutation
    data class OperationSucceeded(val session: RoamingSession) : RoamingSessionMutation
    data class Failed(val message: String) : RoamingSessionMutation
    data class Notice(val message: String) : RoamingSessionMutation
    data class TrackLoaded(val track: TrackMetadata?) : RoamingSessionMutation
    data object SamplingStarted : RoamingSessionMutation
    data class TrackUpdated(val track: TrackMetadata) : RoamingSessionMutation
    data class PlacesLoaded(val routePlaces: List<Place>, val availablePlaces: List<Place>, val plannedRoutePoints: List<GeoPoint>) : RoamingSessionMutation
    data class FavoritesLoaded(val ids: Set<String>) : RoamingSessionMutation
    data class Nearby(val places: List<NearbyRoamingPlace>) : RoamingSessionMutation
    data class CheckInsLoaded(val values: List<CheckIn>) : RoamingSessionMutation
    data class Summary(val distance: Double?, val capsules: List<CityCapsule>) : RoamingSessionMutation
    data class PreviousMemoriesLoaded(val values: List<CityCapsule>) : RoamingSessionMutation
    data class ArrivalSaved(val values: List<CheckIn>, val removedPlaceId: String?) : RoamingSessionMutation
    data class HistoryArchived(val record: RoamingRecord) : RoamingSessionMutation
    data class FavoriteRestored(val placeId: String) : RoamingSessionMutation
    data class TrackPointsLoaded(val points: List<TrackPoint>) : RoamingSessionMutation
    data class TrackPointRecorded(val track: TrackMetadata, val point: TrackPoint) : RoamingSessionMutation
    data object ShowMapPrivacy : RoamingSessionMutation
    data object AcceptMapPrivacy : RoamingSessionMutation
    data object DismissMapPrivacy : RoamingSessionMutation
    data class ShowCapsulePlacePicker(val defaultPlaceId: String?) : RoamingSessionMutation
    data object DismissCapsulePlacePicker : RoamingSessionMutation
    data class SelectCapsulePlace(val placeId: String) : RoamingSessionMutation
    data class MapMessage(val message: String?) : RoamingSessionMutation
}

internal object RoamingSessionReducer {
    fun reduce(state: RoamingSessionUiState, mutation: RoamingSessionMutation): RoamingSessionUiState = when (mutation) {
        RoamingSessionMutation.Loading -> state.copy(loading = true, message = null)
        is RoamingSessionMutation.Loaded -> state.copy(
            loading = false,
            session = mutation.session,
            activeRouteId = mutation.activeRouteId,
            requestedRouteName = mutation.routeName
        )
        RoamingSessionMutation.OperationStarted -> state.copy(busy = true, message = null)
        is RoamingSessionMutation.OperationSucceeded -> state.copy(loading = false, busy = false, session = mutation.session)
        is RoamingSessionMutation.Failed -> state.copy(loading = false, busy = false, message = mutation.message)
        is RoamingSessionMutation.Notice -> state.copy(loading = false, busy = false, message = mutation.message)
        is RoamingSessionMutation.TrackLoaded -> state.copy(track = mutation.track)
        RoamingSessionMutation.SamplingStarted -> state.copy(sampling = true)
        is RoamingSessionMutation.TrackUpdated -> state.copy(track = mutation.track, sampling = false)
        is RoamingSessionMutation.PlacesLoaded -> state.copy(routePlaces = mutation.routePlaces, availablePlaces = mutation.availablePlaces, plannedRoutePoints = mutation.plannedRoutePoints)
        is RoamingSessionMutation.FavoritesLoaded -> state.copy(favoriteIds = mutation.ids)
        is RoamingSessionMutation.Nearby -> state.copy(nearbyPlaces = mutation.places, sampling = false)
        is RoamingSessionMutation.CheckInsLoaded -> state.copy(checkIns = mutation.values)
        is RoamingSessionMutation.Summary -> state.copy(distanceMeters = mutation.distance, relatedCapsules = mutation.capsules)
        is RoamingSessionMutation.PreviousMemoriesLoaded -> state.copy(previousMemories = mutation.values)
        is RoamingSessionMutation.ArrivalSaved -> state.copy(
            checkIns = mutation.values,
            favoriteIds = mutation.removedPlaceId?.let { state.favoriteIds - it } ?: state.favoriteIds,
            lastRemovedFavoriteId = mutation.removedPlaceId,
            message = if (mutation.removedPlaceId != null) "已记录到达，并从想去移出。" else "已记录到达。"
        )
        is RoamingSessionMutation.HistoryArchived -> state.copy(distanceMeters = mutation.record.distanceMeters, busy = false)
        is RoamingSessionMutation.FavoriteRestored -> state.copy(favoriteIds = state.favoriteIds + mutation.placeId, lastRemovedFavoriteId = null, message = "已重新加入想去。")
        is RoamingSessionMutation.TrackPointsLoaded -> state.copy(
            trackPoints = mutation.points,
            currentLocation = mutation.points.lastOrNull()?.let { GeoPoint(it.latitude, it.longitude) }
        )
        is RoamingSessionMutation.TrackPointRecorded -> state.copy(
            track = mutation.track,
            trackPoints = state.trackPoints + mutation.point,
            currentLocation = GeoPoint(mutation.point.latitude, mutation.point.longitude),
            sampling = false
        )
        RoamingSessionMutation.ShowMapPrivacy -> state.copy(showMapPrivacyPrompt = true)
        RoamingSessionMutation.AcceptMapPrivacy -> state.copy(mapPrivacyAccepted = true, showMapPrivacyPrompt = false)
        RoamingSessionMutation.DismissMapPrivacy -> state.copy(showMapPrivacyPrompt = false)
        is RoamingSessionMutation.ShowCapsulePlacePicker -> state.copy(
            showCapsulePlacePicker = true,
            selectedCapsulePlaceId = mutation.defaultPlaceId
        )
        RoamingSessionMutation.DismissCapsulePlacePicker -> state.copy(showCapsulePlacePicker = false)
        is RoamingSessionMutation.SelectCapsulePlace -> state.copy(selectedCapsulePlaceId = mutation.placeId)
        is RoamingSessionMutation.MapMessage -> state.copy(mapMessage = mutation.message)
    }
}

class RoamingSessionStore(
    private val repository: RoamingSessionRepository,
    private val routes: LocalRouteRepository,
    private val tracks: TrackRepository,
    private val location: LocationCapability,
    private val places: PlaceRepository, private val checkIns: CheckInRepository,
    private val capsules: CapsuleRepository, private val files: TrackFileCapability,
    private val favorites: FavoriteRepository,
    private val history: RoamingHistoryRepository,
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
        RoamingSessionIntent.End -> endWithFinalSample()
        RoamingSessionIntent.SampleLocation -> sample()
        is RoamingSessionIntent.ConfirmCheckIn -> addCheckIn(intent.placeId, CheckInMethod.GPS_CONFIRMED)
        is RoamingSessionIntent.ManualCheckIn -> addCheckIn(intent.placeId, CheckInMethod.MANUAL)
        is RoamingSessionIntent.RestoreWantTo -> restoreWantTo(intent.placeId)
        RoamingSessionIntent.SaveAsRoute -> saveAsRoute()
        RoamingSessionIntent.MapRequested -> mutations.send(
            if (mutable.value.mapPrivacyAccepted) RoamingSessionMutation.AcceptMapPrivacy
            else RoamingSessionMutation.ShowMapPrivacy
        )
        RoamingSessionIntent.MapPrivacyAccepted -> mutations.send(RoamingSessionMutation.AcceptMapPrivacy)
        RoamingSessionIntent.MapPrivacyDismissed -> mutations.send(RoamingSessionMutation.DismissMapPrivacy)
        RoamingSessionIntent.OpenCapsulePlacePicker -> openCapsulePlacePicker()
        RoamingSessionIntent.DismissCapsulePlacePicker -> mutations.send(RoamingSessionMutation.DismissCapsulePlacePicker)
        is RoamingSessionIntent.CapsulePlaceSelected -> mutations.send(RoamingSessionMutation.SelectCapsulePlace(intent.placeId))
        RoamingSessionIntent.CreateCapsule -> createCapsule()
        is RoamingSessionIntent.OpenPreviousMemory -> effectChannel.send(RoamingSessionEffect.OpenCapsule(intent.capsuleId))
        is RoamingSessionIntent.MapEventReceived -> mutations.send(
            RoamingSessionMutation.MapMessage(when (val event = intent.event) {
                is MapViewEvent.Unavailable -> when (val reason = event.reason) {
                    MapAvailability.MissingConfiguration -> "地图未配置，轨迹仍在本地记录。"
                    MapAvailability.Offline -> "地图当前离线，轨迹仍在本地记录。"
                    MapAvailability.Unsupported -> "当前设备不支持地图，轨迹仍在本地记录。"
                    is MapAvailability.Failure -> reason.message
                    MapAvailability.Ready -> null
                }
                is MapViewEvent.Ready -> null
                else -> mutable.value.mapMessage
            })
        )
    }

    private fun openCapsulePlacePicker() {
        val defaultId = defaultCapsulePlaceId(mutable.value)
        mutations.trySend(RoamingSessionMutation.ShowCapsulePlacePicker(defaultId))
    }

    private suspend fun createCapsule() {
        val session = mutable.value.session ?: return
        val placeId = mutable.value.selectedCapsulePlaceId ?: return
        if (capsulePlaceCandidates(mutable.value).none { it.id == placeId }) return
        mutations.send(RoamingSessionMutation.DismissCapsulePlacePicker)
        effectChannel.send(RoamingSessionEffect.OpenCapsuleEditor(placeId, session.startedAtEpochMs.toString()))
    }

    private fun restoreWantTo(placeId: String) {
        favorites.setFavorite(placeId, true) { result ->
            if (result is StorageResult.Success && result.value) mutations.trySend(RoamingSessionMutation.FavoriteRestored(placeId))
            else mutations.trySend(RoamingSessionMutation.Notice("想去状态恢复失败，请稍后重试。"))
        }
    }

    private fun load() {
        mutations.trySend(RoamingSessionMutation.Loading)
        routes.getCatalog { routeResult ->
            repository.get { sessionResult ->
                val session = (sessionResult as? StorageResult.Success)?.value
                val recoveredSession = session?.takeIf {
                    it.status == RoamingStatus.ACTIVE || it.status == RoamingStatus.PAUSED
                }
                val activeRouteId = recoveredSession?.routeId ?: mutable.value.requestedRouteId
                val name = (routeResult as? StorageResult.Success)?.value?.routes
                    ?.firstOrNull { it.id == activeRouteId }?.name
                mutations.trySend(RoamingSessionMutation.Loaded(session, activeRouteId, name))
                tracks.get { trackResult ->
                    val track = (trackResult as? StorageResult.Success)?.value
                    mutations.trySend(RoamingSessionMutation.TrackLoaded(track))
                    if (track != null) files.readChunks(track.chunkPaths) { read ->
                        mutations.trySend(RoamingSessionMutation.TrackPointsLoaded((read as? TrackReadResult.Success)?.points.orEmpty()))
                    }
                    if (session?.status == RoamingStatus.ENDED) archive(session, track)
                }
                favorites.getFavoriteIds { result ->
                    mutations.trySend(RoamingSessionMutation.FavoritesLoaded((result as? StorageResult.Success)?.value?.placeIds.orEmpty()))
                }
                loadSummary(session, activeRouteId)
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
            is LocationResult.Success -> {
                val nearby = mutable.value.availablePlaces.mapNotNull { place ->
                    place.geoPoint?.let { NearbyRoamingPlace(place, GeoDistance.meters(result.point, it), place.id in mutable.value.favoriteIds) }
                }.filter { it.distanceMeters <= NEARBY_RADIUS_METERS }
                    .sortedWith(compareByDescending<NearbyRoamingPlace>(NearbyRoamingPlace::isFavorite).thenBy(NearbyRoamingPlace::distanceMeters))
                mutations.trySend(RoamingSessionMutation.Nearby(nearby))
                val point = TrackPoint(result.point.latitude, result.point.longitude, result.accuracyMeters, com.tencent.kuikly.core.datetime.DateTime.currentTimestamp())
                tracks.append(point) { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackPointRecorded(track.value, point)) else interrupt("轨迹文件写入失败") }
            }
            else -> interrupt(when (result) { LocationResult.PermissionDenied, LocationResult.PermissionPermanentlyDenied -> "定位权限不可用"; LocationResult.ServiceDisabled -> "定位服务已关闭"; LocationResult.Unavailable -> "设备定位不可用"; is LocationResult.Failure -> result.message; else -> "定位暂时中断" })
        } }
    }
    private fun interrupt(reason: String) { tracks.interrupt(reason) { track -> if (track is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackUpdated(track.value)) else mutations.trySend(RoamingSessionMutation.Failed(reason)) } }
    private fun addCheckIn(placeId:String,method:CheckInMethod){
        val s=mutable.value.session?:return
        val distance = mutable.value.nearbyPlaces.firstOrNull { it.place.id == placeId }?.distanceMeters
        if(method==CheckInMethod.GPS_CONFIRMED && (distance == null || distance > ARRIVAL_RADIUS_METERS)) return
        checkIns.add(s.startedAtEpochMs, placeId, method, distance, placeId in mutable.value.favoriteIds) { result ->
            if (result !is StorageResult.Success) {
                mutations.trySend(RoamingSessionMutation.Failed("到达记录保存失败，请重试。"))
                return@add
            }
            if (placeId !in mutable.value.favoriteIds) {
                mutations.trySend(RoamingSessionMutation.ArrivalSaved(result.value.checkIns, null))
                return@add
            }
            favorites.setFavorite(placeId, false) { favoriteResult ->
                if (favoriteResult is StorageResult.Success && !favoriteResult.value) {
                    mutations.trySend(RoamingSessionMutation.ArrivalSaved(result.value.checkIns, placeId))
                } else {
                    mutations.trySend(RoamingSessionMutation.ArrivalSaved(result.value.checkIns, null))
                    mutations.trySend(RoamingSessionMutation.Notice("已记录到达，但想去状态更新失败。"))
                }
            }
        }
    }

    private fun endWithFinalSample() {
        if (mutable.value.session?.status == RoamingStatus.ACTIVE) {
            mutations.trySend(RoamingSessionMutation.OperationStarted)
            location.getCurrentLocation { result ->
                if (result is LocationResult.Success) {
                    val point = TrackPoint(result.point.latitude, result.point.longitude, result.accuracyMeters, com.tencent.kuikly.core.datetime.DateTime.currentTimestamp())
                    tracks.append(point) { append ->
                        if (append is StorageResult.Success) mutations.trySend(RoamingSessionMutation.TrackPointRecorded(append.value, point))
                        endAndArchive(operationAlreadyStarted = true)
                    }
                } else endAndArchive(operationAlreadyStarted = true)
            }
        } else endAndArchive()
    }

    private fun endAndArchive(operationAlreadyStarted: Boolean = false) {
        if (!operationAlreadyStarted) mutations.trySend(RoamingSessionMutation.OperationStarted)
        repository.end { result ->
            if (result !is StorageResult.Success) {
                mutations.trySend(RoamingSessionMutation.Failed("漫游结束失败，请重试。"))
                return@end
            }
            mutations.trySend(RoamingSessionMutation.OperationSucceeded(result.value))
            tracks.complete { trackResult ->
                val track = (trackResult as? StorageResult.Success)?.value
                track?.let { mutations.trySend(RoamingSessionMutation.TrackUpdated(it)) }
                archive(result.value, track)
            }
        }
    }

    private fun archive(session: RoamingSession, track: TrackMetadata?) {
        places.getCatalog { placeResult ->
            val allPlaces = (placeResult as? StorageResult.Success)?.value?.places.orEmpty()
            routes.getCatalog { routeResult ->
                val route = (routeResult as? StorageResult.Success)?.value?.routes?.firstOrNull { it.id == session.routeId }
                checkIns.prepare(session.startedAtEpochMs) { checkInResult ->
                    val values = (checkInResult as? StorageResult.Success)?.value?.checkIns.orEmpty()
                    fun persist(distance: Double?) {
                        val record = RoamingRecord(
                            id = session.startedAtEpochMs.toString(),
                            mode = if (session.routeId == null) RoamingMode.FREE else RoamingMode.PLANNED,
                            routeId = session.routeId,
                            routeName = route?.name,
                            orderedPlaceIds = route?.orderedPlaceIds.orEmpty(),
                            startedAtEpochMs = session.startedAtEpochMs,
                            endedAtEpochMs = session.endedAtEpochMs ?: session.startedAtEpochMs,
                            distanceMeters = distance,
                            trackChunkPaths = track?.chunkPaths.orEmpty(),
                            visits = values.mapNotNull { checkIn ->
                                allPlaces.firstOrNull { it.id == checkIn.placeId }?.let { place ->
                                    RoamingVisit(
                                        RoamingPlaceSnapshot(place.id, place.name, place.city, place.district),
                                        checkIn.checkedInAtEpochMs,
                                        checkIn.method,
                                        checkIn.distanceMeters,
                                        checkIn.wasWantTo
                                    )
                                }
                            },
                            plannedDistanceMeters = route?.plannedRoute?.distanceMeters,
                            plannedDurationSeconds = route?.plannedRoute?.durationSeconds,
                            plannedTrackPoints = route?.plannedRoute?.points.orEmpty()
                        )
                        history.archive(record) { archiveResult ->
                            if (archiveResult is StorageResult.Success) mutations.trySend(RoamingSessionMutation.HistoryArchived(archiveResult.value))
                            else mutations.trySend(RoamingSessionMutation.Notice("漫游已结束，但回顾保存失败。"))
                        }
                    }
                    if (track == null) persist(null) else files.readChunks(track.chunkPaths) { read ->
                        val points = (read as? TrackReadResult.Success)?.points.orEmpty()
                        persist(points.takeIf { it.size >= 2 }?.zipWithNext()?.sumOf {
                            GeoDistance.meters(
                                com.y.citycapsule.core.place.GeoPoint(it.first.latitude, it.first.longitude),
                                com.y.citycapsule.core.place.GeoPoint(it.second.latitude, it.second.longitude)
                            )
                        })
                    }
                }
            }
        }
    }

    private fun saveAsRoute() {
        val ids = mutable.value.checkIns.map(CheckIn::placeId).distinct()
        if (ids.isEmpty()) {
            mutations.trySend(RoamingSessionMutation.Failed("至少打卡一个地点后才能保存路线。"))
            return
        }
        mutations.trySend(RoamingSessionMutation.OperationStarted)
        routes.create(LocalRouteDraft("自由漫游路线", ids)) { result ->
            if (result is StorageResult.Success) mutations.trySend(RoamingSessionMutation.Notice("已按打卡顺序保存为路线。"))
            else mutations.trySend(RoamingSessionMutation.Failed("路线保存失败，请重试。"))
        }
    }
    private fun loadSummary(session: RoamingSession?, activeRouteId: String?) {
        places.getCatalog { placeResult ->
            val allPlaces = (placeResult as? StorageResult.Success)?.value?.places.orEmpty()
            routes.getCatalog { routeResult ->
                val activeRoute = (routeResult as? StorageResult.Success)?.value?.routes?.firstOrNull { it.id == activeRouteId }
                val ids = activeRoute?.orderedPlaceIds.orEmpty()
                val placesById = allPlaces.associateBy(Place::id)
                mutations.trySend(RoamingSessionMutation.PlacesLoaded(ids.mapNotNull(placesById::get), allPlaces, activeRoute?.plannedRoute?.points.orEmpty()))
                if (session != null) checkIns.prepare(session.startedAtEpochMs) { result ->
                    if (result is StorageResult.Success) mutations.trySend(RoamingSessionMutation.CheckInsLoaded(result.value.checkIns))
                }
                capsules.getPublished { capsuleResult ->
                    val sessionId = session?.startedAtEpochMs?.toString()
                    val published = (capsuleResult as? StorageResult.Success)?.value.orEmpty()
                    val values = if (sessionId == null) emptyList() else published.filter { it.roamingSessionId == sessionId }
                    mutations.trySend(RoamingSessionMutation.PreviousMemoriesLoaded(
                        published.filter { sessionId == null || it.roamingSessionId != sessionId }.sortedByDescending(CityCapsule::createdAtEpochMs)
                    ))
                    if (session == null) {
                        mutations.trySend(RoamingSessionMutation.Summary(null, emptyList()))
                    } else tracks.get { trackResult ->
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

    private companion object {
        const val NEARBY_RADIUS_METERS = 500.0
        const val ARRIVAL_RADIUS_METERS = 200.0
    }
}

internal fun capsulePlaceCandidates(state: RoamingSessionUiState): List<Place> {
    val nearby = state.nearbyPlaces.map { it.place }
    val visitedIds = state.checkIns.map(CheckIn::placeId).toSet()
    val visited = state.availablePlaces.filter { it.id in visitedIds }
    val route = state.routePlaces
    val prioritized = if (state.activeRouteId != null) route + nearby else nearby + route
    return (prioritized + visited + state.availablePlaces).distinctBy(Place::id)
}

internal fun defaultCapsulePlaceId(state: RoamingSessionUiState): String? {
    val nearest = state.nearbyPlaces.minByOrNull(NearbyRoamingPlace::distanceMeters)?.place?.id
    if (nearest != null) return nearest
    return if (state.activeRouteId != null) state.routePlaces.firstOrNull()?.id
    else capsulePlaceCandidates(state).firstOrNull()?.id
}
