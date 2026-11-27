package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.SecureScreenEffect
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import java.time.Clock
import kotlinx.coroutines.delay

private const val CLAIM_CONTENT_PADDING = 16
private const val CONTENT_SPACING = 14
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
    Column(
        modifier
            .fillMaxSize()
            .background(SensusVioletBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(CLAIM_CONTENT_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp)
    ) {
        ClaimTopBar(onNavigateBack = actions.onNavigateBack)
        val claim = state.result
        if (claim != null) {
            state.error?.let { ClaimErrorCard(it) }
            ClaimConfirmationContent(offer, claim, actions.onNavigateToRoute, actions.clock)
        } else {
            ClaimPendingContent(offer, state, onClaim)
        }
    }
}

@Composable
private fun ClaimPendingContent(offer: ProductOffer, state: ClaimOfferViewState, onClaim: () -> Unit) {
    val summaryItems = state.items.ifEmpty { listOf(CartItem(offer, 1)) }
    val totalUnits = state.totalUnits
    if (totalUnits == 1 && summaryItems.size == 1) {
        ClaimProductCard(offer)
        Text(stringResource(R.string.claims_one_unit), fontSize = 13.sp, color = SensusTextSecondary)
    } else {
        ClaimItemsCard(
            title = stringResource(R.string.claims_multi_units, totalUnits),
            lines = summaryItems.map { stringResource(R.string.claims_item_line, it.quantity, it.offer.name) }
        )
    }
    if (state.loading) ClaimLoadingIndicator()
    state.error?.let { ClaimErrorCard(it) }
    if (state.existingCodeUnavailable) {
        ClaimExistingCodeUnavailableCard()
    } else {
        val label = if (totalUnits == 1) stringResource(R.string.claims_confirm) else stringResource(R.string.claims_confirm_multi, totalUnits)
        ClaimConfirmButton(label = label, enabled = state.canSubmit, onClick = onClaim)
    }
}

@Composable
private fun ClaimConfirmationContent(offer: ProductOffer, claim: OfferClaim, onNavigateToRoute: () -> Unit, clock: Clock) {
    val now by produceState(clock.millis(), claim, clock) {
        while (value < claim.expiresAtEpochMs) {
            delay(EXPIRY_TICK_MS)
            value = clock.millis()
        }
    }
    val expired = now >= claim.expiresAtEpochMs
    val multiUnit = claim.totalUnits > 1 || claim.items.size > 1
    if (!expired) ClaimConfirmedHeader(storeName = offer.storeName)
    if (multiUnit) {
        ClaimItemsCard(
            title = stringResource(R.string.claims_reserved_units, claim.totalUnits),
            lines = claim.items.map { stringResource(R.string.claims_item_line, it.quantity, it.offerTitle ?: it.offerId) }
        )
    } else {
        ClaimProductCard(offer)
    }
    if (expired) {
        ClaimExpiredCard()
    } else {
        ClaimQrCard(claim)
        ClaimCountdownCard(remainingMs = claim.expiresAtEpochMs - now)
    }
    ClaimExpiryCaption(claim.expiresAtEpochMs)
    if (!expired) {
        ClaimPickupLocation(offer)
        ClaimRouteButton(onClick = onNavigateToRoute)
    }
}
