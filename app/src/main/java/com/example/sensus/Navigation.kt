package com.example.sensus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import com.example.sensus.presentation.splash.SplashScreen
import com.example.sensus.presentation.auth.LoginScreen
import com.example.sensus.presentation.auth.OnboardingLocationScreen
import com.example.sensus.presentation.auth.OtpVerificationScreen
import com.example.sensus.presentation.auth.RegisterScreen
import com.example.sensus.presentation.auth.TermsScreen
import com.example.sensus.presentation.main.MainAppContainer
import com.example.sensus.presentation.modals.ForgotPasswordScreen

@Composable
fun MainNavigation(
    initialDeepLinkResetToken: String? = null,
    onResetTokenConsumed: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel()
) {
    val appState by appViewModel.viewState.collectAsState()
    var previousScreenBeforeTerms by rememberSaveable { mutableStateOf<AppScreen?>(null) }

    LaunchedEffect(initialDeepLinkResetToken) {
        if (!initialDeepLinkResetToken.isNullOrBlank()) {
            appViewModel.dispatchIntent(AppViewIntent.DeepLinkReceived(initialDeepLinkResetToken))
            onResetTokenConsumed()
        }
    }

    Crossfade(
        targetState = appState.currentScreen,
        animationSpec = tween(durationMillis = 350),
        modifier = Modifier.fillMaxSize(),
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = {
                        appViewModel.dispatchIntent(AppViewIntent.SplashFinished)
                    }
                )
            }
            AppScreen.ONBOARDING -> {
                OnboardingLocationScreen(
                    onPermissionGranted = {
                        appViewModel.dispatchIntent(AppViewIntent.LocationPermissionSet(true))
                    },
                    onManualLocationSelected = {
                        appViewModel.dispatchIntent(AppViewIntent.LocationPermissionSet(false))
                    }
                )
            }
            AppScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = {
                        if (appState.hasAcceptedTerms) {
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
                        } else {
                            previousScreenBeforeTerms = AppScreen.LOGIN
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
                        }
                    },
                    onNavigateToRegister = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.REGISTER))
                    },
                    onNavigateToOtp = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.OTP))
                    },
                    onContinueAsGuest = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
                    },
                    onForgotPassword = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.FORGOT_PASSWORD))
                    }
                )
            }
            AppScreen.REGISTER -> {
                RegisterScreen(
                    onRegisterSuccess = {
                        if (appState.hasAcceptedTerms) {
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
                        } else {
                            previousScreenBeforeTerms = AppScreen.REGISTER
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
                        }
                    },
                    onNavigateToLogin = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN))
                    },
                    onNavigateToTerms = {
                        previousScreenBeforeTerms = AppScreen.REGISTER
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
                    }
                )
            }
            AppScreen.OTP -> {
                BackHandler { appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN)) }
                OtpVerificationScreen(
                    onVerifySuccess = {
                        if (appState.hasAcceptedTerms) {
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.MAIN))
                        } else {
                            previousScreenBeforeTerms = AppScreen.OTP
                            appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
                        }
                    },
                    onNavigateBack = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN))
                    }
                )
            }
            AppScreen.FORGOT_PASSWORD -> {
                BackHandler {
                    appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN))
                }
                ForgotPasswordScreen(
                    initialToken = appState.activeResetToken,
                    onNavigateBack = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN))
                    },
                    onPasswordResetSuccess = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.LOGIN))
                    }
                )
            }
            AppScreen.TERMS -> {
                BackHandler {
                    appViewModel.dispatchIntent(AppViewIntent.NavigateTo(previousScreenBeforeTerms ?: AppScreen.LOGIN))
                }
                TermsScreen(
                    onTermsAccepted = {
                        appViewModel.dispatchIntent(AppViewIntent.TermsAccepted(true))
                    },
                    onNavigateBack = {
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(previousScreenBeforeTerms ?: AppScreen.LOGIN))
                    }
                )
            }
            AppScreen.MAIN -> {
                MainAppContainer(
                    isGuest = appState.isGuest,
                    onLogout = {
                        appViewModel.dispatchIntent(AppViewIntent.Logout)
                    },
                    onNavigateToTerms = {
                        previousScreenBeforeTerms = AppScreen.MAIN
                        appViewModel.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.TERMS))
                    }
                )
            }
        }
    }
}
