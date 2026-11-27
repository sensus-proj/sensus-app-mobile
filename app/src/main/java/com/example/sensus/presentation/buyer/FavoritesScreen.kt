package com.example.sensus.presentation.buyer

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.components.states.OfflineBanner
import com.example.sensus.presentation.components.states.ShimmerOfferList
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedButtonColors

private const val MENU_CLEARANCE_DP = 110
private const val EMPTY_ICON_BG_ALPHA = 0.12f
private const val PREVIEW_ORIGINAL_PRICE = 120.0
private const val PREVIEW_DISCOUNT_PRICE = 60.0
private const val PREVIEW_DISCOUNT_PCT = 50
private const val PREVIEW_STOCK = 3
private const val PREVIEW_DISTANCE_M = 250

@ComposeUiModel
data class FavoritesCallbacks(
    val onNavigateBack: () -> Unit = {},
    val onOfferClick: (ProductOffer) -> Unit = {},
    val onExploreOffers: () -> Unit = {},
    val onRequireAuth: () -> Unit = {}
)

@Composable
internal fun FavoritesEffectHandler(favoritesViewModel: FavoritesViewModel, onRequireAuth: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(favoritesViewModel) {
        favoritesViewModel.viewEffect.collect { effect ->
            when (effect) {
                FavoritesViewEffect.RequireAuth -> onRequireAuth()
                is FavoritesViewEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(callbacks: FavoritesCallbacks, modifier: Modifier = Modifier, viewModel: FavoritesViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatchIntent(FavoritesViewIntent.Load)
    }
    FavoritesEffectHandler(favoritesViewModel = viewModel, onRequireAuth = callbacks.onRequireAuth)

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.dispatchIntent(FavoritesViewIntent.Refresh) },
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        FavoritesContent(
            state = state,
            callbacks = callbacks,
            onIntent = viewModel::dispatchIntent
        )
    }
}

@Composable
internal fun FavoritesContent(
    state: FavoritesViewState,
    callbacks: FavoritesCallbacks,
    onIntent: (FavoritesViewIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        FavoritesTopBar(totalCount = state.favorites.size, onNavigateBack = callbacks.onNavigateBack)
        FavoritesFilterChips(selected = state.filter, onSelect = { onIntent(FavoritesViewIntent.SelectFilter(it)) })
        if (state.isUsingOfflineCache && state.favorites.isNotEmpty()) {
            OfflineBanner(message = stringResource(R.string.favorites_sync_cached_notice))
        }
        FavoritesBody(state = state, callbacks = callbacks, onIntent = onIntent)
    }
}

@Composable
private fun FavoritesTopBar(totalCount: Int, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SensusSurface)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SensusVioletInk)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = stringResource(R.string.favorites_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            if (totalCount > 0) {
                Text(text = "$totalCount guardada(s)", fontSize = 12.sp, color = SensusTextSecondary)
            }
        }
    }
}

@Composable
private fun FavoritesFilterChips(selected: FavoriteFilter, onSelect: (FavoriteFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == FavoriteFilter.ALL,
            onClick = { onSelect(FavoriteFilter.ALL) },
            label = { Text(stringResource(R.string.favorites_filter_all), fontWeight = FontWeight.SemiBold) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SensusPurple,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
        FilterChip(
            selected = selected == FavoriteFilter.AVAILABLE_ONLY,
            onClick = { onSelect(FavoriteFilter.AVAILABLE_ONLY) },
            label = { Text(stringResource(R.string.favorites_filter_available), fontWeight = FontWeight.SemiBold) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SensusPurple,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
    }
}

@Composable
private fun FavoritesBody(state: FavoritesViewState, callbacks: FavoritesCallbacks, onIntent: (FavoritesViewIntent) -> Unit) {
    when {
        state.isLoading && state.favorites.isEmpty() -> ShimmerOfferList(modifier = Modifier.padding(top = 8.dp))
        state.errorMessage != null && state.favorites.isEmpty() -> FavoritesErrorState(
            message = state.errorMessage,
            onRetry = { onIntent(FavoritesViewIntent.Load) }
        )
        state.favorites.isEmpty() -> FavoritesEmptyState(
            isFiltered = state.filter == FavoriteFilter.AVAILABLE_ONLY,
            onExplore = callbacks.onExploreOffers,
            onShowAll = { onIntent(FavoritesViewIntent.SelectFilter(FavoriteFilter.ALL)) }
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = MENU_CLEARANCE_DP.dp)
        ) {
            items(state.favorites, key = { it.offer.id }) { item ->
                FavoriteProductOfferCard(
                    offer = item.offer,
                    isFavorite = true,
                    onClick = { callbacks.onOfferClick(item.offer) },
                    onFavoriteClick = { onIntent(FavoritesViewIntent.ToggleFavorite(item.offer)) }
                )
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState(isFiltered: Boolean, onExplore: () -> Unit, onShowAll: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = SensusPurple.copy(alpha = EMPTY_ICON_BG_ALPHA),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = SensusVioletInk,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(if (isFiltered) R.string.favorites_empty_filtered_title else R.string.favorites_empty_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(if (isFiltered) R.string.favorites_empty_filtered_subtitle else R.string.favorites_empty_subtitle),
                fontSize = 13.sp,
                color = SensusTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = if (isFiltered) onShowAll else onExplore,
                shape = SensusButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = stringResource(if (isFiltered) R.string.favorites_filter_all else R.string.favorites_explore),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FavoritesErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = SensusRed, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRetry,
            shape = SensusButtonShape,
            colors = sensusOutlinedButtonColors(),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(stringResource(R.string.offline_error_retry), fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun FavoritesLoadingPreview() {
    SensusTheme {
        FavoritesContent(FavoritesViewState(isLoading = true), FavoritesCallbacks(), {})
    }
}

@Preview(showBackground = true)
@Composable
internal fun FavoritesLoadedPreview() {
    val sample = FavoriteOffer(
        offer = ProductOffer(
            id = "fav-1",
            name = "Pan artesanal del día",
            storeName = "Panadería Central",
            category = "Panadería",
            originalPrice = PREVIEW_ORIGINAL_PRICE,
            discountPrice = PREVIEW_DISCOUNT_PRICE,
            discountPercent = PREVIEW_DISCOUNT_PCT,
            unitsLeft = PREVIEW_STOCK,
            distanceMeters = PREVIEW_DISTANCE_M
        )
    )
    SensusTheme {
        FavoritesContent(FavoritesViewState(favorites = listOf(sample)), FavoritesCallbacks(), {})
    }
}

@Preview(showBackground = true)
@Composable
internal fun FavoritesEmptyPreview() {
    SensusTheme {
        FavoritesContent(FavoritesViewState(), FavoritesCallbacks(), {})
    }
}

@Preview(showBackground = true)
@Composable
internal fun FavoritesErrorPreview() {
    SensusTheme {
        FavoritesContent(FavoritesViewState(errorMessage = "No logramos conectarnos. Revisa tu conexión."), FavoritesCallbacks(), {})
    }
}
