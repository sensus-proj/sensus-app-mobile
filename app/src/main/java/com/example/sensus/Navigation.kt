package com.example.sensus

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.sensus.presentation.auth.LoginCallbacks
import com.example.sensus.presentation.auth.LoginScreen
import com.example.sensus.presentation.auth.OnboardingLocationScreen
import com.example.sensus.presentation.auth.OtpVerificationScreen
import com.example.sensus.presentation.auth.RegisterScreen
import com.example.sensus.presentation.auth.TermsScreen
import com.example.sensus.presentation.main.MainAppContainer
import com.example.sensus.presentation.modals.ForgotPasswordScreen
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import com.example.sensus.presentation.root.AppViewState
import com.example.sensus.presentation.splash.SplashScreen

private const val TRANSITION_MILLIS = 350

private val rootRoutes = setOf(
    SensusRoute.Splash,
    SensusRoute.Onboarding,
    SensusRoute.Login,
    SensusRoute.Main
)

@Composable
fun MainNavigation(initialDeepLinkResetToken: String? = null, onResetTokenConsumed: () -> Unit = {}, appViewModel: AppViewModel = hiltViewModel()) {
    val appState by appViewModel.viewState.collectAsState()
    var previousScreenBeforeTerms by rememberSaveable { mutableStateOf<AppScreen?>(null) }
    val backStack = rememberNavBackStack(NavigationKeys.savedStateConfig, SensusRoute.Splash)

    LaunchedEffect(initialDeepLinkResetToken) {
        if (!initialDeepLinkResetToken.isNullOrBlank()) {
            appViewModel.dispatchIntent(AppViewIntent.DeepLinkReceived(initialDeepLinkResetToken))
            onResetTokenConsumed()
        }
    }

    LaunchedEffect(appState.currentScreen) {
        syncBackStack(backStack, appState.currentScreen.toRoute())
    }

    NavDisplay(
        backStack = backStack,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { fadeIn(tween(TRANSITION_MILLIS)) togetherWith fadeOut(tween(TRANSITION_MILLIS)) },
        popTransitionSpec = { fadeIn(tween(TRANSITION_MILLIS)) togetherWith fadeOut(tween(TRANSITION_MILLIS)) },
        onBack = {
            resolveBackScreen(appState.currentScreen, previousScreenBeforeTerms)?.let { target ->
                appViewModel.dispatchIntent(AppViewIntent.NavigateTo(target))
            }
        },
        entryProvider = buildSensusEntryProvider(
            appState = appState,
            previousScreenBeforeTerms = previousScreenBeforeTerms,
            onSetPreviousTermsScreen = { previousScreenBeforeTerms = it },
            onIntent = appViewModel::dispatchIntent
        )
    )
}

