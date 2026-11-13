package com.example.sensus.presentation.merchant

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.sensus.R
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferExpirationPicker(value: Long, error: String?, onChange: (Long) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val local = remember(value) { Calendar.getInstance().apply { timeInMillis = value } }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = expirationCalendarDayUtc(value))
    val timeState = rememberTimePickerState(initialHour = local.get(Calendar.HOUR_OF_DAY), initialMinute = local.get(Calendar.MINUTE), is24Hour = true)
    Column {
        Text(stringResource(R.string.offer_expiration_label))
        TextButton(onClick = { showDate = true }) {
            Text(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value)), color = SensusPurple)
        }
        error?.let { Text(it, color = SensusRed) }
    }
    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    showDate = false
                    showTime = true
                }, enabled = dateState.selectedDateMillis != null) { Text(stringResource(R.string.offer_pick_time)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.publishing_cancel)) } }
        ) { DatePicker(state = dateState) }
    }
    if (showTime) {
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = dateState.selectedDateMillis ?: value
                    }
                    local.set(
                        selected.get(Calendar.YEAR),
                        selected.get(Calendar.MONTH),
                        selected.get(Calendar.DAY_OF_MONTH),
                        timeState.hour,
                        timeState.minute,
                        0
                    )
                    local.set(Calendar.MILLISECOND, 0)
                    onChange(local.timeInMillis)
                    showTime = false
                }) { Text(stringResource(R.string.publishing_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(stringResource(R.string.publishing_cancel)) } }
        )
    }
}

/** DatePicker uses UTC midnight; preserve the local calendar day shown in the form. */
internal fun expirationCalendarDayUtc(value: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = value }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}
