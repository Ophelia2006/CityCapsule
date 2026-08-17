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
import com.y.citycapsule.core.storage.StorageResult
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
data class LocalRouteUiState(
    val loading: Boolean = true, val routes: List<LocalRoute> = emptyList(), val places: List<Place> = emptyList(),
    val editingId: String? = null, val name: String = "", val orderedPlaceIds: List<String> = emptyList(),
    val saving: Boolean = false, val message: String? = null
) { val canSave get() = name.isNotBlank() && orderedPlaceIds.isNotEmpty() && !saving }

sealed interface LocalRouteIntent {
    data object Load : LocalRouteIntent; data object Back : LocalRouteIntent; data object Create : LocalRouteIntent
    data class Open(val id: String) : LocalRouteIntent; data class NameChanged(val value: String) : LocalRouteIntent
    data class AddPlace(val id: String) : LocalRouteIntent; data class RemovePlace(val id: String) : LocalRouteIntent
    data class Move(val id: String, val offset: Int) : LocalRouteIntent
    data object Save : LocalRouteIntent; data object Delete : LocalRouteIntent
    data class StartRoaming(val routeId: String?) : LocalRouteIntent
}
sealed interface LocalRouteEffect { data object Back : LocalRouteEffect; data class Editor(val id: String?) : LocalRouteEffect; data class Roaming(val routeId: String?) : LocalRouteEffect; data object Changed : LocalRouteEffect }

class LocalRouteStore(
    private val routes: LocalRouteRepository, private val places: PlaceRepository, private val mode: LocalRouteMode,
    private val routeId: String?, parentScope: CoroutineScope
) : MviStore<LocalRouteIntent, LocalRouteUiState, LocalRouteEffect> {
    private val job = SupervisorJob(parentScope.coroutineContext[Job]); private val scope = CoroutineScope(parentScope.coroutineContext + job)
    private val intents = Channel<LocalRouteIntent>(Channel.UNLIMITED); private val effectChannel = Channel<LocalRouteEffect>(Channel.UNLIMITED)
    private val mutable = MutableStateFlow(LocalRouteUiState(editingId = routeId)); private var disposed = false
    override val state: StateFlow<LocalRouteUiState> = mutable.asStateFlow(); override val effects: Flow<LocalRouteEffect> = effectChannel.receiveAsFlow()
    init { scope.launch { for (intent in intents) handle(intent) } }
    override fun dispatch(intent: LocalRouteIntent) { if (!disposed) intents.trySend(intent) }
    override fun dispose() { if (!disposed) { disposed = true; intents.close(); effectChannel.close(); scope.cancel() } }
    private suspend fun handle(intent: LocalRouteIntent) { when (intent) {
        LocalRouteIntent.Load -> load(); LocalRouteIntent.Back -> effectChannel.send(LocalRouteEffect.Back)
        LocalRouteIntent.Create -> effectChannel.send(LocalRouteEffect.Editor(null)); is LocalRouteIntent.Open -> effectChannel.send(LocalRouteEffect.Editor(intent.id))
        is LocalRouteIntent.NameChanged -> mutable.value = mutable.value.copy(name = intent.value.take(40), message = null)
        is LocalRouteIntent.AddPlace -> if (intent.id !in mutable.value.orderedPlaceIds && mutable.value.orderedPlaceIds.size < 20) mutable.value = mutable.value.copy(orderedPlaceIds = mutable.value.orderedPlaceIds + intent.id)
        is LocalRouteIntent.RemovePlace -> mutable.value = mutable.value.copy(orderedPlaceIds = mutable.value.orderedPlaceIds - intent.id)
        is LocalRouteIntent.Move -> move(intent.id, intent.offset)
        LocalRouteIntent.Save -> save(); LocalRouteIntent.Delete -> delete()
        is LocalRouteIntent.StartRoaming -> effectChannel.send(LocalRouteEffect.Roaming(intent.routeId))
    } }
    private fun load() { mutable.value = mutable.value.copy(loading = true, message = null); routes.getCatalog { routeResult ->
        places.getCatalog { placeResult -> scope.launch {
            if (routeResult is StorageResult.Success && placeResult is StorageResult.Success) {
                val selected = routeResult.value.routes.firstOrNull { it.id == routeId }
                mutable.value = mutable.value.copy(loading = false, routes = routeResult.value.routes.sortedByDescending { it.createdAtEpochMs }, places = placeResult.value.places,
                    name = if (mode == LocalRouteMode.EDITOR) selected?.name.orEmpty() else "", orderedPlaceIds = if (mode == LocalRouteMode.EDITOR) selected?.orderedPlaceIds.orEmpty() else emptyList(),
                    message = if (mode == LocalRouteMode.EDITOR && routeId != null && selected == null) "路线不存在或已被删除。" else null)
            } else mutable.value = mutable.value.copy(loading = false, message = "本地路线读取失败，请重试。")
        } }
    } }
    private fun move(id: String, offset: Int) { val list = mutable.value.orderedPlaceIds.toMutableList(); val from = list.indexOf(id); val to = from + offset; if (from >= 0 && to in list.indices) { val item = list.removeAt(from); list.add(to, item); mutable.value = mutable.value.copy(orderedPlaceIds = list) } }
    private fun save() { val state = mutable.value; if (!state.canSave) return; mutable.value = state.copy(saving = true, message = null); val callback: (StorageResult<LocalRoute>) -> Unit = { result -> scope.launch { if (result is StorageResult.Success) { effectChannel.send(LocalRouteEffect.Changed); effectChannel.send(LocalRouteEffect.Back) } else mutable.value = mutable.value.copy(saving = false, message = "路线保存失败，请检查地点与名称。") } }
        val existing = state.routes.firstOrNull { it.id == state.editingId }; if (existing == null) routes.create(LocalRouteDraft(state.name, state.orderedPlaceIds), callback) else routes.update(existing.copy(name = state.name, orderedPlaceIds = state.orderedPlaceIds), callback)
    }
    private fun delete() { val id = mutable.value.editingId ?: return; mutable.value = mutable.value.copy(saving = true); routes.delete(id) { scope.launch { if (it is StorageResult.Success) { effectChannel.send(LocalRouteEffect.Changed); effectChannel.send(LocalRouteEffect.Back) } else mutable.value = mutable.value.copy(saving = false, message = "路线删除失败。") } } }
}

object LocalRouteFeatureRuntime {
    var revision: Long by mutableStateOf(0L)
        private set
    fun invalidate() { revision += 1L }
}
