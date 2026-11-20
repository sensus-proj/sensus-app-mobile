package com.example.sensus.presentation.buyer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.NotificationItem
import com.example.sensus.domain.model.NotificationsPreferences
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.sensusTextButtonColors
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNotificationActionClick: (NotificationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.processIntent(if (granted) NotificationsViewIntent.ActivateDevice else NotificationsViewIntent.PermissionDenied)
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.processIntent(NotificationsLocationIntent.PermissionResult(result.values.any { it }))
    }
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when {
                effect != NotificationsViewEffect.RequestLocationPermission -> snackbars.showSnackbar(context.getString(effect.messageResource()))
                context.hasLocationPermission() -> viewModel.processIntent(NotificationsLocationIntent.PermissionResult(true))
                else -> locationPermission.launch(LOCATION_PERMISSIONS)
            }
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        NotificationsPreferencesContent(state, viewModel::processIntent, onNavigateBack, {
            if (context.needsNotificationPermission()) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.processIntent(NotificationsViewIntent.ActivateDevice)
            }
        }, Modifier.weight(1f))
        SnackbarHost(snackbars)
    }
}

@Composable
fun NotificationsPreferencesContent(
    state: NotificationsViewState,
    onIntent: (NotificationsViewIntent) -> Unit,
    onNavigateBack: () -> Unit,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.notification_preferences_back))
        }
        Text(stringResource(R.string.notification_preferences_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.notification_preferences_inbox))
        if (state.isLoading) Text(stringResource(R.string.notification_preferences_loading))
        state.error?.let { Text(stringResource(it.messageResource()), color = MaterialTheme.colorScheme.error) }
        state.preferences?.let { preferences ->
            NotificationsPreferenceControls(preferences, !state.isSaving && !state.isLoading) {
                onIntent(NotificationsViewIntent.Edit(it))
            }
            Button(shape = SensusButtonShape, onClick = { onIntent(NotificationsViewIntent.Save) }, enabled = state.canSave) {
                Text(stringResource(if (state.isSaving) R.string.notification_preferences_working else R.string.notification_preferences_save))
            }
        }
        if (state.preferences == null && !state.isLoading) {
            Button(
                shape = SensusButtonShape,
                onClick = { onIntent(NotificationsViewIntent.Retry) }
            ) { Text(stringResource(R.string.notification_preferences_retry)) }
        }
        Text(stringResource(R.string.notification_preferences_activation_note))
        Button(shape = SensusButtonShape, onClick = onActivate, enabled = !state.isRegistering) {
            Text(stringResource(if (state.isRegistering) R.string.notification_preferences_working else R.string.notification_preferences_activate))
        }
        NotificationsLocationSection(state, onIntent)
    }
}

/** FT-39: interruptor de ubicacion para ofertas cercanas con su dialogo de consentimiento. */
@Composable
private fun NotificationsLocationSection(state: NotificationsViewState, onIntent: (NotificationsViewIntent) -> Unit) {
    NotificationsPreferenceSwitch(R.string.notification_preferences_location, state.locationSharingEnabled, !state.isUpdatingLocation) {
        onIntent(NotificationsLocationIntent.Toggled(it))
    }
    Text(stringResource(R.string.notification_preferences_location_note), style = MaterialTheme.typography.bodySmall)
    if (state.showLocationConsent) LocationConsentDialog { onIntent(NotificationsLocationIntent.ConsentAnswered(it)) }
}

@Composable
private fun LocationConsentDialog(onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.push_location_title)) },
        text = { Text(stringResource(R.string.push_location_body)) },
        confirmButton = { TextButton(colors = sensusTextButtonColors(), onClick = { onAnswer(true) }) { Text(stringResource(R.string.push_location_accept)) } },
        dismissButton = {
            TextButton(
                colors = sensusTextButtonColors(),
                onClick = { onAnswer(false) }
            ) { Text(stringResource(R.string.push_location_decline)) }
        }
    )
}

private val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

private fun Context.hasLocationPermission(): Boolean = LOCATION_PERMISSIONS.any {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

private fun Context.needsNotificationPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

private fun NotificationsViewEffect.messageResource(): Int =
    if (this == NotificationsViewEffect.Saved) R.string.notification_preferences_saved else R.string.notification_preferences_registered

@Composable
private fun NotificationsPreferenceControls(value: NotificationsPreferences, enabled: Boolean, edit: (NotificationsPreferences) -> Unit) {
    NotificationsPreferenceSwitch(R.string.notification_preferences_push, value.pushEnabled, enabled) { edit(value.copy(pushEnabled = it)) }
    NotificationsPreferenceSwitch(R.string.notification_preferences_offers, value.offerAlerts, enabled) { edit(value.copy(offerAlerts = it)) }
    NotificationsPreferenceSwitch(R.string.notification_preferences_social, value.socialAlerts, enabled) { edit(value.copy(socialAlerts = it)) }
    Text(stringResource(R.string.notification_preferences_radius, value.radiusKm))
    val radiusDescription = stringResource(R.string.notification_preferences_radius, value.radiusKm)
    Slider(
        value = value.radiusKm.toFloat(),
        onValueChange = { edit(value.copy(radiusKm = it.roundToInt())) },
        enabled = enabled,
        valueRange = NotificationsPreferences.MIN_RADIUS_KM.toFloat()..NotificationsPreferences.MAX_RADIUS_KM.toFloat(),
        modifier = Modifier.semantics { contentDescription = radiusDescription },
        steps = NotificationsPreferences.MAX_RADIUS_KM - NotificationsPreferences.MIN_RADIUS_KM - 1
    )
}

@Composable
private fun NotificationsPreferenceSwitch(label: Int, checked: Boolean, enabled: Boolean, change: (Boolean) -> Unit) {
    val description = stringResource(label)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked, change, enabled = enabled, modifier = Modifier.semantics { contentDescription = description })
    }
}

private val ERROR_MESSAGES = mapOf(
    NotificationsError.NETWORK to R.string.notification_preferences_network,
    NotificationsError.SESSION to R.string.notification_preferences_session,
    NotificationsError.UNAVAILABLE to R.string.notification_preferences_unavailable,
    NotificationsError.VALIDATION to R.string.notification_preferences_validation,
    NotificationsError.GENERIC to R.string.notification_preferences_error,
    NotificationsError.FIREBASE to R.string.notification_preferences_firebase,
    NotificationsError.PERMISSION to R.string.notification_preferences_permission,
    NotificationsError.LOCATION_PERMISSION to R.string.notification_preferences_location_permission,
    NotificationsError.LOCATION_UNAVAILABLE to R.string.notification_preferences_location_unavailable
)

private fun NotificationsError.messageResource(): Int = ERROR_MESSAGES[this] ?: R.string.notification_preferences_error
