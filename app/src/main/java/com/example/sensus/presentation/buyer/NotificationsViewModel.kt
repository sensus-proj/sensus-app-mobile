package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.NotificationsPushDeviceProvider
import com.example.sensus.domain.usecase.NotificationsGetSettingsUseCase
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
    private val registerDevice: NotificationsRegisterDeviceUseCase
) : MviViewModel<NotificationsViewState, NotificationsViewIntent, NotificationsViewEffect>(NotificationsViewState()) {
    init {
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
                    copy(isRegistering = false, error = result.error.toNotificationError(device = true))
                }
            }
        }
    }
}

private fun AppError.toNotificationError(device: Boolean = false): NotificationsError = when (this) {
    is AppError.Network -> NotificationsError.NETWORK
    is AppError.Unauthorized, is AppError.Forbidden -> NotificationsError.SESSION
    is AppError.NotFound -> if (device && message == NotificationsPushDeviceProvider.CONFIGURATION_UNAVAILABLE) {
        NotificationsError.FIREBASE
    } else {
        NotificationsError.UNAVAILABLE
    }
    is AppError.Validation -> NotificationsError.VALIDATION
    else -> NotificationsError.GENERIC
}
