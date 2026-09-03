package com.y.citycapsule.core.capsule

import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager

fun interface CapsuleDateFormatter {
    fun format(epochMs: Long): String
}

object UtcCapsuleDateFormatter : CapsuleDateFormatter {
    override fun format(epochMs: Long): String = formatCapsuleDate(epochMs)
}

class KuiklyLocalCapsuleDateFormatter internal constructor(
    private val transport: LocalDateFormatterTransport
) : CapsuleDateFormatter {
    constructor(pager: Pager) : this(PagerLocalDateFormatterTransport(pager))

    override fun format(epochMs: Long): String {
        if (epochMs < 0L) return UNKNOWN_DATE
        val raw = transport.format(
            JSONObject().apply { put(FIELD_EPOCH_MS, epochMs) }
        ).trim()
        val parts = raw.split('-')
        val year = parts.getOrNull(0)?.toIntOrNull()
        val month = parts.getOrNull(1)?.toIntOrNull()
        val day = parts.getOrNull(2)?.toIntOrNull()
        return if (year != null && month != null && day != null &&
            month in 1..12 && day in 1..31
        ) {
            "$year 年 $month 月 $day 日"
        } else {
            UNKNOWN_DATE
        }
    }

    fun formatDateTime(epochMs: Long): String {
        if (epochMs < 0L) return UNKNOWN_DATE
        val raw = transport.format(JSONObject().apply { put(FIELD_EPOCH_MS, epochMs); put(FIELD_STYLE, STYLE_DATE_TIME) }).trim()
        val parts = raw.split(' ')
        val date = parts.getOrNull(0)?.split('-').orEmpty()
        val time = parts.getOrNull(1)?.split(':').orEmpty()
        val year = date.getOrNull(0)?.toIntOrNull(); val month = date.getOrNull(1)?.toIntOrNull(); val day = date.getOrNull(2)?.toIntOrNull()
        val hour = time.getOrNull(0)?.toIntOrNull(); val minute = time.getOrNull(1)?.toIntOrNull()
        return if (year != null && month != null && day != null && hour != null && minute != null && month in 1..12 && day in 1..31 && hour in 0..23 && minute in 0..59) "$year 年 $month 月 $day 日 ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}" else UNKNOWN_DATE
    }

    companion object {
        const val MODULE_NAME = "CCLocaleModule"
        const val METHOD_FORMAT_LOCAL_DATE = "formatLocalDate"
        const val FIELD_EPOCH_MS = "epochMs"
        const val FIELD_STYLE = "style"
        const val STYLE_DATE_TIME = "dateTime"
        const val UNKNOWN_DATE = "日期未知"
    }
}

internal fun interface LocalDateFormatterTransport {
    fun format(request: JSONObject): String
}

internal class KuiklyLocaleModule : Module() {
    override fun moduleName(): String = KuiklyLocalCapsuleDateFormatter.MODULE_NAME

    fun format(request: JSONObject): String = toNative(
        false,
        KuiklyLocalCapsuleDateFormatter.METHOD_FORMAT_LOCAL_DATE,
        request.toString(),
        null,
        true
    ).toString()
}

private class PagerLocalDateFormatterTransport(
    private val pager: Pager
) : LocalDateFormatterTransport {
    override fun format(request: JSONObject): String =
        pager.acquireModule<KuiklyLocaleModule>(KuiklyLocalCapsuleDateFormatter.MODULE_NAME)
            .format(request)
}
