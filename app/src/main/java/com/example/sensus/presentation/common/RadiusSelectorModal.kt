package com.example.sensus.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.theme.SensusSurface
import java.util.Locale
import kotlin.math.roundToInt

private const val METERS_PER_KM = 1000
private const val METER_STEP = 100
private const val MAX_METER_OPTION = 500
private const val CHIPS_PER_ROW = 3
private val meterOptions = (METER_STEP..MAX_METER_OPTION step METER_STEP).toList()
private val kilometerOptions = (METERS_PER_KM..GuestRadiusPolicy.MAX_RADIUS_METERS step METERS_PER_KM).toList()

fun formatRadiusText(radiusKm: Float): String {
    val meters = (radiusKm * METERS_PER_KM).roundToInt()
    return if (meters < METERS_PER_KM) {
        "$meters m"
    } else {
        val km = meters.toDouble() / METERS_PER_KM
        val raw = String.format(Locale.US, "%.2f", km).trimEnd('0').trimEnd('.')
        "$raw km"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadiusSelectorBottomSheet(
    currentRadiusKm: Float,
    isGuest: Boolean = false,
    onRadiusSelected: (Float) -> Unit,
    onRequireAuth: () -> Unit = {},
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SensusSurface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.nearby_radius_title), style = MaterialTheme.typography.titleLarge)
            if (isGuest) {
                Text(stringResource(R.string.nearby_radius_guest_fixed, GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS / METERS_PER_KM))
                TextButton(onClick = {
                    onDismiss()
                    onRequireAuth()
                }) {
                    Text(stringResource(R.string.nearby_radius_sign_in))
                }
            } else {
                RadiusChoices(currentRadiusKm) { meters ->
                    onRadiusSelected(meters.toFloat() / METERS_PER_KM)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun RadiusChoices(currentRadiusKm: Float, onApply: (Int) -> Unit) {
    val currentMeters = (currentRadiusKm * METERS_PER_KM).roundToInt()
    var selectedMeters by rememberSaveable(currentMeters) { mutableIntStateOf(currentMeters) }
    var showMeters by rememberSaveable(currentMeters) { mutableStateOf(currentMeters < METERS_PER_KM) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(showMeters, { showMeters = true }, { Text(stringResource(R.string.nearby_radius_meters)) })
        FilterChip(!showMeters, { showMeters = false }, { Text(stringResource(R.string.nearby_radius_kilometers)) })
    }
    val options = if (showMeters) meterOptions else kilometerOptions
    options.chunked(CHIPS_PER_ROW).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { meters ->
                FilterChip(
                    selected = meters == selectedMeters,
                    onClick = { selectedMeters = meters },
                    label = { Text(formatRadiusText(meters.toFloat() / METERS_PER_KM)) }
                )
            }
        }
    }
    Button(
        onClick = { onApply(selectedMeters) },
        enabled = selectedMeters in options,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.nearby_radius_apply))
    }
}
