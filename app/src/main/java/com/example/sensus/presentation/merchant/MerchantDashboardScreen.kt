package com.example.sensus.presentation.merchant

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.MerchantOfferMetrics
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.CommerceFeedFeedback
import com.example.sensus.presentation.common.CommerceHero
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusOrangeDark
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.SensusWhite
import com.example.sensus.theme.sensusOutlinedButtonColors
import com.example.sensus.theme.sensusTextButtonColors
import java.util.Locale

private const val KPI_BG_ALPHA = 0.1f
private const val ACTIVE_BADGE_ALPHA = 0.15f

@ComposeUiModel
data class MerchantDashboardCallbacks(
    val onNewOfferClick: () -> Unit,
    val onScanQrClick: () -> Unit,
    val onSwitchToBuyer: () -> Unit,
    val onEditOfferClick: (ProductOffer) -> Unit = {}
)

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
    val store = dashState.store
    val visibleOffers = remember(dashState.selectedFilter, dashState.allStoreOffers) { dashState.filteredOffers }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MerchantStoreBannerCard(store, dashState.hasMerchantProfile, callbacks.onSwitchToBuyer) }
        item { MerchantQuickActionsRow(onNewOfferClick = callbacks.onNewOfferClick, onScanQrClick = callbacks.onScanQrClick) }
        if (!dashState.isLoading && dashState.errorMessage == null) item { MerchantKpisCard(metrics = dashState.metrics) }
        item { CommerceFeedFeedback(dashState.isLoading, dashState.errorMessage) { onIntent(MerchantDashboardViewIntent.Refresh) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.commerce_merchant_offers, visibleOffers.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                MerchantOfferFilterRow(
                    selectedFilter = dashState.selectedFilter,
                    allOffers = dashState.allStoreOffers,
                    onSelectFilter = { onIntent(MerchantDashboardViewIntent.SelectOfferFilter(it)) }
                )
            }
        }
        if (visibleOffers.isEmpty() && !dashState.isLoading && dashState.errorMessage == null) {
            item {
                EmptyMerchantOffersCard(
                    selectedFilter = dashState.selectedFilter,
                    onNewOfferClick = callbacks.onNewOfferClick
                )
            }
        } else {
            items(visibleOffers, key = { it.id }) { offer ->
                MerchantOfferItem(
                    offer = offer,
                    modifier = Modifier.animateItem(),
                    onEditClick = { callbacks.onEditOfferClick(offer) },
                    onAdjustClick = { onAdjustStockClick(offer) }
                )
            }
        }
    }
}

