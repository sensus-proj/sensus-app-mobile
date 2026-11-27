package com.example.sensus.presentation.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.MerchantOfferMetrics
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.CommerceFeedFeedback
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusTextButtonColors

@Composable
fun MerchantDashboardScreen(callbacks: MerchantDashboardCallbacks, viewModel: MerchantDashboardViewModel = hiltViewModel(), modifier: Modifier = Modifier) {
    val dashState by viewModel.viewState.collectAsStateWithLifecycle()
    var selectedOfferForStock by remember { mutableStateOf<ProductOffer?>(null) }

    LaunchedEffect(Unit) {
        viewModel.dispatchIntent(MerchantDashboardViewIntent.Refresh)
    }

    MerchantDashboardFeed(
        dashState = dashState,
        callbacks = callbacks,
        onIntent = viewModel::dispatchIntent,
        onAdjustStockClick = { selectedOfferForStock = it },
        modifier = modifier
    )

    selectedOfferForStock?.let { offer ->
        AdjustOfferStockDialog(
            offer = offer,
            onDismiss = { selectedOfferForStock = null },
            onConfirm = { newStock ->
                viewModel.dispatchIntent(MerchantDashboardViewIntent.AdjustOfferStock(offer, newStock))
                selectedOfferForStock = null
            }
        )
    }
}

@Composable
private fun MerchantDashboardFeed(
    dashState: MerchantDashboardViewState,
    callbacks: MerchantDashboardCallbacks,
    onIntent: (MerchantDashboardViewIntent) -> Unit,
    onAdjustStockClick: (ProductOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleOffers = remember(dashState.selectedFilter, dashState.allStoreOffers) { dashState.filteredOffers }
    val groups = remember(visibleOffers) { visibleOffers.groupBy { it.category }.toList() }
    val showContent = !dashState.isLoading && dashState.errorMessage == null
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusVioletBg)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = MerchantMenuClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { MerchantStoreHeader(dashState.store, dashState.hasMerchantProfile, callbacks.onSwitchToBuyer) }
        if (showContent) item { MerchantActiveOffersCard(dashState.metrics) }
        item { MerchantQuickActions(onNewOfferClick = callbacks.onNewOfferClick, onScanQrClick = callbacks.onScanQrClick) }
        item { CommerceFeedFeedback(dashState.isLoading, dashState.errorMessage) { onIntent(MerchantDashboardViewIntent.Refresh) } }
        item {
            MerchantInventoryHeader(
                productCount = visibleOffers.size,
                categoryCount = groups.size,
                selectedFilter = dashState.selectedFilter,
                allOffers = dashState.allStoreOffers,
                onSelectFilter = { onIntent(MerchantDashboardViewIntent.SelectOfferFilter(it)) }
            )
        }
        if (visibleOffers.isEmpty() && showContent) {
            item { EmptyMerchantOffersCard(dashState.selectedFilter, callbacks.onNewOfferClick) }
        } else {
            items(groups, key = { it.first }) { (category, offers) ->
                MerchantCategoryGroup(
                    category = category,
                    offers = offers,
                    initiallyExpanded = category == groups.first().first,
                    actions = MerchantInventoryActions(callbacks.onEditOfferClick, onAdjustStockClick),
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun MerchantStoreHeader(store: MerchantStore, hasProfile: Boolean, onSwitchToBuyer: () -> Unit) {
    val status = if (hasProfile) {
        stringResource(if (store.isOpen) R.string.merchant_dashboard_open else R.string.merchant_dashboard_closed)
    } else {
        null
    }
    MerchantScreenHeader(
        title = stringResource(R.string.merchant_dashboard_title),
        subtitle = listOfNotNull(store.name, status).joinToString(" · ")
    ) {
        IconButton(
            onClick = onSwitchToBuyer,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SensusSurface)
        ) {
            Icon(Icons.Default.Storefront, stringResource(R.string.merchant_dashboard_switch_buyer), tint = SensusVioletInk)
        }
    }
}

@Composable
private fun MerchantActiveOffersCard(metrics: MerchantOfferMetrics) {
    MerchantCard {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.merchant_dashboard_active_offers), fontSize = 12.sp, color = SensusTextSecondary, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
            }
            Text("${metrics.active}", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
            Text(
                stringResource(R.string.merchant_dashboard_metrics_line, metrics.availableUnits, metrics.lowStock),
                fontSize = 11.sp,
                color = SensusTextSecondary
            )
        }
    }
}

@Composable
private fun MerchantQuickActions(onNewOfferClick: () -> Unit, onScanQrClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MerchantPrimaryButton(
            text = stringResource(R.string.merchant_dashboard_publish),
            onClick = onNewOfferClick,
            style = MerchantButtonStyle(icon = Icons.Default.Add)
        )
        MerchantSecondaryButton(text = stringResource(R.string.merchant_dashboard_scan), onClick = onScanQrClick, icon = Icons.Default.QrCodeScanner)
    }
}

@Composable
private fun EmptyMerchantOffersCard(selectedFilter: MerchantOfferFilter, onNewOfferClick: () -> Unit) {
    val (title, subtitle) = when (selectedFilter) {
        MerchantOfferFilter.ACTIVE ->
            stringResource(R.string.merchant_dashboard_empty_title) to stringResource(R.string.merchant_dashboard_empty_hint)
        MerchantOfferFilter.PAUSED ->
            stringResource(R.string.merchant_inventory_empty_paused) to stringResource(R.string.merchant_inventory_empty_paused_hint)
        MerchantOfferFilter.FINISHED ->
            stringResource(R.string.merchant_inventory_empty_finished) to stringResource(R.string.merchant_inventory_empty_finished_hint)
    }
    MerchantCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SensusVioletBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(26.dp))
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary, textAlign = TextAlign.Center)
            Text(subtitle, fontSize = 13.sp, color = SensusTextSecondary, textAlign = TextAlign.Center)
            if (selectedFilter == MerchantOfferFilter.ACTIVE) {
                TextButton(colors = sensusTextButtonColors(), onClick = onNewOfferClick) {
                    Text(stringResource(R.string.merchant_dashboard_empty_action), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdjustOfferStockDialog(offer: ProductOffer, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var stockValue by remember(offer.id) { mutableIntStateOf(offer.unitsLeft) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Ajustar stock: ${offer.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Define las unidades disponibles (0 para pausar/agotar la oferta):",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StockStepButton(Icons.Default.Remove) { if (stockValue > 0) stockValue -= 1 }
                    Text(text = "$stockValue u.", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
                    StockStepButton(Icons.Default.Add) {
                        if (stockValue < OfferPublishingPolicy.MAX_STOCK) stockValue += 1
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = SensusButtonShape,
                onClick = { onConfirm(stockValue) },
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
            ) {
                Text("Guardar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(colors = sensusTextButtonColors(), onClick = onDismiss) { Text("Cancelar", color = SensusTextSecondary) }
        }
    )
}

@Composable
private fun StockStepButton(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = SensusBackground,
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
        }
    }
}
