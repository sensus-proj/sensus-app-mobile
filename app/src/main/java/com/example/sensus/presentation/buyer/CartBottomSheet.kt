package com.example.sensus.presentation.buyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import java.util.Locale

private const val INFO_BG_ALPHA = 0.08f
private const val LIST_MAX_HEIGHT_DP = 240

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(viewModel: CartViewModel = hiltViewModel(), onDismiss: () -> Unit, onCheckoutOrder: (ApartadoOrder) -> Unit) {
    val cartState by viewModel.viewState.collectAsState()
    val cart = cartState.storeCart
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            CartSheetHeader(
                cart = cart,
                onClearCart = { viewModel.dispatchIntent(CartViewIntent.ClearCart) }
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (cart.isEmpty) {
                EmptyCartContent()
            } else {
                FilledCartContent(
                    cart = cart,
                    onUpdateQuantity = { id, next -> viewModel.dispatchIntent(CartViewIntent.UpdateQuantity(id, next)) },
                    onRemoveOffer = { id -> viewModel.dispatchIntent(CartViewIntent.RemoveOffer(id)) },
                    onGenerateQr = {
                        val order = ApartadoOrder.fromCart(cart)
                        viewModel.dispatchIntent(CartViewIntent.ClearCart)
                        onCheckoutOrder(order)
                    }
                )
            }
        }
    }
}

@Composable
private fun CartSheetHeader(cart: StoreCart, onClearCart: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = "Bolsa", tint = SensusPurple, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = "Bolsa de apartado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                cart.storeName?.let { storeName ->
                    Text(text = storeName, fontSize = 12.sp, color = SensusPurple, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!cart.isEmpty) {
            TextButton(onClick = onClearCart) {
                Text(text = "Vaciar", fontSize = 12.sp, color = SensusRed)
            }
        }
    }
}

@Composable
private fun EmptyCartContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Tu bolsa de compras está vacía", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Agrega ofertas de una tienda para apartarlas juntas con un solo QR.", fontSize = 12.sp, color = SensusTextMuted)
        }
    }
}

@Composable
private fun FilledCartContent(cart: StoreCart, onUpdateQuantity: (String, Int) -> Unit, onRemoveOffer: (String) -> Unit, onGenerateQr: () -> Unit) {
    SingleStoreBanner(storeName = cart.storeName.orEmpty())
    Spacer(modifier = Modifier.height(12.dp))
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(LIST_MAX_HEIGHT_DP.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(cart.items, key = { it.offer.id }) { item ->
            CartItemRowCard(item = item, onUpdateQuantity = onUpdateQuantity, onRemoveOffer = onRemoveOffer)
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = SensusBackground)
    Spacer(modifier = Modifier.height(12.dp))
    CartSummaryTotals(cart = cart)
    Spacer(modifier = Modifier.height(16.dp))
    Button(
        onClick = onGenerateQr,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Generar Código QR (${cart.totalItemsCount} productos)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SingleStoreBanner(storeName: String) {
    Surface(color = SensusPurple.copy(alpha = INFO_BG_ALPHA), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Todas las ofertas son de $storeName. Se generará un solo QR.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SensusPurple)
        }
    }
}

@Composable
private fun CartItemRowCard(item: CartItem, onUpdateQuantity: (String, Int) -> Unit, onRemoveOffer: (String) -> Unit) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SensusBackground), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.offer.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.totalDiscountPrice)} MXN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SensusPurple
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.totalOriginalPrice)}",
                        fontSize = 11.sp,
                        color = SensusTextMuted,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
            CartQuantityControls(item = item, onUpdateQuantity = onUpdateQuantity, onRemoveOffer = onRemoveOffer)
        }
    }
}

@Composable
private fun CartQuantityControls(item: CartItem, onUpdateQuantity: (String, Int) -> Unit, onRemoveOffer: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable { onUpdateQuantity(item.offer.id, item.quantity - 1) }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Menos", tint = SensusPurple, modifier = Modifier.size(16.dp))
            }
        }
        Text(text = "${item.quantity}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Surface(
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable {
                    if (item.quantity < item.offer.unitsLeft) onUpdateQuantity(item.offer.id, item.quantity + 1)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Más",
                    tint = if (item.quantity < item.offer.unitsLeft) SensusPurple else Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        IconButton(onClick = { onRemoveOffer(item.offer.id) }, modifier = Modifier.size(30.dp)) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Eliminar", tint = SensusRed, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CartSummaryTotals(cart: StoreCart) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Ahorro total estimado:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusSuccess)
            Text(
                text = "-$${String.format(Locale.US, "%.2f", cart.totalSavedAmount)} MXN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SensusSuccess
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Total a pagar en tienda:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = "$${String.format(Locale.US, "%.2f", cart.totalDiscountPrice)} MXN",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SensusPurple
            )
        }
    }
}
