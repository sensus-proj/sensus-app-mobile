package com.example.sensus.presentation.root

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : MviViewModel<AppViewState, AppViewIntent, AppViewEffect>(AppViewState()) {

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                updateState {
                    copy(
                        isSessionActive = session.isSessionActive,
                        isGuest = session.isGuest,
                        hasAcceptedTerms = session.hasAcceptedTerms,
                        hasLocationPermission = session.hasLocationPermission,
                        hasCompletedSplash = session.hasCompletedSplash
                    )
                }
            }
        }
    }

    override fun processIntent(intent: AppViewIntent) {
        when (intent) {
            is AppViewIntent.DeepLinkReceived -> {
                updateState { copy(activeResetToken = intent.token, currentScreen = AppScreen.FORGOT_PASSWORD) }
            }
            is AppViewIntent.SplashFinished -> {
                viewModelScope.launch {
                    sessionRepository.setSplashCompleted(true)
                    val s = sessionRepository.sessionState.value
                    val nextScreen = if (s.isSessionActive) {
                        if (!s.hasAcceptedTerms && !s.isGuest) AppScreen.TERMS else AppScreen.MAIN
                    } else if (!s.hasLocationPermission) {
                        AppScreen.ONBOARDING
                    } else {
                        AppScreen.LOGIN
                    }
                    updateState { copy(currentScreen = nextScreen) }
                }
            }
            is AppViewIntent.LocationPermissionSet -> {
                viewModelScope.launch {
                    sessionRepository.setLocationPermission(intent.granted)
                    updateState { copy(currentScreen = AppScreen.LOGIN) }
                }
            }
            is AppViewIntent.TermsAccepted -> {
                viewModelScope.launch {
                    sessionRepository.setTermsAccepted(intent.accepted)
                    updateState { copy(currentScreen = AppScreen.MAIN) }
                }
            }
            is AppViewIntent.Logout -> {
                viewModelScope.launch {
                    sessionRepository.clearSession()
                    updateState { copy(currentScreen = AppScreen.LOGIN) }
                }
            }
            is AppViewIntent.NavigateTo -> {
                updateState { copy(currentScreen = intent.screen) }
            }
        }
    }
}
