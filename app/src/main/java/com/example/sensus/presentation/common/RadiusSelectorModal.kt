package com.example.sensus.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.util.Locale
import kotlin.math.roundToInt

private const val RADIUS_50M_KM = 0.05f
private const val RADIUS_100M_KM = 0.1f
private const val RADIUS_250M_KM = 0.25f
private const val RADIUS_500M_KM = 0.5f
private const val RADIUS_1KM = 1.0f
private const val RADIUS_2KM = 2.0f
private const val RADIUS_3_5KM = 3.5f
private const val RADIUS_5KM = 5.0f

private const val METERS_PER_KM = 1000
private const val METERS_PER_KM_FLOAT = 1000f
private const val METERS_PER_KM_DOUBLE = 1000.0
private const val STEP_METERS = 50
private const val STEP_METERS_FLOAT = 50f
private const val MAX_METER_DIGITS = 4

private const val HEADER_ICON_ALPHA = 0.12f
private const val GUEST_BANNER_BG_ALPHA = 0.08f
private const val GUEST_BANNER_BORDER_ALPHA = 0.25f
private const val READOUT_BORDER_ALPHA = 0.2f
private const val DISABLED_STEPPER_ALPHA = 0.4f
private const val SLIDER_INACTIVE_ALPHA = 0.2f

private const val DIVIDER_HEX = 0xFFF1F5F9
private const val READOUT_BG_HEX = 0xFFF8F7FF
private const val BORDER_LIGHT_HEX = 0xFFE2E8F0
private const val BORDER_MEDIUM_HEX = 0xFFCBD5E1

private data class RadiusSheetProps(
    val selectedMeters: Int,
    val customMetersInput: String,
    val attemptedGuestOverflow: Boolean,
    val isInputOutOfBounds: Boolean,
    val isGuest: Boolean,
    val maxAllowedMeters: Int
)

private data class QuickChipPalette(
    val containerColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val weight: FontWeight
)

val availableRadiusOptions = listOf(
    RADIUS_50M_KM to "50 m",
    RADIUS_100M_KM to "100 m",
    RADIUS_250M_KM to "250 m",
    RADIUS_500M_KM to "500 m",
    RADIUS_1KM to "1 km",
    RADIUS_2KM to "2 km",
    RADIUS_3_5KM to "3.5 km",
    RADIUS_5KM to "5 km"
)

fun formatRadiusText(radiusKm: Float): String {
    val meters = (radiusKm * METERS_PER_KM_FLOAT).roundToInt()
    return if (meters < METERS_PER_KM) {
        "$meters m"
    } else {
        val km = meters / METERS_PER_KM_DOUBLE
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
    val maxAllowedMeters = if (isGuest) GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS else GuestRadiusPolicy.MAX_RADIUS_METERS
    val initialMeters = (GuestRadiusPolicy.clampRadius(currentRadiusKm, isGuest) * METERS_PER_KM_FLOAT)
        .roundToInt()
        .coerceIn(GuestRadiusPolicy.MIN_RADIUS_METERS, maxAllowedMeters)

    var selectedMeters by remember(currentRadiusKm, isGuest) { mutableIntStateOf(initialMeters) }
    var customMetersInput by remember(currentRadiusKm, isGuest) { mutableStateOf(initialMeters.toString()) }
    var attemptedGuestOverflow by remember { mutableStateOf(false) }

    val parsedTypedMeters = customMetersInput.toIntOrNull()
    val isInputOutOfBounds = parsedTypedMeters == null ||
        parsedTypedMeters < GuestRadiusPolicy.MIN_RADIUS_METERS ||
        parsedTypedMeters > GuestRadiusPolicy.MAX_RADIUS_METERS

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        RadiusSelectorSheetBody(
            state = RadiusSheetProps(selectedMeters, customMetersInput, attemptedGuestOverflow, isInputOutOfBounds, isGuest, maxAllowedMeters),
            onSetMeters = { meters, overflow ->
                selectedMeters = meters
                customMetersInput = meters.toString()
                attemptedGuestOverflow = overflow
            },
            onTypeDigits = { digits ->
                customMetersInput = digits
                digits.toIntOrNull()?.let { parsed ->
                    applyTypedMeters(parsed, isGuest) { meters, overflow ->
                        selectedMeters = meters
                        attemptedGuestOverflow = overflow
                    }
                }
            },
            onRequireAuth = {
                onDismiss()
                onRequireAuth()
            },
            onApply = {
                val sheetState = RadiusSheetProps(
                    selectedMeters,
                    customMetersInput,
                    attemptedGuestOverflow,
                    isInputOutOfBounds,
                    isGuest,
                    maxAllowedMeters
                )
                applyFinalRadiusSelection(sheetState, onRadiusSelected, onDismiss) {
                    onDismiss()
                    onRequireAuth()
                }
            }
        )
    }
}

