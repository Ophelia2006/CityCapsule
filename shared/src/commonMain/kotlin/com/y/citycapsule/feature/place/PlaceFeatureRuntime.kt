package com.y.citycapsule.feature.place

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-local invalidation only. Persistent truth remains behind the typed repositories.
 *
 * It lets a covered list or detail page reload after an editor/detail page mutates data.
 */
internal object PlaceFeatureRuntime {
    var revision: Long by mutableStateOf(0L)
        private set

    fun invalidate() {
        revision += 1L
    }
}
