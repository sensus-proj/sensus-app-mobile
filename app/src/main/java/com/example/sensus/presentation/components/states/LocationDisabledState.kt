package com.example.sensus.presentation.components.states

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R

@Composable
fun LocationDisabledState(modifier: Modifier = Modifier, onOpenSettings: (() -> Unit)? = null, onChooseManualLocation: () -> Unit = {}) {
    val context = LocalContext.current
    val handleOpenSettings = onOpenSettings ?: {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .testTag("location_disabled_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LocationDisabledHeader()
        Spacer(modifier = Modifier.height(24.dp))
        LocationStepsCard()
        Spacer(modifier = Modifier.height(28.dp))
        LocationActionButtons(
            onOpenSettings = handleOpenSettings,
            onChooseManualLocation = onChooseManualLocation
        )
    }
}

@Composable
private fun LocationDisabledHeader() {
    Icon(
        imageVector = Icons.Default.LocationOff,
        contentDescription = stringResource(R.string.location_disabled_title),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(72.dp)
            .testTag("location_disabled_icon")
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.location_disabled_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.location_disabled_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun LocationStepsCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LocationStepItem(stepNumber = "1", text = stringResource(R.string.location_step_1))
            Spacer(modifier = Modifier.height(10.dp))
            LocationStepItem(stepNumber = "2", text = stringResource(R.string.location_step_2))
            Spacer(modifier = Modifier.height(10.dp))
            LocationStepItem(stepNumber = "3", text = stringResource(R.string.location_step_3))
        }
    }
}

@Composable
private fun LocationActionButtons(onOpenSettings: () -> Unit, onChooseManualLocation: () -> Unit) {
    Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("location_open_settings_button")
    ) {
        Text(
            text = stringResource(R.string.location_open_settings),
            style = MaterialTheme.typography.labelLarge
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = onChooseManualLocation,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("location_choose_manual_button")
    ) {
        Text(
            text = stringResource(R.string.location_choose_manual),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LocationStepItem(stepNumber: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
