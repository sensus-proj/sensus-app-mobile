package com.example.sensus.presentation.root

import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    LOGIN,
    REGISTER,
    OTP,
    TERMS,
    INTERESTS_ONBOARDING,
    FORGOT_PASSWORD,
    MAIN
}

data class AppViewState(
    val currentScreen: AppScreen = AppScreen.SPLASH,
    val isSessionActive: Boolean = false,
    val isGuest: Boolean = false,
    val hasAcceptedTerms: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasCompletedSplash: Boolean = false,
    val isMerchantRegistered: Boolean = false,
    val pendingInterestsOnboarding: Boolean = false,
    val activeResetToken: String? = null,
    val friendInviteSignal: Int = 0,
    val isOnline: Boolean = true,
    val pendingSyncCount: Int = 0
) : ViewState

sealed interface AppViewIntent : ViewIntent {
    data class DeepLinkReceived(val token: String) : AppViewIntent
    data class FriendInviteDeepLinkReceived(val inviteUrl: String) : AppViewIntent
    data object SplashFinished : AppViewIntent
    data class LocationPermissionSet(val granted: Boolean) : AppViewIntent
    data class TermsAccepted(val accepted: Boolean) : AppViewIntent
    data object RegistrationCompleted : AppViewIntent
    data object InterestsOnboardingCompleted : AppViewIntent
    data object Logout : AppViewIntent
    data class NavigateTo(val screen: AppScreen) : AppViewIntent
}

sealed interface AppViewEffect : ViewEffect {
    data class ShowNetworkSnackbar(val message: String) : AppViewEffect
}
