package com.y.citycapsule.feature.capsule

import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.CapsuleMood
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.media.PhotoPickerResult
import com.y.citycapsule.core.place.LocalPlaceRepository
import com.y.citycapsule.core.storage.InMemoryKeyValueStore
import com.y.citycapsule.core.storage.StorageResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CapsuleFeatureStateTest {
    @Test
    fun placeEditorTimelineAndDetailFormACompleteRecordFlow() {
        val fixture = fixture()
        val editor = CapsuleEditorStateHolder(
            capsuleId = null,
            placeId = PLACE_ID,
            capsuleRepository = fixture.capsules,
            placeRepository = fixture.places
        )

        editor.load()
        assertEquals(CapsuleUiStatus.READY, editor.state.status)
        editor.pickImages(PhotoPickerCapability { _, callback ->
            callback(PhotoPickerResult.Success(listOf("file:///memory.jpg")))
        })
        editor.updateContent("在博物馆门口遇见了晚霞。")
        editor.updateMood(CapsuleMood.SURPRISED)
        editor.updateTags("建筑，晚霞，建筑")

        var publishedId: String? = null
        editor.publish { publishedId = it.id }
        val capsuleId = assertNotNull(publishedId)

        val timeline = CapsuleTimelineStateHolder(fixture.capsules, fixture.places)
        timeline.load()
        assertEquals(CapsuleUiStatus.READY, timeline.state.status)
        assertEquals(listOf(capsuleId), timeline.state.items.map { it.capsule.id })
        assertEquals(listOf("file:///memory.jpg"), timeline.state.items.single().capsule.imagePaths)

        val detail = CapsuleDetailStateHolder(capsuleId, fixture.capsules, fixture.places)
        detail.load()
        assertEquals("在博物馆门口遇见了晚霞。", detail.state.capsule?.content)
        assertEquals("上海博物馆", detail.state.place?.name)

        var deleted = false
        detail.delete { deleted = true }
        assertTrue(deleted)
        timeline.load()
        assertTrue(timeline.state.items.isEmpty())
    }

    @Test
    fun newAndExistingEditorsOnlyResumeTheirOwnDrafts() {
        val fixture = fixture()
        fixture.capsules.saveDraft(
            CapsuleDraft(content = "另一个地点的草稿", placeId = "seed_west_lake")
        ) {}

        val freshEditor = CapsuleEditorStateHolder(null, PLACE_ID, fixture.capsules, fixture.places)
        freshEditor.load()
        assertEquals("", freshEditor.state.draft.content)

        freshEditor.updateContent("已发布内容")
        var capsuleId: String? = null
        freshEditor.publish { capsuleId = it.id }
        assertEquals("seed_west_lake", draft(fixture).placeId)

        val id = assertNotNull(capsuleId)
        fixture.capsules.saveDraft(
            CapsuleDraft(capsuleId = id, content = "尚未发布的修改", placeId = PLACE_ID)
        ) {}
        val edit = CapsuleEditorStateHolder(id, PLACE_ID, fixture.capsules, fixture.places)
        edit.load()
        assertEquals("尚未发布的修改", edit.state.draft.content)

        val anotherFreshEditor = CapsuleEditorStateHolder(null, PLACE_ID, fixture.capsules, fixture.places)
        anotherFreshEditor.load()
        assertEquals("", anotherFreshEditor.state.draft.content)
    }

    @Test
    fun discardingLocalChangesDoesNotDeleteAnotherEditorsDraft() {
        val fixture = fixture()
        fixture.capsules.saveDraft(
            CapsuleDraft(content = "西湖草稿", placeId = "seed_west_lake")
        ) {}
        val editor = CapsuleEditorStateHolder(null, PLACE_ID, fixture.capsules, fixture.places)
        editor.load()
        editor.updateContent("临时修改")
        editor.requestClose {}
        assertTrue(editor.state.showDiscardConfirmation)

        var closed = false
        editor.discard { closed = true }

        assertTrue(closed)
        assertFalse(editor.state.showDiscardConfirmation)
        assertEquals("seed_west_lake", draft(fixture).placeId)
    }

    private fun fixture(): Fixture {
        val storage = InMemoryKeyValueStore()
        var sequence = 0
        return Fixture(
            capsules = LocalCapsuleRepository(
                storage = storage,
                idGenerator = CapsuleIdGenerator { "capsule_${sequence++}" }
            ),
            places = LocalPlaceRepository(storage)
        )
    }

    private fun draft(fixture: Fixture): CapsuleDraft {
        var result: StorageResult<CapsuleDraft>? = null
        fixture.capsules.getDraft { result = it }
        return (requireNotNull(result) as StorageResult.Success).value
    }

    private data class Fixture(
        val capsules: LocalCapsuleRepository,
        val places: LocalPlaceRepository
    )

    private companion object {
        const val PLACE_ID = "seed_shanghai_museum"
    }
}
