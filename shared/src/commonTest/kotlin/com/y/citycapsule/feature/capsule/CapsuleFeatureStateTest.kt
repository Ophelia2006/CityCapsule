package com.y.citycapsule.feature.capsule

import com.y.citycapsule.core.capsule.CapsuleDraft
import com.y.citycapsule.core.capsule.CapsuleIdGenerator
import com.y.citycapsule.core.capsule.CapsuleMediaCleanup
import com.y.citycapsule.core.capsule.CapsuleMediaCleanupResult
import com.y.citycapsule.core.capsule.CapsuleMood
import com.y.citycapsule.core.capsule.CityCapsule
import com.y.citycapsule.core.capsule.LocalCapsuleRepository
import com.y.citycapsule.core.media.PhotoPickerCapability
import com.y.citycapsule.core.media.PhotoPickerResult
import com.y.citycapsule.core.media.CameraCapability
import com.y.citycapsule.core.media.CameraCaptureResult
import com.y.citycapsule.core.navigation.AppNavigator
import com.y.citycapsule.core.navigation.AppRoute
import com.y.citycapsule.core.navigation.AppRouteKey
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

    @Test
    fun removingANewlyPickedPhotoRequestsSafeMediaCleanup() {
        val fixture = fixture()
        val candidates = mutableListOf<String>()
        val cleanup = CapsuleMediaCleanup { paths, callback ->
            candidates += paths
            callback(CapsuleMediaCleanupResult.Success(paths.toList()))
        }
        val editor = CapsuleEditorStateHolder(
            null,
            PLACE_ID,
            fixture.capsules,
            fixture.places,
            cleanup
        )

        editor.load()
        editor.addImages(listOf("file:///new-photo.jpg"))
        editor.removeImage("file:///new-photo.jpg")

        assertEquals(listOf("file:///new-photo.jpg"), candidates)
        assertTrue(editor.state.draft.imagePaths.isEmpty())
    }

    @Test
    fun timelineAndDetailUseTheInjectedLocalDateFormatter() {
        val fixture = fixture()
        var capsuleId: String? = null
        fixture.capsules.publish(
            CapsuleDraft(content = "跨午夜的记忆", placeId = PLACE_ID)
        ) { result -> capsuleId = (result as StorageResult.Success).value.id }
        val formatter = com.y.citycapsule.core.capsule.CapsuleDateFormatter {
            "2026 年 7 月 28 日"
        }

        val timeline = CapsuleTimelineStateHolder(
            fixture.capsules,
            fixture.places,
            formatter
        )
        timeline.load()
        val detail = CapsuleDetailStateHolder(
            assertNotNull(capsuleId),
            fixture.capsules,
            fixture.places,
            dateFormatter = formatter
        )
        detail.load()

        assertEquals("2026 年 7 月 28 日", timeline.state.items.single().dateLabel)
        assertEquals("2026 年 7 月 28 日", detail.state.dateLabel)
    }

    @Test
    fun capturedPhotoEntersTheExistingDraftImagePathsProtocol() {
        val fixture = fixture()
        val editor = CapsuleEditorStateHolder(null, PLACE_ID, fixture.capsules, fixture.places)
        editor.load()
        editor.openMediaSourcePicker()
        assertTrue(editor.state.showMediaSourcePicker)

        editor.captureImage(CameraCapability { callback ->
            callback(CameraCaptureResult.Success("file:///sandbox/images/original/camera.jpg"))
        })

        assertFalse(editor.state.showMediaSourcePicker)
        assertFalse(editor.state.capturingImage)
        assertEquals(
            listOf("file:///sandbox/images/original/camera.jpg"),
            editor.state.draft.imagePaths
        )
    }

    @Test
    fun unavailableCameraLeavesAlbumAndTextEditingAvailable() {
        val fixture = fixture()
        val editor = CapsuleEditorStateHolder(null, PLACE_ID, fixture.capsules, fixture.places)
        editor.load()

        editor.captureImage(CameraCapability { callback ->
            callback(CameraCaptureResult.Unsupported)
        })
        editor.pickImages(PhotoPickerCapability { _, callback ->
            callback(PhotoPickerResult.Success(listOf("file:///sandbox/images/original/album.jpg")))
        })
        editor.updateContent("没有相机也能记录")

        assertEquals(listOf("file:///sandbox/images/original/album.jpg"), editor.state.draft.imagePaths)
        assertEquals("没有相机也能记录", editor.state.draft.content)
        assertEquals(CapsuleUiStatus.READY, editor.state.status)
    }

    @Test
    fun saveDraftAndClosePersistsTheCurrentEditorContext() {
        val fixture = fixture()
        val editor = CapsuleEditorStateHolder(
            null,
            PLACE_ID,
            fixture.capsules,
            fixture.places
        )
        editor.load()
        editor.updateContent("先保存，下一次继续写。")
        editor.updateMood(CapsuleMood.CALM)
        editor.requestClose {}
        assertTrue(editor.state.showDiscardConfirmation)

        var closed = false
        editor.saveDraftAndClose { closed = true }

        assertTrue(closed)
        assertFalse(editor.state.showDiscardConfirmation)
        assertEquals("先保存，下一次继续写。", draft(fixture).content)
        assertEquals(PLACE_ID, draft(fixture).placeId)
    }

    @Test
    fun timelineGroupsUseLocalYearAndMonthWithoutReorderingMemories() {
        val julyFirst = timelineItem("july-1", "2026 年 7 月 28 日")
        val julySecond = timelineItem("july-2", "2026 年 7 月 20 日")
        val june = timelineItem("june", "2026 年 6 月 30 日")

        val groups = groupTimelineItems(listOf(julyFirst, julySecond, june))

        assertEquals(listOf("2026-7", "2026-6"), groups.map { it.monthKey })
        assertEquals(listOf("2026 年 7 月", "2026 年 6 月"), groups.map { it.monthLabel })
        assertEquals(listOf("july-1", "july-2"), groups.first().items.map { it.capsule.id })
        assertEquals("28", parseCapsuleCalendarLabel(julyFirst.dateLabel).dayLabel)
    }

    @Test
    fun malformedDateLabelsRemainVisibleInAnUnknownDateGroup() {
        val item = timelineItem("unknown", "日期服务暂不可用")

        val calendar = parseCapsuleCalendarLabel(item.dateLabel)
        val groups = groupTimelineItems(listOf(item))

        assertEquals("unknown", calendar.monthKey)
        assertEquals("日期未知", calendar.monthLabel)
        assertEquals("—", calendar.dayLabel)
        assertEquals(listOf("unknown"), groups.single().items.map { it.capsule.id })
    }

    @Test
    fun galleryLoadsOriginalsInBoundedBatchesUntilAllAreVisible() {
        assertEquals(18, nextGalleryVisibleCount(current = 0, total = 40))
        assertEquals(36, nextGalleryVisibleCount(current = 18, total = 40))
        assertEquals(40, nextGalleryVisibleCount(current = 36, total = 40))
        assertEquals(0, nextGalleryVisibleCount(current = 18, total = 0))
    }

    @Test
    fun newPublishOpensDetailButEditingReturnsToTheExistingDetail() {
        val newNavigator = RecordingCapsuleNavigator()
        completeCapsuleEditorNavigation(null, "capsule-new", newNavigator)
        assertEquals(AppRoute.CapsuleDetail("capsule-new"), newNavigator.replaced)
        assertEquals(0, newNavigator.backCount)

        val editNavigator = RecordingCapsuleNavigator()
        completeCapsuleEditorNavigation("capsule-existing", "capsule-existing", editNavigator)
        assertEquals(null, editNavigator.replaced)
        assertEquals(1, editNavigator.backCount)
    }

    @Test
    fun detailUsesEqualTwoUpLayoutForExactlyTwoPhotos() {
        assertEquals(
            CapsuleDetailPhotoLayoutMode.SINGLE_HERO,
            capsuleDetailPhotoLayoutMode(1)
        )
        assertEquals(
            CapsuleDetailPhotoLayoutMode.TWO_UP,
            capsuleDetailPhotoLayoutMode(2)
        )
        assertEquals(
            CapsuleDetailPhotoLayoutMode.HERO_WITH_GRID,
            capsuleDetailPhotoLayoutMode(3)
        )
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

    private fun timelineItem(id: String, dateLabel: String): CapsuleTimelineItem =
        CapsuleTimelineItem(
            capsule = CityCapsule(
                id = id,
                content = "城市记忆 $id",
                placeId = PLACE_ID,
                createdAtEpochMs = 0L,
                updatedAtEpochMs = 0L
            ),
            place = null,
            dateLabel = dateLabel
        )

    private data class Fixture(
        val capsules: LocalCapsuleRepository,
        val places: LocalPlaceRepository
    )

    private companion object {
        const val PLACE_ID = "seed_shanghai_museum"
    }
}

private class RecordingCapsuleNavigator : AppNavigator {
    var replaced: AppRoute? = null
    var backCount: Int = 0

    override fun navigate(route: AppRoute) = Unit
    override fun replace(route: AppRoute) { replaced = route }
    override fun back() { backCount += 1 }
    override fun backTo(routeKey: AppRouteKey) = Unit
}
