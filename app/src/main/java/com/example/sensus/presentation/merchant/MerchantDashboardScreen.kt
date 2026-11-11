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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import java.util.Locale

private const val STATUS_BADGE_ALPHA = 0.25f
private const val RATING_ALPHA = 0.85f
private const val CATEGORY_ALPHA = 0.7f
private const val KPI_BG_ALPHA = 0.1f
private const val ACTIVE_BADGE_ALPHA = 0.15f

@Composable
fun MerchantDashboardScreen(
    viewModel: MerchantDashboardViewModel = hiltViewModel(),
    onNewOfferClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onSwitchToBuyer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dashState by viewModel.viewState.collectAsState()
    val store = dashState.store
    val activeStoreOffers = dashState.activeStoreOffers

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MerchantStoreBannerCard(store = store, onSwitchToBuyer = onSwitchToBuyer) }
        item { MerchantKpisCard(store = store) }
        item { MerchantQuickActionsRow(onNewOfferClick = onNewOfferClick, onScanQrClick = onScanQrClick) }
        item {
            Text(
                text = "Tus ofertas en curso (${activeStoreOffers.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
        }
        items(activeStoreOffers) { offer ->
            MerchantOfferItem(offer = offer)
        }
    }
}

@Composable
private fun MerchantStoreBannerCard(store: MerchantStore, onSwitchToBuyer: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SensusPurple),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoreOpenStatusBadge()
                SwitchToBuyerButton(onClick = onSwitchToBuyer)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = store.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${store.rating} (${store.reviewCount} opiniones)",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = RATING_ALPHA)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "• ${store.category}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = CATEGORY_ALPHA)
                )
            }
        }
    }
}

@Composable
private fun StoreOpenStatusBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SensusSuccess.copy(alpha = STATUS_BADGE_ALPHA)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Tienda Abierta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun SwitchToBuyerButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Modo Comprador", fontSize = 11.sp, color = Color.White)
    }
}

@Composable
private fun MerchantKpisCard(store: MerchantStore) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MerchantKpi(icon = Icons.Default.CheckCircle, value = "${store.redeemedTodayCount}", label = "Canjes hechos")
            MerchantKpi(icon = Icons.Default.Inventory, value = "${store.activeOffersCount}", label = "Ofertas activas")
        }
    }
}

@Composable
private fun MerchantQuickActionsRow(onNewOfferClick: () -> Unit, onScanQrClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "Nueva Oferta",
            onClick = onNewOfferClick,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.QrCodeScanner,
            label = "Escanear QR",
            onClick = onScanQrClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            Icon(imageVector = icon, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Text(text = label, fontSize = 11.sp, color = SensusTextMuted)
    }
}

@Composable
private fun MerchantOfferItem(offer: ProductOffer) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = offer.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${offer.category} • Quedan ${offer.unitsLeft} piezas", fontSize = 12.sp, color = SensusTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                OfferPriceAndStatusRow(discountPrice = offer.discountPrice)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SensusBackground,
                modifier = Modifier.clickable { }
            ) {
                Text(
                    text = "Ajustar",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusPurple,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OfferPriceAndStatusRow(discountPrice: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", discountPrice)} MXN",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SensusPurple
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(color = SensusSuccess.copy(alpha = ACTIVE_BADGE_ALPHA), shape = RoundedCornerShape(4.dp)) {
            Text(
                text = "Activo",
                color = SensusSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
