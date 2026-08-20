package com.y.citycapsule.core.capsule

import com.y.citycapsule.core.storage.AppStorageKeys
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CapsuleCodecRepositoryTest {
    @Test
    fun catalogRoundTripPreservesImagesMoodTagsAndTimestamps() {
        val value = CapsuleCatalog(
            capsules = listOf(capsule(id = "capsule_a", images = listOf("file:///a.jpg")).copy(roamingSessionId = "1000"))
        )

        assertEquals(value, CapsuleCatalogCodec.decode(CapsuleCatalogCodec.encode(value)))
    }

    @Test
    fun publishPreservesExplicitRoamingSessionAssociation() {
        val repository = LocalCapsuleRepository(
            InMemoryKeyValueStore(),
            MutableCapsuleClock(1_000L),
            CapsuleIdGenerator { "capsule_roam" }
        )

        val created = repository.publishNow(
            CapsuleDraft(content = "漫游中的一刻", placeId = "place_a", roamingSessionId = "900")
        ).successValue()

        assertEquals("900", created.roamingSessionId)
    }

    @Test
    fun malformedAndOversizedPayloadsAreRejected() {
        assertNull(CapsuleCatalogCodec.decode("{broken"))
        assertNull(
            CapsuleDraftCodec.decode(
                """{"schemaVersion":1,"content":"ok","tags":[],"placeId":"p","imagePaths":[""],"updatedAtEpochMs":"1"}"""
            )
        )
    }

    @Test
    fun publishUpdateFilterDeleteAndDraftLifecycleArePersistent() {
        val storage = InMemoryKeyValueStore()
        val clock = MutableCapsuleClock(1_000L)
        var sequence = 0
        val repository = LocalCapsuleRepository(
            storage = storage,
            clock = clock,
            idGenerator = CapsuleIdGenerator { "capsule_${sequence++}" }
        )
        val draft = CapsuleDraft(
            content = "  街角的风很轻。 ",
            mood = CapsuleMood.CALM,
            tags = listOf("散步", " 散步 "),
            placeId = "place_a",
            imagePaths = listOf("file:///one.jpg")
        )

        assertIs<StorageResult.Success<Unit>>(repository.saveDraftNow(draft))
        val created = repository.publishNow(draft).successValue()
        assertEquals("街角的风很轻。", created.content)
        assertEquals(listOf("散步"), created.tags)
        assertEquals(listOf("file:///one.jpg"), created.imagePaths)
        assertEquals(CapsuleDraft.EMPTY, repository.getDraftNow().successValue())

        clock.value = 2_000L
        val updated = repository.publishNow(
            draft.copy(capsuleId = created.id, content = "后来下起了小雨。")
        ).successValue()
        assertEquals(created.createdAtEpochMs, updated.createdAtEpochMs)
        assertEquals(2_000L, updated.updatedAtEpochMs)
        assertEquals(1, repository.getForPlaceNow("place_a").successValue().size)

        assertIs<StorageResult.Success<Unit>>(repository.deleteNow(created.id))
        assertTrue(repository.getPublishedNow().successValue().isEmpty())
        assertIs<StorageResult.Missing>(repository.getByIdNow(created.id))
        assertEquals(
            emptyList(),
            storage.getNow(AppStorageKeys.Capsules.CATALOG).successValue().capsules
        )
    }

    @Test
    fun publishedItemsAreNewestFirst() {
        val storage = InMemoryKeyValueStore()
        val clock = MutableCapsuleClock(1_000L)
        var sequence = 0
        val repository = LocalCapsuleRepository(
            storage,
            clock,
            CapsuleIdGenerator { "capsule_${sequence++}" }
        )
        val first = repository.publishNow(CapsuleDraft(content = "第一条", placeId = "p")).successValue()
        clock.value = 2_000L
        val second = repository.publishNow(CapsuleDraft(content = "第二条", placeId = "p")).successValue()

        assertEquals(listOf(second.id, first.id), repository.getPublishedNow().successValue().map { it.id })
    }

    @Test
    fun updatingMissingCapsuleDoesNotCreateANewRecord() {
        val repository = LocalCapsuleRepository(
            InMemoryKeyValueStore(),
            MutableCapsuleClock(1_000L),
            CapsuleIdGenerator { "generated" }
        )

        val result = repository.publishNow(
            CapsuleDraft(
                capsuleId = "missing",
                content = "不应悄悄变成新记录",
                placeId = "place_a"
            )
        )

        assertIs<StorageResult.Missing>(result)
        assertTrue(repository.getPublishedNow().successValue().isEmpty())
    }

    @Test
    fun publishingOnlyClearsTheDraftForTheSameEditorContext() {
        val repository = LocalCapsuleRepository(
            InMemoryKeyValueStore(),
            MutableCapsuleClock(1_000L),
            CapsuleIdGenerator { "capsule_new" }
        )
        val unrelated = CapsuleDraft(content = "另一处未完成的记忆", placeId = "place_b")
        assertIs<StorageResult.Success<Unit>>(repository.saveDraftNow(unrelated))

        repository.publishNow(CapsuleDraft(content = "已经完成", placeId = "place_a")).successValue()

        assertEquals("place_b", repository.getDraftNow().successValue().placeId)
        assertEquals("另一处未完成的记忆", repository.getDraftNow().successValue().content)
    }

    @Test
    fun deletingACapsuleAlsoClearsItsOwnEditDraft() {
        val repository = LocalCapsuleRepository(
            InMemoryKeyValueStore(),
            MutableCapsuleClock(1_000L),
            CapsuleIdGenerator { "capsule_a" }
        )
        val capsule = repository.publishNow(
            CapsuleDraft(content = "准备删除", placeId = "place_a")
        ).successValue()
        repository.saveDraftNow(
            CapsuleDraft(
                capsuleId = capsule.id,
                content = "尚未发布的修改",
                placeId = capsule.placeId
            )
        )

        assertIs<StorageResult.Success<Unit>>(repository.deleteNow(capsule.id))

        assertEquals(CapsuleDraft.EMPTY, repository.getDraftNow().successValue())
    }

    private fun capsule(
        id: String,
        images: List<String> = emptyList()
    ) = CityCapsule(
        id = id,
        content = "一条城市记忆",
        mood = CapsuleMood.SURPRISED,
        tags = listOf("街道"),
        placeId = "place_a",
        imagePaths = images,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = 1_000L
    )
}

