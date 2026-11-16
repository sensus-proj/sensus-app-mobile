package com.example.sensus.presentation.buyer

import android.Manifest
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            val message = when (effect) {
                NotificationsViewEffect.Saved -> R.string.notification_preferences_saved
                NotificationsViewEffect.DeviceRegistered -> R.string.notification_preferences_registered
            }
            snackbars.showSnackbar(context.getString(message))
        }
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        NotificationsPreferencesContent(state, viewModel::processIntent, onNavigateBack, {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
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
            Button(onClick = { onIntent(NotificationsViewIntent.Save) }, enabled = state.canSave) {
                Text(stringResource(if (state.isSaving) R.string.notification_preferences_working else R.string.notification_preferences_save))
            }
        }
        if (state.preferences == null && !state.isLoading) {
            Button(onClick = { onIntent(NotificationsViewIntent.Retry) }) { Text(stringResource(R.string.notification_preferences_retry)) }
        }
        Text(stringResource(R.string.notification_preferences_activation_note))
        Button(onClick = onActivate, enabled = !state.isRegistering) {
            Text(stringResource(if (state.isRegistering) R.string.notification_preferences_working else R.string.notification_preferences_activate))
        }
    }
}

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

private fun NotificationsError.messageResource(): Int = when (this) {
    NotificationsError.NETWORK -> R.string.notification_preferences_network
    NotificationsError.SESSION -> R.string.notification_preferences_session
    NotificationsError.UNAVAILABLE -> R.string.notification_preferences_unavailable
    NotificationsError.VALIDATION -> R.string.notification_preferences_validation
    NotificationsError.GENERIC -> R.string.notification_preferences_error
    NotificationsError.FIREBASE -> R.string.notification_preferences_firebase
    NotificationsError.PERMISSION -> R.string.notification_preferences_permission
}
