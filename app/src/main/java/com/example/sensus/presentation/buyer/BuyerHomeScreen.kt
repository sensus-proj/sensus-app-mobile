package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.presentation.common.RadiusSelectorBottomSheet
import com.example.sensus.presentation.common.formatRadiusText
import com.example.sensus.presentation.common.getCategoryIcon
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.util.Locale

private const val METERS_PER_KM = 1000
private const val MAP_PILL_BG_ALPHA = 0.08f
private const val UNITS_BADGE_ALPHA = 0.10f
private const val DISTANCE_BADGE_ALPHA = 0.08f

private val categoryKeywords = mapOf(
    StoreCategory.GROCERY to "Abarrotes",
    StoreCategory.CLEANING to "Limpieza",
    StoreCategory.PERISHABLES to "Perecederos",
    StoreCategory.DAIRY to "Lácteos",
    StoreCategory.FOOD to "Comida",
    StoreCategory.BAKERY to "Panadería",
    StoreCategory.BEVERAGES to "Bebidas",
    StoreCategory.PETS to "Mascotas",
    StoreCategory.PHARMACY to "Farmacia"
)

@ComposeUiModel
data class BuyerHomeCallbacks(
    val merchantId: String? = null,
    val onClearMerchantFilter: () -> Unit = {},
    val isGuest: Boolean = false,
    val onRequireAuth: () -> Unit = {},
    val onProductClick: (ProductOffer) -> Unit = {},
    val onMapToggle: () -> Unit = {},
    val onNotificationsClick: () -> Unit = {},
    val onOpenCart: (() -> Unit)? = null
)

@ComposeUiModel
private data class HomeSearchProps(
    val query: String,
    val effectiveIsGuest: Boolean,
    val effectiveRadiusKm: Float,
    val locationLabel: String,
    val selectedCategory: StoreCategory
)

@ComposeUiModel
private data class HomeContentData(
    val searchState: HomeSearchProps,
    val filteredOffers: List<ProductOffer>,
    val cart: StoreCart,
    val showRadiusSheet: Boolean
)

@ComposeUiModel
private data class HomeHostActions(
    val onRefreshLocation: () -> Unit,
    val onSearchChange: (String) -> Unit,
    val onCategoryToggle: (StoreCategory) -> Unit,
    val onToggleRadiusSheet: (Boolean) -> Unit,
    val onRadiusSelected: (Float) -> Unit
)

@Composable
fun BuyerHomeScreen(
    homeViewModel: BuyerHomeViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    callbacks: BuyerHomeCallbacks,
    modifier: Modifier = Modifier
) {
    val merchantId = callbacks.merchantId
    val context = LocalContext.current
    val homeState by homeViewModel.viewState.collectAsState()
    val cartState by cartViewModel.viewState.collectAsState()
    val effectiveIsGuest = callbacks.isGuest || homeState.isGuest
    val effectiveRadiusKm = GuestRadiusPolicy.clampRadius(homeState.selectedRadiusKm, effectiveIsGuest)

    var locationLabel by remember { mutableStateOf("Ubicación actual") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var showRadiusSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        homeViewModel.viewEffect.collect { effect ->
            if (effect is BuyerHomeViewEffect.RequireAuthForRadius) callbacks.onRequireAuth()
        }
    }
    LaunchedEffect(homeState.userLocation) {
        locationLabel = OsmMapUtils.resolveLocationLabel(context, homeState.userLocation)
    }

    val filteredOffers = remember(searchQuery, selectedCategory, homeState.userLocation, homeState.offers, effectiveRadiusKm, merchantId) {
        filterHomeOffers(homeState.userLocation, homeState.offers, effectiveRadiusKm, searchQuery, selectedCategory)
            .filter { merchantId == null || it.merchantId == merchantId }
    }

    MerchantOfferFilter(merchantId, callbacks.onClearMerchantFilter, modifier) {
        BuyerHomeHost(
            data = HomeContentData(
                searchState = HomeSearchProps(searchQuery, effectiveIsGuest, effectiveRadiusKm, locationLabel, selectedCategory),
                filteredOffers = filteredOffers,
                cart = cartState.storeCart,
                showRadiusSheet = showRadiusSheet
            ),
            callbacks = callbacks,
            actions = HomeHostActions(
                onRefreshLocation = { homeViewModel.dispatchIntent(BuyerHomeViewIntent.RefreshLocation) },
                onSearchChange = { searchQuery = it },
                onCategoryToggle = { clicked -> selectedCategory = if (selectedCategory == clicked) StoreCategory.ALL else clicked },
                onToggleRadiusSheet = { showRadiusSheet = it },
                onRadiusSelected = { km -> homeViewModel.dispatchIntent(BuyerHomeViewIntent.RadiusSelected(km)) }
            ),
            modifier = Modifier
        )
    }
}

