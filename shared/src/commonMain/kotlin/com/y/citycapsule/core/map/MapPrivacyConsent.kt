package com.y.citycapsule.core.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageResult

object MapPrivacyConsentRuntime {
    var accepted: Boolean by mutableStateOf(false)
        private set

    fun update(value: Boolean) {
        accepted = value
    }
}

class MapPrivacyConsentRepository(private val storage: KeyValueStore) {
    fun load(callback: (Boolean) -> Unit) {
        storage.get(AppStorageKeys.Settings.MAP_PRIVACY_ACCEPTED) { result ->
            val accepted = (result as? StorageResult.Success)?.value == true
            MapPrivacyConsentRuntime.update(accepted)
            callback(accepted)
        }
    }

    fun accept(callback: StorageCallback<Unit> = {}) {
        storage.put(AppStorageKeys.Settings.MAP_PRIVACY_ACCEPTED, true) { result ->
            if (result is StorageResult.Success) MapPrivacyConsentRuntime.update(true)
            callback(result)
        }
    }
}
