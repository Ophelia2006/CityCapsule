package com.y.citycapsule.core.favorite

import com.y.citycapsule.core.place.PlaceRepository
import com.y.citycapsule.core.place.PlaceValidator
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.KeyValueStore
import com.y.citycapsule.core.storage.StorageCallback
import com.y.citycapsule.core.storage.StorageError
import com.y.citycapsule.core.storage.StorageErrorCode
import com.y.citycapsule.core.storage.StorageResult

class LocalFavoriteRepository(
    private val storage: KeyValueStore,
    private val placeRepository: PlaceRepository
) : FavoriteRepository {
    private val mutationQueue = mutableListOf<(() -> Unit) -> Unit>()
    private var mutationInFlight = false

    override fun getFavoriteIds(callback: StorageCallback<FavoritePlaceIds>) {
        getRawFavorites { favoriteResult ->
            when (favoriteResult) {
                is StorageResult.Success -> placeRepository.getCatalog { catalogResult ->
                    when (catalogResult) {
                        is StorageResult.Success -> {
                            val validIds = catalogResult.value.places.mapTo(
                                mutableSetOf()
                            ) { it.id }
                            val pruned = favoriteResult.value.copy(
                                placeIds = favoriteResult.value.placeIds.intersect(validIds)
                            )
                            if (pruned == favoriteResult.value) {
                                callback(favoriteResult)
                            } else {
                                storage.put(AppStorageKeys.Favorites.PLACE_IDS, pruned) {
                                    // Stale ids are hidden even when best-effort cleanup cannot persist.
                                    callback(StorageResult.Success(pruned))
                                }
                            }
                        }
                        StorageResult.Missing -> callback(StorageResult.Success(FavoritePlaceIds.EMPTY))
                        is StorageResult.Failure -> callback(catalogResult)
                    }
                }
                StorageResult.Missing -> callback(StorageResult.Success(FavoritePlaceIds.EMPTY))
                is StorageResult.Failure -> callback(favoriteResult)
            }
        }
    }

    override fun isFavorite(placeId: String, callback: StorageCallback<Boolean>) {
        val id = placeId.trim()
        if (!PlaceValidator.isValidId(id)) {
            callback(invalidFavoriteRequest())
            return
        }
        getFavoriteIds { result ->
            callback(
                when (result) {
                    is StorageResult.Success -> StorageResult.Success(id in result.value.placeIds)
                    StorageResult.Missing -> StorageResult.Success(false)
                    is StorageResult.Failure -> result
                }
            )
        }
    }

    override fun setFavorite(
        placeId: String,
        favorite: Boolean,
        callback: StorageCallback<Boolean>
    ) {
        val id = placeId.trim()
        if (!PlaceValidator.isValidId(id)) {
            callback(invalidFavoriteRequest())
            return
        }
        enqueueMutation { complete ->
            if (favorite) {
                placeRepository.getPlace(id) { placeResult ->
                    when (placeResult) {
                        is StorageResult.Success -> mutate(id, true, callback, complete)
                        StorageResult.Missing -> deliver(callback, StorageResult.Missing, complete)
                        is StorageResult.Failure -> deliver(callback, placeResult, complete)
                    }
                }
            } else {
                mutate(id, false, callback, complete)
            }
        }
    }

    override fun toggleFavorite(placeId: String, callback: StorageCallback<Boolean>) {
        val id = placeId.trim()
        if (!PlaceValidator.isValidId(id)) {
            callback(invalidFavoriteRequest())
            return
        }
        enqueueMutation { complete ->
            getRawFavorites { result ->
                when (result) {
                    is StorageResult.Success -> {
                        val target = id !in result.value.placeIds
                        if (target) {
                            placeRepository.getPlace(id) { placeResult ->
                                when (placeResult) {
                                    is StorageResult.Success -> writeMutation(
                                        result.value,
                                        id,
                                        true,
                                        callback,
                                        complete
                                    )
                                    StorageResult.Missing -> deliver(
                                        callback,
                                        StorageResult.Missing,
                                        complete
                                    )
                                    is StorageResult.Failure -> deliver(
                                        callback,
                                        placeResult,
                                        complete
                                    )
                                }
                            }
                        } else {
                            writeMutation(result.value, id, false, callback, complete)
                        }
                    }
                    StorageResult.Missing -> placeRepository.getPlace(id) { placeResult ->
                        when (placeResult) {
                            is StorageResult.Success -> writeMutation(
                                FavoritePlaceIds.EMPTY,
                                id,
                                true,
                                callback,
                                complete
                            )
                            StorageResult.Missing -> deliver(
                                callback,
                                StorageResult.Missing,
                                complete
                            )
                            is StorageResult.Failure -> deliver(
                                callback,
                                placeResult,
                                complete
                            )
                        }
                    }
                    is StorageResult.Failure -> deliver(callback, result, complete)
                }
            }
        }
    }

    private fun mutate(
        placeId: String,
        favorite: Boolean,
        callback: StorageCallback<Boolean>,
        complete: () -> Unit
    ) {
        getRawFavorites { result ->
            when (result) {
                is StorageResult.Success -> writeMutation(
                    result.value,
                    placeId,
                    favorite,
                    callback,
                    complete
                )
                StorageResult.Missing -> writeMutation(
                    FavoritePlaceIds.EMPTY,
                    placeId,
                    favorite,
                    callback,
                    complete
                )
                is StorageResult.Failure -> deliver(callback, result, complete)
            }
        }
    }

    private fun writeMutation(
        current: FavoritePlaceIds,
        placeId: String,
        favorite: Boolean,
        callback: StorageCallback<Boolean>,
        complete: () -> Unit
    ) {
        val ids = if (favorite) {
            current.placeIds + placeId
        } else {
            current.placeIds - placeId
        }
        if (ids == current.placeIds) {
            deliver(callback, StorageResult.Success(favorite), complete)
            return
        }
        storage.put(
            AppStorageKeys.Favorites.PLACE_IDS,
            current.copy(placeIds = ids)
        ) { result ->
            deliver(
                callback,
                when (result) {
                    is StorageResult.Success -> StorageResult.Success(favorite)
                    StorageResult.Missing -> StorageResult.Failure(
                        StorageError(
                            StorageErrorCode.NATIVE_FAILURE,
                            "Favorite update was not confirmed."
                        )
                    )
                    is StorageResult.Failure -> result
                },
                complete
            )
        }
    }

    private fun getRawFavorites(callback: StorageCallback<FavoritePlaceIds>) {
        storage.get(AppStorageKeys.Favorites.PLACE_IDS, callback)
    }

    private fun enqueueMutation(operation: (() -> Unit) -> Unit) {
        mutationQueue += operation
        startNextMutation()
    }

    private fun startNextMutation() {
        if (mutationInFlight || mutationQueue.isEmpty()) {
            return
        }
        mutationInFlight = true
        val operation = mutationQueue.removeAt(0)
        operation {
            mutationInFlight = false
            startNextMutation()
        }
    }

    private fun <T> deliver(
        callback: StorageCallback<T>,
        result: StorageResult<T>,
        complete: () -> Unit
    ) {
        try {
            callback(result)
        } finally {
            complete()
        }
    }
}

private fun invalidFavoriteRequest(): StorageResult.Failure =
    StorageResult.Failure(
        StorageError(
            StorageErrorCode.INVALID_REQUEST,
            "Favorite place id is invalid."
        )
    )
