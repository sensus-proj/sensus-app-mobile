package com.example.sensus.presentation.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.BuyerHomeCallbacks
import com.example.sensus.presentation.buyer.BuyerHomeScreen
import com.example.sensus.presentation.buyer.BuyerMapCallbacks
import com.example.sensus.presentation.buyer.BuyerMapScreen
import com.example.sensus.presentation.buyer.BuyerProfileCallbacks
import com.example.sensus.presentation.buyer.BuyerProfileScreen
import com.example.sensus.presentation.buyer.ClaimOfferScreen
import com.example.sensus.presentation.buyer.ClaimsScreenActions
import com.example.sensus.presentation.buyer.CommunityScreen
import com.example.sensus.presentation.buyer.FriendDeepLinkHandler
import com.example.sensus.presentation.buyer.NotificationsScreen
import com.example.sensus.presentation.buyer.ProductDetailCallbacks
import com.example.sensus.presentation.buyer.ProductDetailScreen
import com.example.sensus.presentation.buyer.RouteMapScreen
import com.example.sensus.presentation.buyer.SavedInterestsCallbacks
import com.example.sensus.presentation.buyer.SavedInterestsScreen
import com.example.sensus.presentation.buyer.SensiChatScreen
import com.example.sensus.presentation.buyer.SensiOfferActions
import com.example.sensus.presentation.merchant.MerchantDashboardCallbacks
import com.example.sensus.presentation.merchant.MerchantDashboardScreen
import com.example.sensus.presentation.merchant.MerchantEditOfferScreen
import com.example.sensus.presentation.merchant.MerchantNewOfferScreen
import com.example.sensus.presentation.merchant.MerchantProfileScreen
import com.example.sensus.presentation.merchant.MerchantRegisterScreen
import com.example.sensus.presentation.merchant.MerchantSalesScreen
import com.example.sensus.theme.SensusBackground

private const val TAB_SENSI = 2
private const val TAB_COMMUNITY = 3
private const val TAB_PROFILE = 4
private const val MERCHANT_TAB_SALES = 2
private const val MERCHANT_TAB_PROFILE = 3

@Composable
fun MainAppContainer(
    modifier: Modifier = Modifier,
    isGuest: Boolean = false,
    isMerchantRegistered: Boolean = false,
    onLogout: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {}
) {
    val state = rememberMainAppState()
    val context = LocalContext.current

    LaunchedEffect(isMerchantRegistered) {
        if (isMerchantRegistered) {
            state.isMerchantRegistered = true
        } else if (!state.isMerchantMode) {
            state.isMerchantRegistered = false
        }
    }

    FriendDeepLinkHandler(
        isGuest = isGuest,
        onRequireAuth = { state.showGuestPrompt = true },
        onFriendLinked = {
            state.isMerchantMode = false
            state.buyerTabIndex = TAB_COMMUNITY
            state.openFriendsTabSignal += 1
        }
    )

    BackHandler(enabled = true) {
        handleMainBackNavigation(state, context)
    }

    val renderedOverlay = renderFullscreenOverlay(state, isGuest)
    if (!renderedOverlay) {
        MainAppScaffoldContent(
            state = state,
            isGuest = isGuest,
            onLogout = onLogout,
            onNavigateToTerms = onNavigateToTerms,
            modifier = modifier
        )
    }
}

@Composable
private fun renderFullscreenOverlay(state: MainAppState, isGuest: Boolean): Boolean =
    renderPrimaryOverlay(state, isGuest) || renderSecondaryOverlay(state, isGuest)

@Composable
private fun renderPrimaryOverlay(state: MainAppState, isGuest: Boolean): Boolean {
    when {
        state.showNotifications -> RenderNotificationsOverlay(state)
        state.activeRouteOffer != null -> RenderRouteOverlay(state, isGuest)
        state.activeApartadoOffer != null -> RenderApartadoOfferOverlay(state)
        else -> return false
    }
    return true
}

@Composable
private fun renderSecondaryOverlay(state: MainAppState, isGuest: Boolean): Boolean {
    when {
        state.selectedProductForDetail != null -> RenderProductDetailOverlay(state, isGuest)
        renderStoreAndFriendOverlays(state) -> Unit
        state.showSavedInterests -> RenderSavedInterestsOverlay(state)
        state.selectedOfferForEdit != null -> RenderMerchantEditOfferOverlay(state)
        state.showMerchantRegister -> RenderMerchantRegisterOverlay(state)
        else -> return false
    }
    return true
}

