package com.example.sensus.presentation.merchant

import java.time.LocalDateTime
import java.time.Month
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferExpirationTimeTest {

    private val mexicoCity: TimeZone = TimeZone.getTimeZone("America/Mexico_City")
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    private fun millis(zone: TimeZone, dateTime: LocalDateTime): Long = dateTime.atZone(zone.toZoneId()).toInstant().toEpochMilli()

    @Test
    fun combineExpirationUsesSelectedDayAndLocalTime() {
        val selectedDay = millis(utc, LocalDateTime.of(2026, Month.OCTOBER, 7, 0, 0))

        val result = combineExpiration(selectedDay, hour = 20, minute = 0, zone = mexicoCity)

        assertEquals(millis(mexicoCity, LocalDateTime.of(2026, Month.OCTOBER, 7, 20, 0)), result)
    }

    @Test
    fun combineExpirationDropsSecondsAndMillis() {
        val selectedDay = millis(utc, LocalDateTime.of(2026, Month.DECEMBER, 31, 0, 0))

        val result = combineExpiration(selectedDay, hour = 23, minute = 59, zone = mexicoCity)

        val local = Calendar.getInstance(mexicoCity).apply { timeInMillis = result }
        assertEquals(2026, local.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, local.get(Calendar.MONTH))
        assertEquals(31, local.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, local.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, local.get(Calendar.MINUTE))
        assertEquals(0, local.get(Calendar.SECOND))
        assertEquals(0, local.get(Calendar.MILLISECOND))
    }

    @Test
    fun combineExpirationKeepsDayWhenUtcMidnightIsPreviousLocalDay() {
        // La medianoche UTC del 15 es el 14 por la tarde en Ciudad de México; se respeta el día 15.
        val selectedDay = millis(utc, LocalDateTime.of(2026, Month.MARCH, 15, 0, 0))

        val result = combineExpiration(selectedDay, hour = 9, minute = 30, zone = mexicoCity)

        assertEquals(millis(mexicoCity, LocalDateTime.of(2026, Month.MARCH, 15, 9, 30)), result)
    }

    @Test
    fun calendarDayUtcRoundTripsWithCombineExpiration() {
        val original = millis(mexicoCity, LocalDateTime.of(2026, Month.NOVEMBER, 27, 22, 45))

        val dayUtc = expirationCalendarDayUtc(original, mexicoCity)
        val rebuilt = combineExpiration(dayUtc, hour = 22, minute = 45, zone = mexicoCity)

        assertEquals(millis(utc, LocalDateTime.of(2026, Month.NOVEMBER, 27, 0, 0)), dayUtc)
        assertEquals(original, rebuilt)
    }
}
