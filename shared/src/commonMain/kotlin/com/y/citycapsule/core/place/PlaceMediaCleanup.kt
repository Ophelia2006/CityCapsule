package com.y.citycapsule.core.place

import com.y.citycapsule.core.capsule.CapsuleRepository
import com.y.citycapsule.core.media.ManagedMediaDeleteResult
import com.y.citycapsule.core.media.ManagedMediaFileCapability
import com.y.citycapsule.core.storage.StorageResult

fun interface PlaceMediaCleanup {
    fun cleanupCandidates(paths: Collection<String>, callback: (Boolean) -> Unit)

    companion object {
        val NO_OP = PlaceMediaCleanup { _, callback -> callback(true) }
    }
}

/** Deletes a managed cover only after all place and capsule references can be read safely. */
class RepositoryPlaceMediaCleanup(
    private val places: PlaceRepository,
    private val capsules: CapsuleRepository,
    private val media: ManagedMediaFileCapability
) : PlaceMediaCleanup {
    override fun cleanupCandidates(paths: Collection<String>, callback: (Boolean) -> Unit) {
        val candidates = paths.map(String::trim).filter(String::isNotEmpty).distinct()
        if (candidates.isEmpty()) return callback(true)
        places.getCatalog { placeResult ->
            if (placeResult !is StorageResult.Success) return@getCatalog callback(false)
            capsules.getPublished { published ->
                if (published !is StorageResult.Success) return@getPublished callback(false)
                capsules.getDraft { draft ->
                    if (draft !is StorageResult.Success) return@getDraft callback(false)
                    val referenced = buildSet {
                        placeResult.value.places.forEach { place ->
                            place.visualRef?.takeIf { it.type == PlaceVisualType.MANAGED_FILE }?.value?.let(::add)
                        }
                        published.value.forEach { addAll(it.imagePaths) }
                        addAll(draft.value.imagePaths)
                    }
                    val unreferenced = candidates.filterNot(referenced::contains)
                    if (unreferenced.isEmpty()) return@getDraft callback(true)
                    media.deleteManagedImages(unreferenced) { result ->
                        callback(result is ManagedMediaDeleteResult.Success)
                    }
                }
            }
        }
    }
}
