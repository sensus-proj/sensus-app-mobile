package com.example.sensus.presentation.buyer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.OfferDiscoveryFilters
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.usecase.nearbyRadiusKm
import com.example.sensus.presentation.common.CommerceFeedFeedback
import com.example.sensus.presentation.common.CommerceHero
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.presentation.common.RadiusSelectorBottomSheet
import com.example.sensus.presentation.common.formatRadiusText
import com.example.sensus.presentation.common.getCategoryIcon
import com.example.sensus.presentation.components.states.LocationDisabledState
import com.example.sensus.presentation.components.states.OfflineErrorState
import com.example.sensus.presentation.components.states.ShimmerOfferList
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import com.example.sensus.theme.sensusTextButtonColors
import java.util.Locale

private const val METERS_PER_KM = 1000

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
    val onOpenCart: (() -> Unit)? = null,
    val onOpenSearch: (() -> Unit)? = null
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
    val showRadiusSheet: Boolean,
    val savedOfferIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val nextPage: Int? = null,
    val pageLoadFailed: Boolean = false,
    val usingCachedOffers: Boolean = false,
    val filters: OfferDiscoveryFilters = OfferDiscoveryFilters(),
    val isLocationDisabled: Boolean = false
)

@ComposeUiModel
private data class HomeHostActions(
    val onRefreshLocation: () -> Unit,
    val onSearchChange: (String) -> Unit,
    val onCategoryToggle: (StoreCategory) -> Unit,
    val onToggleRadiusSheet: (Boolean) -> Unit,
    val onRadiusSelected: (Float) -> Unit,
    val onToggleSaveOffer: (ProductOffer) -> Unit = {},
    val onRefreshOffers: () -> Unit = {},
    val onLoadNextPage: () -> Unit = {},
    val onChangeFilters: (OfferDiscoveryFilters) -> Unit = {}
)

@Composable
private fun rememberFilteredHomeOffers(
    homeState: BuyerHomeViewState,
    searchQuery: String,
    selectedCategory: StoreCategory,
    radiusKm: Float,
    merchantId: String?
): List<ProductOffer> {
    val minute = merchantLocalMinute()
    val filters = homeState.filters
    return remember(searchQuery, selectedCategory, homeState.userLocation, homeState.offers, radiusKm, merchantId, filters, minute) {
        filters.applyTo(
            filterHomeOffers(homeState.userLocation, homeState.offers, radiusKm, searchQuery, selectedCategory)
                .filter { merchantId == null || it.merchantId == merchantId },
            minute
        )
    }
}

