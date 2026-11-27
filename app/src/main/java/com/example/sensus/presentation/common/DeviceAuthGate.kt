package com.example.sensus.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensus.domain.security.DeviceAuthOutcome
import com.example.sensus.domain.security.DeviceAuthPrompt
import com.example.sensus.domain.security.DeviceAuthenticator
import com.example.sensus.domain.security.runDeviceAuthentication
import com.example.sensus.domain.security.userFacingErrorMessage
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusRed
import kotlinx.coroutines.launch

val LocalDeviceAuthenticator = staticCompositionLocalOf<DeviceAuthenticator?> { null }

@Composable
fun currentDeviceAuthenticator(): DeviceAuthenticator? = LocalDeviceAuthenticator.current ?: rememberDeviceAuthenticator()

@Composable
fun SensitiveQrGate(unlockKey: String, deviceAuthenticator: DeviceAuthenticator?, modifier: Modifier = Modifier, unlockedContent: @Composable () -> Unit) {
    if (deviceAuthenticator == null) {
        unlockedContent()
        return
    }
    var unlocked by rememberSaveable(unlockKey) { mutableStateOf(false) }
    var authError by rememberSaveable(unlockKey) { mutableStateOf<String?>(null) }
    var isAuthenticating by rememberSaveable(unlockKey) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (unlocked) {
        unlockedContent()
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        authError?.let { message ->
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = SensusRed)
        }
        Button(
            shape = SensusButtonShape,
            enabled = !isAuthenticating,
            onClick = {
                scope.launch {
                    isAuthenticating = true
                    val outcome = runDeviceAuthentication(
                        authenticator = deviceAuthenticator,
                        prompt = DeviceAuthPrompt(
                            title = "Confirma tu identidad",
                            subtitle = "Verifica tu biometría o PIN del dispositivo para mostrar tu código QR"
                        )
                    )
                    isAuthenticating = false
                    if (outcome == DeviceAuthOutcome.Authenticated) {
                        unlocked = true
                        authError = null
                    } else {
                        authError = outcome.userFacingErrorMessage()
                    }
                }
            }
        ) {
            Text("Mostrar código QR")
        }
    }
}
