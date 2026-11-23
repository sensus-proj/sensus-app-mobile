package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.domain.usecase.NotificationsRegisterDeviceUseCase
import com.example.sensus.domain.usecase.NotificationsUpdateSettingsUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val getSettings: NotificationsGetSettingsUseCase,
    private val updateSettings: NotificationsUpdateSettingsUseCase,
    private val registerDevice: NotificationsRegisterDeviceUseCase,
    private val location: NotificationsLocationUseCase
) : MviViewModel<NotificationsViewState, NotificationsViewIntent, NotificationsViewEffect>(NotificationsViewState()) {
    init {
        updateState { copy(locationSharingEnabled = location.isEnabled()) }
        viewModelScope.launch {
            getSettings.sessionChanges.drop(1).collect {
                updateState { NotificationsViewState(isLoading = false, error = NotificationsError.SESSION) }
            }
        }
        load()
    }

    override fun processIntent(intent: NotificationsViewIntent) {
        viewModelScope.launch { handleIntent(intent) }
    }

    private fun handleIntent(intent: NotificationsViewIntent) {
        val state = viewState.value
        when (intent) {
            NotificationsViewIntent.Retry -> if (!state.isLoading && !state.isSaving) load()
            is NotificationsViewIntent.Edit -> edit(intent)
            NotificationsViewIntent.Save -> save()
            NotificationsViewIntent.ActivateDevice -> activate()
            NotificationsViewIntent.PermissionDenied -> updateState { copy(error = NotificationsError.PERMISSION) }
            is NotificationsLocationIntent -> handleLocationIntent(intent)
        }
    }

    private fun handleLocationIntent(intent: NotificationsLocationIntent) {
        when (intent) {
            is NotificationsLocationIntent.Toggled -> if (intent.enabled) {
                updateState { copy(showLocationConsent = true, error = null) }
            } else {
                setLocationSharing(false)
            }
            is NotificationsLocationIntent.ConsentAnswered -> {
                updateState { copy(showLocationConsent = false) }
                if (intent.accepted) emitEffect(NotificationsViewEffect.RequestLocationPermission)
            }
            is NotificationsLocationIntent.PermissionResult -> if (intent.granted) {
                setLocationSharing(true)
            } else {
                updateState { copy(error = NotificationsError.LOCATION_PERMISSION) }
            }
        }
    }

    private fun edit(intent: NotificationsViewIntent.Edit) {
        val state = viewState.value
        if (state.preferences == null || state.isSaving || state.isLoading) return
        updateState { copy(preferences = intent.preferences, error = null) }
    }

    private fun load() {
        updateState { copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = getSettings()) {
                is Result.Success -> updateState {
                    copy(preferences = result.data, savedPreferences = result.data, isLoading = false)
                }
                is Result.Failure -> updateState { copy(isLoading = false, error = result.error.toNotificationError()) }
            }
        }
    }

    private fun save() {
        val state = viewState.value
        val preferences = state.preferences ?: return
        if (!state.canSave) return
        updateState { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val result = updateSettings(preferences)) {
                is Result.Success -> {
                    updateState { copy(preferences = result.data, savedPreferences = result.data, isSaving = false) }
                    emitEffect(NotificationsViewEffect.Saved)
                }
                is Result.Failure -> updateState { copy(isSaving = false, error = result.error.toNotificationError()) }
            }
        }
    }

    private fun activate() {
        if (viewState.value.isRegistering) return
        updateState { copy(isRegistering = true, error = null) }
        viewModelScope.launch {
            when (val result = registerDevice()) {
                is Result.Success -> {
                    updateState { copy(isRegistering = false) }
                    emitEffect(NotificationsViewEffect.DeviceRegistered)
                }
                is Result.Failure -> updateState {
                    copy(isRegistering = false, error = result.error.toDeviceNotificationError())
                }
            }
        }
    }

    private fun setLocationSharing(enabled: Boolean) {
        if (viewState.value.isUpdatingLocation) return
        updateState { copy(isUpdatingLocation = true, error = null) }
        viewModelScope.launch {
            val result = if (enabled) location.enable() else location.disable()
            updateState {
                copy(
                    isUpdatingLocation = false,
                    locationSharingEnabled = location.isEnabled(),
                    error = (result as? Result.Failure)?.error?.toLocationError()
                )
            }
        }
    }
}

private fun AppError.toLocationError(): NotificationsError = when {
    this is AppError.Forbidden && message == NotificationsLocationUseCase.PERMISSION_REQUIRED -> NotificationsError.LOCATION_PERMISSION
    this is AppError.NotFound && message == NotificationsLocationUseCase.LOCATION_UNAVAILABLE -> NotificationsError.LOCATION_UNAVAILABLE
    else -> toNotificationError()
}

private fun AppError.toDeviceNotificationError(): NotificationsError = when {
    this is AppError.Forbidden && message == NotificationsRegisterDeviceUseCase.PERMISSION_REQUIRED -> NotificationsError.PERMISSION
    this is AppError.NotFound && message == NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE -> NotificationsError.FIREBASE
    else -> toNotificationError()
}

private fun AppError.toNotificationError(): NotificationsError = when (this) {
    is AppError.Network -> NotificationsError.NETWORK
    is AppError.Unauthorized, is AppError.Forbidden -> NotificationsError.SESSION
    is AppError.NotFound -> NotificationsError.UNAVAILABLE
    is AppError.Validation -> NotificationsError.VALIDATION
    else -> NotificationsError.GENERIC
}
