package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.NavigationIntentUtils
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary

@Composable
fun ProductDetailScreen(
    cartViewModel: CartViewModel = hiltViewModel(),
    offer: ProductOffer,
    callbacks: ProductDetailCallbacks,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val savedViewModel: SavedInterestsViewModel = hiltViewModel()
    val friendsViewModel: FriendsViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val savedState by savedViewModel.viewState.collectAsStateWithLifecycle()
    val friendsState by friendsViewModel.viewState.collectAsStateWithLifecycle()
    var showShareSheet by remember(offer.id) { mutableStateOf(false) }

    ProductDetailEffects(offer = offer, viewModel = viewModel, onRequireAuth = callbacks.onRequireAuth)
    SavedInterestsEffectHandler(savedViewModel = savedViewModel, onRequireAuth = callbacks.onRequireAuth)
    ShareOfferEffectsHandler(
        friendsViewModel = friendsViewModel,
        onShared = { showShareSheet = false },
        onRequireAuth = callbacks.onRequireAuth
    )

    ProductDetailContent(
        offer = offer,
        state = state,
        actions = ProductDetailActions(
            navigation = callbacks,
            onIntent = viewModel::dispatchIntent,
            onAddToCart = { current, quantity -> cartViewModel.dispatchIntent(CartViewIntent.AddOffer(current, quantity)) },
            isSaved = offer.id in savedState.savedOfferIds,
            onToggleSaved = { target -> savedViewModel.dispatchIntent(SavedInterestsViewIntent.ToggleOffer(target)) },
            onShareOffer = {
                if (friendsState.isGuest) {
                    callbacks.onRequireAuth()
                } else {
                    showShareSheet = true
                }
            }
        ),
        modifier = modifier
    )

    if (showShareSheet) {
        val offerToShare = state.details?.offer ?: offer
        ShareOfferToFriendsDialog(
            offer = offerToShare,
            friends = friendsState.friends,
            isSharing = friendsState.isSharingOffer,
            actions = ShareOfferDialogActions(
                onRefreshFriends = { friendsViewModel.dispatchIntent(FriendsViewIntent.Refresh) },
                onDismiss = { showShareSheet = false },
                onSendToFriends = { recipientIds ->
                    showShareSheet = false
                    friendsViewModel.dispatchIntent(FriendsViewIntent.ShareOffer(offerToShare.id, recipientIds))
                }
            )
        )
    }
}

