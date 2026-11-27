package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.presentation.common.ConfirmedClaimQr
import com.example.sensus.presentation.common.SecureScreenEffect
import com.example.sensus.presentation.common.SensitiveQrGate
import com.example.sensus.presentation.common.claimsErrorText
import com.example.sensus.presentation.common.currentDeviceAuthenticator
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.sensusTextButtonColors
import java.time.Clock
import java.time.Instant

private const val MY_CLAIMS_PADDING = 20
private const val MY_CLAIMS_SPACING = 12

@Composable
fun MyClaimsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier, viewModel: MyClaimsViewModel = hiltViewModel()) {
    SecureScreenEffect()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.processIntent(MyClaimsViewIntent.Load) }
    MyClaimsContent(state = state, onNavigateBack = onNavigateBack, onIntent = viewModel::processIntent, modifier = modifier)
}

@Composable
fun MyClaimsContent(
    state: MyClaimsViewState,
    onNavigateBack: () -> Unit,
    onIntent: (MyClaimsViewIntent) -> Unit,
    modifier: Modifier = Modifier,
    clock: Clock = Clock.systemUTC()
) {
    Column(modifier.fillMaxSize().padding(MY_CLAIMS_PADDING.dp), verticalArrangement = Arrangement.spacedBy(MY_CLAIMS_SPACING.dp)) {
        TextButton(colors = sensusTextButtonColors(), onClick = onNavigateBack) { Text(stringResource(R.string.claims_back)) }
        Text(stringResource(R.string.my_claims_title), fontWeight = FontWeight.Bold)
        MyClaimsStatusContent(state, onIntent)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(MY_CLAIMS_SPACING.dp)) {
            items(state.claims, key = { it.claimId }) { claim ->
                MyClaimCard(claim, clock.millis())
            }
        }
    }
}

@Composable
private fun MyClaimsStatusContent(state: MyClaimsViewState, onIntent: (MyClaimsViewIntent) -> Unit) {
    if (state.loading) CircularProgressIndicator()
    state.error?.let { error ->
        Text(claimsErrorText(error))
        OutlinedButton(shape = SensusButtonShape, onClick = { onIntent(MyClaimsViewIntent.Load) }) { Text(stringResource(R.string.my_claims_retry)) }
    }
    if (state.isEmpty) Text(stringResource(R.string.my_claims_empty))
}

@Composable
private fun MyClaimCard(claim: ClaimSummary, nowEpochMs: Long) {
    val active = claim.isActiveAt(nowEpochMs)
    Card(Modifier.fillMaxWidth().testTag("my_claim_${claim.claimId}")) {
        Column(Modifier.padding(MY_CLAIMS_SPACING.dp), verticalArrangement = Arrangement.spacedBy(MY_CLAIMS_SPACING.dp)) {
            Text(claim.offerTitle ?: stringResource(R.string.my_claims_untitled), fontWeight = FontWeight.SemiBold)
            Text(stringResource(claimStatusLabel(claim, active)))
            Text(stringResource(R.string.claims_expiry, Instant.ofEpochMilli(claim.expiresAtEpochMs).toString()))
            if (active) {
                claim.qrPayload?.let { payload ->
                    SensitiveQrGate(unlockKey = claim.claimId, deviceAuthenticator = currentDeviceAuthenticator()) {
                        ConfirmedClaimQr(payload)
                    }
                }
            }
        }
    }
}

internal fun claimStatusLabel(claim: ClaimSummary, active: Boolean): Int = when {
    active -> R.string.my_claims_status_active
    claim.status == ClaimStatus.ACTIVE || claim.status == ClaimStatus.EXPIRED -> R.string.my_claims_status_expired
    claim.status == ClaimStatus.REDEEMED -> R.string.my_claims_status_redeemed
    claim.status == ClaimStatus.CANCELLED -> R.string.my_claims_status_cancelled
    else -> R.string.my_claims_status_unknown
}