@Composable
private fun BuyerHomeHost(data: HomeContentData, callbacks: BuyerHomeCallbacks, actions: HomeHostActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        BuyerHomeLazyFeed(data = data, callbacks = callbacks, actions = actions)
        if (data.cart.totalItemsCount > 0) {
            FloatingHomeCartPill(
                totalItems = data.cart.totalItemsCount,
                totalDiscountPrice = data.cart.totalDiscountPrice,
                onClick = { callbacks.onOpenCart?.invoke() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 105.dp)
            )
        }
        if (data.showRadiusSheet) {
            RadiusSelectorBottomSheet(
                currentRadiusKm = data.searchState.effectiveRadiusKm,
                isGuest = data.searchState.effectiveIsGuest,
                onRadiusSelected = actions.onRadiusSelected,
                onRequireAuth = callbacks.onRequireAuth,
                onDismiss = { actions.onToggleRadiusSheet(false) }
            )
        }
    }
}

@Composable
private fun BuyerHomeLazyFeed(data: HomeContentData, callbacks: BuyerHomeCallbacks, actions: HomeHostActions) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            BuyerHomeLocationHeader(
                locationLabel = data.searchState.locationLabel,
                onRefreshLocation = actions.onRefreshLocation,
                onNotificationsClick = callbacks.onNotificationsClick
            )
        }
        item { BuyerHomeHeroTitle() }
        item {
            BuyerHomeSearchSection(
                searchState = data.searchState,
                onSearchChange = actions.onSearchChange,
                onRadiusClick = { actions.onToggleRadiusSheet(true) },
                onMapToggle = callbacks.onMapToggle
            )
        }
        item { BuyerCategoryChipsRow(selectedCategory = data.searchState.selectedCategory, onCategoryToggle = actions.onCategoryToggle) }
        item {
            Text(
                text = "Ofertas ordenadas por cercanía (${data.filteredOffers.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(data.filteredOffers, key = { it.id }) { offer ->
            ProductOfferCard(offer = offer, onClick = { callbacks.onProductClick(offer) })
        }
    }
}

@Composable
private fun BuyerHomeLocationHeader(locationLabel: String, onRefreshLocation: () -> Unit, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).clickable(onClick = onRefreshLocation)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Ubicación", tint = SensusPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = locationLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Cambiar", tint = SensusTextSecondary, modifier = Modifier.size(18.dp))
        }
        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White)
            ) {
                Icon(imageVector = Icons.Default.NotificationsNone, contentDescription = "Notificaciones", tint = SensusPurple, modifier = Modifier.size(22.dp))
            }
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(SensusPurple).align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun BuyerHomeHeroTitle() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = "Encuentra tu próximo ahorro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Aprovecha excedentes en buen estado cerca de ti", fontSize = 14.sp, color = SensusTextSecondary)
    }
}

@Composable
private fun BuyerHomeSearchSection(searchState: HomeSearchProps, onSearchChange: (String) -> Unit, onRadiusClick: () -> Unit, onMapToggle: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onSearchChange,
            placeholder = { Text(text = "Buscar productos o comercios...", fontSize = 14.sp, color = SensusTextMuted) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = SensusTextSecondary) },
            shape = RoundedCornerShape(14.dp),
            colors = sensusOutlinedTextFieldColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeRadiusChip(searchState.effectiveIsGuest, searchState.effectiveRadiusKm, onRadiusClick)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SensusPurple.copy(alpha = MAP_PILL_BG_ALPHA))
                    .clickable(onClick = onMapToggle)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = "Ver en mapa", tint = SensusPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Ver en mapa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
            }
        }
    }
}

