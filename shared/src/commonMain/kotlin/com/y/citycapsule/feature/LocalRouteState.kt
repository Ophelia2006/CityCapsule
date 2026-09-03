package com.y.citycapsule.feature.route

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.citycapsule.core.mvi.MviStore
import com.y.citycapsule.core.place.Place
import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.route.LocalRoute
import com.y.citycapsule.core.route.LocalRouteDraft
import com.y.citycapsule.core.route.LocalRouteRepository
import com.y.citycapsule.core.route.PlannedWalkingRoute
import com.y.citycapsule.core.route.PlannedRouteSnapshot
import com.y.citycapsule.core.map.MapTrackDisplayPolicy
import com.y.citycapsule.core.route.RouteOrderOptimizer
import com.y.citycapsule.core.route.RoutePlanningRemoteDataSource
import com.y.citycapsule.core.route.WalkingLeg
import com.y.citycapsule.core.route.WalkingLegResult
import com.y.citycapsule.core.storage.StorageResult
import com.y.citycapsule.core.favorite.FavoriteRepository
import com.y.citycapsule.core.city.ExploreCityRepository
import com.y.citycapsule.core.roaming.RoamingSession
import com.y.citycapsule.core.roaming.RoamingSessionRepository
import com.y.citycapsule.core.roaming.RoamingStatus
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

enum class LocalRouteMode { LIST, EDITOR }
enum class RoutePlanningStatus { IDLE, LOADING_MANUAL, LOADING_RECOMMENDED, READY, ERROR }
data class LocalRouteUiState(
    val loading: Boolean = true, val routes: List<LocalRoute> = emptyList(), val places: List<Place> = emptyList(),
    val editingId: String? = null, val name: String = "", val orderedPlaceIds: List<String> = emptyList(),
    val favoriteIds: Set<String> = emptySet(), val activeSession: RoamingSession? = null,
    val currentCityName: String? = null,
    val planningStatus: RoutePlanningStatus = RoutePlanningStatus.IDLE,
    val plannedRoute: PlannedWalkingRoute? = null, val recommendedPlaceIds: List<String>? = null,
    val saving: Boolean = false, val message: String? = null
) { val canSave get() = name.isNotBlank() && orderedPlaceIds.isNotEmpty() && !saving }

sealed interface LocalRouteIntent {
    data object Load : LocalRouteIntent; data object Back : LocalRouteIntent; data object Create : LocalRouteIntent
    data class Open(val id: String) : LocalRouteIntent; data class NameChanged(val value: String) : LocalRouteIntent
    data class AddPlace(val id: String) : LocalRouteIntent; data class RemovePlace(val id: String) : LocalRouteIntent
    data class Reorder(val fromIndex: Int, val toIndex: Int) : LocalRouteIntent
    data object PlanManualOrder : LocalRouteIntent; data object RecommendOrder : LocalRouteIntent
    data object ApplyRecommendedOrder : LocalRouteIntent
    data class PlanningFinished(val route: PlannedWalkingRoute?, val recommended: List<String>?, val error: String?) : LocalRouteIntent
    data object Save : LocalRouteIntent; data object Delete : LocalRouteIntent
    data class StartRoaming(val routeId: String?) : LocalRouteIntent
}
sealed interface LocalRouteEffect { data object Back : LocalRouteEffect; data class Editor(val id: String?) : LocalRouteEffect; data class Roaming(val routeId: String?) : LocalRouteEffect; data object Changed : LocalRouteEffect }

