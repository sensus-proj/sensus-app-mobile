package com.example.sensus.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

val availableRadiusOptions = listOf(
    0.05f to "50 m",
    0.1f to "100 m",
    0.25f to "250 m",
    0.5f to "500 m",
    1.0f to "1 km",
    2.0f to "2 km",
    3.5f to "3.5 km",
    5.0f to "5 km"
)

fun formatRadiusText(radiusKm: Float): String {
    val meters = (radiusKm * 1000f).roundToInt()
    return if (meters < 1000) {
        "$meters m"
    } else {
        val km = meters / 1000.0
        val raw = String.format(Locale.US, "%.2f", km)
            .trimEnd('0')
            .trimEnd('.')
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxAllowedMeters = if (isGuest) GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS else GuestRadiusPolicy.MAX_RADIUS_METERS
    val initialMeters = (GuestRadiusPolicy.clampRadius(currentRadiusKm, isGuest) * 1000f)
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
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
            // Encabezado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SensusPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        tint = SensusPurple,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Distancia de búsqueda",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Text(
                        text = if (isGuest) {
                            "Modo Invitado: de 50 m hasta 2 km"
                        } else {
                            "Personaliza desde 50 m hasta 5 km"
                        },
                        fontSize = 12.sp,
                        color = if (isGuest) SensusPurple else SensusTextSecondary
                    )
                }
            }

            if (isGuest || attemptedGuestOverflow) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SensusPurple.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, SensusPurple.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onRequireAuth()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SensusPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attemptedGuestOverflow) {
                                "Límite de 2 km en Modo Invitado. Toca aquí para iniciar sesión y desbloquear hasta 5 km."
                            } else {
                                "Regístrate o inicia sesión para ampliar tu cobertura hasta 5 km."
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusPurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta central con lectura en vivo y botones finos (- 50 m / + 50 m)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF8F7FF),
                border = BorderStroke(1.dp, SensusPurple.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canDecrement = selectedMeters > GuestRadiusPolicy.MIN_RADIUS_METERS
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (canDecrement) 1f else 0.4f)
                            .clickable(enabled = canDecrement) {
                                val next = (selectedMeters - 50).coerceAtLeast(GuestRadiusPolicy.MIN_RADIUS_METERS)
                                selectedMeters = next
                                customMetersInput = next.toString()
                                attemptedGuestOverflow = false
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Reducir 50 metros",
                                tint = SensusPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatRadiusText(selectedMeters / 1000f),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SensusPurple
                        )
                        Text(
                            text = "$selectedMeters metros alrededor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SensusTextSecondary
                        )
                    }

                    val atGuestCeiling = isGuest && selectedMeters >= GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
                    val canIncrement = selectedMeters < GuestRadiusPolicy.MAX_RADIUS_METERS
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 2.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .size(40.dp)
                            .alpha(if (canIncrement) 1f else 0.4f)
                            .clickable(enabled = canIncrement) {
                                val next = (selectedMeters + 50).coerceAtMost(GuestRadiusPolicy.MAX_RADIUS_METERS)
                                if (isGuest && next > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
                                    attemptedGuestOverflow = true
                                } else {
                                    selectedMeters = next
                                    customMetersInput = next.toString()
                                    attemptedGuestOverflow = false
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (atGuestCeiling) Icons.Default.Lock else Icons.Default.Add,
                                contentDescription = "Aumentar 50 metros",
                                tint = SensusPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Control deslizante continuo de 50 m a 5,000 m (con ajuste cada 50 m)
            Slider(
                value = selectedMeters.toFloat(),
                onValueChange = { raw ->
                    val snapped = ((raw / 50f).roundToInt() * 50).coerceIn(
                        GuestRadiusPolicy.MIN_RADIUS_METERS,
                        GuestRadiusPolicy.MAX_RADIUS_METERS
                    )
                    if (isGuest && snapped > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
                        selectedMeters = GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
                        customMetersInput = GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS.toString()
                        attemptedGuestOverflow = true
                    } else {
                        selectedMeters = snapped
                        customMetersInput = snapped.toString()
                        attemptedGuestOverflow = false
                    }
                },
                valueRange = GuestRadiusPolicy.MIN_RADIUS_METERS.toFloat()..GuestRadiusPolicy.MAX_RADIUS_METERS.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = SensusPurple,
                    activeTrackColor = SensusPurple,
                    inactiveTrackColor = SensusPurple.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "50 m", fontSize = 11.sp, color = SensusTextMuted, fontWeight = FontWeight.Medium)
                Text(
                    text = if (isGuest) "2 km (Tope invitado)" else "2.5 km",
                    fontSize = 11.sp,
                    color = if (isGuest) SensusPurple else SensusTextMuted,
                    fontWeight = if (isGuest) FontWeight.Bold else FontWeight.Medium
                )
                Text(text = "5 km", fontSize = 11.sp, color = SensusTextMuted, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Entrada numérica exacta personalizada en metros
            OutlinedTextField(
                value = customMetersInput,
                onValueChange = { rawInput ->
                    val digitsOnly = rawInput.filter { it.isDigit() }.take(4)
                    customMetersInput = digitsOnly
                    val parsed = digitsOnly.toIntOrNull()
                    if (parsed != null) {
                        if (isGuest && parsed > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
                            selectedMeters = GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
                            attemptedGuestOverflow = true
                        } else if (parsed in GuestRadiusPolicy.MIN_RADIUS_METERS..GuestRadiusPolicy.MAX_RADIUS_METERS) {
                            selectedMeters = parsed
                            attemptedGuestOverflow = false
                        }
                    }
                },
                label = { Text("Distancia exacta en metros (50 – 5000 m)") },
                suffix = { Text("m", color = SensusTextSecondary, fontWeight = FontWeight.Bold) },
                isError = customMetersInput.isNotEmpty() && isInputOutOfBounds,
                supportingText = if (customMetersInput.isNotEmpty() && isInputOutOfBounds) {
                    {
                        Text(
                            text = "Ingresa un valor entre 50 m y 5,000 m",
                            color = SensusRed,
                            fontSize = 11.sp
                        )
                    }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = sensusOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Accesos rápidos",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips de selección rápida (incluyendo distancias cortas 50m, 100m, 250m)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableRadiusOptions.forEach { (km, label) ->
                    val optionMeters = (km * 1000f).roundToInt()
                    val isLocked = isGuest && optionMeters > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS
                    val isSelected = !isLocked && selectedMeters == optionMeters

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isSelected -> SensusPurple
                            isLocked -> Color(0xFFF1F5F9)
                            else -> Color.White
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = when {
                                isSelected -> SensusPurple
                                isLocked -> Color(0xFFE2E8F0)
                                else -> Color(0xFFCBD5E1)
                            }
                        ),
                        modifier = Modifier.clickable {
                            if (isLocked) {
                                onDismiss()
                                onRequireAuth()
                            } else {
                                selectedMeters = optionMeters
                                customMetersInput = optionMeters.toString()
                                attemptedGuestOverflow = false
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Bloqueado",
                                    tint = SensusTextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> Color.White
                                    isLocked -> SensusTextMuted
                                    else -> SensusTextPrimary
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val typed = customMetersInput.toIntOrNull()
                    if (isGuest && typed != null && typed > GuestRadiusPolicy.GUEST_MAX_RADIUS_METERS) {
                        onDismiss()
                        onRequireAuth()
                    } else {
                        val finalMeters = (typed ?: selectedMeters).coerceIn(
                            GuestRadiusPolicy.MIN_RADIUS_METERS,
                            maxAllowedMeters
                        )
                        onRadiusSelected(finalMeters / 1000f)
                        onDismiss()
                    }
                },
                enabled = !isInputOutOfBounds || customMetersInput.isBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Aplicar distancia (${formatRadiusText(selectedMeters / 1000f)})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
