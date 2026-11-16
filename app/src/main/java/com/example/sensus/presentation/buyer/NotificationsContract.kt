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
    val error: NotificationsError? = null
) : ViewState {
    val canSave: Boolean get() = preferences != null && preferences != savedPreferences && !isLoading && !isSaving
}

enum class NotificationsError { NETWORK, SESSION, UNAVAILABLE, VALIDATION, GENERIC, FIREBASE, PERMISSION }

sealed interface NotificationsViewIntent : ViewIntent {
    data object Retry : NotificationsViewIntent
    data class Edit(val preferences: NotificationsPreferences) : NotificationsViewIntent
    data object Save : NotificationsViewIntent
    data object ActivateDevice : NotificationsViewIntent
    data object PermissionDenied : NotificationsViewIntent
}

sealed interface NotificationsViewEffect : ViewEffect {
    data object Saved : NotificationsViewEffect
    data object DeviceRegistered : NotificationsViewEffect
}
