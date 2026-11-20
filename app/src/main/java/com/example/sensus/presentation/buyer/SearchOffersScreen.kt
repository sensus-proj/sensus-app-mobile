package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val HISTORY_BG_ALPHA = 0.08f
private const val LIST_BOTTOM_PAD_DP = 40
private const val PREVIEW_SEARCH_ORIG_PRICE = 60.0
private const val PREVIEW_SEARCH_FINAL_PRICE = 35.0
private const val PREVIEW_SEARCH_STOCK = 4
private const val PREVIEW_SEARCH_DIST = 210
private const val PREVIEW_SEARCH_SCORE = 0.92
private const val PREVIEW_ALT_ORIG_PRICE = 40.0
private const val PREVIEW_ALT_FINAL_PRICE = 25.0
private const val PREVIEW_ALT_STOCK = 3
private const val PREVIEW_ALT_DIST = 310

@Composable
fun SearchOffersScreen(
    onNavigateBack: () -> Unit,
    onOfferClick: (ProductOffer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchOffersViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.dispatchIntent(SearchOffersViewIntent.Initialize)
    }

    SearchOffersContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onIntent = viewModel::dispatchIntent,
        onOfferClick = onOfferClick,
        modifier = modifier
    )
}

@Composable
internal fun SearchOffersContent(
    state: SearchOffersViewState,
    onNavigateBack: () -> Unit,
    onIntent: (SearchOffersViewIntent) -> Unit,
    onOfferClick: (ProductOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        SearchTopInputBar(
            query = state.query,
            onNavigateBack = onNavigateBack,
            onQueryChanged = { onIntent(SearchOffersViewIntent.QueryChanged(it)) },
            onClearQuery = { onIntent(SearchOffersViewIntent.QueryChanged("")) }
        )

        if (!state.isGuest && state.history.isNotEmpty() && !state.hasSearched) {
            SearchHistorySection(
                history = state.history,
                onSelectHistory = { onIntent(SearchOffersViewIntent.SelectHistory(it)) },
                onClearHistory = { onIntent(SearchOffersViewIntent.ClearHistory) }
            )
        }

        if (!state.errorMessage.isNullOrBlank()) {
            Text(
                text = state.errorMessage,
                color = SensusRed,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        SearchOffersBody(state = state, onOfferClick = onOfferClick, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun SearchOffersBody(state: SearchOffersViewState, onOfferClick: (ProductOffer) -> Unit, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SensusPurple)
        }
        state.hasSearched && state.results.isEmpty() -> SearchEmptyWithAlternativesState(
            searchedQuery = state.lastCompletedQuery,
            alternatives = state.alternatives,
            onOfferClick = onOfferClick
        )
        else -> LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = LIST_BOTTOM_PAD_DP.dp)) {
            if (state.isOfflineFallback) {
                item {
                    Text(
                        text = "Mostrando resultados guardados localmente (sin conexión).",
                        fontSize = 12.sp,
                        color = SensusTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
            items(state.results, key = { it.offerId }) { item ->
                val offer = item.toProductOffer()
                ProductOfferCard(offer = offer, onClick = { onOfferClick(offer) })
            }
        }
    }
}

@Composable
private fun SearchTopInputBar(query: String, onNavigateBack: () -> Unit, onQueryChanged: (String) -> Unit, onClearQuery: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusPurple)
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text(text = "Buscar ofertas o comercios...", color = SensusTextMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = SensusPurple) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar búsqueda", tint = SensusTextSecondary)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = sensusOutlinedTextFieldColors(containerColor = Color.White),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistorySection(history: List<String>, onSelectHistory: (String) -> Unit, onClearHistory: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Búsquedas recientes", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            TextButton(onClick = onClearHistory, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(text = "Limpiar historial", fontSize = 12.sp, color = SensusPurple)
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            history.forEach { term ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SensusPurple.copy(alpha = HISTORY_BG_ALPHA),
                    modifier = Modifier.heightIn(min = 48.dp).clickable { onSelectHistory(term) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = term, fontSize = 13.sp, color = SensusPurple, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyWithAlternativesState(searchedQuery: String, alternatives: List<OfferAlternativeItem>, onOfferClick: (ProductOffer) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LIST_BOTTOM_PAD_DP.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.SearchOff, contentDescription = "Sin coincidencias", tint = SensusPurple, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (alternatives.isNotEmpty() && searchedQuery.isNotEmpty()) {
                        "No encontramos $searchedQuery, pero te recomendamos estas ofertas similares cerca de ti"
                    } else {
                        "No encontramos ofertas para \"$searchedQuery\" en tu zona"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(alternatives, key = { it.offerId }) { alt ->
            val offer = alt.toProductOffer()
            ProductOfferCard(offer = offer, onClick = { onOfferClick(offer) })
        }
    }
}

@Preview(showBackground = true, name = "SearchRecent")
@Composable
internal fun SearchRecentPreview() {
    SensusTheme {
        SearchOffersContent(
            state = SearchOffersViewState(history = listOf("Panadería", "Lácteos")),
            onNavigateBack = {},
            onIntent = {},
            onOfferClick = {}
        )
    }
}

@Preview(showBackground = true, name = "SearchResults")
@Composable
internal fun SearchResultsPreview() {
    SensusTheme {
        SearchOffersContent(
            state = SearchOffersViewState(
                query = "Pan",
                hasSearched = true,
                results = listOf(
                    OfferSearchItem(
                        offerId = "1",
                        title = "Pan dulce artesanal",
                        description = "Conchas del día",
                        categoryCode = "Bakery",
                        categoryDisplayName = "Panadería",
                        imageUrl = null,
                        originalPrice = PREVIEW_SEARCH_ORIG_PRICE,
                        finalPrice = PREVIEW_SEARCH_FINAL_PRICE,
                        stock = PREVIEW_SEARCH_STOCK,
                        distanceMeters = PREVIEW_SEARCH_DIST,
                        expiresAtUtc = "2026-11-21T00:00:00Z",
                        merchantId = "m1",
                        merchantName = "La Espiga",
                        score = PREVIEW_SEARCH_SCORE
                    )
                )
            ),
            onNavigateBack = {},
            onIntent = {},
            onOfferClick = {}
        )
    }
}

@Preview(showBackground = true, name = "SearchEmptyAlternatives")
@Composable
internal fun SearchEmptyAlternativesPreview() {
    SensusTheme {
        SearchOffersContent(
            state = SearchOffersViewState(
                query = "Croissant",
                lastCompletedQuery = "Croissant",
                hasSearched = true,
                results = emptyList(),
                alternatives = listOf(
                    OfferAlternativeItem(
                        offerId = "2",
                        title = "Bolillos horneados",
                        categoryCode = "Bakery",
                        categoryDisplayName = "Panadería",
                        imageUrl = null,
                        originalPrice = PREVIEW_ALT_ORIG_PRICE,
                        finalPrice = PREVIEW_ALT_FINAL_PRICE,
                        stock = PREVIEW_ALT_STOCK,
                        distanceMeters = PREVIEW_ALT_DIST,
                        expiresAtUtc = "2026-11-21T00:00:00Z",
                        merchantId = "m2",
                        merchantName = "Horno Sur"
                    )
                )
            ),
            onNavigateBack = {},
            onIntent = {},
            onOfferClick = {}
        )
    }
}
