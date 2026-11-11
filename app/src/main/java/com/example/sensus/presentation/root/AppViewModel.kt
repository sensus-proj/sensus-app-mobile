package com.example.sensus.presentation.root

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase
) : MviViewModel<AppViewState, AppViewIntent, AppViewEffect>(AppViewState()) {

    init {
        viewModelScope.launch {
            getPreferredMapLayerUseCase()
        }
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                updateState { applySessionUpdate(session) }
            }
        }
    }

    override fun processIntent(intent: AppViewIntent) {
        when (intent) {
            is AppViewIntent.DeepLinkReceived -> {
                updateState { copy(activeResetToken = intent.token, currentScreen = AppScreen.FORGOT_PASSWORD) }
            }
            is AppViewIntent.SplashFinished -> handleSplashFinished()
            is AppViewIntent.LocationPermissionSet -> handleLocationPermissionSet(intent.granted)
            is AppViewIntent.TermsAccepted -> handleTermsAccepted(intent.accepted)
            is AppViewIntent.Logout -> handleLogout()
            is AppViewIntent.NavigateTo -> updateState { copy(currentScreen = intent.screen) }
        }
    }

    private fun handleSplashFinished() {
        viewModelScope.launch {
            sessionRepository.syncSession()
            sessionRepository.setSplashCompleted(true)
            val nextScreen = resolveSplashDestination(sessionRepository.sessionState.value)
            updateState { copy(currentScreen = nextScreen) }
        }
    }

    private fun handleLocationPermissionSet(granted: Boolean) {
        viewModelScope.launch {
            sessionRepository.setLocationPermission(granted)
            updateState { copy(currentScreen = AppScreen.LOGIN) }
        }
    }

    private fun handleTermsAccepted(accepted: Boolean) {
        viewModelScope.launch {
            sessionRepository.setTermsAccepted(accepted)
            updateState { copy(currentScreen = AppScreen.MAIN) }
        }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            logoutUseCase()
            updateState { copy(currentScreen = AppScreen.LOGIN) }
        }
    }
}

private fun AppViewState.applySessionUpdate(session: SessionState): AppViewState {
    val sessionDroppedInMain = currentScreen == AppScreen.MAIN && !session.isSessionActive
    return copy(
        currentScreen = if (sessionDroppedInMain) AppScreen.LOGIN else currentScreen,
        isSessionActive = session.isSessionActive,
        isGuest = session.isGuest,
        hasAcceptedTerms = session.hasAcceptedTerms,
        hasLocationPermission = session.hasLocationPermission,
        hasCompletedSplash = session.hasCompletedSplash
    )
}

private fun resolveSplashDestination(session: SessionState): AppScreen = when {
    session.isSessionActive && !session.hasAcceptedTerms && !session.isGuest -> AppScreen.TERMS
    session.isSessionActive -> AppScreen.MAIN
    !session.hasLocationPermission -> AppScreen.ONBOARDING
    else -> AppScreen.LOGIN
}
