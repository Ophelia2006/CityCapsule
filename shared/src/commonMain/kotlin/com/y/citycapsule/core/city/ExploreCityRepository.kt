package com.y.citycapsule.core.city

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

interface ExploreCityRepository {
    fun get(callback: StorageCallback<ExploreCitySelection>)
    fun select(cityId: String, callback: StorageCallback<ExploreCitySelection>)
}

class LocalExploreCityRepository(private val storage: KeyValueStore) : ExploreCityRepository {
    override fun get(callback: StorageCallback<ExploreCitySelection>) {
        storage.get(AppStorageKeys.Explore.CITY_SELECTION) { result ->
            when (result) {
                is StorageResult.Success -> callback(result)
                StorageResult.Missing -> storage.put(
                    AppStorageKeys.Explore.CITY_SELECTION,
                    ExploreCitySelection.DEFAULT
                ) { write ->
                    callback(
                        if (write is StorageResult.Success) {
                            StorageResult.Success(ExploreCitySelection.DEFAULT)
                        } else {
                            failure("Explore city initialization failed.")
                        }
                    )
                }
                is StorageResult.Failure -> callback(result)
            }
        }
    }

    override fun select(cityId: String, callback: StorageCallback<ExploreCitySelection>) {
        val city = CityRegistry.byId(cityId)
        if (city == null) return callback(failure("Explore city is unknown."))
        get { result ->
            if (result !is StorageResult.Success) return@get callback(
                if (result is StorageResult.Failure) result else failure("Explore city is unavailable.")
            )
            val updated = result.value.copy(
                selectedCityId = city.id,
                recentCityIds = (listOf(city.id) + result.value.recentCityIds)
                    .distinct()
                    .take(ExploreCitySelection.MAX_RECENT_CITIES)
            )
            storage.put(AppStorageKeys.Explore.CITY_SELECTION, updated) { write ->
                callback(if (write is StorageResult.Success) StorageResult.Success(updated) else failure("Explore city write failed."))
            }
        }
    }

    private fun failure(message: String) = StorageResult.Failure(
        StorageError(StorageErrorCode.INVALID_REQUEST, message)
    )
}

object ExploreCityRuntime {
    var revision: Long by mutableStateOf(0L)
        private set

    fun invalidate() {
        revision += 1L
    }
}
