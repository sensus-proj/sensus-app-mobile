package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary

private const val ERROR_BG_ALPHA = 0.1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreReviewsScreen(
    cluster: MapStoreCluster,
    onNavigateBack: () -> Unit,
    onRequireAuth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoreReviewsViewModel = hiltViewModel()
) {
    val merchantId = cluster.merchantId.orEmpty()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(merchantId) {
        if (merchantId.isNotBlank()) {
            viewModel.dispatchIntent(
                StoreReviewsViewIntent.Load(
                    merchantId = merchantId,
                    storeName = cluster.storeName,
                    storeAddress = cluster.address,
                    storePhotoUrl = cluster.merchantProfile?.fotoFachadaUrl
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                StoreReviewsViewEffect.RequireAuth -> onRequireAuth()
                is StoreReviewsViewEffect.ShowMessage ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        containerColor = SensusBackground,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusTextPrimary)
                    }
                },
                title = {
                    Text(
                        text = "Calificaciones y reseñas",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        StoreReviewsBody(
            state = state,
            merchantId = merchantId,
            onIntent = viewModel::dispatchIntent,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        )
    }
}

@Composable
private fun StoreReviewsBody(state: StoreReviewsViewState, merchantId: String, onIntent: (StoreReviewsViewIntent) -> Unit, modifier: Modifier = Modifier) {
    val reviews = state.summary?.reviews.orEmpty()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PlayStoreSummaryCard(state = state, summary = state.summary) }
        state.errorMessage?.let { msg ->
            item {
                Surface(color = SensusRed.copy(alpha = ERROR_BG_ALPHA), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(text = msg, fontSize = 12.sp, color = SensusRed, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(12.dp))
                }
            }
        }
        if (merchantId.isNotBlank()) {
            item { ReviewComposerCard(state = state, onIntent = onIntent, merchantId = merchantId) }
        }
        if (state.isLoading && state.summary == null) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SensusPurple)
                }
            }
        } else if (reviews.isEmpty()) {
            item {
                Text(
                    text = "Aún no hay comentarios. ¡Sé el primero en calificar esta tienda!",
                    fontSize = 13.sp,
                    color = SensusTextMuted,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(reviews, key = { it.id }) { review ->
                StoreReviewCard(
                    review = review,
                    isReplying = state.replyingToReviewId == review.id,
                    replyDraft = state.replyDraft,
                    isSendingReply = state.isSendingReply,
                    onIntent = onIntent
                )
            }
        }
    }
}
