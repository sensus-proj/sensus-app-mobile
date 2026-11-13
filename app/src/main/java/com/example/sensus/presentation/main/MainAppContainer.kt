package com.example.sensus.presentation.main

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.buyer.ActiveApartadoScreen
import com.example.sensus.presentation.buyer.BuyerHomeCallbacks
import com.example.sensus.presentation.buyer.BuyerHomeScreen
import com.example.sensus.presentation.buyer.BuyerMapCallbacks
import com.example.sensus.presentation.buyer.BuyerMapScreen
import com.example.sensus.presentation.buyer.BuyerProfileCallbacks
import com.example.sensus.presentation.buyer.BuyerProfileScreen
import com.example.sensus.presentation.buyer.CommunityScreen
import com.example.sensus.presentation.buyer.NotificationsScreen
import com.example.sensus.presentation.buyer.ProductDetailCallbacks
import com.example.sensus.presentation.buyer.ProductDetailScreen
import com.example.sensus.presentation.buyer.RouteMapScreen
import com.example.sensus.presentation.buyer.SensiChatScreen
import com.example.sensus.presentation.merchant.MerchantDashboardScreen
import com.example.sensus.presentation.merchant.MerchantNewOfferScreen
import com.example.sensus.presentation.merchant.MerchantProfileScreen
import com.example.sensus.presentation.merchant.MerchantRegisterScreen
import com.example.sensus.presentation.merchant.MerchantSalesScreen
import com.example.sensus.theme.SensusBackground

private const val FALLBACK_ORIG_PRICE = 100.0
private const val FALLBACK_DISC_PRICE = 50.0
private const val FALLBACK_DISCOUNT_PCT = 50
private const val FALLBACK_UNITS_LEFT = 10
private const val FALLBACK_DIST_METERS = 200
private const val TAB_SENSI = 2
private const val TAB_COMMUNITY = 3
private const val TAB_PROFILE = 4
private const val MERCHANT_TAB_SALES = 2
private const val MERCHANT_TAB_PROFILE = 3

private val defaultFallbackOffer = ProductOffer(
    id = "sample_fallback",
    name = "Oferta Local Sensus",
    storeName = "Tienda Local",
    category = "Abarrotes",
    originalPrice = FALLBACK_ORIG_PRICE,
    discountPrice = FALLBACK_DISC_PRICE,
    discountPercent = FALLBACK_DISCOUNT_PCT,
    unitsLeft = FALLBACK_UNITS_LEFT,
    distanceMeters = FALLBACK_DIST_METERS
)

@Composable
fun MainAppContainer(modifier: Modifier = Modifier, isGuest: Boolean = false, onLogout: () -> Unit = {}, onNavigateToTerms: () -> Unit = {}) {
    val state = rememberMainAppState()
    val context = LocalContext.current

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
private fun renderFullscreenOverlay(state: MainAppState, isGuest: Boolean): Boolean {
    when {
        state.showNotifications -> RenderNotificationsOverlay(state, isGuest)
        state.activeRouteOffer != null -> RenderRouteOverlay(state, isGuest)
        state.activeApartadoOrder != null -> RenderApartadoOrderOverlay(state)
        state.activeApartadoOffer != null -> RenderApartadoOfferOverlay(state)
        state.selectedProductForDetail != null -> RenderProductDetailOverlay(state, isGuest)
        state.showMerchantRegister -> RenderMerchantRegisterOverlay(state)
        else -> return false
    }
    return true
}

@Composable
private fun RenderNotificationsOverlay(state: MainAppState, isGuest: Boolean) {
    NotificationsScreen(
        onNavigateBack = { state.showNotifications = false },
        onNotificationActionClick = { notif ->
            state.showNotifications = false
            if (notif.category == "Ofertas") {
                state.selectedProductForDetail = defaultFallbackOffer
            } else if (notif.category == "Reservas") {
                if (isGuest) state.showGuestPrompt = true else state.activeApartadoOffer = defaultFallbackOffer
            }
        }
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
            if (isGuest) state.showGuestPrompt = true else state.activeApartadoOffer = offer
        }
    )
}

