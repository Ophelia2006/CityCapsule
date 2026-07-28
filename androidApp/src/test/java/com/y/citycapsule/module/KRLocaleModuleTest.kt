package com.y.citycapsule.module

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class KRLocaleModuleTest {
    @Test
    fun localCalendarDateUsesTheDeviceTimeZoneAcrossUtcMidnight() {
        val epochMs = 1_704_063_600_000L // 2023-12-31 23:00:00 UTC

        assertEquals(
            "2023-12-31",
            LocalDateFormatterEngine.format(epochMs, TimeZone.getTimeZone("UTC"))
        )
        assertEquals(
            "2024-01-01",
            LocalDateFormatterEngine.format(epochMs, TimeZone.getTimeZone("Asia/Shanghai"))
        )
    }
}