@Composable
private fun MerchantOfferFilterRow(selectedFilter: MerchantOfferFilter, allOffers: List<ProductOffer>, onSelectFilter: (MerchantOfferFilter) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MerchantOfferFilter.entries.forEach { filter ->
            val count = allOffers.count(filter::matches)
            val selected = filter == selectedFilter
            val background by animateColorAsState(if (selected) SensusVioletBg else SensusSurface, label = "offerFilter")
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = background,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected) { onSelectFilter(filter) }
            ) {
                Text(
                    text = stringResource(R.string.merchant_inventory_filter_count, stringResource(filter.labelResource), count),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) SensusVioletInk else SensusTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyMerchantOffersCard(selectedFilter: MerchantOfferFilter, onNewOfferClick: () -> Unit) {
    val (title, subtitle) = when (selectedFilter) {
        MerchantOfferFilter.ACTIVE ->
            "Aún no tienes ofertas activas" to
                "Pulsa 'Nueva Oferta' para registrar tus productos con stock real y que aparezcan en el mapa de clientes."
        MerchantOfferFilter.PAUSED ->
            stringResource(R.string.merchant_inventory_empty_paused) to stringResource(R.string.merchant_inventory_empty_paused_hint)
        MerchantOfferFilter.FINISHED ->
            stringResource(R.string.merchant_inventory_empty_finished) to stringResource(R.string.merchant_inventory_empty_finished_hint)
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = SensusTextSecondary,
                textAlign = TextAlign.Center
            )
            if (selectedFilter == MerchantOfferFilter.ACTIVE) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(colors = sensusTextButtonColors(), onClick = onNewOfferClick) {
                    Text(text = "+ Publicar mi primera oferta", color = SensusVioletInk, fontWeight = FontWeight.Bold)
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

@Composable
private fun MerchantStoreBannerCard(store: MerchantStore, hasProfile: Boolean, onSwitchToBuyer: () -> Unit) {
    CommerceHero(title = store.name, modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        if (hasProfile) StoreOpenStatusBadge(store.isOpen)
        TextButton(
            onClick = onSwitchToBuyer,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = SensusWhite)
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Modo Comprador")
        }
    }
}

@Composable
private fun StoreOpenStatusBadge(isOpen: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isOpen) SensusSuccessBg else SensusRedBg
    ) {
        Text(
            text = if (isOpen) "Tienda Abierta" else "Tienda Cerrada",
            style = MaterialTheme.typography.labelMedium,
            color = SensusTextPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MerchantKpisCard(metrics: MerchantOfferMetrics) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = stringResource(R.string.merchant_metrics_title),
                style = MaterialTheme.typography.titleSmall,
                color = SensusTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MerchantKpi(Icons.Default.Inventory, "${metrics.active}", stringResource(R.string.merchant_metrics_active))
                MerchantKpi(Icons.Default.Inventory2, "${metrics.availableUnits}", stringResource(R.string.merchant_metrics_units))
                MerchantKpi(Icons.Default.WarningAmber, "${metrics.lowStock}", stringResource(R.string.merchant_metrics_low_stock))
            }
        }
    }
}

@Composable
private fun MerchantQuickActionsRow(onNewOfferClick: () -> Unit, onScanQrClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(shape = SensusButtonShape, onClick = onNewOfferClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Nueva Oferta")
        }
        OutlinedButton(
            colors = sensusOutlinedButtonColors(),
            shape = SensusButtonShape,
            onClick = onScanQrClick,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Escanear QR")
        }
    }
}

@Composable
private fun MerchantKpi(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SensusPurple.copy(alpha = KPI_BG_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = SensusTextPrimary)
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = SensusTextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun MerchantOfferItem(offer: ProductOffer, onEditClick: () -> Unit, onAdjustClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(offer.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${offer.category} • Quedan ${offer.unitsLeft} piezas",
                style = MaterialTheme.typography.bodySmall,
                color = SensusTextSecondary
            )
            OfferPriceAndStatusRow(offer.discountPrice, offer.status, offer.unitsLeft)
            offer.viewCount?.let { views ->
                Text(stringResource(R.string.merchant_offer_views, views), style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(colors = sensusTextButtonColors(), onClick = onEditClick, modifier = Modifier.heightIn(min = 48.dp)) { Text("Gestionar") }
                OutlinedButton(
                    colors = sensusOutlinedButtonColors(),
                    shape = SensusButtonShape,
                    onClick = onAdjustClick,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Stock")
                }
            }
        }
    }
}

@Composable
private fun OfferPriceAndStatusRow(discountPrice: Double, status: OfferLifecycleStatus, unitsLeft: Int) {
    val (label, color) = resolveStatusBadge(status, unitsLeft)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", discountPrice)} MXN",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SensusVioletInk
        )
        Surface(color = color.copy(alpha = ACTIVE_BADGE_ALPHA), shape = RoundedCornerShape(4.dp)) {
            Text(
                text = label,
                color = SensusTextPrimary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun resolveStatusBadge(status: OfferLifecycleStatus, unitsLeft: Int): Pair<String, Color> = when {
    status == OfferLifecycleStatus.ARCHIVED -> "De baja" to SensusRed
    status == OfferLifecycleStatus.EXPIRED -> "Expirada" to SensusRed
    status == OfferLifecycleStatus.PAUSED -> status.displayLabel to SensusOrangeDark
    status == OfferLifecycleStatus.SOLD_OUT || unitsLeft <= 0 -> OfferLifecycleStatus.SOLD_OUT.displayLabel to SensusRed
    else -> "Activa" to SensusSuccess
}
