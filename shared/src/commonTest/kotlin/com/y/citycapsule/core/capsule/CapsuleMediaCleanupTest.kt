package com.y.citycapsule.core.capsule

import com.y.citycapsule.core.media.ManagedMediaDeleteResult
import com.y.citycapsule.core.media.ManagedMediaFileCapability
import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CapsuleMediaCleanupTest {
    @Test
    fun cleanupDeletesOnlyCandidatesNoLongerReferencedByCatalogOrDraft() {
        val storage = InMemoryKeyValueStore()
        var sequence = 0
        val repository = LocalCapsuleRepository(
            storage,
            idGenerator = CapsuleIdGenerator { "capsule_${sequence++}" }
        )
        repository.publishNow(
            CapsuleDraft(
                content = "已发布",
                placeId = "place_a",
                imagePaths = listOf(PUBLISHED)
            )
        )
        repository.saveDraftNow(
            CapsuleDraft(
                content = "草稿",
                placeId = "place_b",
                imagePaths = listOf(DRAFT)
            )
        )
        val deleted = mutableListOf<String>()
        val cleanup = RepositoryCapsuleMediaCleanup(
            repository,
            ManagedMediaFileCapability { paths, callback ->
                deleted += paths
                callback(ManagedMediaDeleteResult.Success(paths))
            }
        )
        var result: CapsuleMediaCleanupResult? = null

        cleanup.cleanupCandidates(listOf(PUBLISHED, DRAFT, ORPHAN)) { result = it }

        assertEquals(listOf(ORPHAN), deleted)
        assertEquals(
            listOf(ORPHAN),
            assertIs<CapsuleMediaCleanupResult.Success>(result).deletedPaths
        )
    }

    @Test
    fun cleanupDefersWithoutCallingNativeWhenReferencesCannotBeRead() {
        val storage = InMemoryKeyValueStore().apply {
            seedRaw(AppStorageKeys.Capsules.CATALOG, encodedValue = "{broken")
        }
        val repository = LocalCapsuleRepository(storage)
        var nativeCalled = false
        val cleanup = RepositoryCapsuleMediaCleanup(
            repository,
            ManagedMediaFileCapability { _, _ -> nativeCalled = true }
        )
        var result: CapsuleMediaCleanupResult? = null

        cleanup.cleanupCandidates(listOf(ORPHAN)) { result = it }

        assertIs<CapsuleMediaCleanupResult.Deferred>(result)
        assertTrue(!nativeCalled)
    }

    private companion object {
        const val PUBLISHED = "file:///published.jpg"
        const val DRAFT = "file:///draft.jpg"
        const val ORPHAN = "file:///orphan.jpg"
    }
}

private fun LocalCapsuleRepository.publishNow(draft: CapsuleDraft): StorageResult<CityCapsule> {
    var result: StorageResult<CityCapsule>? = null
    publish(draft) { result = it }
    return requireNotNull(result)
}

private fun LocalCapsuleRepository.saveDraftNow(draft: CapsuleDraft): StorageResult<Unit> {
    var result: StorageResult<Unit>? = null
    saveDraft(draft) { result = it }
    return requireNotNull(result)
}
