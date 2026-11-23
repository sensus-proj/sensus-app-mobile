package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.components.states.OfflineBanner
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val PREFETCH_DISTANCE = 5
private const val EXPANDED_RADIUS_KM = 25
private const val BOTTOM_NAV_CLEARANCE_DP = 110
private const val PREVIEW_ORIG_PRICE_A = 120.0
private const val PREVIEW_FINAL_PRICE_A = 59.0
private const val PREVIEW_STOCK_A = 4
private const val PREVIEW_DIST_A = 320
private const val PREVIEW_ORIG_PRICE_B = 80.0
private const val PREVIEW_FINAL_PRICE_B = 45.0
private const val PREVIEW_DIST_B = 410

@ComposeUiModel
data class OfferFeedCallbacks(
    val isGuest: Boolean = false,
    val onProductClick: (ProductOffer) -> Unit = {},
    val onOpenInterests: () -> Unit = {},
    val onRequireAuth: () -> Unit = {}
)

@Composable
fun OfferFeedScreen(callbacks: OfferFeedCallbacks, modifier: Modifier = Modifier, viewModel: OfferFeedViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(callbacks.isGuest) {
        viewModel.dispatchIntent(OfferFeedViewIntent.LoadInitial)
    }

    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                OfferFeedViewEffect.RequireAuth -> callbacks.onRequireAuth()
                OfferFeedViewEffect.NavigateToInterestsPicker -> callbacks.onOpenInterests()
            }
        }
    }

    OfferFeedContent(
        state = state,
        callbacks = callbacks,
        onIntent = viewModel::dispatchIntent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferFeedContent(
    state: OfferFeedViewState,
    callbacks: OfferFeedCallbacks,
    onIntent: (OfferFeedViewIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
    ) {
        OfferFeedHeader(onOpenInterests = { onIntent(OfferFeedViewIntent.OpenInterestsPicker) })

        if (state.isOfflineCached) {
            OfflineBanner(onRetry = { onIntent(OfferFeedViewIntent.Refresh) })
        }

        when {
            callbacks.isGuest || state.isGuestBlocked -> OfferFeedGuestLocked(onRequireAuth = callbacks.onRequireAuth)
            state.isLoading -> OfferFeedLoadingState()
            state.errorMessage != null && state.items.isEmpty() -> OfferFeedErrorState(
                message = state.errorMessage,
                onRetry = { onIntent(OfferFeedViewIntent.Refresh) }
            )
            state.items.isEmpty() -> OfferFeedEmptyState(
                hasUserInterests = state.hasUserInterests,
                onChooseInterests = { onIntent(OfferFeedViewIntent.OpenInterestsPicker) },
                onExpandRadius = { onIntent(OfferFeedViewIntent.ExpandRadius(EXPANDED_RADIUS_KM)) }
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onIntent(OfferFeedViewIntent.Refresh) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                OfferFeedList(
                    items = state.items,
                    canLoadMore = state.canLoadMore,
                    isLoadingMore = state.isLoadingMore,
                    onLoadNextPage = { onIntent(OfferFeedViewIntent.LoadNextPage) },
                    onProductClick = callbacks.onProductClick
                )
            }
        }
    }
}

@Composable
private fun OfferFeedHeader(onOpenInterests: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Promociones para ti",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = "Ofertas cercanas ordenadas según tus intereses",
                fontSize = 12.sp,
                color = SensusTextSecondary
            )
        }
        OutlinedButton(
            onClick = onOpenInterests,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Tune, contentDescription = "Editar intereses", tint = SensusPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Intereses", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
        }
    }
}

@Composable
private fun OfferFeedList(
    items: List<OfferFeedItem>,
    canLoadMore: Boolean,
    isLoadingMore: Boolean,
    onLoadNextPage: () -> Unit,
    onProductClick: (ProductOffer) -> Unit
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(items.size, canLoadMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            canLoadMore && items.isNotEmpty() && lastVisible >= (items.size - PREFETCH_DISTANCE).coerceAtLeast(0)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadNextPage()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = BOTTOM_NAV_CLEARANCE_DP.dp)
    ) {
        items(items, key = { it.offerId }) { feedItem ->
            val offer = feedItem.toProductOffer()
            ProductOfferCard(
                offer = offer,
                onClick = { onProductClick(offer) }
            )
        }
        if (isLoadingMore) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SensusPurple, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun OfferFeedLoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SensusPurple)
    }
}

