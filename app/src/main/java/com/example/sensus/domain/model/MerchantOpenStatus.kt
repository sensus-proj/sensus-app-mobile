package com.example.sensus.domain.model

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Open/closed state derived from the schedule published in the merchant profile (FT-11 / FT-12). */
enum class MerchantOpenStatus {
    OPEN,
    CLOSED,
    UNKNOWN
}

private const val TIME_TEXT_LENGTH = 5
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

private val DAY_NAMES = mapOf(
    "lunes" to DayOfWeek.MONDAY,
    "martes" to DayOfWeek.TUESDAY,
    "miércoles" to DayOfWeek.WEDNESDAY,
    "miercoles" to DayOfWeek.WEDNESDAY,
    "jueves" to DayOfWeek.THURSDAY,
    "viernes" to DayOfWeek.FRIDAY,
    "sábado" to DayOfWeek.SATURDAY,
    "sabado" to DayOfWeek.SATURDAY,
    "domingo" to DayOfWeek.SUNDAY
)

private data class OpeningRange(val opens: LocalTime, val closes: LocalTime) {
    fun isOpenOnStartingDay(time: LocalTime): Boolean = !time.isBefore(opens) && (closes <= opens || time.isBefore(closes))

    fun carriesIntoNextDay(time: LocalTime): Boolean = closes <= opens && time.isBefore(closes)
}

/**
 * Returns [MerchantOpenStatus.UNKNOWN] when the merchant has not published a schedule for that day,
 * so the UI never claims a state the backend did not provide. Overnight ranges (22:00 - 02:00) are supported.
 */
fun MerchantProfile?.openStatusAt(moment: LocalDateTime): MerchantOpenStatus {
    val today = this?.horarios?.forDay(moment.dayOfWeek)
    val range = today?.openingRange()
    val yesterday = this?.horarios?.forDay(moment.dayOfWeek.minus(1))?.openingRange()
    val time = moment.toLocalTime()
    return when {
        this == null -> MerchantOpenStatus.UNKNOWN
        !activo -> MerchantOpenStatus.CLOSED
        yesterday?.carriesIntoNextDay(time) == true -> MerchantOpenStatus.OPEN
        today?.estaCerrado == true -> MerchantOpenStatus.CLOSED
        range == null -> MerchantOpenStatus.UNKNOWN
        range.isOpenOnStartingDay(time) -> MerchantOpenStatus.OPEN
        else -> MerchantOpenStatus.CLOSED
    }
}

/** Human readable schedule for the given day, e.g. "08:00 - 20:00", or null when not published. */
fun MerchantProfile?.scheduleFor(day: DayOfWeek): String? = this?.horarios?.forDay(day)
    ?.publishedHours()

fun HorarioDia.publishedHours(): String? = openingRange()?.let { "${horaApertura?.trim()} - ${horaCierre?.trim()}" }

private fun List<HorarioDia>.forDay(day: DayOfWeek): HorarioDia? = firstOrNull { DAY_NAMES[it.dia.trim().lowercase()] == day }

private fun HorarioDia.openingRange(): OpeningRange? {
    val opens = parseTime(horaApertura)
    val closes = parseTime(horaCierre)
    return if (estaCerrado || opens == null || closes == null) null else OpeningRange(opens, closes)
}

private fun parseTime(raw: String?): LocalTime? = try {
    raw?.trim()?.takeIf { it.isNotEmpty() }?.let { LocalTime.parse(it.take(TIME_TEXT_LENGTH), TIME_FORMAT) }
} catch (_: DateTimeParseException) {
    null
}