@Composable
private fun RenderNotificationsOverlay(state: MainAppState) {
    NotificationsScreen(
        onNavigateBack = { state.showNotifications = false },
        onNotificationActionClick = {}
    )
}

@Composable
private fun RenderRouteOverlay(state: MainAppState, isGuest: Boolean) {
    val offer = state.activeRouteOffer ?: return
    RouteMapScreen(
        offer = offer,
        onNavigateBack = { state.activeRouteOffer = null },
        onShowQrClick = {
            state.activeRouteOffer = null
            if (isGuest) {
                state.showGuestPrompt = true
            } else {
                state.activeClaimExpected = false
                state.activeApartadoOffer = offer
            }
        }
    )
}

@Composable
private fun RenderApartadoOfferOverlay(state: MainAppState) {
    val offer = state.activeApartadoOffer ?: return
    ClaimOfferScreen(
        offer = offer,
        actions = ClaimsScreenActions(
            expectExisting = state.activeClaimExpected,
            onNavigateBack = { state.activeApartadoOffer = null },
            onNavigateToRoute = {
                state.activeApartadoOffer = null
                state.activeRouteOffer = offer
            }
        )
    )
}

@Composable
private fun RenderProductDetailOverlay(state: MainAppState, isGuest: Boolean) {
    val product = state.selectedProductForDetail ?: return
    ProductDetailScreen(
        offer = product,
        callbacks = ProductDetailCallbacks(
            onNavigateBack = { state.selectedProductForDetail = null },
            onApartarClick = { offer -> openPublishedClaim(state, isGuest, offer) },
            onApartarDirect = { offer, _ -> openPublishedClaim(state, isGuest, offer) },
            onNavigateToRoute = { offer ->
                state.selectedProductForDetail = null
                state.activeRouteOffer = offer
            },
            onViewActiveClaim = { offer -> openPublishedClaim(state, isGuest, offer, expectExisting = true) },
            onRequireAuth = { state.showGuestPrompt = true }
        )
    )
}

@Composable
private fun RenderSavedInterestsOverlay(state: MainAppState) {
    SavedInterestsScreen(
        callbacks = SavedInterestsCallbacks(
            onNavigateBack = { state.showSavedInterests = false },
            onOfferClick = { offer -> state.selectedProductForDetail = offer },
            onMerchantClick = { merchantId ->
                state.showSavedInterests = false
                state.communityMerchantId = merchantId
                state.buyerTabIndex = 1
            },
            onExploreOffers = {
                state.showSavedInterests = false
                state.buyerTabIndex = 1
            }
        )
    )
}

@Composable
private fun RenderMerchantEditOfferOverlay(state: MainAppState) {
    val offer = state.selectedOfferForEdit ?: return
    MerchantEditOfferScreen(
        offer = offer,
        onNavigateBack = { state.selectedOfferForEdit = null },
        onOfferChanged = { state.selectedOfferForEdit = null }
    )
}

@Composable
private fun RenderMerchantRegisterOverlay(state: MainAppState) {
    MerchantRegisterScreen(
        onNavigateBack = { state.showMerchantRegister = false },
        onRegisterSuccess = {
            state.showMerchantRegister = false
            state.isMerchantRegistered = true
            state.isMerchantMode = true
            state.merchantTabIndex = 0
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainAppScaffoldContent(state: MainAppState, isGuest: Boolean, onLogout: () -> Unit, onNavigateToTerms: () -> Unit, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue = if (state.isFamOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "RainbowAccordionProgress"
    )
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0 || WindowInsets.isImeVisible

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SensusBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!state.isMerchantMode) {
                BuyerTabContent(state = state, isGuest = isGuest, onNavigateToTerms = onNavigateToTerms)
            } else {
                MerchantTabContent(state = state)
            }
            FamBackdropScrim(state.isFamOpen, animProgress) { state.isFamOpen = false }
            if (!isKeyboardOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 6.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    RainbowFloatingMenuBar(state = state, animProgress = animProgress)
                }
            }
            MainAppDialogs(state = state, isGuest = isGuest, onLogout = onLogout)
        }
    }
}

private fun sensiOfferActions(state: MainAppState, isGuest: Boolean, context: android.content.Context) = SensiOfferActions(
    onApartarPackage = {
        runWhenAuthenticated(state, isGuest) {
            Toast.makeText(context, context.getString(R.string.detail_feature_unavailable), Toast.LENGTH_SHORT).show()
        }
    },
    onDetail = { state.selectedProductForDetail = it },
    onApartar = { openPublishedClaim(state, isGuest, it) },
    onRoute = { state.activeRouteOffer = it }
)