@Composable
private fun OfferFeedErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, tint = SensusRed, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, color = SensusTextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = "Reintentar", color = Color.White)
        }
    }
}

@Composable
private fun OfferFeedEmptyState(hasUserInterests: Boolean, onChooseInterests: () -> Unit, onExpandRadius: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = "Sin promociones", tint = SensusPurple, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (hasUserInterests) "No encontramos promociones en este radio" else "Elige tus intereses para mejores promociones",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasUserInterests) {
                "Amplía el radio de búsqueda para descubrir comercios cercanos."
            } else {
                "Selecciona tus categorías favoritas y ordenaremos las mejores ofertas cercanas."
            },
            fontSize = 13.sp,
            color = SensusTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = if (hasUserInterests) onExpandRadius else onChooseInterests,
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text(
                text = if (hasUserInterests) "Ampliar radio" else "Elegir intereses",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun OfferFeedGuestLocked(onRequireAuth: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Solo usuarios registrados", tint = SensusPurple, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Promociones personalizadas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Inicia sesión o regístrate para ver promociones ordenadas según tus intereses.",
            fontSize = 13.sp,
            color = SensusTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRequireAuth,
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = "Crear cuenta o iniciar sesión", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, name = "FeedLoading")
@Composable
internal fun FeedLoadingPreview() {
    SensusTheme {
        OfferFeedContent(
            state = OfferFeedViewState(isLoading = true),
            callbacks = OfferFeedCallbacks(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "FeedLoaded")
@Composable
internal fun FeedLoadedPreview() {
    SensusTheme {
        OfferFeedContent(
            state = OfferFeedViewState(
                items = listOf(
                    OfferFeedItem(
                        offerId = "1",
                        title = "Bolsa sorpresa de panadería",
                        categoryCode = "Bakery",
                        categoryDisplayName = "Panadería",
                        imageUrl = null,
                        originalPrice = PREVIEW_ORIG_PRICE_A,
                        finalPrice = PREVIEW_FINAL_PRICE_A,
                        stock = PREVIEW_STOCK_A,
                        distanceMeters = PREVIEW_DIST_A,
                        expiresAtUtc = "2026-11-21T02:00:00Z",
                        merchantId = "m1",
                        merchantName = "La Espiga",
                        slot = FeedSlot.RANKED
                    )
                )
            ),
            callbacks = OfferFeedCallbacks(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "FeedEmpty")
@Composable
internal fun FeedEmptyPreview() {
    SensusTheme {
        OfferFeedContent(
            state = OfferFeedViewState(items = emptyList(), hasUserInterests = false),
            callbacks = OfferFeedCallbacks(),
            onIntent = {}
        )
    }
}

@Preview(showBackground = true, name = "FeedOffline")
@Composable
internal fun FeedOfflinePreview() {
    SensusTheme {
        OfferFeedContent(
            state = OfferFeedViewState(
                items = listOf(
                    OfferFeedItem(
                        offerId = "1",
                        title = "Lácteos frescos",
                        categoryCode = "Dairy",
                        categoryDisplayName = "Lácteos",
                        imageUrl = null,
                        originalPrice = PREVIEW_ORIG_PRICE_B,
                        finalPrice = PREVIEW_FINAL_PRICE_B,
                        stock = 2,
                        distanceMeters = PREVIEW_DIST_B,
                        expiresAtUtc = "2026-11-21T02:00:00Z",
                        merchantId = "m2",
                        merchantName = "Cremería Sur",
                        slot = FeedSlot.EXPLORATION
                    )
                ),
                isOfflineCached = true
            ),
            callbacks = OfferFeedCallbacks(),
            onIntent = {}
        )
    }
}
