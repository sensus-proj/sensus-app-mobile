package com.example.sensus.presentation.root

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.network.NetworkMonitor
import com.example.sensus.domain.repository.FriendsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.sync.SyncManager
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.LogoutUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NETWORK_DEBOUNCE_MILLIS = 300L

@OptIn(FlowPreview::class)
@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val logoutUseCase: LogoutUseCase,
    private val getPreferredMapLayerUseCase: GetPreferredMapLayerUseCase,
    private val friendsRepository: Lazy<FriendsRepository>? = null,
    private val networkMonitor: NetworkMonitor? = null,
    private val syncManager: SyncManager? = null
) : MviViewModel<AppViewState, AppViewIntent, AppViewEffect>(AppViewState()) {

    internal var ioDispatcher = Dispatchers.IO
    private var previousOnlineStatus: Boolean? = null

    init {
        viewModelScope.launch {
            getPreferredMapLayerUseCase()
        }
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                updateState { applySessionUpdate(session) }
            }
        }
        observeNetworkStatus()
        observeSyncStatus()
    }

    private fun observeNetworkStatus() {
        val monitor = networkMonitor ?: return
        viewModelScope.launch {
            monitor.isOnline
                .distinctUntilChanged()
                .debounce(NETWORK_DEBOUNCE_MILLIS)
                .collect { isOnline ->
                    val prev = previousOnlineStatus
                    previousOnlineStatus = isOnline
                    updateState { copy(isOnline = isOnline) }

                    if (prev != null) {
                        if (prev && !isOnline) {
                            emitEffect(
                                AppViewEffect.ShowNetworkSnackbar(
                                    "Sin conexión a internet. Mostrando datos guardados."
                                )
                            )
                        } else if (!prev && isOnline) {
                            emitEffect(
                                AppViewEffect.ShowNetworkSnackbar(
                                    "Conexión restablecida."
                                )
                            )
                            syncManager?.syncPendingOperations()
                        }
                    }
                }
        }
    }

    private fun observeSyncStatus() {
        val sync = syncManager ?: return
        viewModelScope.launch {
            sync.pendingOperationsCount.collect { count ->
                updateState { copy(pendingSyncCount = count) }
            }
        }
    }

    override fun processIntent(intent: AppViewIntent) {
        when (intent) {
            is AppViewIntent.DeepLinkReceived -> updateState { copy(activeResetToken = intent.token, currentScreen = AppScreen.FORGOT_PASSWORD) }
            is AppViewIntent.FriendInviteDeepLinkReceived -> handleFriendInviteDeepLink(intent.inviteUrl)
            is AppViewIntent.SplashFinished -> handleSplashFinished()
            is AppViewIntent.LocationPermissionSet -> handleLocationPermissionSet(intent.granted)
            is AppViewIntent.TermsAccepted -> handleTermsAccepted(intent.accepted)
            is AppViewIntent.Logout -> handleLogout()
            else -> handleNavigationFlowIntent(intent)
        }
    }

    private fun handleNavigationFlowIntent(intent: AppViewIntent) {
        when (intent) {
            AppViewIntent.RegistrationCompleted -> handleRegistrationCompleted()
            AppViewIntent.InterestsOnboardingCompleted -> updateState {
                copy(pendingInterestsOnboarding = false, currentScreen = AppScreen.MAIN)
            }
            is AppViewIntent.NavigateTo -> updateState { copy(currentScreen = intent.screen) }
            else -> Unit
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
            val target = if (viewState.value.pendingInterestsOnboarding) {
                AppScreen.INTERESTS_ONBOARDING
            } else {
                AppScreen.MAIN
            }
            updateState { copy(currentScreen = target) }
        }
    }

    private fun handleRegistrationCompleted() {
        val next = if (viewState.value.hasAcceptedTerms) {
            AppScreen.INTERESTS_ONBOARDING
        } else {
            AppScreen.TERMS
        }
        updateState { copy(pendingInterestsOnboarding = true, currentScreen = next) }
    }

    private fun handleLogout() {
        viewModelScope.launch {
            logoutUseCase()
            updateState { copy(currentScreen = AppScreen.LOGIN) }
        }
    }

    private fun handleFriendInviteDeepLink(inviteUrl: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                friendsRepository?.get()?.queuePendingInvite(inviteUrl)
            }
            val session = sessionRepository.sessionState.value
            updateState {
                copy(
                    friendInviteSignal = friendInviteSignal + 1,
                    currentScreen = if (session.isSessionActive && !session.isGuest && session.hasAcceptedTerms) {
                        AppScreen.MAIN
                    } else {
                        currentScreen
                    }
                )
            }
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
        hasCompletedSplash = session.hasCompletedSplash,
        isMerchantRegistered = session.isMerchantRegistered
    )
}

private fun resolveSplashDestination(session: SessionState): AppScreen = when {
    session.isSessionActive && !session.hasAcceptedTerms && !session.isGuest -> AppScreen.TERMS
    session.isSessionActive -> AppScreen.MAIN
    !session.hasLocationPermission -> AppScreen.ONBOARDING
    else -> AppScreen.LOGIN
}
