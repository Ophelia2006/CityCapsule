package com.y.citycapsule.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidRouteRequestDecoderTest {

    @Test
    fun missingMetadataFallsBackToLegacyKuiklyPush() {
        val request = AndroidRouteRequestDecoder.decodeValues(
            pageName = "image_adapter",
            actionValue = null,
            routeKeyValue = null,
            targetTypeValue = null,
            pageDataJson = "{}"
        )

        assertEquals(AndroidRouteAction.PUSH, request.action)
        assertEquals(AndroidRouteTargetType.KUIKLY, request.targetType)
        assertEquals("image_adapter", request.routeKey)
        assertEquals("image_adapter", request.target)
    }

    @Test
    fun sharedReplaceEnvelopeIsDecoded() {
        val request = AndroidRouteRequestDecoder.decodeValues(
            pageName = "settings",
            actionValue = "replace",
            routeKeyValue = "settings",
            targetTypeValue = "kuikly",
            pageDataJson = """{"source":"home"}"""
        )

        assertEquals(AndroidRouteAction.REPLACE, request.action)
        assertEquals(AndroidRouteTargetType.KUIKLY, request.targetType)
        assertEquals("settings", request.routeKey)
        assertEquals("""{"source":"home"}""", request.pageDataJson)
    }

    @Test
    fun nativePrefixIsRecognizedForLegacyRequests() {
        val request = AndroidRouteRequestDecoder.decodeValues(
            pageName = "/native/file-import",
            actionValue = null,
            routeKeyValue = "native_file_import",
            targetTypeValue = null,
            pageDataJson = "{}"
        )

        assertEquals(AndroidRouteTargetType.NATIVE, request.targetType)
        assertEquals("/native/file-import", request.target)
    }

    @Test
    fun unsupportedActionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidRouteRequestDecoder.decodeValues(
                pageName = "settings",
                actionValue = "clearAll",
                routeKeyValue = "settings",
                targetTypeValue = "kuikly",
                pageDataJson = "{}"
            )
        }
    }
}
