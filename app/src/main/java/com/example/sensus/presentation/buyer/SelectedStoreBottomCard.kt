package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import java.util.Locale

private const val BORDER_LIGHT_HEX = 0xFFE2E8F0
private const val STORE_CARD_ACTIVE_ALPHA = 0.14f
private const val MATCHING_LIST_MAX_HEIGHT_DP = 172
private const val OVERVIEW_CATEGORY_LIMIT = 3

@Composable
internal fun SelectedStoreBottomCard(
    cluster: MapStoreCluster,
    onClose: () -> Unit,
    onRouteClick: (ProductOffer) -> Unit,
    onViewMoreClick: (MapStoreCluster) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllFilter = cluster.initialCategory == StoreCategory.ALL
    val routeTarget = cluster.featuredOffer ?: buildStoreRouteTarget(cluster)
    Card(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 76.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(BORDER_LIGHT_HEX)))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = SensusTextMuted, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            SelectedStoreHeaderRow(cluster = cluster)
            Spacer(modifier = Modifier.height(10.dp))
            if (isAllFilter) {
                SelectedStoreCatalogOverview(cluster = cluster)
            } else {
                SelectedStoreCategoryOffersList(offers = cluster.matchingOffers)
            }
            Spacer(modifier = Modifier.height(12.dp))
            SelectedStoreActionButtons(
                cluster = cluster,
                routeTarget = routeTarget,
                onRouteClick = onRouteClick,
                onViewMoreClick = onViewMoreClick
            )
        }
    }
}

@Composable
private fun SelectedStoreHeaderRow(cluster: MapStoreCluster) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Tienda", tint = SensusPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = cluster.storeName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${cluster.formattedDistance} • ${cluster.address}",
                fontSize = 12.sp,
                color = SensusTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val countBadge = if (cluster.allStoreOffers.isNotEmpty()) "${cluster.allStoreOffers.size} ofertas" else "Activo"
        Surface(color = SensusSuccess.copy(alpha = STORE_CARD_ACTIVE_ALPHA), shape = RoundedCornerShape(8.dp)) {
            Text(
                text = countBadge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = SensusSuccess,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SelectedStoreCatalogOverview(cluster: MapStoreCluster) {
    val totalOffers = cluster.allStoreOffers.size
    val categories = cluster.allStoreOffers.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
    val subtitle = when {
        totalOffers == 0 -> "Este comercio aún no tiene ofertas activas cerca de ti."
        categories.isEmpty() -> "Catálogo disponible con $totalOffers oferta(s) activas."
        else -> "Categorías: ${categories.take(OVERVIEW_CATEGORY_LIMIT).joinToString(", ")}"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SensusBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SensusVioletBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = cluster.storeName, tint = SensusPurple, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (totalOffers > 0) "Catálogo de ${cluster.storeName}" else cluster.storeName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = SensusTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SelectedStoreCategoryOffersList(offers: List<ProductOffer>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = MATCHING_LIST_MAX_HEIGHT_DP.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        offers.forEach { offer ->
            SelectedStoreProductPreview(offer = offer)
        }
    }
}

@Composable
private fun SelectedStoreProductPreview(offer: ProductOffer) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SensusBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StorePreviewThumbnail(offer = offer)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = offer.category,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusPurple,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = offer.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)} MXN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SensusPurple
                    )
                    Text(text = "-${offer.discountPercent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SensusSuccess)
                }
                Text(text = "Quedan ${offer.unitsLeft} pzs", fontSize = 11.sp, color = SensusTextMuted)
            }
        }
    }
}

@Composable
private fun StorePreviewThumbnail(offer: ProductOffer) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SensusVioletBg),
        contentAlignment = Alignment.Center
    ) {
        if (!offer.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = offer.imageUrl,
                contentDescription = offer.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(imageVector = Icons.Default.Storefront, contentDescription = offer.name, tint = SensusPurple, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun SelectedStoreActionButtons(
    cluster: MapStoreCluster,
    routeTarget: ProductOffer,
    onRouteClick: (ProductOffer) -> Unit,
    onViewMoreClick: (MapStoreCluster) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = { onRouteClick(routeTarget) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
            Icon(imageVector = Icons.Default.Navigation, contentDescription = "Cómo llegar", tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Cómo llegar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
        }
        Button(
            onClick = { onViewMoreClick(cluster) },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = "Ver más", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Ver más", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

private fun buildStoreRouteTarget(cluster: MapStoreCluster): ProductOffer = ProductOffer(
    id = cluster.storeKey,
    merchantId = cluster.merchantId,
    name = cluster.storeName,
    storeName = cluster.storeName,
    address = cluster.address,
    postalCode = cluster.postalCode,
    category = "Tienda",
    originalPrice = 0.0,
    discountPrice = 0.0,
    discountPercent = 0,
    unitsLeft = 0,
    distanceMeters = cluster.distanceMeters,
    latitude = cluster.latitude,
    longitude = cluster.longitude
)
