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
    val activeResetToken: String? = null
) : ViewState

sealed interface AppViewIntent : ViewIntent {
    data class DeepLinkReceived(val token: String) : AppViewIntent
    data object SplashFinished : AppViewIntent
    data class LocationPermissionSet(val granted: Boolean) : AppViewIntent
    data class TermsAccepted(val accepted: Boolean) : AppViewIntent
    data object Logout : AppViewIntent
    data class NavigateTo(val screen: AppScreen) : AppViewIntent
}

sealed interface AppViewEffect : ViewEffect
