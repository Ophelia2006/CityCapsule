package com.y.citycapsule.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Minimal cross-platform contract for one feature-owned, lifecycle-bound store. */
interface MviStore<Intent, State, Effect> {
    val state: StateFlow<State>
    val effects: Flow<Effect>

    fun dispatch(intent: Intent)

    fun dispose()
}
