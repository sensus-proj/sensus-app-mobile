package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedButtonColors
import java.util.Locale
import kotlin.math.roundToInt

private const val LOW_STOCK_THRESHOLD = 3
private const val IMAGE_HEIGHT_DP = 150
private const val METERS_PER_KM = 1000
private const val PURPLE_TINT_ALPHA = 0.10f

/** Acciones que el usuario puede ejecutar sobre las ofertas que muestra Sensi (FT-24). */
@ComposeUiModel
data class SensiOfferActions(
    val onApartarPackage: () -> Unit = {},
    val onDetail: (ProductOffer) -> Unit = {},
    val onApartar: (ProductOffer) -> Unit = {},
    val onRoute: (ProductOffer) -> Unit = {}
)

/**
 * Tarjeta con la información completa de una oferta mencionada por Sensi: imagen, descuento, tienda, precios,
 * disponibilidad, vigencia y dirección, con los botones Apartar, Cómo llegar y Ver detalle.
 */
@Composable
internal fun SensiOfferCard(offer: CitedOffer, actions: SensiOfferActions, modifier: Modifier = Modifier) {
    val product = remember(offer) { offer.toProductOffer() }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            OfferImage(offer = offer, discountPercent = product.discountPercent)
            Column(modifier = Modifier.padding(14.dp)) {
                OfferInfo(offer)
                Spacer(modifier = Modifier.height(12.dp))
                OfferButtons(offer = offer, product = product, actions = actions)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfferInfo(offer: CitedOffer) {
    Text(
        text = offer.title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = SensusTextPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(4.dp))
    MerchantLine(offer)

    offer.description?.takeIf { it.isNotBlank() }?.let { description ->
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = SensusTextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    PriceLine(offer)

    Spacer(modifier = Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StockChip(offer.availableStock)
        offer.endsAtMillis?.let { InfoChip(Icons.Default.AccessTime, "Hasta ${formatOfferDay(it)}") }
    }

    offer.merchantAddress?.takeIf { it.isNotBlank() }?.let { address ->
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = SensusTextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = address, fontSize = 11.sp, color = SensusTextMuted)
        }
    }
}

@Composable
private fun OfferButtons(offer: CitedOffer, product: ProductOffer, actions: SensiOfferActions) {
    val hasStock = offer.availableStock > 0
    val hasCoordinates = offer.latitude != null && offer.longitude != null

    Button(
        onClick = { actions.onApartar(product) },
        enabled = hasStock,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        contentPadding = PaddingValues(vertical = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = if (hasStock) "Apartar" else "Agotado", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (hasCoordinates) {
            OutlinedActionButton(
                text = "Cómo llegar",
                icon = Icons.Default.Directions,
                onClick = { actions.onRoute(product) },
                modifier = Modifier.weight(1f)
            )
        }
        OutlinedActionButton(
            text = "Ver detalle",
            icon = null,
            onClick = { actions.onDetail(product) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OfferImage(offer: CitedOffer, discountPercent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IMAGE_HEIGHT_DP.dp)
            .background(SensusPurple.copy(alpha = PURPLE_TINT_ALPHA))
    ) {
        if (offer.imageUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = SensusVioletInk,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp)
            )
        } else {
            AsyncImage(
                model = offer.imageUrl,
                contentDescription = offer.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (discountPercent > 0) {
            Surface(
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 12.dp, bottomStart = 0.dp),
                color = SensusDiscountTag,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "-$discountPercent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun MerchantLine(offer: CitedOffer) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            tint = SensusVioletInk,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = offer.merchantName.ifBlank { "Comercio Sensus" },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = SensusTextSecondary
        )
        offer.distanceKm?.let { km ->
            Text(
                text = " • ${formatDistance(km)}",
                fontSize = 13.sp,
                color = SensusTextMuted
            )
        }
    }
}

@Composable
private fun PriceLine(offer: CitedOffer) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$${money(offer.offerPriceMxn)}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SensusVioletInk
        )
        if (offer.regularPriceMxn > offer.offerPriceMxn) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$${money(offer.regularPriceMxn)}",
                fontSize = 13.sp,
                color = SensusTextMuted,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Ahorras $${money(offer.regularPriceMxn - offer.offerPriceMxn)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusSuccess,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun StockChip(stock: Int) {
    val text = when {
        stock <= 0 -> "Agotado"
        stock == 1 -> "¡Última unidad!"
        stock <= LOW_STOCK_THRESHOLD -> "¡Quedan $stock!"
        else -> "$stock disponibles"
    }
    InfoChip(Icons.Default.Inventory2, text, highlight = stock <= LOW_STOCK_THRESHOLD)
}

@Composable
private fun InfoChip(icon: ImageVector, text: String, highlight: Boolean = false) {
    val tint = if (highlight) SensusRed else SensusTextSecondary
    Surface(shape = CircleShape, color = SensusBackground) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = tint)
        }
    }
}

@Composable
private fun OutlinedActionButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        colors = sensusOutlinedButtonColors(),
        onClick = onClick,
        shape = SensusButtonShape,
        border = BorderStroke(1.dp, SensusPurple),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
        modifier = modifier
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusVioletInk)
    }
}

private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun formatDistance(km: Double): String = if (km < 1.0) "${(km * METERS_PER_KM).roundToInt()} m" else String.format(Locale.US, "%.1f km", km)

@Preview(showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
internal fun SensiOfferCardPreview() {
    SensiOfferCard(
        offer = CitedOffer(
            offerId = "off_1",
            title = "Teclado Gaming",
            merchantId = "m_1",
            merchantName = "Tienda 3B",
            offerPriceMxn = 150.0,
            regularPriceMxn = 200.0,
            availableStock = 1,
            isOpenNow = true,
            distanceKm = 0.4,
            description = "Teclado Gaming con RGB para más FPS",
            discountPercent = 25,
            endsAtMillis = 1_795_000_000_000L,
            merchantAddress = "Calle Falsa 123, Centro",
            latitude = 19.43,
            longitude = -99.13
        ),
        actions = SensiOfferActions()
    )
}
