package com.y.citycapsule.module

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class KRMediaModuleTest {
    @Test
    fun responseUsesTheSharedJsonCallbackContract() {
        val response = JSONObject(
            KRMediaModule.response(
                status = KRMediaModule.STATUS_SUCCESS,
                paths = listOf("file:///one.jpg", "file:///two.jpg")
            )
        )

        assertEquals("success", response.getString("status"))
        assertEquals("", response.getString("message"))
        assertEquals(2, response.getJSONArray("paths").length())
    }
}
