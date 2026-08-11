package com.y.citycapsule.core.media

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PhotoPickerCapabilityTest {
    @Test
    fun pagerModuleUsesTheSameNameAsTheNativeContract() {
        assertEquals(KuiklyPhotoPicker.MODULE_NAME, KuiklyMediaModule().moduleName())
    }

    @Test
    fun successIsNormalizedAndLimitedToRequestedCount() {
        val picker = KuiklyPhotoPicker(PhotoPickerTransport { _, callback ->
            callback(
                JSONObject().apply {
                    put(KuiklyPhotoPicker.FIELD_STATUS, KuiklyPhotoPicker.STATUS_SUCCESS)
                    put(
                        KuiklyPhotoPicker.FIELD_PATHS,
                        JSONArray().put("file:///a.jpg").put("file:///a.jpg").put("file:///b.jpg")
                    )
                }
            )
        })
        var result: PhotoPickerResult? = null

        picker.pickImages(1) { result = it }

        assertEquals(listOf("file:///a.jpg"), assertIs<PhotoPickerResult.Success>(result).paths)
    }

    @Test
    fun nativeStatusesRemainExplicit() {
        val picker = KuiklyPhotoPicker(PhotoPickerTransport { _, callback ->
            callback(JSONObject().apply {
                put(KuiklyPhotoPicker.FIELD_STATUS, KuiklyPhotoPicker.STATUS_UNSUPPORTED)
            })
        })
        var result: PhotoPickerResult? = null

        picker.pickImages(2) { result = it }

        assertIs<PhotoPickerResult.Unsupported>(result)
    }

    @Test
    fun missingNativePickerModuleBecomesFailureInsteadOfCrashingThePage() {
        val picker = KuiklyPhotoPicker(PhotoPickerTransport { _, _ ->
            throw IllegalStateException("CCMediaModule is not registered")
        })
        var result: PhotoPickerResult? = null

        picker.pickImages(1) { result = it }

        assertIs<PhotoPickerResult.Failure>(result)
    }

    @Test
    fun cameraReturnsExactlyOneManagedPath() {
        val camera = KuiklyCameraCapability(CameraTransport { _, callback ->
            callback(JSONObject().apply {
                put(KuiklyCameraCapability.FIELD_STATUS, KuiklyCameraCapability.STATUS_SUCCESS)
                put(
                    KuiklyCameraCapability.FIELD_PATHS,
                    JSONArray().put("file:///sandbox/images/original/camera.jpg")
                )
            })
        })
        var result: CameraCaptureResult? = null

        camera.captureImage { result = it }

        assertEquals(
            "file:///sandbox/images/original/camera.jpg",
            assertIs<CameraCaptureResult.Success>(result).path
        )
    }

    @Test
    fun cameraCancellationAndMissingModuleRemainExplicit() {
        val cancelled = KuiklyCameraCapability(CameraTransport { _, callback ->
            callback(JSONObject().apply {
                put(KuiklyCameraCapability.FIELD_STATUS, KuiklyCameraCapability.STATUS_CANCELLED)
            })
        })
        var cancelledResult: CameraCaptureResult? = null
        cancelled.captureImage { cancelledResult = it }
        assertIs<CameraCaptureResult.Cancelled>(cancelledResult)

        val missing = KuiklyCameraCapability(CameraTransport { _, _ ->
            error("CCMediaModule is not registered")
        })
        var missingResult: CameraCaptureResult? = null
        missing.captureImage { missingResult = it }
        assertIs<CameraCaptureResult.Failure>(missingResult)
    }

    @Test
    fun managedDeleteNormalizesCandidatesAndParsesDeletedPaths() {
        var requestedPaths = emptyList<String>()
        val files = KuiklyManagedMediaFiles(ManagedMediaTransport { request, callback ->
            val paths = request.optJSONArray(KuiklyManagedMediaFiles.FIELD_PATHS)
            requestedPaths = buildList {
                if (paths != null) {
                    for (index in 0 until paths.length()) add(paths.optString(index).orEmpty())
                }
            }
            callback(JSONObject().apply {
                put(KuiklyManagedMediaFiles.FIELD_STATUS, KuiklyManagedMediaFiles.STATUS_SUCCESS)
                put(
                    KuiklyManagedMediaFiles.FIELD_PATHS,
                    JSONArray().put("file:///owned.jpg")
                )
            })
        })
        var result: ManagedMediaDeleteResult? = null

        files.deleteManagedImages(
            listOf(" file:///owned.jpg ", "file:///owned.jpg")
        ) { result = it }

        assertEquals(listOf("file:///owned.jpg"), requestedPaths)
        assertEquals(
            listOf("file:///owned.jpg"),
            assertIs<ManagedMediaDeleteResult.Success>(result).deletedPaths
        )
    }

    @Test
    fun missingNativeDeleteModuleBecomesFailureInsteadOfCrashingThePage() {
        val files = KuiklyManagedMediaFiles(ManagedMediaTransport { _, _ ->
            throw IllegalStateException("CCMediaModule is not registered")
        })
        var result: ManagedMediaDeleteResult? = null

        files.deleteManagedImages(listOf("file:///owned.jpg")) { result = it }

        assertIs<ManagedMediaDeleteResult.Failure>(result)
    }
}
