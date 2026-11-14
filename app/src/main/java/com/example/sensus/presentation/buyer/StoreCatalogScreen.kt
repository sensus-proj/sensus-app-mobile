package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary

private const val CATALOG_GRID_COLUMNS = 2

@Composable
fun StoreCatalogScreen(cluster: MapStoreCluster, onNavigateBack: () -> Unit, onProductClick: (ProductOffer) -> Unit, modifier: Modifier = Modifier) {
    val availableCategories = remember(cluster.allStoreOffers) {
        cluster.allStoreOffers.map { it.category.trim() }.filter { it.isNotBlank() }.distinct()
    }
    var selectedCategory by remember(cluster.storeKey) { mutableStateOf<String?>(null) }

    val visibleOffers = remember(cluster.allStoreOffers, selectedCategory) {
        val filter = selectedCategory
        if (filter == null) {
            cluster.allStoreOffers
        } else {
            cluster.allStoreOffers.filter { it.category.equals(filter, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        containerColor = SensusBackground,
        topBar = {
            StoreCatalogTopBar(
                storeName = cluster.storeName,
                offersCount = visibleOffers.size,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            StoreCatalogHeaderCard(
                cluster = cluster,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            if (availableCategories.isNotEmpty()) {
                StoreCatalogCategoryRow(
                    categories = availableCategories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            StoreCatalogGridBody(offers = visibleOffers, onProductClick = onProductClick)
        }
    }
}

@Composable
private fun StoreCatalogGridBody(offers: List<ProductOffer>, onProductClick: (ProductOffer) -> Unit) {
    if (offers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(SensusBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = SensusTextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Este comercio aún no tiene ofertas activas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Vuelve más tarde para descubrir sus promociones.", fontSize = 12.sp, color = SensusTextMuted)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(CATALOG_GRID_COLUMNS),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(offers, key = { it.id }) { offer ->
                StoreCatalogProductCard(offer = offer, onClick = { onProductClick(offer) })
            }
        }
    }
}
