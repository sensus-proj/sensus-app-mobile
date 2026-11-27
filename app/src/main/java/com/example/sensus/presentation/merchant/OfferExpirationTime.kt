package com.example.sensus.presentation.merchant

import java.util.Calendar
import java.util.TimeZone

/** DatePicker uses UTC midnight; preserve the local calendar day shown in the form. */
internal fun expirationCalendarDayUtc(value: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = value }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

/** Une el día elegido en el DatePicker (medianoche UTC) con la hora local seleccionada, sin segundos. */
internal fun combineExpiration(selectedDayUtcMillis: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long {
    val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedDayUtcMillis }
    return Calendar.getInstance(zone).apply {
        clear()
        set(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
    }.timeInMillis
}
