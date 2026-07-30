package com.y.citycapsule.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object ProfileFeatureRuntime {
    var revision: Long by mutableStateOf(0L)
        private set

    fun invalidate() {
        revision += 1L
    }
}
