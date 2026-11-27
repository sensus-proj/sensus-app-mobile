package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.OfferThumbnailSize
import com.example.sensus.presentation.common.PublicOfferThumbnail
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import java.util.Locale

private const val OFFER_PHOTO_RATIO = 1.85f
private const val UNITS_BADGE_ALPHA = 0.10f
private const val DISTANCE_BADGE_ALPHA = 0.08f

@Composable
fun ProductOfferCard(offer: ProductOffer, onClick: () -> Unit, modifier: Modifier = Modifier, isSaved: Boolean = false, onSaveClick: (() -> Unit)? = null) {
    OfferCardSurface(offer = offer, onClick = onClick, modifier = modifier) {
        val icon = if (isSaved) Icons.Filled.PushPin else Icons.Outlined.PushPin
        val tint = if (isSaved) SensusVioletInk else SensusTextMuted
        val desc = if (isSaved) stringResource(R.string.saved_interests_pin_saved) else stringResource(R.string.saved_interests_pin_add)
        OfferCardBadgeAction(icon = icon, tint = tint, description = desc, onClick = onSaveClick)
    }
}

@Composable
fun FavoriteProductOfferCard(offer: ProductOffer, isFavorite: Boolean, onClick: () -> Unit, onFavoriteClick: () -> Unit, modifier: Modifier = Modifier) {
    OfferCardSurface(offer = offer, onClick = onClick, modifier = modifier) {
        val icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        val tint = if (isFavorite) SensusVioletInk else SensusTextMuted
        val desc = if (isFavorite) stringResource(R.string.favorites_remove_cd) else stringResource(R.string.favorites_add_cd)
        OfferCardBadgeAction(icon = icon, tint = tint, description = desc, onClick = onFavoriteClick)
    }
}

@Composable
private fun OfferCardSurface(offer: ProductOffer, onClick: () -> Unit, modifier: Modifier = Modifier, badgeAction: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.commerce_offer_details), onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            offer.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                PublicOfferThumbnail(
                    url = url,
                    size = OfferThumbnailSize.CATALOG,
                    description = offer.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(OFFER_PHOTO_RATIO).clip(RoundedCornerShape(12.dp)).background(SensusVioletBg)
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                OfferCardBadges(offer = offer, modifier = Modifier.weight(1f))
                badgeAction()
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = offer.name, style = MaterialTheme.typography.titleMedium, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Tienda", tint = SensusVioletInk, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${offer.storeName} • Caduca ${offer.expirationDate}", fontSize = 13.sp, color = SensusTextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (offer.merchantProfile?.isVerified == true) Text(stringResource(R.string.discovery_verified))
            MerchantOpenStatusText(offer.merchantProfile)
            ProductOfferCardPriceFooter(offer = offer)
        }
    }
}

@Composable
private fun OfferCardBadges(offer: ProductOffer, modifier: Modifier = Modifier) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(color = SensusDiscountTag, shape = RoundedCornerShape(6.dp)) {
            Text(
                text = "-${offer.discountPercent}%",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Surface(color = SensusPurple.copy(alpha = UNITS_BADGE_ALPHA), shape = RoundedCornerShape(6.dp)) {
            Text(
                text = "Quedan ${offer.unitsLeft} u.",
                color = SensusVioletInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Surface(color = SensusPurple.copy(alpha = DISTANCE_BADGE_ALPHA), shape = RoundedCornerShape(6.dp)) {
            Text(
                text = "A ${offer.formattedDistance}",
                color = SensusVioletInk,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OfferCardBadgeAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    description: String,
    onClick: (() -> Unit)?
) {
    if (onClick != null) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
        }
    } else {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProductOfferCardPriceFooter(offer: ProductOffer) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", offer.originalPrice)} MXN",
            style = MaterialTheme.typography.labelMedium,
            color = SensusTextSecondary,
            textDecoration = TextDecoration.LineThrough
        )
        Text(
            text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)} MXN",
            style = MaterialTheme.typography.titleLarge,
            color = SensusVioletInk
        )
    }
}
