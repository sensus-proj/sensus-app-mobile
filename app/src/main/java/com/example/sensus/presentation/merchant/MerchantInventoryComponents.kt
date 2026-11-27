package com.example.sensus.presentation.merchant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusDisabled
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
import com.example.sensus.theme.sensusTextButtonColors
import java.util.Locale

@Composable
internal fun MerchantInventoryHeader(
    productCount: Int,
    categoryCount: Int,
    selectedFilter: MerchantOfferFilter,
    allOffers: List<ProductOffer>,
    onSelectFilter: (MerchantOfferFilter) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MerchantSectionTitle(
            text = stringResource(R.string.merchant_dashboard_inventory),
            trailing = stringResource(R.string.merchant_dashboard_inventory_count, productCount, categoryCount)
        )
        MerchantOfferFilterRow(selectedFilter, allOffers, onSelectFilter)
    }
}

@Composable
private fun MerchantOfferFilterRow(selectedFilter: MerchantOfferFilter, allOffers: List<ProductOffer>, onSelectFilter: (MerchantOfferFilter) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MerchantOfferFilter.entries.forEach { filter ->
            val selected = filter == selectedFilter
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) SensusPurple else SensusSurface,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected, role = Role.Tab) { onSelectFilter(filter) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.merchant_inventory_filter_count, stringResource(filter.labelResource), allOffers.count(filter::matches)),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else SensusTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@ComposeUiModel
internal data class MerchantInventoryActions(
    val onEditClick: (ProductOffer) -> Unit,
    val onAdjustClick: (ProductOffer) -> Unit,
    val filterKey: String = ""
)

@Composable
internal fun MerchantCategoryGroup(
    category: String,
    offers: List<ProductOffer>,
    initiallyExpanded: Boolean,
    actions: MerchantInventoryActions,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(actions.filterKey, category) { mutableStateOf(initiallyExpanded) }
    MerchantCard(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .background(if (expanded) SensusVioletBg else SensusSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Category, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(category, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusVioletInk, modifier = Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.merchant_dashboard_products, offers.size, offers.size), fontSize = 11.sp, color = SensusTextSecondary)
            Icon(
                if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SensusTextSecondary
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                offers.forEachIndexed { index, offer ->
                    if (index > 0) HorizontalDivider(color = SensusCardBorder)
                    MerchantOfferRow(offer, onEditClick = { actions.onEditClick(offer) }, onAdjustClick = { actions.onAdjustClick(offer) })
                }
            }
        }
    }
}

/** Oferta como tarjeta independiente (se conserva para las pruebas de inventario). */
@Composable
internal fun MerchantOfferItem(offer: ProductOffer, onEditClick: () -> Unit, onAdjustClick: () -> Unit, modifier: Modifier = Modifier) {
    MerchantCard(modifier) { MerchantOfferRow(offer, onEditClick, onAdjustClick) }
}

@Composable
private fun MerchantOfferRow(offer: ProductOffer, onEditClick: () -> Unit, onAdjustClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MerchantOfferThumbnail(offer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(offer.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatMerchantPrice(offer.discountPrice), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.merchant_dashboard_offer_units, offer.category, offer.unitsLeft),
                        fontSize = 11.sp,
                        color = SensusTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                offer.viewCount?.let { views ->
                    Text(stringResource(R.string.merchant_offer_views, views), fontSize = 11.sp, color = SensusTextSecondary)
                }
            }
            MerchantOfferStatusChip(offer.status, offer.unitsLeft)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(colors = sensusTextButtonColors(), onClick = onEditClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.merchant_dashboard_manage))
            }
            TextButton(colors = sensusTextButtonColors(), onClick = onAdjustClick, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.merchant_dashboard_stock))
            }
        }
    }
}

@Composable
private fun MerchantOfferThumbnail(offer: ProductOffer) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SensusVioletBg),
        contentAlignment = Alignment.Center
    ) {
        if (!offer.imageUrl.isNullOrBlank()) {
            AsyncImage(model = offer.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun MerchantOfferStatusChip(status: OfferLifecycleStatus, unitsLeft: Int) {
    when {
        status == OfferLifecycleStatus.ARCHIVED -> MerchantStatusChip(stringResource(R.string.merchant_status_archived), SensusRed, SensusRedBg)
        status == OfferLifecycleStatus.EXPIRED -> MerchantStatusChip(stringResource(R.string.merchant_status_expired), SensusRed, SensusRedBg)
        status == OfferLifecycleStatus.PAUSED -> MerchantStatusChip(status.displayLabel, SensusTextSecondary, SensusVioletBg)
        status == OfferLifecycleStatus.SOLD_OUT || unitsLeft <= 0 ->
            MerchantStatusChip(OfferLifecycleStatus.SOLD_OUT.displayLabel, SensusDisabled, SensusVioletBg)
        else -> MerchantStatusChip(stringResource(R.string.merchant_status_active), SensusSuccess, SensusSuccessBg)
    }
}

internal fun formatMerchantPrice(amount: Double): String =
    if (amount % 1.0 == 0.0) String.format(Locale.US, "$%.0f", amount) else String.format(Locale.US, "$%.2f", amount)
