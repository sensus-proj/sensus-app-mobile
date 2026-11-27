package com.example.sensus.domain.security

sealed interface DeviceAuthOutcome {
    data object Authenticated : DeviceAuthOutcome
    data object Cancelled : DeviceAuthOutcome
    data object Unavailable : DeviceAuthOutcome
    data class Error(val message: String) : DeviceAuthOutcome
}

data class DeviceAuthPrompt(
    val title: String,
    val subtitle: String
)

fun interface DeviceAuthenticator {
    suspend fun authenticate(prompt: DeviceAuthPrompt): DeviceAuthOutcome
}

fun DeviceAuthOutcome.userFacingErrorMessage(): String? = when (this) {
    DeviceAuthOutcome.Authenticated -> null
    DeviceAuthOutcome.Cancelled -> "Verificación cancelada. Autentícate para continuar."
    DeviceAuthOutcome.Unavailable -> "Configura un bloqueo biométrico o PIN en tu dispositivo para continuar."
    is DeviceAuthOutcome.Error -> message.ifBlank { "No fue posible verificar tu identidad en el dispositivo." }
}