@Composable
private fun RadiusSelectorSheetBody(
    state: RadiusSheetProps,
    onSetMeters: (Int, Boolean) -> Unit,
    onTypeDigits: (String) -> Unit,
    onRequireAuth: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 16.dp)
    ) {
        RadiusSheetHeader(isGuest = state.isGuest)
        if (state.isGuest || state.attemptedGuestOverflow) {
            Spacer(modifier = Modifier.height(10.dp))
            GuestRadiusLimitBanner(attemptedOverflow = state.attemptedGuestOverflow, onClick = onRequireAuth)
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color(DIVIDER_HEX))
        Spacer(modifier = Modifier.height(14.dp))
        RadiusStepperCard(
            selectedMeters = state.selectedMeters,
            isGuest = state.isGuest,
            onMetersUpdated = onSetMeters,
            onGuestOverflow = { onSetMeters(state.selectedMeters, true) }
        )
        Spacer(modifier = Modifier.height(14.dp))
        RadiusSliderSection(selectedMeters = state.selectedMeters, isGuest = state.isGuest, onValueSnap = onSetMeters)
        Spacer(modifier = Modifier.height(14.dp))
        ExactMetersInputField(input = state.customMetersInput, isOutOfBounds = state.isInputOutOfBounds, isGuest = state.isGuest, onValueChange = onTypeDigits)
        Spacer(modifier = Modifier.height(10.dp))
        QuickRadiusChipsRow(
            selectedMeters = state.selectedMeters,
            isGuest = state.isGuest,
            onSelectMeters = { meters -> onSetMeters(meters, false) },
            onLockedClick = onRequireAuth
        )
        Spacer(modifier = Modifier.height(18.dp))
        ApplyRadiusButton(selectedMeters = state.selectedMeters, enabled = !state.isInputOutOfBounds || state.customMetersInput.isBlank(), onClick = onApply)
    }
}

private fun applyFinalRadiusSelection(state: RadiusSheetProps, onRadiusSelected: (Float) -> Unit, onDismiss: () -> Unit, onRequireAuth: () -> Unit) {
    val typed = state.customMetersInput.toIntOrNull()
    if (state.isGuest && typed != null && typed > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
        onRequireAuth()
    } else {
        val finalMeters = (typed ?: state.selectedMeters).coerceIn(GuestRadiusPolicy.MIN_RADIUS_METERS, state.maxAllowedMeters)
        onRadiusSelected(finalMeters / METERS_PER_KM_FLOAT)
        onDismiss()
    }
}

@Composable
private fun RadiusSheetHeader(isGuest: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = RoundedCornerShape(10.dp), color = SensusPurple.copy(alpha = HEADER_ICON_ALPHA), modifier = Modifier.size(38.dp)) {
            Icon(imageVector = Icons.Default.NearMe, contentDescription = null, tint = SensusPurple, modifier = Modifier.padding(8.dp).size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "Distancia de búsqueda", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = if (isGuest) "Modo Invitado: de 50 m hasta 2 km" else "Personaliza desde 50 m hasta 5 km",
                fontSize = 12.sp,
                color = if (isGuest) SensusPurple else SensusTextSecondary
            )
        }
    }
}

@Composable
private fun GuestRadiusLimitBanner(attemptedOverflow: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SensusPurple.copy(alpha = GUEST_BANNER_BG_ALPHA),
        border = BorderStroke(1.dp, SensusPurple.copy(alpha = GUEST_BANNER_BORDER_ALPHA)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            val text = if (attemptedOverflow) {
                "Límite de 2 km en Modo Invitado. Toca aquí para iniciar sesión y desbloquear hasta 5 km."
            } else {
                "Regístrate o inicia sesión para ampliar tu cobertura hasta 5 km."
            }
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
        }
    }
}

@Composable
private fun RadiusStepperCard(selectedMeters: Int, isGuest: Boolean, onMetersUpdated: (Int, Boolean) -> Unit, onGuestOverflow: () -> Unit) {
    val canDecrement = selectedMeters > GuestRadiusPolicy.MIN_RADIUS_METERS
    val atGuestCeiling = isGuest && selectedMeters >= GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
    val canIncrement = selectedMeters < GuestRadiusPolicy.MAX_RADIUS_METERS

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(READOUT_BG_HEX),
        border = BorderStroke(1.dp, SensusPurple.copy(alpha = READOUT_BORDER_ALPHA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadiusStepCircleButton(Icons.Default.Remove, "Reducir 50 metros", canDecrement) {
                val next = (selectedMeters - STEP_METERS).coerceAtLeast(GuestRadiusPolicy.MIN_RADIUS_METERS)
                onMetersUpdated(next, false)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = formatRadiusText(selectedMeters / METERS_PER_KM_FLOAT), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = SensusPurple)
                Text(text = "$selectedMeters metros alrededor", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = SensusTextSecondary)
            }
            RadiusStepCircleButton(if (atGuestCeiling) Icons.Default.Lock else Icons.Default.Add, "Aumentar 50 metros", canIncrement) {
                val next = (selectedMeters + STEP_METERS).coerceAtMost(GuestRadiusPolicy.MAX_RADIUS_METERS)
                if (isGuest && next > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) onGuestOverflow() else onMetersUpdated(next, false)
            }
        }
    }
}

