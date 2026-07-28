package com.y.citycapsule.core.capsule

/**
 * Deterministic cross-platform calendar label derived from the persisted epoch timestamp.
 * Kuikly commonMain currently has no timezone database, so the label uses UTC rather than
 * delegating to platform formatters with divergent wire behavior.
 */
fun formatCapsuleDate(epochMillis: Long): String {
    if (epochMillis < 0L) return "日期未知"
    val date = civilDateFromEpochDay(epochMillis / MILLIS_PER_DAY)
    return "${date.year} 年 ${date.month} 月 ${date.day} 日"
}

internal data class CapsuleCivilDate(
    val year: Long,
    val month: Long,
    val day: Long
)

internal fun civilDateFromEpochDay(epochDay: Long): CapsuleCivilDate {
    val shifted = epochDay + 719_468L
    val era = shifted / 146_097L
    val dayOfEra = shifted - era * 146_097L
    val yearOfEra = (
        dayOfEra - dayOfEra / 1_460L + dayOfEra / 36_524L - dayOfEra / 146_096L
        ) / 365L
    var year = yearOfEra + era * 400L
    val dayOfYear = dayOfEra - (365L * yearOfEra + yearOfEra / 4L - yearOfEra / 100L)
    val monthPrime = (5L * dayOfYear + 2L) / 153L
    val day = dayOfYear - (153L * monthPrime + 2L) / 5L + 1L
    val month = monthPrime + if (monthPrime < 10L) 3L else -9L
    if (month <= 2L) year += 1L
    return CapsuleCivilDate(year, month, day)
}

private const val MILLIS_PER_DAY = 86_400_000L