@Composable
private fun HomeRadiusChip(effectiveIsGuest: Boolean, effectiveRadiusKm: Float, onClick: () -> Unit) {
    val label = if (effectiveIsGuest) {
        "Radio: ${formatRadiusText(effectiveRadiusKm)} (Máx. 2 km)"
    } else {
        "Radio: ${formatRadiusText(effectiveRadiusKm)}"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (effectiveIsGuest) Icons.Default.Lock else Icons.Default.Tune,
            contentDescription = "Filtro radio",
            tint = SensusPurple,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Seleccionar", tint = SensusTextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun BuyerCategoryChipsRow(selectedCategory: StoreCategory, onCategoryToggle: (StoreCategory) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        StoreCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) SensusPurple else Color.White,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier.clickable { onCategoryToggle(category) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.displayName,
                        tint = if (isSelected) Color.White else SensusTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SensusTextPrimary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun FloatingHomeCartPill(totalItems: Int, totalDiscountPrice: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = SensusPurple, shadowElevation = 8.dp, modifier = modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Bolsa ($totalItems) • $${String.format(Locale.US, "%.0f", totalDiscountPrice)} MXN",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ProductOfferCard(offer: ProductOffer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ProductOfferCardBadgesRow(offer = offer)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = offer.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storefront, contentDescription = "Tienda", tint = SensusPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${offer.storeName} • Caduca ${offer.expirationDate}", fontSize = 12.sp, color = SensusTextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            ProductOfferCardPriceFooter(offer = offer)
        }
    }
}

@Composable
private fun ProductOfferCardBadgesRow(offer: ProductOffer) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(color = SensusDiscountTag, shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = "-${offer.discountPercent}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(color = SensusPurple.copy(alpha = UNITS_BADGE_ALPHA), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = "Quedan ${offer.unitsLeft} u.",
                    color = SensusPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Surface(color = SensusPurple.copy(alpha = DISTANCE_BADGE_ALPHA), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = "A ${offer.formattedDistance}",
                    color = SensusPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = "Guardar", tint = SensusTextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ProductOfferCardPriceFooter(offer: ProductOffer) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                text = "$${String.format(Locale.US, "%.2f", offer.originalPrice)} MXN",
                fontSize = 12.sp,
                color = SensusTextMuted,
                textDecoration = TextDecoration.LineThrough
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)}",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple
                )
                Text(
                    text = " MXN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusPurple,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Surface(shape = RoundedCornerShape(10.dp), color = SensusPurple) {
            Text(
                text = "Ver detalle",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

private fun filterHomeOffers(
    userLocation: com.example.sensus.domain.model.UserLocation,
    offers: List<ProductOffer>,
    effectiveRadiusKm: Float,
    searchQuery: String,
    selectedCategory: StoreCategory
): List<ProductOffer> {
    val maxMeters = (effectiveRadiusKm * METERS_PER_KM).toInt()
    return OsmMapUtils.localizeAndMeasureOffers(userLocation, offers).filter { offer ->
        offer.distanceMeters <= maxMeters &&
            matchesHomeSearch(offer, searchQuery) &&
            matchesCategoryKeyword(offer, selectedCategory)
    }
}

private fun matchesHomeSearch(offer: ProductOffer, query: String): Boolean = query.isBlank() ||
    offer.name.contains(query, ignoreCase = true) ||
    offer.storeName.contains(query, ignoreCase = true)

private fun matchesCategoryKeyword(offer: ProductOffer, category: StoreCategory): Boolean {
    val keyword = categoryKeywords[category] ?: return true
    return offer.category.contains(keyword, ignoreCase = true)
}

@Composable
private fun MerchantOfferFilter(merchantId: String?, onClear: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxSize()) {
        if (merchantId != null) {
            androidx.compose.material3.TextButton(onClick = onClear) { Text("Ver ofertas de todos los comercios") }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
