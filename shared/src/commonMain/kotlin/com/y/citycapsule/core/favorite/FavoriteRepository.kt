package com.y.citycapsule.core.favorite

import com.y.citycapsule.core.storage.StorageCallback

/**
 * Favorites are persisted independently from Place so a toggle never rewrites the catalog.
 * Stale ids are tolerated by the codec and pruned against the catalog by the T108-T112
 * repository implementation.
 */
interface FavoriteRepository {
    fun getFavoriteIds(callback: StorageCallback<FavoritePlaceIds>)

    fun isFavorite(placeId: String, callback: StorageCallback<Boolean>)

    fun setFavorite(
        placeId: String,
        favorite: Boolean,
        callback: StorageCallback<Boolean>
    )

    fun toggleFavorite(placeId: String, callback: StorageCallback<Boolean>)
}
