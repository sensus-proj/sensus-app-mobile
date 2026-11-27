package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ConfirmedClaimQr
import com.example.sensus.presentation.common.SecureScreenEffect
import com.example.sensus.presentation.common.SensitiveQrGate
import com.example.sensus.presentation.common.claimsErrorText
import com.example.sensus.presentation.common.currentDeviceAuthenticator
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.sensusTextButtonColors
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.delay

private const val CLAIM_CONTENT_PADDING = 20
private const val CONTENT_SPACING = 16
private const val EXPIRY_TICK_MS = 1000L

@Composable
fun ClaimOfferScreen(
    offer: ProductOffer,
    actions: ClaimsScreenActions,
    modifier: Modifier = Modifier,
    cartItems: List<CartItem> = emptyList(),
    viewModel: ClaimOfferViewModel = hiltViewModel()
) {
    SecureScreenEffect()
    val effectiveItems = if (cartItems.isNotEmpty()) cartItems else listOf(CartItem(offer, 1))
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val confirmedCallback by rememberUpdatedState(actions.onClaimConfirmed)
    LaunchedEffect(offer.id, effectiveItems, actions.expectExisting, viewModel) {
        viewModel.processIntent(ClaimOfferViewIntent.Initialize(offer.id, actions.expectExisting, effectiveItems))
    }
    LaunchedEffect(offer.id, viewModel, "confirmation") {
        viewModel.viewEffect.collect { effect ->
            if (effect is ClaimOfferViewEffect.Confirmed && effect.offerId == offer.id) confirmedCallback()
        }
    }
    val currentState = state.takeIf { it.offerId == offer.id } ?: ClaimOfferViewState(items = effectiveItems)
    val displayedState = if (actions.expectExisting) currentState.copy(expectExisting = true) else currentState
    ClaimOfferContent(
        offer = offer,
        state = displayedState,
        actions = actions,
        onClaim = { viewModel.processIntent(ClaimOfferViewIntent.Submit(offer.id, effectiveItems)) },
        modifier = modifier
    )
}

@Composable
fun ClaimOfferContent(offer: ProductOffer, state: ClaimOfferViewState, actions: ClaimsScreenActions, onClaim: () -> Unit, modifier: Modifier = Modifier) {
    val summaryItems = state.items.ifEmpty { listOf(CartItem(offer, 1)) }
    val totalUnits = state.totalUnits
    Column(modifier.verticalScroll(rememberScrollState()).padding(CLAIM_CONTENT_PADDING.dp), verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp)) {
        TextButton(colors = sensusTextButtonColors(), onClick = actions.onNavigateBack) { Text(stringResource(R.string.claims_back)) }
        ClaimSummaryHeader(offer, summaryItems, totalUnits)
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(claimsErrorText(it)) }
        ClaimOfferBody(state, actions, totalUnits, onClaim)
    }
}

@Composable
private fun ClaimSummaryHeader(offer: ProductOffer, summaryItems: List<CartItem>, totalUnits: Int) {
    Text(offer.storeName)
    if (totalUnits == 1 && summaryItems.size == 1) {
        Text(offer.name)
        Text(stringResource(R.string.claims_one_unit))
    } else {
        Text(stringResource(R.string.claims_multi_units, totalUnits))
        summaryItems.forEach { item ->
            Text(stringResource(R.string.claims_item_line, item.quantity, item.offer.name))
        }
    }
}

@Composable
private fun ClaimOfferBody(state: ClaimOfferViewState, actions: ClaimsScreenActions, totalUnits: Int, onClaim: () -> Unit) {
    val claim = state.result
    when {
        claim != null -> ClaimConfirmationContent(claim, actions.onNavigateToRoute, actions.clock)
        state.existingCodeUnavailable -> Text(stringResource(R.string.claims_existing_code_unavailable))
        else -> Button(shape = SensusButtonShape, onClick = onClaim, enabled = state.canSubmit) {
            val labelRes = if (totalUnits == 1) R.string.claims_confirm else R.string.claims_confirm_multi
            Text(if (totalUnits == 1) stringResource(labelRes) else stringResource(labelRes, totalUnits))
        }
    }
}

@Composable
private fun ClaimConfirmationContent(claim: OfferClaim, onNavigateToRoute: () -> Unit, clock: Clock) {
    val now by produceState(clock.millis(), claim, clock) {
        while (value < claim.expiresAtEpochMs) {
            delay(EXPIRY_TICK_MS)
            value = clock.millis()
        }
    }
    if (now >= claim.expiresAtEpochMs) {
        Text(stringResource(R.string.claims_expired))
    } else {
        if (claim.totalUnits > 1 || claim.items.size > 1) {
            Text(stringResource(R.string.claims_reserved_units, claim.totalUnits))
            claim.items.forEach { item ->
                Text(stringResource(R.string.claims_item_line, item.quantity, item.offerTitle ?: item.offerId))
            }
        }
        SensitiveQrGate(unlockKey = claim.claimId, deviceAuthenticator = currentDeviceAuthenticator()) {
            ConfirmedClaimQr(claim.qrPayload)
            Text(claim.code)
        }
        Button(shape = SensusButtonShape, onClick = onNavigateToRoute) { Text(stringResource(R.string.claims_route)) }
    }
    Text(stringResource(R.string.claims_expiry, Instant.ofEpochMilli(claim.expiresAtEpochMs).toString()))
}
