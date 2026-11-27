package com.example.sensus.presentation.buyer

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
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

private const val CARD_CORNER_DP = 16
private const val IMAGE_CORNER_DP = 12
private const val THUMBNAIL_SIZE_DP = 76
private const val IMAGE_BG_ALPHA = 0.08f
private const val STOCK_BG_ALPHA = 0.12f

@ComposeUiModel
data class SavedInterestsCallbacks(
    val onNavigateBack: () -> Unit = {},
    val onOfferClick: (ProductOffer) -> Unit = {},
    val onExploreOffers: () -> Unit = {}
)

@ComposeUiModel
private data class SavedInterestsActions(
    val onOfferClick: (ProductOffer) -> Unit,
    val onRemoveOffer: (String) -> Unit,
    val onExploreOffers: () -> Unit
)

@Composable
internal fun SavedInterestsEffectHandler(savedViewModel: SavedInterestsViewModel, onRequireAuth: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(savedViewModel) {
        savedViewModel.viewEffect.collect { effect ->
            when (effect) {
                is SavedInterestsViewEffect.SavedStateChanged -> {
                    val msgRes = if (effect.isSaved) {
                        R.string.saved_interests_toast_added
                    } else {
                        R.string.saved_interests_toast_removed
                    }
                    Toast.makeText(context, context.getString(msgRes), Toast.LENGTH_SHORT).show()
                }
                SavedInterestsViewEffect.RequireAuth -> onRequireAuth()
            }
        }
    }
}

@Composable
fun SavedInterestsScreen(callbacks: SavedInterestsCallbacks, modifier: Modifier = Modifier, viewModel: SavedInterestsViewModel = hiltViewModel()) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        containerColor = SensusBackground,
        topBar = {
            SavedInterestsTopBar(
                totalCount = state.totalCount,
                onNavigateBack = callbacks.onNavigateBack
            )
        }
    ) { innerPadding ->
        SavedInterestsBody(
            state = state,
            actions = SavedInterestsActions(
                onOfferClick = callbacks.onOfferClick,
                onRemoveOffer = { viewModel.dispatchIntent(SavedInterestsViewIntent.RemoveOffer(it)) },
                onExploreOffers = callbacks.onExploreOffers
            ),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedInterestsTopBar(totalCount: Int, onNavigateBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusTextPrimary)
            }
        },
        title = {
            Column {
                Text(
                    text = stringResource(R.string.saved_interests_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary
                )
                Text(
                    text = "$totalCount elemento(s) guardados",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = SensusSurface)
    )
}

@Composable
private fun SavedInterestsBody(state: SavedInterestsViewState, actions: SavedInterestsActions, modifier: Modifier = Modifier) {
    if (state.totalCount == 0) {
        SavedInterestsEmptyState(onExploreOffers = actions.onExploreOffers, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.savedOffers, key = { "offer_${it.id}" }) { offer ->
            SavedOfferCard(
                offer = offer,
                onClick = { actions.onOfferClick(offer) },
                onRemoveClick = { actions.onRemoveOffer(offer.id) }
            )
        }
    }
}

@Composable
private fun SavedOfferCard(offer: ProductOffer, onClick: () -> Unit, onRemoveClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SavedOfferThumbnail(offer = offer)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = offer.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = offer.storeName,
                        fontSize = 13.sp,
                        color = SensusTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SavedOfferPricesRow(offer = offer)
                Text(
                    text = "Quedan ${offer.unitsLeft} pzs • ${offer.expirationDate}",
                    fontSize = 11.sp,
                    color = SensusTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Filled.PushPin,
                    contentDescription = stringResource(R.string.saved_interests_unpin),
                    tint = SensusVioletInk
                )
            }
        }
    }
}

@Composable
private fun SavedOfferThumbnail(offer: ProductOffer) {
    Box(
        modifier = Modifier
            .size(THUMBNAIL_SIZE_DP.dp)
            .clip(RoundedCornerShape(IMAGE_CORNER_DP.dp))
            .background(SensusPurple.copy(alpha = IMAGE_BG_ALPHA)),
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
            Icon(imageVector = Icons.Default.Storefront, contentDescription = offer.name, tint = SensusVioletInk, modifier = Modifier.size(28.dp))
        }
        Surface(
            color = SensusDiscountTag,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
        ) {
            Text(
                text = "-${offer.discountPercent}%",
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SavedOfferPricesRow(offer: ProductOffer) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)} MXN",
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
        Surface(color = SensusSuccess.copy(alpha = STOCK_BG_ALPHA), shape = RoundedCornerShape(6.dp)) {
            Text(
                text = stringResource(R.string.saved_interests_open_offer),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusSuccess,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SavedInterestsEmptyState(onExploreOffers: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(SensusBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SensusVioletBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = SensusVioletInk,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = stringResource(R.string.saved_interests_empty_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = stringResource(R.string.saved_interests_empty_subtitle),
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onExploreOffers,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = SensusButtonShape
            ) {
                Text(text = stringResource(R.string.saved_interests_explore), color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