class CapsuleDateTest {
    @Test
    fun pagerLocaleModuleUsesTheSameNameAsTheNativeContract() {
        assertEquals(
            KuiklyLocalCapsuleDateFormatter.MODULE_NAME,
            KuiklyLocaleModule().moduleName()
        )
    }

    @Test
    fun epochDatesAreFormattedDeterministically() {
        assertEquals("1970 年 1 月 1 日", formatCapsuleDate(0L))
        assertEquals("2024 年 1 月 1 日", formatCapsuleDate(1_704_067_200_000L))
        assertEquals("日期未知", formatCapsuleDate(-1L))
    }

    @Test
    fun nativeLocalDateProtocolIsNormalizedForProductCopy() {
        var capturedEpochMs = -1L
        val formatter = KuiklyLocalCapsuleDateFormatter(
            LocalDateFormatterTransport { request ->
                capturedEpochMs = request.optLong(KuiklyLocalCapsuleDateFormatter.FIELD_EPOCH_MS)
                "2024-01-01"
            }
        )

        assertEquals("2024 年 1 月 1 日", formatter.format(1_704_063_600_000L))
        assertEquals(1_704_063_600_000L, capturedEpochMs)
    }

    @Test
    fun invalidNativeLocalDateNeverLeaksRawPlatformText() {
        val formatter = KuiklyLocalCapsuleDateFormatter(
            LocalDateFormatterTransport { "not-a-date" }
        )

        assertEquals("日期未知", formatter.format(0L))
    }
}

private class MutableCapsuleClock(var value: Long) : CapsuleClock {
    override fun nowEpochMs(): Long = value
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

private fun LocalCapsuleRepository.getDraftNow(): StorageResult<CapsuleDraft> {
    var result: StorageResult<CapsuleDraft>? = null
    getDraft { result = it }
    return requireNotNull(result)
}

private fun LocalCapsuleRepository.getPublishedNow(): StorageResult<List<CityCapsule>> {
    var result: StorageResult<List<CityCapsule>>? = null
    getPublished { result = it }
    return requireNotNull(result)
}

private fun LocalCapsuleRepository.getForPlaceNow(id: String): StorageResult<List<CityCapsule>> {
    var result: StorageResult<List<CityCapsule>>? = null
    getPublishedForPlace(id) { result = it }
    return requireNotNull(result)
}

private fun LocalCapsuleRepository.getByIdNow(id: String): StorageResult<CityCapsule> {
    var result: StorageResult<CityCapsule>? = null
    getById(id) { result = it }
    return requireNotNull(result)
}

private fun LocalCapsuleRepository.deleteNow(id: String): StorageResult<Unit> {
    var result: StorageResult<Unit>? = null
    delete(id) { result = it }
    return requireNotNull(result)
}

private fun <T> InMemoryKeyValueStore.getNow(key: com.y.citycapsule.core.storage.StorageKey<T>): StorageResult<T> {
    var result: StorageResult<T>? = null
    get(key) { result = it }
    return requireNotNull(result)
}

private fun <T> StorageResult<T>.successValue(): T = assertIs<StorageResult.Success<T>>(this).value