class LocalRouteStore(
    private val routes: LocalRouteRepository, private val places: PlaceRepository, private val mode: LocalRouteMode,
    private val routeId: String?, parentScope: CoroutineScope, private val favorites: FavoriteRepository? = null,
    private val sessions: RoamingSessionRepository? = null, private val routePlanning: RoutePlanningRemoteDataSource? = null,
    private val cityRepository: ExploreCityRepository? = null
) : MviStore<LocalRouteIntent, LocalRouteUiState, LocalRouteEffect> {
    private val job = SupervisorJob(parentScope.coroutineContext[Job]); private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val intents = Channel<LocalRouteIntent>(Channel.UNLIMITED); private val effectChannel = Channel<LocalRouteEffect>(Channel.UNLIMITED)
    private val mutable = MutableStateFlow(LocalRouteUiState(editingId = routeId)); private var disposed = false
    override val state: StateFlow<LocalRouteUiState> = mutable.asStateFlow(); override val effects: Flow<LocalRouteEffect> = effectChannel.receiveAsFlow()
    init { scope.launch { for (intent in intents) handle(intent) } }
    override fun dispatch(intent: LocalRouteIntent) { if (!disposed) intents.trySend(intent) }
    override fun dispose() { if (!disposed) { disposed = true; intents.close(); effectChannel.close(); scope.cancel() } }
    private suspend fun handle(intent: LocalRouteIntent) { when (intent) {
        LocalRouteIntent.Load -> { loadCity(); load() }; LocalRouteIntent.Back -> effectChannel.send(LocalRouteEffect.Back)
        LocalRouteIntent.Create -> effectChannel.send(LocalRouteEffect.Editor(null)); is LocalRouteIntent.Open -> effectChannel.send(LocalRouteEffect.Editor(intent.id))
        is LocalRouteIntent.NameChanged -> mutable.value = mutable.value.copy(name = intent.value.take(40), message = null)
        is LocalRouteIntent.AddPlace -> if (intent.id !in mutable.value.orderedPlaceIds && mutable.value.orderedPlaceIds.size < 20 && mutable.value.places.firstOrNull { it.id == intent.id }?.let(::isCurrentCityPlace) == true) updateOrder(mutable.value.orderedPlaceIds + intent.id)
        is LocalRouteIntent.RemovePlace -> updateOrder(mutable.value.orderedPlaceIds - intent.id)
        is LocalRouteIntent.Reorder -> reorder(intent.fromIndex, intent.toIndex)
        LocalRouteIntent.PlanManualOrder -> planManualOrder()
        LocalRouteIntent.RecommendOrder -> recommendOrder()
        LocalRouteIntent.ApplyRecommendedOrder -> mutable.value.recommendedPlaceIds?.let { mutable.value = mutable.value.copy(orderedPlaceIds = it, recommendedPlaceIds = null, message = "已采用真实道路距离推荐顺序，保存后生效。") }
        is LocalRouteIntent.PlanningFinished -> mutable.value = mutable.value.copy(planningStatus = if (intent.route == null) RoutePlanningStatus.ERROR else RoutePlanningStatus.READY, plannedRoute = intent.route, recommendedPlaceIds = intent.recommended, message = intent.error)
        LocalRouteIntent.Save -> save(); LocalRouteIntent.Delete -> delete()
        is LocalRouteIntent.StartRoaming -> startRoaming(intent.routeId)
    } }
    private fun loadCity() {
        cityRepository?.get { result ->
            val name = (result as? StorageResult.Success)?.value?.selectedCity?.displayName
            scope.launch { mutable.value = mutable.value.copy(currentCityName = name) }
        }
    }
    private fun isCurrentCityPlace(place: Place): Boolean {
        val current = mutable.value.currentCityName ?: return false
        return normalizeCity(place.city) == normalizeCity(current)
    }
    private fun load() { mutable.value = mutable.value.copy(loading = true, message = null); routes.getCatalog { routeResult ->
        places.getCatalog { placeResult ->
            val finish: (Set<String>, RoamingSession?) -> Unit = { favoriteIds, activeSession -> scope.launch {
            if (routeResult is StorageResult.Success && placeResult is StorageResult.Success) {
                val selected = routeResult.value.routes.firstOrNull { it.id == routeId }
                val sortedPlaces = placeResult.value.places.sortedWith(compareByDescending<Place> { it.id in favoriteIds }.thenBy(Place::name))
                mutable.value = mutable.value.copy(loading = false, routes = routeResult.value.routes.sortedByDescending { it.createdAtEpochMs }, places = sortedPlaces, favoriteIds = favoriteIds, activeSession = activeSession,
                    name = if (mode == LocalRouteMode.EDITOR) selected?.name.orEmpty() else "", orderedPlaceIds = if (mode == LocalRouteMode.EDITOR) selected?.orderedPlaceIds.orEmpty() else emptyList(),
                    message = if (mode == LocalRouteMode.EDITOR && routeId != null && selected == null) "路线不存在或已被删除。" else null)
            } else mutable.value = mutable.value.copy(loading = false, message = "本地路线读取失败，请重试。")
        } }
            val loadSession: (Set<String>) -> Unit = { favoriteIds ->
                val sessionRepository = sessions
                if (sessionRepository == null) finish(favoriteIds, null) else sessionRepository.get { result ->
                    finish(favoriteIds, (result as? StorageResult.Success)?.value?.takeIf { it.status != RoamingStatus.ENDED })
                }
            }
            val repository = favorites
            if (repository == null) loadSession(emptySet()) else repository.getFavoriteIds { result ->
                loadSession((result as? StorageResult.Success)?.value?.placeIds.orEmpty())
            }
        }
    } }
    private fun reorder(fromIndex: Int, toIndex: Int) {
        val reordered = reorderPlaceIds(mutable.value.orderedPlaceIds, fromIndex, toIndex)
        if (reordered != mutable.value.orderedPlaceIds) {
            updateOrder(reordered)
        }
    }
    private fun updateOrder(ids: List<String>) {
        if (ids != mutable.value.orderedPlaceIds) mutable.value = mutable.value.copy(orderedPlaceIds = ids, planningStatus = RoutePlanningStatus.IDLE, plannedRoute = null, recommendedPlaceIds = null, message = null)
    }
    private fun planManualOrder() {
        val source = routePlanning ?: return planningFailure("当前版本没有可用的道路规划服务。")
        val selected = selectedPlaces() ?: return
        mutable.value = mutable.value.copy(planningStatus = RoutePlanningStatus.LOADING_MANUAL, message = null, plannedRoute = null, recommendedPlaceIds = null)
        requestLegsInOrder(source, selected) { route, error -> dispatch(LocalRouteIntent.PlanningFinished(route, null, error)) }
    }
    private fun recommendOrder() {
        val source = routePlanning ?: return planningFailure("当前版本没有可用的道路规划服务。")
        val selected = selectedPlaces() ?: return
        if (selected.size > RouteOrderOptimizer.MAX_OPTIMIZED_PLACES) return planningFailure("道路顺序推荐最多支持 ${RouteOrderOptimizer.MAX_OPTIMIZED_PLACES} 个地点；更多地点请手动排序。")
        mutable.value = mutable.value.copy(planningStatus = RoutePlanningStatus.LOADING_RECOMMENDED, message = null, plannedRoute = null, recommendedPlaceIds = null)
        val pairs = buildList { for (from in 0 until selected.lastIndex) for (to in from + 1 until selected.size) add(selected[from] to selected[to]) }
        val legs = linkedMapOf<Pair<String, String>, WalkingLeg>()
        fun request(index: Int) {
            if (index >= pairs.size) {
                val recommended = RouteOrderOptimizer.recommend(selected.map(Place::id), legs.mapValues { it.value.distanceMeters })
                val route = recommended?.let { assembleRoute(it, legs) }
                dispatch(LocalRouteIntent.PlanningFinished(route, recommended, if (route == null) "无法根据道路距离生成推荐顺序。" else null)); return
            }
            val (from, to) = pairs[index]
            source.walkingLeg(from.id, from.geoPoint!!, to.id, to.geoPoint!!) { result -> when (result) {
                is WalkingLegResult.Success -> { legs[from.id to to.id] = result.leg; request(index + 1) }
                is WalkingLegResult.Failure -> dispatch(LocalRouteIntent.PlanningFinished(null, null, result.message))
                WalkingLegResult.Unavailable -> dispatch(LocalRouteIntent.PlanningFinished(null, null, "道路规划服务不可用，请检查网络与高德 Web Key。"))
            } }
        }
        request(0)
    }
    private fun selectedPlaces(): List<Place>? {
        val selected = mutable.value.orderedPlaceIds.mapNotNull { id -> mutable.value.places.firstOrNull { it.id == id } }
        if (selected.size < 2) { planningFailure("至少选择两个地点才能规划步行路线。"); return null }
        if (selected.any { it.geoPoint == null }) { planningFailure("路线中存在没有坐标的地点，请先补充坐标。"); return null }
        return selected
    }
    private fun planningFailure(message: String) { mutable.value = mutable.value.copy(planningStatus = RoutePlanningStatus.ERROR, plannedRoute = null, recommendedPlaceIds = null, message = message) }
    private fun save() = persistRoute { _ -> effectChannel.send(LocalRouteEffect.Back) }

    private fun startRoaming(routeId: String?) {
        val state = mutable.value
        if (mode == LocalRouteMode.LIST || state.activeSession != null ||
            (state.editingId != null && routeId != state.editingId)
        ) {
            scope.launch { effectChannel.send(LocalRouteEffect.Roaming(routeId)) }
            return
        }
        val existingPlan = state.routes.firstOrNull { it.id == state.editingId }?.plannedRoute
            ?.takeIf { it.orderedPlaceIds == state.orderedPlaceIds }
        if (state.plannedRoute != null || existingPlan != null) {
            persistRoute { saved -> effectChannel.send(LocalRouteEffect.Roaming(saved.id)) }
        } else {
            planAndStart()
        }
    }

    private fun planAndStart() {
        val source = routePlanning ?: return planningFailure("当前版本没有可用的道路规划服务。")
        val selected = selectedPlaces() ?: return
        mutable.value = mutable.value.copy(planningStatus = RoutePlanningStatus.LOADING_MANUAL, saving = true, message = "正在自动生成真实步行路线…")
        requestLegsInOrder(source, selected) { route, error -> scope.launch {
            if (route == null) {
                mutable.value = mutable.value.copy(
                    planningStatus = RoutePlanningStatus.ERROR,
                    saving = false,
                    message = (error ?: "自动路线规划失败。") + " 未取得真实道路，已停止出发；请检查网络或高德 Web Key 后重试。"
                )
            } else {
                mutable.value = mutable.value.copy(planningStatus = RoutePlanningStatus.READY, plannedRoute = route, saving = false, message = null)
                persistRoute { saved -> effectChannel.send(LocalRouteEffect.Roaming(saved.id)) }
            }
        } }
    }

    private fun persistRoute(onSuccess: suspend (LocalRoute) -> Unit) { val state = mutable.value; if (!state.canSave) return; mutable.value = state.copy(saving = true, message = null); val callback: (StorageResult<LocalRoute>) -> Unit = { result -> scope.launch { if (result is StorageResult.Success) { mutable.value = mutable.value.copy(saving = false); effectChannel.send(LocalRouteEffect.Changed); onSuccess(result.value) } else mutable.value = mutable.value.copy(saving = false, message = "路线保存失败，请检查地点与名称。") } }
        val existing = state.routes.firstOrNull { it.id == state.editingId }
        val freshPlan = state.plannedRoute?.takeIf { it.orderedPlaceIds == state.orderedPlaceIds }?.let {
            PlannedRouteSnapshot(it.orderedPlaceIds, it.distanceMeters, it.durationSeconds, MapTrackDisplayPolicy.sample(it.points))
        }
        val retainedPlan = existing?.plannedRoute?.takeIf { existing.orderedPlaceIds == state.orderedPlaceIds }
        val plan = freshPlan ?: retainedPlan
        if (existing == null) routes.create(LocalRouteDraft(state.name, state.orderedPlaceIds, plan), callback)
        else routes.update(existing.copy(name = state.name, orderedPlaceIds = state.orderedPlaceIds, plannedRoute = plan), callback)
    }
    private fun delete() { val id = mutable.value.editingId ?: return; mutable.value = mutable.value.copy(saving = true); routes.delete(id) { scope.launch { if (it is StorageResult.Success) { effectChannel.send(LocalRouteEffect.Changed); effectChannel.send(LocalRouteEffect.Back) } else mutable.value = mutable.value.copy(saving = false, message = "路线删除失败。") } } }
}

