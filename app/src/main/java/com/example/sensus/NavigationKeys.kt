package com.example.sensus

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import com.example.sensus.presentation.root.AppScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object NavigationKeys {
    val savedStateConfig = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(SensusRoute.Splash::class, SensusRoute.Splash.serializer())
                subclass(SensusRoute.Onboarding::class, SensusRoute.Onboarding.serializer())
                subclass(SensusRoute.Login::class, SensusRoute.Login.serializer())
                subclass(SensusRoute.Register::class, SensusRoute.Register.serializer())
                subclass(SensusRoute.Otp::class, SensusRoute.Otp.serializer())
                subclass(SensusRoute.Terms::class, SensusRoute.Terms.serializer())
                subclass(SensusRoute.InterestsOnboarding::class, SensusRoute.InterestsOnboarding.serializer())
                subclass(SensusRoute.ForgotPassword::class, SensusRoute.ForgotPassword.serializer())
                subclass(SensusRoute.Main::class, SensusRoute.Main.serializer())
            }
        }
    }
}

@Serializable
sealed interface SensusRoute : NavKey {
    @Serializable data object Splash : SensusRoute

    @Serializable data object Onboarding : SensusRoute

    @Serializable data object Login : SensusRoute

    @Serializable data object Register : SensusRoute

    @Serializable data object Otp : SensusRoute

    @Serializable data object Terms : SensusRoute

    @Serializable data object InterestsOnboarding : SensusRoute

    @Serializable data object ForgotPassword : SensusRoute

    @Serializable data object Main : SensusRoute
}

private val appScreenToRoute: Map<AppScreen, SensusRoute> = mapOf(
    AppScreen.SPLASH to SensusRoute.Splash,
    AppScreen.ONBOARDING to SensusRoute.Onboarding,
    AppScreen.LOGIN to SensusRoute.Login,
    AppScreen.REGISTER to SensusRoute.Register,
    AppScreen.OTP to SensusRoute.Otp,
    AppScreen.TERMS to SensusRoute.Terms,
    AppScreen.INTERESTS_ONBOARDING to SensusRoute.InterestsOnboarding,
    AppScreen.FORGOT_PASSWORD to SensusRoute.ForgotPassword,
    AppScreen.MAIN to SensusRoute.Main
)

fun AppScreen.toRoute(): SensusRoute = appScreenToRoute.getValue(this)
