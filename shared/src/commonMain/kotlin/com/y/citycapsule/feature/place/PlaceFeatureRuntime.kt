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
    private var nextOwnerToken = 0L
    private var lastInvalidationOwner: Long? = null

    fun invalidate() {
        invalidateFrom(ownerToken = null)
    }

    fun newOwnerToken(): Long = ++nextOwnerToken

    fun invalidateFrom(ownerToken: Long?) {
        lastInvalidationOwner = ownerToken
        revision += 1L
    }

    fun shouldReload(ownerToken: Long): Boolean = lastInvalidationOwner != ownerToken
}
