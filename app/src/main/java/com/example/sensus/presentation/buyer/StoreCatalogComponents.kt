package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.OfferThumbnailSize
import com.example.sensus.presentation.common.PublicOfferThumbnail
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import java.util.Locale

private const val IMAGE_BG_ALPHA = 0.08f
private const val STOCK_BADGE_ALPHA = 0.12f
private const val GRID_CARD_SHADOW_DP = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StoreCatalogTopBar(storeName: String, offersCount: Int, onNavigateBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusTextPrimary)
            }
        },
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = storeName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verificado",
                        tint = SensusVioletInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$offersCount productos activos",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SensusSurface)
    )
}

private const val CATALOG_THUMB_DP = 54

@Composable
internal fun StoreCatalogHeaderCard(cluster: MapStoreCluster, modifier: Modifier = Modifier) {
    val addressText = if (cluster.postalCode.isNotBlank()) "${cluster.address} • C.P. ${cluster.postalCode}" else cluster.address
    val openDays = cluster.merchantProfile?.horarios?.count { !it.estaCerrado } ?: 0
    val scheduleText = if (openDays > 0) "Abierto $openDays días por semana" else "Horario publicado en tienda"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreAvatarThumbnail(photoUrl = cluster.merchantProfile?.fotoFachadaUrl, storeName = cluster.storeName, sizeDp = CATALOG_THUMB_DP)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${cluster.formattedDistance} • $addressText",
                        fontSize = 13.sp,
                        color = SensusTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = scheduleText, fontSize = 13.sp, color = SensusTextMuted)
                }
            }
        }
    }
}

@Composable
internal fun StoreCatalogCategoryRow(categories: List<String>, selectedCategory: String?, onSelectCategory: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text(text = "Todos", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SensusPurple,
                    selectedLabelColor = Color.White
                )
            )
        }
        items(categories, key = { it }) { category ->
            val selected = selectedCategory.equals(category, ignoreCase = true)
            FilterChip(
                selected = selected,
                onClick = { onSelectCategory(if (selected) null else category) },
                label = { Text(text = category, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SensusPurple,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
internal fun StoreCatalogProductCard(
    offer: ProductOffer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    onSaveClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = GRID_CARD_SHADOW_DP.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CatalogProductBoundedImage(offer = offer, isSaved = isSaved, onSaveClick = onSaveClick)
            Text(
                text = offer.category,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusVioletInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = offer.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusVioletInk
                )
                Text(
                    text = "$${String.format(Locale.US, "%.0f", offer.originalPrice)}",
                    fontSize = 11.sp,
                    color = SensusTextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            CatalogProductStockFooter(offer = offer)
        }
    }
}

@Composable
private fun CatalogProductBoundedImage(offer: ProductOffer, isSaved: Boolean = false, onSaveClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SensusPurple.copy(alpha = IMAGE_BG_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        if (!offer.imageUrl.isNullOrBlank()) {
            PublicOfferThumbnail(
                url = offer.imageUrl,
                size = OfferThumbnailSize.CATALOG,
                description = offer.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = offer.name,
                tint = SensusVioletInk,
                modifier = Modifier.size(36.dp)
            )
        }
        Surface(
            color = SensusDiscountTag,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        ) {
            Text(
                text = "-${offer.discountPercent}%",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        if (onSaveClick != null) {
            CatalogPinBadge(
                isSaved = isSaved,
                onSaveClick = onSaveClick,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
            )
        }
    }
}

@Composable
private fun CatalogPinBadge(isSaved: Boolean, onSaveClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (isSaved) SensusPurple else SensusSurface,
        shape = CircleShape,
        modifier = modifier.size(28.dp).clip(CircleShape).clickable(onClick = onSaveClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = if (isSaved) "Guardada" else "Me interesa",
                tint = if (isSaved) Color.White else SensusVioletInk,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CatalogProductStockFooter(offer: ProductOffer) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(color = SensusSuccess.copy(alpha = STOCK_BADGE_ALPHA), shape = RoundedCornerShape(6.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Quedan ${offer.unitsLeft} pzs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusSuccess
                )
            }
        }
        Surface(color = SensusVioletBg, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Ver información • ${offer.expirationDate}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusVioletInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}
