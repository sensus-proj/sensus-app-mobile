package com.example.sensus.presentation.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusTextButtonColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val DATE_BOX_WEIGHT = 1.4f
private val SPANISH_MX: Locale = Locale.forLanguageTag("es-MX")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferExpirationPicker(value: Long, error: String?, onChange: (Long) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    val local = remember(value) { Calendar.getInstance().apply { timeInMillis = value } }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = expirationCalendarDayUtc(value))
    val timeState = rememberTimePickerState(initialHour = local.get(Calendar.HOUR_OF_DAY), initialMinute = local.get(Calendar.MINUTE), is24Hour = true)
    Column {
        MerchantFieldLabel(stringResource(R.string.merchant_new_offer_deadline))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExpirationBox(
                icon = Icons.Default.CalendarMonth,
                text = SimpleDateFormat("EEE, d MMM yyyy", SPANISH_MX).format(Date(value)),
                modifier = Modifier.weight(DATE_BOX_WEIGHT),
                onClick = { showDate = true }
            )
            ExpirationBox(
                icon = Icons.Outlined.Schedule,
                text = SimpleDateFormat("HH:mm", SPANISH_MX).format(Date(value)),
                modifier = Modifier.weight(1f),
                onClick = { showTime = true }
            )
        }
        Text(stringResource(R.string.merchant_new_offer_deadline_hint), fontSize = 11.sp, color = SensusTextSecondary, modifier = Modifier.padding(top = 6.dp))
        error?.let { MerchantErrorText(it) }
    }
    if (showDate) {
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(colors = sensusTextButtonColors(), onClick = {
                    showDate = false
                    showTime = true
                }, enabled = dateState.selectedDateMillis != null) { Text(stringResource(R.string.offer_pick_time)) }
            },
            dismissButton = { ExpirationCancelButton { showDate = false } }
        ) { DatePicker(state = dateState) }
    }
    if (showTime) {
        AlertDialog(
            onDismissRequest = { showTime = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(colors = sensusTextButtonColors(), onClick = {
                    onChange(combineExpiration(local, dateState.selectedDateMillis ?: value, timeState.hour, timeState.minute))
                    showTime = false
                }) { Text(stringResource(R.string.publishing_confirm)) }
            },
            dismissButton = { ExpirationCancelButton { showTime = false } }
        )
    }
}

/** Une el día elegido (medianoche UTC del DatePicker) con la hora local seleccionada. */
private fun combineExpiration(local: Calendar, selectedDayUtcMillis: Long, hour: Int, minute: Int): Long {
    val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selectedDayUtcMillis }
    local.set(selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH), hour, minute, 0)
    local.set(Calendar.MILLISECOND, 0)
    return local.timeInMillis
}

@Composable
private fun ExpirationBox(icon: ImageVector, text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        shape = MerchantFieldShape,
        color = SensusSurface,
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, fontSize = 13.sp, color = SensusTextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = SensusTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExpirationCancelButton(onClick: () -> Unit) {
    TextButton(colors = sensusTextButtonColors(), onClick = onClick) { Text(stringResource(R.string.publishing_cancel)) }
}

/** DatePicker uses UTC midnight; preserve the local calendar day shown in the form. */
internal fun expirationCalendarDayUtc(value: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = value }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}