internal fun normalizeCity(value: String): String = value.trim().removeSuffix("市")

private fun requestLegsInOrder(source: RoutePlanningRemoteDataSource, places: List<Place>, callback: (PlannedWalkingRoute?, String?) -> Unit) {
    val legs = mutableListOf<WalkingLeg>()
    fun request(index: Int) {
        if (index >= places.lastIndex) { callback(PlannedWalkingRoute(places.map(Place::id), legs), null); return }
        val from = places[index]; val to = places[index + 1]
        source.walkingLeg(from.id, from.geoPoint!!, to.id, to.geoPoint!!) { result -> when (result) {
            is WalkingLegResult.Success -> { legs += result.leg; request(index + 1) }
            is WalkingLegResult.Failure -> callback(null, result.message)
            WalkingLegResult.Unavailable -> callback(null, "道路规划服务不可用，请检查网络与高德 Web Key。")
        } }
    }
    request(0)
}

private fun assembleRoute(order: List<String>, values: Map<Pair<String, String>, WalkingLeg>): PlannedWalkingRoute? {
    val legs = order.zipWithNext().map { (from, to) -> values[from to to] ?: values[to to from]?.let { it.copy(fromPlaceId = from, toPlaceId = to, points = it.points.asReversed()) } ?: return null }
    return PlannedWalkingRoute(order, legs)
}

internal fun reorderPlaceIds(ids: List<String>, fromIndex: Int, toIndex: Int): List<String> {
    if (fromIndex !in ids.indices || toIndex !in ids.indices || fromIndex == toIndex) return ids
    return ids.toMutableList().apply {
        val moved = removeAt(fromIndex)
        add(toIndex, moved)
    }
}

object LocalRouteFeatureRuntime {
    var revision: Long by mutableStateOf(0L)
        private set
    fun invalidate() { revision += 1L }
}
