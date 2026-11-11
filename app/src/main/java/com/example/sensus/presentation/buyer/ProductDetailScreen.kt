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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.AddToCartResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.NavigationIntentUtils
import com.example.sensus.theme.SensusBackground

@Composable
fun ProductDetailScreen(cartViewModel: CartViewModel = hiltViewModel(), offer: ProductOffer, callbacks: ProductDetailCallbacks, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var quantity by remember { mutableIntStateOf(1) }
    var conflictDialogState by remember { mutableStateOf<AddToCartResult.StoreConflict?>(null) }
    var showAddedNotice by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        ProductDetailScrollSection(
            offer = offer,
            quantity = quantity,
            showAddedNotice = showAddedNotice,
            callbacks = callbacks,
            onQuantityChange = { next -> quantity = next }
        )

        ProductDetailBottomBar(
            offer = offer,
            quantity = quantity,
            onAddToCart = {
                cartViewModel.dispatchIntent(CartViewIntent.AddOffer(offer, quantity))
                showAddedNotice = true
                Toast.makeText(context, "$quantity x ${offer.name} agregado a la bolsa", Toast.LENGTH_SHORT).show()
            },
            onApartarDirect = { callbacks.onApartarDirect(offer, quantity) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        conflictDialogState?.let { conflict ->
            StoreConflictAlertDialog(
                conflict = conflict,
                onConfirmReplace = {
                    cartViewModel.dispatchIntent(CartViewIntent.ClearCart)
                    cartViewModel.dispatchIntent(CartViewIntent.AddOffer(offer, quantity))
                    conflictDialogState = null
                    showAddedNotice = true
                    Toast.makeText(context, "Bolsa actualizada con ${offer.storeName}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { conflictDialogState = null }
            )
        }
    }
}

@Composable
private fun ProductDetailScrollSection(
    offer: ProductOffer,
    quantity: Int,
    showAddedNotice: Boolean,
    callbacks: ProductDetailCallbacks,
    onQuantityChange: (Int) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        ProductDetailTopBar(
            onNavigateBack = callbacks.onNavigateBack,
            onShareClick = { Toast.makeText(context, "Enlace de oferta copiado", Toast.LENGTH_SHORT).show() },
            onFavoriteClick = { Toast.makeText(context, "Guardado en favoritos", Toast.LENGTH_SHORT).show() }
        )
        if (showAddedNotice) {
            ProductAddedNoticeBanner(storeName = offer.storeName)
        }
        ProductVisualHeroCard(offer = offer)
        Spacer(modifier = Modifier.height(16.dp))
        ProductTitleAndDatesSection(offer = offer)
        Spacer(modifier = Modifier.height(16.dp))
        QuantitySelectorCard(
            quantity = quantity,
            maxUnits = offer.unitsLeft,
            onDecrease = { if (quantity > 1) onQuantityChange(quantity - 1) },
            onIncrease = { if (quantity < offer.unitsLeft) onQuantityChange(quantity + 1) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductMerchantCard(
            offer = offer,
            hasRouteNavigation = callbacks.onNavigateToRoute != null,
            onGoogleMapsClick = {
                NavigationIntentUtils.launchGoogleMapsDirections(context, offer.latitude, offer.longitude, offer.storeName)
            },
            onRouteClick = { callbacks.onNavigateToRoute?.invoke(offer) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProductDescriptionCard(description = offer.description, quantity = quantity)
    }
}
