package com.example.sensus.presentation.push

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.PushTarget
import com.example.sensus.domain.repository.NotificationsLocalStore
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.NotificationsDeviceSync
import com.example.sensus.domain.usecase.NotificationsLocationUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Notificaciones en la pantalla principal (FT-39): permisos con explicacion previa, ubicacion para ofertas
 * cercanas y apertura del destino de una notificacion tocada.
 */
@HiltViewModel
class PushViewModel @Inject constructor(
    private val openRequests: PushOpenRequests,
    private val getOfferDetail: GetOfferDetailUseCase,
    private val location: NotificationsLocationUseCase,
    private val store: NotificationsLocalStore,
    private val deviceSync: NotificationsDeviceSync
) : MviViewModel<PushViewState, PushViewIntent, PushViewEffect>(PushViewState()) {

    private var isGuest = true
    private var isReady = false

    init {
        viewModelScope.launch {
            openRequests.pending.filterNotNull().collect { if (isReady) openNext() }
        }
    }

    override fun processIntent(intent: PushViewIntent) {
        when (intent) {
            is PushViewIntent.Started -> start(intent)
            PushViewIntent.Resumed -> if (isReady && !isGuest) viewModelScope.launch { location.refreshIfDue() }
            is PushViewIntent.RationaleAnswered -> when (intent.prompt) {
                NotificationsPrompt.NOTIFICATIONS -> answerNotificationRationale(intent.accepted)
                NotificationsPrompt.LOCATION -> answerLocationRationale(intent.accepted)
            }
            is PushViewIntent.PermissionResult -> onPermissionResult(intent)
        }
    }

    private fun onPermissionResult(intent: PushViewIntent.PermissionResult) {
        when (intent.prompt) {
            NotificationsPrompt.NOTIFICATIONS -> onNotificationPermission(intent.granted)
            NotificationsPrompt.LOCATION -> if (intent.granted) viewModelScope.launch { location.enable() }
        }
    }

    private fun start(intent: PushViewIntent.Started) {
        isGuest = intent.isGuest
        isReady = true
        if (!isGuest) {
            showNextPrompt(intent.needsNotificationPermission)
            viewModelScope.launch { location.refreshIfDue() }
        }
        openNext()
    }

    private fun showNextPrompt(needsNotificationPermission: Boolean) {
        val askNotifications = needsNotificationPermission && !store.wasPromptShown(NotificationsPrompt.NOTIFICATIONS)
        val askLocation = !askNotifications && !store.wasPromptShown(NotificationsPrompt.LOCATION) && !location.isEnabled()
        updateState { copy(showNotificationRationale = askNotifications, showLocationRationale = askLocation) }
    }

    private fun answerNotificationRationale(accepted: Boolean) {
        store.markPromptShown(NotificationsPrompt.NOTIFICATIONS)
        updateState { copy(showNotificationRationale = false) }
        if (accepted) emitEffect(PushViewEffect.RequestNotificationPermission) else showNextPrompt(needsNotificationPermission = false)
    }

    private fun onNotificationPermission(granted: Boolean) {
        if (granted) deviceSync.sync()
        showNextPrompt(needsNotificationPermission = false)
    }

    private fun answerLocationRationale(accepted: Boolean) {
        store.markPromptShown(NotificationsPrompt.LOCATION)
        updateState { copy(showLocationRationale = false) }
        if (accepted) emitEffect(PushViewEffect.RequestLocationPermission)
    }

    private fun openNext() {
        val target = openRequests.consume() ?: return
        viewModelScope.launch { open(target) }
    }

    private suspend fun open(target: PushTarget) {
        when (target) {
            is PushTarget.Offer -> openOffer(target.offerId) { PushViewEffect.OpenOffer(it) }
            is PushTarget.Claim -> whenRegistered { openOffer(target.offerId) { PushViewEffect.OpenClaim(it) } }
            PushTarget.MerchantClaims -> whenRegistered { emitEffect(PushViewEffect.OpenMerchantClaims) }
            PushTarget.Friends -> whenRegistered { emitEffect(PushViewEffect.OpenFriends) }
            PushTarget.Home -> Unit
        }
    }

    private suspend fun whenRegistered(action: suspend () -> Unit) {
        if (isGuest) emitEffect(PushViewEffect.GuestRequired) else action()
    }

    private suspend fun openOffer(offerId: String, effect: (ProductOffer) -> PushViewEffect) {
        updateState { copy(isOpeningTarget = true) }
        val result = getOfferDetail(offerId)
        updateState { copy(isOpeningTarget = false) }
        emitEffect(if (result is Result.Success) effect(result.data.offer) else PushViewEffect.OfferUnavailable)
    }
}
