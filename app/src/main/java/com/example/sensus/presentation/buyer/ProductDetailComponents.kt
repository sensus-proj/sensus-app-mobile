package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.AddToCartResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRating
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import java.util.Locale

private const val NOTICE_BG_ALPHA = 0.15f
private const val STEPPER_ACTIVE_ALPHA = 0.12f
private const val STEPPER_INACTIVE_HEX = 0xFFF1F5F9
private const val STORE_AVATAR_ALPHA = 0.1f
private const val IMPACT_BG_ALPHA = 0.1f

@ComposeUiModel
data class ProductDetailCallbacks(
    val onNavigateBack: () -> Unit = {},
    val onApartarClick: (ProductOffer) -> Unit = {},
    val onApartarDirect: (ProductOffer, Int) -> Unit = { off, _ -> onApartarClick(off) },
    val onNavigateToRoute: ((ProductOffer) -> Unit)? = null
)

@Composable
internal fun ProductDetailTopBar(onNavigateBack: () -> Unit, onShareClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleWhiteIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Regresar", onNavigateBack)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleWhiteIconButton(Icons.Default.Share, "Compartir", onShareClick)
            CircleWhiteIconButton(Icons.Default.BookmarkBorder, "Favorito", onFavoriteClick)
        }
    }
}

@Composable
private fun CircleWhiteIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = SensusPurple)
    }
}

@Composable
internal fun ProductAddedNoticeBanner(storeName: String) {
    Surface(
        color = SensusSuccess.copy(alpha = NOTICE_BG_ALPHA),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Agregado a la bolsa de $storeName",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusSuccess
            )
        }
    }
}

@Composable
internal fun ProductVisualHeroCard(offer: ProductOffer) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = offer.name, tint = SensusPurple, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = offer.category, fontSize = 13.sp, color = SensusTextMuted)
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = SensusDiscountTag, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "-${offer.discountPercent}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Surface(color = SensusPurple, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Stock: ${offer.unitsLeft} disponibles",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun ProductTitleAndDatesSection(offer: ProductOffer) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = offer.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "Caducidad", tint = SensusPurple, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Caduca: ${offer.expirationDate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Tiempo", tint = SensusTextSecondary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Límite: ${offer.timeToCollectMinutes} min", fontSize = 12.sp, color = SensusTextSecondary)
            }
        }
    }
}

@Composable
internal fun QuantitySelectorCard(quantity: Int, maxUnits: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Selecciona cantidad a apartar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(text = "$quantity de $maxUnits disponibles", fontSize = 12.sp, color = SensusTextMuted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuantityCircleButton(Icons.Default.Remove, "Disminuir", quantity > 1, onDecrease)
                Text(text = "$quantity", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = SensusPurple)
                QuantityCircleButton(Icons.Default.Add, "Aumentar", quantity < maxUnits, onIncrease)
            }
        }
    }
}

@Composable
private fun QuantityCircleButton(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (enabled) SensusPurple.copy(alpha = STEPPER_ACTIVE_ALPHA) else Color(STEPPER_INACTIVE_HEX),
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) SensusPurple else Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun ProductMerchantCard(offer: ProductOffer, hasRouteNavigation: Boolean, onGoogleMapsClick: () -> Unit, onRouteClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MerchantCardHeaderRow(offer = offer)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SensusBackground)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Dirección", tint = SensusPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${offer.address} • A ${offer.formattedDistance}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            MerchantNavigationButtons(hasRouteNavigation, onGoogleMapsClick, onRouteClick)
        }
    }
}

@Composable
private fun MerchantCardHeaderRow(offer: ProductOffer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SensusPurple.copy(alpha = STORE_AVATAR_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Comercio", tint = SensusPurple, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = offer.storeName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verificado", tint = SensusSuccess, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Comercio verificado", fontSize = 11.sp, color = SensusSuccess, fontWeight = FontWeight.Medium)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = SensusRating, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${offer.rating}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = " (${offer.reviewCount})", fontSize = 11.sp, color = SensusTextMuted)
        }
    }
}

@Composable
private fun MerchantNavigationButtons(hasRouteNavigation: Boolean, onGoogleMapsClick: () -> Unit, onRouteClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onGoogleMapsClick,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Cómo llegar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (hasRouteNavigation) {
            OutlinedButton(
                onClick = onRouteClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = SensusPurple)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Ruta Sensus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
            }
        }
    }
}

@Composable
internal fun ProductDescriptionCard(description: String, quantity: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Descripción de la oferta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = description, fontSize = 13.sp, color = SensusTextSecondary, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SensusSuccess.copy(alpha = IMPACT_BG_ALPHA))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Impacto", tint = SensusSuccess, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Evitas el desperdicio de $quantity lote(s) en perfecto estado.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SensusSuccess
                )
            }
        }
    }
}

@Composable
internal fun ProductDetailBottomBar(offer: ProductOffer, quantity: Int, onAddToCart: () -> Unit, onApartarDirect: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shadowElevation = 12.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductBottomPriceSummary(offer = offer, quantity = quantity)
            ProductBottomActionButtons(quantity = quantity, onAddToCart = onAddToCart, onApartarDirect = onApartarDirect)
        }
    }
}

@Composable
private fun ProductBottomPriceSummary(offer: ProductOffer, quantity: Int) {
    val totalDiscount = offer.discountPrice * quantity
    val totalOriginal = offer.originalPrice * quantity
    Column {
        Text(text = "Total ($quantity u.):", fontSize = 11.sp, color = SensusTextMuted)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$${String.format(Locale.US, "%.2f", totalDiscount)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SensusPurple
            )
            Text(
                text = " MXN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SensusPurple,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$${String.format(Locale.US, "%.2f", totalOriginal)}",
                fontSize = 12.sp,
                color = SensusTextMuted,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun ProductBottomActionButtons(quantity: Int, onAddToCart: () -> Unit, onApartarDirect: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onAddToCart,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddShoppingCart,
                contentDescription = "Agregar a la bolsa",
                tint = SensusPurple,
                modifier = Modifier.size(18.dp)
            )
        }
        Button(
            onClick = onApartarDirect,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = "Apartar ahora", tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Apartar ($quantity)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
internal fun StoreConflictAlertDialog(conflict: AddToCartResult.StoreConflict, onConfirmReplace: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Bolsa de otra tienda", fontWeight = FontWeight.Bold, color = SensusTextPrimary) },
        text = {
            Text(
                text = "Tu bolsa ya contiene productos de '${conflict.currentStoreName}'. " +
                    "Solo puedes apartar ofertas del mismo comercio a la vez.\n\n" +
                    "¿Deseas vaciar tu bolsa actual para agregar las ofertas de '${conflict.newStoreName}'?",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        },
        confirmButton = {
            Button(onClick = onConfirmReplace, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
                Text("Vaciar y agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Conservar bolsa", color = SensusTextSecondary)
            }
        }
    )
}