private fun syncBackStack(backStack: NavBackStack<NavKey>, target: SensusRoute) {
    if (backStack.lastOrNull() == target) return
    Snapshot.withMutableSnapshot {
        if (target in rootRoutes) {
            backStack[0] = target
            while (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        } else {
            val existingIndex = backStack.indexOf(target)
            if (existingIndex >= 0) {
                while (backStack.lastIndex > existingIndex) {
                    backStack.removeAt(backStack.lastIndex)
                }
            } else {
                backStack.add(target)
            }
        }
    }
}

private fun resolveBackScreen(current: AppScreen, previousBeforeTerms: AppScreen?): AppScreen? = when (current) {
    AppScreen.OTP, AppScreen.FORGOT_PASSWORD, AppScreen.REGISTER -> AppScreen.LOGIN
    AppScreen.TERMS -> previousBeforeTerms ?: AppScreen.LOGIN
    else -> null
}

private fun buildSensusEntryProvider(
    appState: AppViewState,
    previousScreenBeforeTerms: AppScreen?,
    onSetPreviousTermsScreen: (AppScreen) -> Unit,
    onIntent: (AppViewIntent) -> Unit
): (NavKey) -> NavEntry<NavKey> = entryProvider {
    registerOnboardingEntries(onIntent)
    registerAuthEntries(appState, onSetPreviousTermsScreen, onIntent)
    registerSecondaryEntries(appState, previousScreenBeforeTerms, onIntent)
    registerMainEntry(appState, onSetPreviousTermsScreen, onIntent)
}

private fun EntryProviderScope<NavKey>.registerOnboardingEntries(onIntent: (AppViewIntent) -> Unit) {
    entry<SensusRoute.Splash> {
        SplashScreen(onSplashFinished = { onIntent(AppViewIntent.SplashFinished) })
    }
    entry<SensusRoute.Onboarding> {
        OnboardingLocationScreen(
            onPermissionGranted = { onIntent(AppViewIntent.LocationPermissionSet(true)) },
            onManualLocationSelected = { onIntent(AppViewIntent.LocationPermissionSet(false)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.registerAuthEntries(
    appState: AppViewState,
    onSetPreviousTermsScreen: (AppScreen) -> Unit,
    onIntent: (AppViewIntent) -> Unit
) {
    entry<SensusRoute.Login> {
        LoginScreen(
            callbacks = LoginCallbacks(
                onLoginSuccess = {
                    navigateAfterAuth(AppScreen.LOGIN, appState.hasAcceptedTerms, onSetPreviousTermsScreen, onIntent)
                },
                onNavigateToRegister = { onIntent(AppViewIntent.NavigateTo(AppScreen.REGISTER)) },
                onNavigateToOtp = { onIntent(AppViewIntent.NavigateTo(AppScreen.OTP)) },
                onContinueAsGuest = { onIntent(AppViewIntent.NavigateTo(AppScreen.MAIN)) },
                onForgotPassword = { onIntent(AppViewIntent.NavigateTo(AppScreen.FORGOT_PASSWORD)) }
            )
        )
    }
    entry<SensusRoute.Register> {
        RegisterScreen(
            onRegisterSuccess = {
                navigateAfterAuth(AppScreen.REGISTER, appState.hasAcceptedTerms, onSetPreviousTermsScreen, onIntent)
            },
            onNavigateToLogin = { onIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN)) },
            onNavigateToTerms = {
                onSetPreviousTermsScreen(AppScreen.REGISTER)
                onIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
            }
        )
    }
}

private fun EntryProviderScope<NavKey>.registerSecondaryEntries(
    appState: AppViewState,
    previousScreenBeforeTerms: AppScreen?,
    onIntent: (AppViewIntent) -> Unit
) {
    entry<SensusRoute.Otp> {
        OtpVerificationScreen(
            onVerifySuccess = {
                val target = if (appState.hasAcceptedTerms) AppScreen.MAIN else AppScreen.TERMS
                onIntent(AppViewIntent.NavigateTo(target))
            },
            onNavigateBack = { onIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN)) }
        )
    }
    entry<SensusRoute.ForgotPassword> {
        ForgotPasswordScreen(
            initialToken = appState.activeResetToken,
            onNavigateBack = { onIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN)) },
            onPasswordResetSuccess = { onIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN)) }
        )
    }
    entry<SensusRoute.Terms> {
        TermsScreen(
            onTermsAccepted = { onIntent(AppViewIntent.TermsAccepted(true)) },
            onNavigateBack = { onIntent(AppViewIntent.NavigateTo(previousScreenBeforeTerms ?: AppScreen.LOGIN)) }
        )
    }
}

private fun EntryProviderScope<NavKey>.registerMainEntry(
    appState: AppViewState,
    onSetPreviousTermsScreen: (AppScreen) -> Unit,
    onIntent: (AppViewIntent) -> Unit
) {
    entry<SensusRoute.Main> {
        MainAppContainer(
            isGuest = appState.isGuest,
            onLogout = { onIntent(AppViewIntent.Logout) },
            onNavigateToTerms = {
                onSetPreviousTermsScreen(AppScreen.MAIN)
                onIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
            }
        )
    }
}

private fun navigateAfterAuth(origin: AppScreen, hasAcceptedTerms: Boolean, onSetPreviousTermsScreen: (AppScreen) -> Unit, onIntent: (AppViewIntent) -> Unit) {
    if (hasAcceptedTerms) {
        onIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
    } else {
        onSetPreviousTermsScreen(origin)
        onIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
    }
}
