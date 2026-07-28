package com.y.citycapsule.module

import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class KRLocaleModule : KuiklyRenderBaseModule() {
    override fun call(
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    ): Any? {
        if (method != METHOD_FORMAT_LOCAL_DATE) return ""
        val epochMs = runCatching {
            JSONObject(params ?: "{}").optLong(FIELD_EPOCH_MS, -1L)
        }.getOrDefault(-1L)
        return if (epochMs >= 0L) {
            LocalDateFormatterEngine.format(epochMs, TimeZone.getDefault())
        } else {
            ""
        }
    }

    companion object {
        const val MODULE_NAME = "CCLocaleModule"
        private const val METHOD_FORMAT_LOCAL_DATE = "formatLocalDate"
        private const val FIELD_EPOCH_MS = "epochMs"
    }
}

internal object LocalDateFormatterEngine {
    fun format(epochMs: Long, timeZone: TimeZone): String {
        val calendar = Calendar.getInstance(timeZone, Locale.ROOT).apply {
            timeInMillis = epochMs
        }
        return "%04d-%02d-%02d".format(
            Locale.ROOT,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}
