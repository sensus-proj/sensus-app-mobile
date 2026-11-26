package com.example.sensus.presentation.push

import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

/** Estado de los avisos de permiso de la pantalla principal (FT-39). */
data class PushViewState(
    val showNotificationRationale: Boolean = false,
    val showLocationRationale: Boolean = false,
    val isOpeningTarget: Boolean = false
) : ViewState

sealed interface PushViewIntent : ViewIntent {
    /** La pantalla principal se mostro: [isGuest] y si Android aun requiere `POST_NOTIFICATIONS`. */
    data class Started(val isGuest: Boolean, val needsNotificationPermission: Boolean) : PushViewIntent
    data object Resumed : PushViewIntent

    /** Respuesta a la explicacion previa de [prompt]: [accepted] lleva a pedir el permiso de Android. */
    data class RationaleAnswered(val prompt: NotificationsPrompt, val accepted: Boolean) : PushViewIntent

    /** Resultado del dialogo del sistema para [prompt]. */
    data class PermissionResult(val prompt: NotificationsPrompt, val granted: Boolean) : PushViewIntent
}

sealed interface PushViewEffect : ViewEffect {
    data object RequestNotificationPermission : PushViewEffect
    data object RequestLocationPermission : PushViewEffect
    data class OpenOffer(val offer: ProductOffer) : PushViewEffect
    data class OpenClaim(val offer: ProductOffer) : PushViewEffect
    data object OpenMerchantClaims : PushViewEffect
    data object OpenFriends : PushViewEffect
    data object OfferUnavailable : PushViewEffect
    data object GuestRequired : PushViewEffect
}
