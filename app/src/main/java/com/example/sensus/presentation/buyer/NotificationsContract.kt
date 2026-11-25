package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class NotificationsViewState(
    val preferences: NotificationsPreferences? = null,
    val savedPreferences: NotificationsPreferences? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isRegistering: Boolean = false,
    val locationSharingEnabled: Boolean = false,
    val isUpdatingLocation: Boolean = false,
    val showLocationConsent: Boolean = false,
    val error: NotificationsError? = null
) : ViewState {
    val canSave: Boolean get() = preferences != null && preferences != savedPreferences && !isLoading && !isSaving
}

enum class NotificationsError { NETWORK, SESSION, UNAVAILABLE, VALIDATION, GENERIC, FIREBASE, PERMISSION, LOCATION_PERMISSION, LOCATION_UNAVAILABLE }

sealed interface NotificationsViewIntent : ViewIntent {
    data object Retry : NotificationsViewIntent
    data class Edit(val preferences: NotificationsPreferences) : NotificationsViewIntent
    data object Save : NotificationsViewIntent
    data object ActivateDevice : NotificationsViewIntent
    data object PermissionDenied : NotificationsViewIntent
}

/** FT-39: interruptor «Ofertas nuevas cerca de mí»: consentimiento explícito y permiso antes de compartir. */
sealed interface NotificationsLocationIntent : NotificationsViewIntent {
    data class Toggled(val enabled: Boolean) : NotificationsLocationIntent
    data class ConsentAnswered(val accepted: Boolean) : NotificationsLocationIntent
    data class PermissionResult(val granted: Boolean) : NotificationsLocationIntent
}

sealed interface NotificationsViewEffect : ViewEffect {
    data object Saved : NotificationsViewEffect
    data object DeviceRegistered : NotificationsViewEffect
    data object RequestLocationPermission : NotificationsViewEffect
}
