package com.example.sensus.presentation.merchant

import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.presentation.common.QrScannerDialog
import com.example.sensus.presentation.common.claimsErrorText
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.sensusOutlinedButtonColors
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.time.Instant

private const val REDEEM_CONTENT_PADDING = 20
private const val CONTENT_SPACING = 24
private const val MENU_CLEARANCE = 96

@Composable
fun MerchantSalesScreen(modifier: Modifier = Modifier) {
    val window = LocalActivity.current?.window
    DisposableEffect(window) {
        val shouldSetSecure = window != null && window.attributes.flags and FLAG_SECURE == 0
        if (shouldSetSecure) window.addFlags(FLAG_SECURE)
        onDispose { if (shouldSetSecure) window.clearFlags(FLAG_SECURE) }
    }
    val viewModel: MerchantSalesViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    MerchantSalesContent(state, viewModel::processIntent, modifier)
}

@Composable
fun MerchantSalesContent(state: MerchantSalesViewState, onIntent: (MerchantSalesViewIntent) -> Unit, modifier: Modifier = Modifier) {
    var showScanner by remember { mutableStateOf(false) }
    Column(
        modifier.statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(REDEEM_CONTENT_PADDING.dp)
            .padding(bottom = MENU_CLEARANCE.dp),
        verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp)
    ) {
        Text(stringResource(R.string.claims_redeem_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(
            onClick = { showScanner = true },
            enabled = !state.loading,
            shape = SensusButtonShape,
            colors = sensusOutlinedButtonColors(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.claims_scan_qr))
        }
        OutlinedTextField(
            value = state.input,
            onValueChange = { onIntent(MerchantSalesViewIntent.Edit(it)) },
            label = { Text(stringResource(R.string.claims_code)) },
            singleLine = true,
            enabled = !state.loading,
            colors = sensusOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            shape = SensusButtonShape,
            onClick = { onIntent(MerchantSalesViewIntent.Submit(state.input)) },
            enabled = !state.loading && state.input.isNotBlank() && state.result == null,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.claims_redeem)) }
        MerchantSalesStatus(state)
    }
    if (showScanner && !state.loading) {
        QrScannerDialog(
            onQrDecoded = { code ->
                if (showScanner) {
                    showScanner = false
                    onIntent(MerchantSalesViewIntent.Scan(code))
                }
            },
            onDismiss = { showScanner = false },
            instructions = stringResource(R.string.claims_scan_instructions),
            securePolicy = SecureFlagPolicy.SecureOn
        )
    }
}

@Composable
private fun MerchantSalesStatus(state: MerchantSalesViewState) {
    if (state.loading) CircularProgressIndicator()
    if (state.scanned && !state.loading && state.error == null) {
        Text(stringResource(R.string.claims_scan_ready), style = MaterialTheme.typography.bodyMedium)
    }
    state.error?.let { Text(claimsErrorText(it), color = SensusRed) }
    state.result?.let {
        Text(stringResource(R.string.claims_redeemed, Instant.ofEpochMilli(it.redeemedAtEpochMs).toString()), color = SensusSuccess)
    }
}