@Composable
private fun BuyerTabContent(state: MainAppState, isGuest: Boolean, onNavigateToTerms: () -> Unit) {
    val openCartGuarded = { runWhenAuthenticated(state, isGuest) { state.showCartSheet = true } }
    val context = LocalContext.current
    when (state.buyerTabIndex) {
        0 -> BuyerMapScreen(
            callbacks = BuyerMapCallbacks(
                isGuest = isGuest,
                onRequireAuth = { state.showGuestPrompt = true },
                onProductClick = { state.selectedProductForDetail = it },
                onOpenStoreCatalog = { state.selectedStoreCatalog = it },
                onRouteClick = { state.activeRouteOffer = it },
                onListToggle = { state.buyerTabIndex = 1 },
                onOpenCart = openCartGuarded
            )
        )
        1 -> BuyerHomeScreen(
            callbacks = BuyerHomeCallbacks(
                merchantId = state.communityMerchantId,
                onClearMerchantFilter = { state.communityMerchantId = null },
                isGuest = isGuest,
                onRequireAuth = { state.showGuestPrompt = true },
                onProductClick = { state.selectedProductForDetail = it },
                onMapToggle = { state.buyerTabIndex = 0 },
                onNotificationsClick = { state.showNotifications = true },
                onOpenCart = openCartGuarded
            )
        )
        TAB_SENSI -> SensiChatScreen(
            actions = sensiOfferActions(state, isGuest, context),
            onRequireAuth = { state.showGuestPrompt = true }
        )
        TAB_COMMUNITY -> CommunityScreen(
            callbacks = buildCommunityCallbacks(state),
            openFriendsSignal = state.openFriendsTabSignal
        )
        TAB_PROFILE -> BuyerProfileScreen(
            callbacks = BuyerProfileCallbacks(
                isMerchantMode = state.isMerchantMode,
                hasActiveMerchant = state.isMerchantRegistered,
                onMerchantStatusSynced = { if (it) state.isMerchantRegistered = true },
                onToggleMerchantMode = { toggleMerchantMode(state, isGuest, it) },
                onLogoutClick = { state.showConfirmLogout = true },
                onDeleteAccountClick = { state.showConfirmDeleteAccount = true },
                onTermsClick = onNavigateToTerms,
                onHistoryClick = { runWhenAuthenticated(state, isGuest) { state.showSavedInterests = true } },
                onGuestActionRequired = { state.showGuestPrompt = true }
            )
        )
    }
}

private fun runWhenAuthenticated(state: MainAppState, isGuest: Boolean, action: () -> Unit) {
    if (isGuest) state.showGuestPrompt = true else action()
}

@Composable
private fun MerchantTabContent(state: MainAppState) {
    when (state.merchantTabIndex) {
        0 -> MerchantDashboardScreen(
            callbacks = MerchantDashboardCallbacks(
                onNewOfferClick = { state.merchantTabIndex = 1 },
                onScanQrClick = { state.merchantTabIndex = MERCHANT_TAB_SALES },
                onSwitchToBuyer = { state.isMerchantMode = false },
                onEditOfferClick = { offer -> state.selectedOfferForEdit = offer }
            )
        )
        1 -> MerchantNewOfferScreen(
            onNavigateBack = { state.merchantTabIndex = 0 },
            onOfferCreated = { state.merchantTabIndex = 0 }
        )
        MERCHANT_TAB_SALES -> MerchantSalesScreen()
        MERCHANT_TAB_PROFILE -> MerchantProfileScreen(
            onSwitchToBuyer = { state.isMerchantMode = false }
        )
    }
}

internal fun toggleMerchantMode(state: MainAppState, isGuest: Boolean, enableMerchant: Boolean) {
    when {
        isGuest -> state.showGuestPrompt = true
        enableMerchant && state.isMerchantRegistered -> {
            state.isMerchantMode = true
            state.merchantTabIndex = 0
        }
        enableMerchant -> state.showMerchantRegister = true
        else -> state.isMerchantMode = false
    }
}

internal fun openPublishedClaim(state: MainAppState, isGuest: Boolean, offer: ProductOffer, expectExisting: Boolean = false) {
    runWhenAuthenticated(state, isGuest) {
        state.selectedProductForDetail = null
        state.activeClaimExpected = expectExisting
        state.activeApartadoOffer = offer
    }
}