@Composable
fun BuyerHomeScreen(
    homeViewModel: BuyerHomeViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    callbacks: BuyerHomeCallbacks,
    modifier: Modifier = Modifier
) {
    val favoritesViewModel: FavoritesViewModel = hiltViewModel()
    val merchantId = callbacks.merchantId
    val context = LocalContext.current
    val homeState by homeViewModel.viewState.collectAsStateWithLifecycle()
    val cartState by cartViewModel.viewState.collectAsStateWithLifecycle()
    val favoritesState by favoritesViewModel.viewState.collectAsStateWithLifecycle()
    val effectiveIsGuest = callbacks.isGuest || homeState.isGuest
    val effectiveRadiusKm = nearbyRadiusKm(homeState.selectedRadiusKm, effectiveIsGuest)

    var locationLabel by remember { mutableStateOf("Ubicación actual") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var showRadiusSheet by remember { mutableStateOf(false) }
    val filters = homeState.filters

    LaunchedEffect(Unit) {
        homeViewModel.viewEffect.collect { effect ->
            if (effect is BuyerHomeViewEffect.RequireAuthForRadius) callbacks.onRequireAuth()
        }
    }
    FavoritesEffectHandler(favoritesViewModel = favoritesViewModel, onRequireAuth = callbacks.onRequireAuth)
    LaunchedEffect(homeState.userLocation) { locationLabel = OsmMapUtils.resolveLocationLabel(context, homeState.userLocation) }

    val filteredOffers = rememberFilteredHomeOffers(homeState, searchQuery, selectedCategory, effectiveRadiusKm, merchantId)

    MerchantOfferFilter(merchantId, callbacks.onClearMerchantFilter, modifier) {
        BuyerHomeHost(
            data = HomeContentData(
                searchState = HomeSearchProps(searchQuery, effectiveIsGuest, effectiveRadiusKm, locationLabel, selectedCategory),
                filteredOffers = filteredOffers,
                cart = cartState.storeCart,
                showRadiusSheet = showRadiusSheet,
                savedOfferIds = favoritesState.favoriteOfferIds,
                isLoading = homeState.isLoading,
                errorMessage = homeState.errorMessage,
                nextPage = homeState.nextPage,
                pageLoadFailed = homeState.pageLoadFailed,
                usingCachedOffers = homeState.usingCachedOffers,
                filters = filters,
                isLocationDisabled = homeState.isLocationDisabled
            ),
            callbacks = callbacks,
            actions = HomeHostActions(
                onRefreshLocation = { homeViewModel.dispatchIntent(BuyerHomeViewIntent.RefreshLocation) },
                onSearchChange = { searchQuery = it },
                onCategoryToggle = { clicked -> selectedCategory = if (selectedCategory == clicked) StoreCategory.ALL else clicked },
                onToggleRadiusSheet = { showRadiusSheet = it },
                onRadiusSelected = { km -> homeViewModel.dispatchIntent(BuyerHomeViewIntent.RadiusSelected(km)) },
                onToggleSaveOffer = { offer -> favoritesViewModel.dispatchIntent(FavoritesViewIntent.ToggleFavorite(offer)) },
                onRefreshOffers = { homeViewModel.dispatchIntent(BuyerHomeViewIntent.RefreshOffers) },
                onLoadNextPage = { homeViewModel.dispatchIntent(BuyerHomeViewIntent.LoadNextPage) },
                onChangeFilters = { homeViewModel.dispatchIntent(BuyerHomeViewIntent.ChangeFilters(it)) }
            ),
            modifier = Modifier
        )
    }
}

@Composable
private fun BuyerHomeHost(data: HomeContentData, callbacks: BuyerHomeCallbacks, actions: HomeHostActions, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            BuyerHomeLazyFeed(data = data, callbacks = callbacks, actions = actions)
        }
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
private fun BuyerHomeBlockingState(data: HomeContentData, actions: HomeHostActions): Boolean {
    when {
        data.isLocationDisabled -> LocationDisabledState(onChooseManualLocation = { actions.onRefreshLocation() })
        data.errorMessage != null && data.filteredOffers.isEmpty() -> OfflineErrorState(onRetry = actions.onRefreshOffers)
        else -> return false
    }
    return true
}

@Composable
private fun BuyerHomeLazyFeed(data: HomeContentData, callbacks: BuyerHomeCallbacks, actions: HomeHostActions) {
    if (BuyerHomeBlockingState(data, actions)) return
    LazyColumn(
        modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
        contentPadding = PaddingValues(bottom = 168.dp)
    ) {
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
                onMapToggle = callbacks.onMapToggle,
                onOpenSearch = callbacks.onOpenSearch
            )
        }
        item { BuyerCategoryChipsRow(selectedCategory = data.searchState.selectedCategory, onCategoryToggle = actions.onCategoryToggle) }
        item { BuyerOfferFilterControl(data.filters, actions.onChangeFilters) }
        item { BuyerFeedSummary(data, actions.onRefreshOffers) }
        if (data.isLoading && data.filteredOffers.isEmpty()) {
            item { ShimmerOfferList() }
        } else {
            items(data.filteredOffers, key = { it.id }) { offer ->
                FavoriteProductOfferCard(
                    offer = offer,
                    isFavorite = offer.id in data.savedOfferIds,
                    onClick = { callbacks.onProductClick(offer) },
                    onFavoriteClick = { actions.onToggleSaveOffer(offer) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        data.nextPage?.let { page ->
            item(key = "nearby-next-page-$page") {
                if (data.pageLoadFailed) {
                    TextButton(onClick = actions.onLoadNextPage) {
                        Text(stringResource(R.string.nearby_retry_page))
                    }
                } else {
                    LaunchedEffect(page) { actions.onLoadNextPage() }
                }
            }
        }
    }
}

@Composable
private fun BuyerHomeLocationHeader(locationLabel: String, onRefreshLocation: () -> Unit, onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp)
                .clip(RoundedCornerShape(20.dp)).background(SensusSurface).clickable(onClick = onRefreshLocation)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = "Ubicación", tint = SensusVioletInk, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = locationLabel, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f, fill = false))
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Cambiar", tint = SensusTextSecondary, modifier = Modifier.size(18.dp))
        }
        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(SensusSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = "Notificaciones",
                    tint = SensusVioletInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun BuyerHomeHeroTitle() {
    CommerceHero(
        title = stringResource(R.string.commerce_buyer_title),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(stringResource(R.string.commerce_buyer_intro))
    }
}

@Composable
private fun BuyerFeedSummary(data: HomeContentData, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val message = if (data.usingCachedOffers) stringResource(R.string.nearby_cached_offers) else data.errorMessage
        CommerceFeedFeedback(data.isLoading, message, onRetry)
        Text(
            stringResource(R.string.commerce_buyer_results, data.filteredOffers.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        if (data.filteredOffers.isEmpty() && !data.isLoading && data.errorMessage.isNullOrBlank()) {
            Surface(shape = RoundedCornerShape(16.dp), color = SensusVioletBg, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.commerce_buyer_empty), style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.commerce_buyer_empty_hint), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun BuyerHomeSearchSection(
    searchState: HomeSearchProps,
    onSearchChange: (String) -> Unit,
    onRadiusClick: () -> Unit,
    onMapToggle: () -> Unit,
    onOpenSearch: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onSearchChange,
            placeholder = { Text(text = "Buscar productos o comercios...", fontSize = 15.sp, color = SensusTextMuted) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = SensusTextSecondary) },
            trailingIcon = {
                if (onOpenSearch != null) {
                    IconButton(onClick = onOpenSearch, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Búsqueda avanzada", tint = SensusPurple)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = sensusOutlinedTextFieldColors(containerColor = SensusSurface),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeRadiusChip(searchState.effectiveIsGuest, searchState.effectiveRadiusKm, onRadiusClick)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SensusVioletBg)
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onMapToggle)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Map, contentDescription = "Ver en mapa", tint = SensusVioletInk, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Ver en mapa", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
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
            .background(SensusSurface)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (effectiveIsGuest) Icons.Default.Lock else Icons.Default.Tune,
            contentDescription = "Filtro radio",
            tint = SensusVioletInk,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
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
            val background by animateColorAsState(if (isSelected) SensusPurple else SensusSurface, label = "category")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = background,
                shadowElevation = if (isSelected) 2.dp else 0.dp,
                modifier = Modifier.heightIn(min = 48.dp).selectable(isSelected) { onCategoryToggle(category) }
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
                        fontSize = 13.sp,
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
    Surface(shape = RoundedCornerShape(20.dp), color = SensusPurple, shadowElevation = 2.dp, modifier = modifier.clickable(onClick = onClick)) {
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
    if (StoreCategory.fromApiCode(offer.category) == category) return true
    val keyword = categoryKeywords[category] ?: return true
    return offer.category.contains(keyword, ignoreCase = true)
}

@Composable
private fun MerchantOfferFilter(merchantId: String?, onClear: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxSize()) {
        if (merchantId != null) {
            androidx.compose.material3.TextButton(colors = sensusTextButtonColors(), onClick = onClear) { Text("Ver ofertas de todos los comercios") }
        }
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
