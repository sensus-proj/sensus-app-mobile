package com.example.sensus.presentation.modals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.sensus.domain.security.DeviceAuthOutcome
import com.example.sensus.domain.security.DeviceAuthPrompt
import com.example.sensus.domain.security.runDeviceAuthentication
import com.example.sensus.domain.security.userFacingErrorMessage
import com.example.sensus.presentation.common.SecureScreenEffect
import com.example.sensus.presentation.common.currentDeviceAuthenticator
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.StatusRed
import com.example.sensus.theme.sensusOutlinedButtonColors
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import com.example.sensus.theme.sensusTextButtonColors
import kotlinx.coroutines.launch

private const val ICON_BG_ALPHA = 0.12f
private const val OFFLINE_SLATE_HEX = 0xFF64748B
private const val OFFLINE_BG_ALPHA = 0.15f

@Composable
fun ConfirmLogoutDialog(onDismiss: () -> Unit, onConfirmLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = SensusSurface,
        icon = { DialogIconBadge(Icons.AutoMirrored.Filled.Logout, SensusPurple, SensusPurple.copy(alpha = ICON_BG_ALPHA)) },
        title = {
            Text(text = "¿Deseas cerrar sesión?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        },
        text = {
            Text(
                text = "Podrás volver a ingresar en cualquier momento con tus credenciales registradas.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onConfirmLogout, shape = SensusButtonShape, colors = ButtonDefaults.buttonColors(containerColor = StatusRed)) {
                Text("Cerrar sesión", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(colors = sensusTextButtonColors(), onClick = onDismiss) { Text("Cancelar", color = SensusTextSecondary) }
        }
    )
}

@Composable
fun ConfirmDeleteAccountDialog(onDismiss: () -> Unit, onConfirmDeleteWithPassword: (password: String) -> Unit, onConfirmDelete: (() -> Unit)? = null) {
    SecureScreenEffect()
    val authenticator = currentDeviceAuthenticator()
    val scope = rememberCoroutineScope()
    var passwordConfirmation by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val executeDelete = {
        onConfirmDeleteWithPassword(passwordConfirmation)
        onConfirmDelete?.invoke()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        shape = RoundedCornerShape(18.dp),
        containerColor = SensusSurface,
        icon = { DialogIconBadge(Icons.Default.DeleteForever, SensusRed, SensusRed.copy(alpha = ICON_BG_ALPHA)) },
        title = { Text("¿Eliminar cuenta definitivamente?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary) },
        text = { DeleteAccountDialogBody(passwordConfirmation, { passwordConfirmation = it }, authError) },
        confirmButton = {
            DeleteAccountConfirmButton(
                enabled = passwordConfirmation.isNotBlank() && !isAuthenticating,
                onClick = {
                    if (authenticator == null) {
                        executeDelete()
                    } else {
                        scope.launch {
                            isAuthenticating = true
                            val prompt = DeviceAuthPrompt(
                                title = "Confirmar eliminación de cuenta",
                                subtitle = "Verifica tu identidad para eliminar definitivamente tu cuenta"
                            )
                            val outcome = runDeviceAuthentication(authenticator, prompt)
                            isAuthenticating = false
                            if (outcome == DeviceAuthOutcome.Authenticated) {
                                authError = null
                                executeDelete()
                            } else {
                                authError = outcome.userFacingErrorMessage()
                            }
                        }
                    }
                }
            )
        },
        dismissButton = {
            TextButton(colors = sensusTextButtonColors(), onClick = onDismiss) { Text("Cancelar", color = SensusTextSecondary) }
        }
    )
}

@Composable
private fun DeleteAccountConfirmButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
    ) {
        Text("Eliminar definitivamente", fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun DeleteAccountDialogBody(passwordConfirmation: String, onPasswordChanged: (String) -> Unit, authError: String? = null) {
    Column {
        Text(
            text = "Esta acción es irreversible. Se eliminarán de forma permanente tus ahorros " +
                "acumulados, historial de apartados y nivel de usuario.",
            fontSize = 13.sp,
            color = SensusTextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ingresa tu contraseña para confirmar:",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = passwordConfirmation,
            onValueChange = onPasswordChanged,
            placeholder = { Text("Contraseña actual", fontSize = 13.sp) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = sensusOutlinedTextFieldColors(),
            singleLine = true
        )
        if (!authError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = authError, fontSize = 12.sp, color = SensusRed)
        }
    }
}

@Composable
fun ConfirmDeleteAccountDialog(onDismiss: () -> Unit, onConfirmDelete: () -> Unit) {
    ConfirmDeleteAccountDialog(
        onDismiss = onDismiss,
        onConfirmDeleteWithPassword = {},
        onConfirmDelete = onConfirmDelete
    )
}

@Composable
fun GuestPromptDialog(onDismiss: () -> Unit, onNavigateToLogin: () -> Unit, onNavigateToRegister: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = SensusSurface,
        icon = { DialogIconBadge(Icons.Default.Person, SensusPurple, SensusPurple.copy(alpha = ICON_BG_ALPHA)) },
        title = {
            Text(text = "Inicia sesión para continuar", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        },
        text = {
            Text(
                text = "Estás navegando en Modo Invitado (2 km). Para apartar ofertas, ampliar tu radio, " +
                    "publicar productos o guardar favoritos necesitas una cuenta Sensus.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onNavigateToLogin, shape = SensusButtonShape, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
                Text("Iniciar Sesión", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(colors = sensusOutlinedButtonColors(), onClick = onNavigateToRegister, shape = SensusButtonShape) {
                Text("Registrarme", color = SensusVioletInk)
            }
        }
    )
}

@Composable
fun OfflineDialog(onDismiss: () -> Unit) {
    val slateColor = Color(OFFLINE_SLATE_HEX)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(18.dp),
        containerColor = SensusSurface,
        icon = { DialogIconBadge(Icons.Default.WifiOff, slateColor, slateColor.copy(alpha = OFFLINE_BG_ALPHA)) },
        title = {
            Text(text = "Sin conexión a internet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        },
        text = {
            Text(
                text = "Estás navegando en modo fuera de línea. Mostrando ofertas y mapas guardados " +
                    "en la memoria caché de tu dispositivo.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = SensusButtonShape, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
                Text("Entendido", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

@Composable
private fun DialogIconBadge(icon: ImageVector, tint: Color, containerColor: Color) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}