@Composable
private fun RadiusStepCircleButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(BORDER_LIGHT_HEX)),
        modifier = Modifier.size(40.dp).alpha(if (enabled) 1f else DISABLED_STEPPER_ALPHA).clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, tint = SensusPurple, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RadiusSliderSection(selectedMeters: Int, isGuest: Boolean, onValueSnap: (Int, Boolean) -> Unit) {
    Slider(
        value = selectedMeters.toFloat(),
        onValueChange = { raw ->
            val snapped = ((raw / STEP_METERS_FLOAT).roundToInt() * STEP_METERS)
                .coerceIn(GuestRadiusPolicy.MIN_RADIUS_METERS, GuestRadiusPolicy.MAX_RADIUS_METERS)
            if (isGuest && snapped > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
                onValueSnap(GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS, true)
            } else {
                onValueSnap(snapped, false)
            }
        },
        valueRange = GuestRadiusPolicy.MIN_RADIUS_METERS.toFloat()..GuestRadiusPolicy.MAX_RADIUS_METERS.toFloat(),
        colors = SliderDefaults.colors(
            thumbColor = SensusPurple,
            activeTrackColor = SensusPurple,
            inactiveTrackColor = SensusPurple.copy(alpha = SLIDER_INACTIVE_ALPHA)
        ),
        modifier = Modifier.fillMaxWidth()
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = "50 m", fontSize = 11.sp, color = SensusTextMuted, fontWeight = FontWeight.Medium)
        Text(
            text = if (isGuest) "2 km (Tope invitado)" else "2.5 km",
            fontSize = 11.sp,
            color = if (isGuest) SensusPurple else SensusTextMuted,
            fontWeight = if (isGuest) FontWeight.Bold else FontWeight.Medium
        )
        Text(text = "5 km", fontSize = 11.sp, color = SensusTextMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ExactMetersInputField(input: String, isOutOfBounds: Boolean, isGuest: Boolean, onValueChange: (String) -> Unit) {
    val showError = input.isNotEmpty() && isOutOfBounds
    val maxText = if (isGuest) "50 – 2000 m" else "50 – 5000 m"
    OutlinedTextField(
        value = input,
        onValueChange = { rawInput -> onValueChange(rawInput.filter(Char::isDigit).take(MAX_METER_DIGITS)) },
        label = { Text("Distancia exacta en metros ($maxText)") },
        suffix = { Text("m", color = SensusTextSecondary, fontWeight = FontWeight.Bold) },
        isError = showError,
        supportingText = if (showError) {
            { Text(text = "Ingresa un valor entre 50 m y 5,000 m", color = SensusRed, fontSize = 11.sp) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = sensusOutlinedTextFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun QuickRadiusChipsRow(selectedMeters: Int, isGuest: Boolean, onSelectMeters: (Int) -> Unit, onLockedClick: () -> Unit) {
    Text(text = "Accesos rápidos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        availableRadiusOptions.forEach { (km, label) ->
            val optionMeters = (km * METERS_PER_KM_FLOAT).roundToInt()
            val isLocked = isGuest && optionMeters > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
            val isSelected = !isLocked && selectedMeters == optionMeters
            QuickRadiusChip(label, isLocked, isSelected) { if (isLocked) onLockedClick() else onSelectMeters(optionMeters) }
        }
    }
}

@Composable
private fun QuickRadiusChip(label: String, isLocked: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val palette = resolveQuickChipPalette(isSelected = isSelected, isLocked = isLocked)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.containerColor,
        border = BorderStroke(width = 1.dp, color = palette.borderColor),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            if (isLocked) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Bloqueado", tint = SensusTextMuted, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(text = label, fontSize = 12.sp, fontWeight = palette.weight, color = palette.textColor)
        }
    }
}

private fun resolveQuickChipPalette(isSelected: Boolean, isLocked: Boolean): QuickChipPalette = when {
    isSelected -> QuickChipPalette(SensusPurple, SensusPurple, Color.White, FontWeight.Bold)
    isLocked -> QuickChipPalette(Color(DIVIDER_HEX), Color(BORDER_LIGHT_HEX), SensusTextMuted, FontWeight.Medium)
    else -> QuickChipPalette(Color.White, Color(BORDER_MEDIUM_HEX), SensusTextPrimary, FontWeight.Medium)
}

@Composable
private fun ApplyRadiusButton(selectedMeters: Int, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text(
            text = "Aplicar distancia (${formatRadiusText(selectedMeters / METERS_PER_KM_FLOAT)})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

private fun applyTypedMeters(parsed: Int, isGuest: Boolean, onResult: (Int, Boolean) -> Unit) {
    if (isGuest && parsed > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
        onResult(GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS, true)
    } else if (parsed in GuestRadiusPolicy.MIN_RADIUS_METERS..GuestRadiusPolicy.MAX_RADIUS_METERS) {
        onResult(parsed, false)
    }
}
