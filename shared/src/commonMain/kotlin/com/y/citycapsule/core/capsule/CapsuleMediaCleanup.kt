package com.y.citycapsule.core.capsule

import com.y.citycapsule.core.media.ManagedMediaDeleteResult
import com.y.citycapsule.core.media.ManagedMediaFileCapability
import com.y.citycapsule.core.storage.StorageResult

sealed interface CapsuleMediaCleanupResult {
    data class Success(val deletedPaths: List<String>) : CapsuleMediaCleanupResult
    data class Deferred(val message: String) : CapsuleMediaCleanupResult
}

fun interface CapsuleMediaCleanup {
    fun cleanupCandidates(
        candidatePaths: Collection<String>,
        callback: (CapsuleMediaCleanupResult) -> Unit
    )

    companion object {
        val NO_OP = CapsuleMediaCleanup { _, callback ->
            callback(CapsuleMediaCleanupResult.Success(emptyList()))
        }
    }
}

/**
 * Deletes only app-managed files that are no longer referenced by either the published catalog
 * or the recoverable draft. If either reference source cannot be read, cleanup is deferred so a
 * storage failure can never turn into accidental media loss.
 */
class RepositoryCapsuleMediaCleanup(
    private val capsuleRepository: CapsuleRepository,
    private val mediaFiles: ManagedMediaFileCapability
) : CapsuleMediaCleanup {
    override fun cleanupCandidates(
        candidatePaths: Collection<String>,
        callback: (CapsuleMediaCleanupResult) -> Unit
    ) {
        val candidates = candidatePaths
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
        if (candidates.isEmpty()) {
            callback(CapsuleMediaCleanupResult.Success(emptyList()))
            return
        }
        capsuleRepository.getPublished { publishedResult ->
            if (publishedResult !is StorageResult.Success) {
                callback(CapsuleMediaCleanupResult.Deferred(READ_FAILURE_MESSAGE))
                return@getPublished
            }
            capsuleRepository.getDraft { draftResult ->
                if (draftResult !is StorageResult.Success) {
                    callback(CapsuleMediaCleanupResult.Deferred(READ_FAILURE_MESSAGE))
                    return@getDraft
                }
                val referenced = buildSet {
                    publishedResult.value.forEach { addAll(it.imagePaths) }
                    addAll(draftResult.value.imagePaths)
                }
                val unreferenced = candidates.filterNot(referenced::contains)
                if (unreferenced.isEmpty()) {
                    callback(CapsuleMediaCleanupResult.Success(emptyList()))
                    return@getDraft
                }
                mediaFiles.deleteManagedImages(unreferenced) { result ->
                    callback(
                        when (result) {
                            is ManagedMediaDeleteResult.Success ->
                                CapsuleMediaCleanupResult.Success(result.deletedPaths)
                            is ManagedMediaDeleteResult.Failure ->
                                CapsuleMediaCleanupResult.Deferred(result.message)
                            ManagedMediaDeleteResult.Unsupported ->
                                CapsuleMediaCleanupResult.Deferred("当前平台暂不支持照片文件清理。")
                        }
                    )
                }
            }
        }
    }

    private companion object {
        const val READ_FAILURE_MESSAGE = "无法确认照片是否仍被引用，本次未删除文件。"
    }
}