@Composable
private fun RenderApartadoOrderOverlay(state: MainAppState) {
    val order = state.activeApartadoOrder ?: return
    ActiveApartadoScreen(
        order = order,
        onNavigateBack = { state.activeApartadoOrder = null },
        onNavigateToRoute = {
            val firstOffer = order.items.first().offer
            state.activeApartadoOrder = null
            state.activeRouteOffer = firstOffer
        },
        onCancelApartado = { state.activeApartadoOrder = null }
    )
}

@Composable
private fun RenderApartadoOfferOverlay(state: MainAppState) {
    val offer = state.activeApartadoOffer ?: return
    ActiveApartadoScreen(
        offer = offer,
        onNavigateBack = { state.activeApartadoOffer = null },
        onNavigateToRoute = {
            state.activeApartadoOffer = null
            state.activeRouteOffer = offer
        },
        onCancelApartado = { state.activeApartadoOffer = null }
    )
}

@Composable
private fun RenderProductDetailOverlay(state: MainAppState, isGuest: Boolean) {
    val product = state.selectedProductForDetail ?: return
    ProductDetailScreen(
        offer = product,
        callbacks = ProductDetailCallbacks(
            onNavigateBack = { state.selectedProductForDetail = null },
            onApartarClick = { offer ->
                if (isGuest) {
                    state.showGuestPrompt = true
                } else {
                    state.selectedProductForDetail = null
                    state.activeApartadoOrder = ApartadoOrder.fromSingleOffer(offer, 1)
                }
            },
            onApartarDirect = { offer, quantity ->
                if (isGuest) {
                    state.showGuestPrompt = true
                } else {
                    state.selectedProductForDetail = null
                    state.activeApartadoOrder = ApartadoOrder.fromSingleOffer(offer, quantity)
                }
            },
            onNavigateToRoute = { offer ->
                state.selectedProductForDetail = null
                state.activeRouteOffer = offer
            }
        )
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

@Composable
private fun BuyerTabContent(state: MainAppState, isGuest: Boolean, onNavigateToTerms: () -> Unit) {
    val openCartGuarded = { runWhenAuthenticated(state, isGuest) { state.showCartSheet = true } }
    val openApartadoGuarded = { runWhenAuthenticated(state, isGuest) { state.activeApartadoOffer = defaultFallbackOffer } }
    when (state.buyerTabIndex) {
        0 -> BuyerMapScreen(
            callbacks = BuyerMapCallbacks(
                isGuest = isGuest,
                onRequireAuth = { state.showGuestPrompt = true },
                onProductClick = { state.selectedProductForDetail = it },
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
        TAB_SENSI -> SensiChatScreen(onApartarComboClick = openApartadoGuarded)
        TAB_COMMUNITY -> CommunityScreen(
            onRequireAuth = { state.showGuestPrompt = true },
            onMerchantClick = { id ->
                state.communityMerchantId = id
                state.buyerTabIndex = 1
            }
        )
        TAB_PROFILE -> BuyerProfileScreen(
            callbacks = BuyerProfileCallbacks(
                isMerchantMode = state.isMerchantMode,
                onToggleMerchantMode = { enableMerchant -> toggleMerchantMode(state, isGuest, enableMerchant) },
                onLogoutClick = { state.showConfirmLogout = true },
                onDeleteAccountClick = { state.showConfirmDeleteAccount = true },
                onTermsClick = onNavigateToTerms,
                onHistoryClick = openApartadoGuarded,
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
            onNewOfferClick = { state.merchantTabIndex = 1 },
            onScanQrClick = { state.merchantTabIndex = MERCHANT_TAB_SALES },
            onSwitchToBuyer = { state.isMerchantMode = false }
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