@Composable
private fun ProductDetailEffects(offer: ProductOffer, viewModel: ProductDetailViewModel, onRequireAuth: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(offer.id) {
        viewModel.dispatchIntent(ProductDetailViewIntent.Load(offer.id, offer.likesCount, offer.isLikedByMe))
    }
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                ProductDetailViewEffect.RequireAuth -> onRequireAuth()
                is ProductDetailViewEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ShareOfferEffectsHandler(friendsViewModel: FriendsViewModel, onShared: () -> Unit, onRequireAuth: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(friendsViewModel) {
        friendsViewModel.viewEffect.collect { effect ->
            when (effect) {
                is FriendsViewEffect.OfferShared -> onShared()
                is FriendsViewEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                FriendsViewEffect.RequireAuth -> onRequireAuth()
                FriendsViewEffect.OpenFriendsTab -> Unit
            }
        }
    }
}

@Composable
internal fun ProductDetailContent(offer: ProductOffer, state: ProductDetailViewState, actions: ProductDetailActions, modifier: Modifier = Modifier) {
    val current = state.details?.offer?.copy(distanceMeters = offer.distanceMeters) ?: offer.copy(isVerified = false, rating = 0.0, reviewCount = 0)
    val canClaim = state.details?.canClaim(System.currentTimeMillis()) == true
    val context = LocalContext.current
    var showAddedNotice by remember(offer.id) { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        ProductDetailScrollSection(
            offer = current,
            showAddedNotice = showAddedNotice,
            state = state,
            actions = actions
        )
        ProductDetailBottomBar(
            offer = current,
            quantity = state.quantity,
            actions = ProductBottomBarActions(
                enabled = canClaim,
                onAddToCart = {
                    actions.onAddToCart(current, state.quantity)
                    showAddedNotice = true
                    Toast.makeText(context, context.getString(R.string.detail_added_to_cart), Toast.LENGTH_SHORT).show()
                },
                onApartarDirect = {
                    if (state.details?.hasActiveClaim == true) {
                        actions.navigation.onViewActiveClaim(current)
                    } else {
                        actions.navigation.onApartarDirect(current, 1)
                    }
                },
                hasActiveClaim = state.details?.hasActiveClaim == true
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProductDetailScrollSection(offer: ProductOffer, showAddedNotice: Boolean, state: ProductDetailViewState, actions: ProductDetailActions) {
    val context = LocalContext.current
    val quantity = state.quantity
    val callbacks = actions.navigation
    val hasCoordinates = offer.latitude.isFinite() && offer.longitude.isFinite()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        ProductDetailTopBar(
            onNavigateBack = callbacks.onNavigateBack,
            onShareClick = actions.onShareOffer,
            onFavoriteClick = { actions.onToggleSaved(offer) },
            isSaved = actions.isSaved
        )
        ProductDetailStatus(state) { actions.onIntent(ProductDetailViewIntent.Load(offer.id)) }
        if (showAddedNotice) {
            ProductAddedNoticeBanner(storeName = offer.storeName)
        }
        ProductVisualHeroCard(offer = offer)
        Spacer(modifier = Modifier.height(16.dp))
        ProductTitleAndDatesSection(offer = offer)
        Spacer(modifier = Modifier.height(12.dp))
        ProductDetailSocialSection(offer = offer, state = state, actions = actions)
        QuantitySelectorCard(
            quantity = quantity,
            maxUnits = offer.unitsLeft,
            onDecrease = { if (quantity > 1) actions.onIntent(ProductDetailViewIntent.ChangeQuantity(quantity - 1)) },
            onIncrease = { if (quantity < offer.unitsLeft) actions.onIntent(ProductDetailViewIntent.ChangeQuantity(quantity + 1)) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductMerchantCard(
            offer = offer,
            hasRouteNavigation = callbacks.onNavigateToRoute != null && hasCoordinates,
            onGoogleMapsClick = {
                if (hasCoordinates) NavigationIntentUtils.launchGoogleMapsDirections(context, offer.latitude, offer.longitude, offer.storeName)
            },
            onRouteClick = { callbacks.onNavigateToRoute?.invoke(offer) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductDescriptionCard(description = offer.description, quantity = quantity)
        if (state.alternatives.isNotEmpty()) {
            ProductDetailAlternativesSection(
                alternatives = state.alternatives,
                onSelectAlternative = { altOffer ->
                    actions.onIntent(ProductDetailViewIntent.Load(altOffer.id))
                }
            )
        }
    }
}

@Composable
private fun ProductDetailSocialSection(offer: ProductOffer, state: ProductDetailViewState, actions: ProductDetailActions) {
    ProductSocialLikeRow(
        likesCount = state.likesCount,
        isLiked = state.isLiked,
        isBusy = state.isTogglingLike,
        onToggleLike = { actions.onIntent(ProductDetailViewIntent.ToggleLike(offer.id)) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    ProductSaveInterestPill(isSaved = actions.isSaved, onClick = { actions.onToggleSaved(offer) })
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun ProductDetailAlternativesSection(
    alternatives: List<com.example.sensus.domain.model.OfferAlternativeItem>,
    onSelectAlternative: (ProductOffer) -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Ofertas similares cerca de ti",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = SensusTextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
    alternatives.forEach { alt ->
        val altOffer = alt.toProductOffer()
        ProductOfferCard(
            offer = altOffer,
            onClick = { onSelectAlternative(altOffer) }
        )
    }
}

@Composable
internal fun ProductDetailStatus(state: ProductDetailViewState, onRetry: () -> Unit) {
    when {
        state.isLoading -> Text(stringResource(R.string.detail_refreshing), modifier = Modifier.padding(16.dp))
        state.error != null -> Column(modifier = Modifier.padding(16.dp)) {
            Text(state.error.message, color = SensusRed)
            TextButton(onClick = onRetry) { Text(stringResource(R.string.detail_retry)) }
        }
        state.details?.hasActiveClaim == true -> Text(stringResource(R.string.detail_active_claim), modifier = Modifier.padding(16.dp))
        state.details?.canClaim(System.currentTimeMillis()) == false -> Text(stringResource(R.string.detail_unavailable), modifier = Modifier.padding(16.dp))
    }
}

@ComposeUiModel
internal data class ProductDetailActions(
    val navigation: ProductDetailCallbacks,
    val onIntent: (ProductDetailViewIntent) -> Unit,
    val onAddToCart: (ProductOffer, Int) -> Unit,
    val isSaved: Boolean = false,
    val onToggleSaved: (ProductOffer) -> Unit = {},
    val onShareOffer: () -> Unit = {}
)
