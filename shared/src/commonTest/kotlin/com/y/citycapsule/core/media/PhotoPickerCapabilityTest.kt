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
}
