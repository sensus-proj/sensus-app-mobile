package com.example.sensus.presentation.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.repository.NotificationsPrompt
import com.example.sensus.presentation.push.PushViewEffect
import com.example.sensus.presentation.push.PushViewIntent
import com.example.sensus.presentation.push.PushViewModel
import com.example.sensus.presentation.push.PushViewState
import com.example.sensus.theme.sensusTextButtonColors

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/** Textos de un dialogo de explicacion previa al permiso de Android. */
internal class PushRationaleTexts(@StringRes val title: Int, @StringRes val body: Int, @StringRes val confirm: Int, @StringRes val dismiss: Int)

private val NOTIFICATION_RATIONALE = PushRationaleTexts(
    R.string.push_rationale_title,
    R.string.push_rationale_body,
    R.string.push_rationale_accept,
    R.string.push_rationale_later
)
private val LOCATION_RATIONALE = PushRationaleTexts(
    R.string.push_location_title,
    R.string.push_location_body,
    R.string.push_location_accept,
    R.string.push_location_decline
)

/**
 * FT-39: permisos con explicacion previa, renovacion de la ubicacion al volver a la app y navegacion al destino
 * de una notificacion tocada. Se compone dentro de la pantalla principal, ya con la sesion resuelta.
 */
@Composable
internal fun MainPushHost(state: MainAppState, isGuest: Boolean, viewModel: PushViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val pushState by viewModel.viewState.collectAsStateWithLifecycle()
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.processIntent(PushViewIntent.PermissionResult(NotificationsPrompt.NOTIFICATIONS, granted))
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.processIntent(PushViewIntent.PermissionResult(NotificationsPrompt.LOCATION, result.values.any { it }))
    }

    LaunchedEffect(viewModel, isGuest) {
        viewModel.processIntent(PushViewIntent.Started(isGuest, context.needsNotificationPermission()))
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.processIntent(PushViewIntent.Resumed)
        onPauseOrDispose { }
    }
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            val prompt = effect.permissionPrompt()
            when {
                prompt == null -> applyPushNavigation(state, effect, context)
                context.isGranted(prompt) -> viewModel.processIntent(PushViewIntent.PermissionResult(prompt, true))
                prompt == NotificationsPrompt.NOTIFICATIONS -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> locationLauncher.launch(LOCATION_PERMISSIONS)
            }
        }
    }

    pushState.activePrompt()?.let { prompt ->
        PushRationaleDialog(prompt.rationaleTexts()) { accepted -> viewModel.processIntent(PushViewIntent.RationaleAnswered(prompt, accepted)) }
    }
}

internal fun PushViewState.activePrompt(): NotificationsPrompt? = when {
    showNotificationRationale -> NotificationsPrompt.NOTIFICATIONS
    showLocationRationale -> NotificationsPrompt.LOCATION
    else -> null
}

internal fun NotificationsPrompt.rationaleTexts(): PushRationaleTexts =
    if (this == NotificationsPrompt.NOTIFICATIONS) NOTIFICATION_RATIONALE else LOCATION_RATIONALE

internal fun PushViewEffect.permissionPrompt(): NotificationsPrompt? = when (this) {
    PushViewEffect.RequestNotificationPermission -> NotificationsPrompt.NOTIFICATIONS
    PushViewEffect.RequestLocationPermission -> NotificationsPrompt.LOCATION
    else -> null
}

internal fun Context.isGranted(prompt: NotificationsPrompt): Boolean =
    if (prompt == NotificationsPrompt.NOTIFICATIONS) !needsNotificationPermission() else hasLocationPermission()

@Composable
private fun PushRationaleDialog(texts: PushRationaleTexts, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(texts.title)) },
        text = { Text(stringResource(texts.body)) },
        confirmButton = { TextButton(colors = sensusTextButtonColors(), onClick = { onAnswer(true) }) { Text(stringResource(texts.confirm)) } },
        dismissButton = { TextButton(colors = sensusTextButtonColors(), onClick = { onAnswer(false) }) { Text(stringResource(texts.dismiss)) } }
    )
}

internal fun applyPushNavigation(state: MainAppState, effect: PushViewEffect, context: Context) {
    when (effect) {
        is PushViewEffect.OpenOffer -> state.openFromPush { selectedProductForDetail = effect.offer }
        is PushViewEffect.OpenClaim -> state.openFromPush {
            activeClaimExpected = true
            activeApartadoOffer = effect.offer
        }
        PushViewEffect.OpenMerchantClaims -> state.openMerchantSales()
        PushViewEffect.OpenFriends -> state.openFromPush {
            buyerTabIndex = TAB_COMMUNITY
            openFriendsTabSignal += 1
        }
        PushViewEffect.OfferUnavailable -> Toast.makeText(context, R.string.push_offer_unavailable, Toast.LENGTH_SHORT).show()
        PushViewEffect.GuestRequired -> state.showGuestPrompt = true
        PushViewEffect.RequestNotificationPermission, PushViewEffect.RequestLocationPermission -> Unit
    }
}

private fun MainAppState.openFromPush(open: MainAppState.() -> Unit) {
    closePushOverlays()
    isMerchantMode = false
    open()
}

/** Solo un comercio activo tiene pestana de ventas; si no, la notificacion abre la app sin mas. */
private fun MainAppState.openMerchantSales() {
    if (!isMerchantRegistered) return
    closePushOverlays()
    isMerchantMode = true
    merchantTabIndex = MERCHANT_TAB_SALES
}

/** Cierra pantallas superpuestas para que el destino de la notificacion quede visible. */
private fun MainAppState.closePushOverlays() {
    showNotifications = false
    activeRouteOffer = null
    activeApartadoOffer = null
    activeClaimExpected = false
    selectedProductForDetail = null
    selectedStoreCatalog = null
    selectedFriendForOffers = null
    showSavedInterests = false
    selectedOfferForEdit = null
    showMerchantRegister = false
    showCartSheet = false
}

private fun Context.needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}
