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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.SharedOfferMessage
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.Ink
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusDiscountTag
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val THREAD_POLL_INTERVAL_MS = 5_000L
private const val BUBBLE_MAX_WIDTH_DP = 292

@ComposeUiModel
data class FriendSharedOffersActions(
    val onNavigateBack: () -> Unit,
    val onOfferClick: (ProductOffer) -> Unit,
    val onExploreOffers: () -> Unit
)

@Composable
fun FriendSharedOffersScreen(
    friend: ConnectedFriend,
    actions: FriendSharedOffersActions,
    modifier: Modifier = Modifier,
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    val isCurrentFriendLoaded = state.activeThreadFriendUserId == friend.userId
    val threadOffers = if (isCurrentFriendLoaded) state.sharedOffers else emptyList()

    DisposableEffect(friend.userId) {
        onDispose { viewModel.dispatchIntent(FriendsViewIntent.ClearSharedOffers) }
    }

    LaunchedEffect(friend.userId) {
        while (true) {
            if (friend.userId.isNotBlank()) {
                viewModel.dispatchIntent(FriendsViewIntent.LoadSharedOffers(friend.userId))
            }
            delay(THREAD_POLL_INTERVAL_MS)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        FriendThreadTopBar(
            friend = friend,
            onNavigateBack = actions.onNavigateBack,
            onRefresh = {
                if (friend.userId.isNotBlank()) {
                    viewModel.dispatchIntent(FriendsViewIntent.LoadSharedOffers(friend.userId))
                }
            }
        )
        FriendSharedOffersThreadBody(
            friend = friend,
            isLoading = !isCurrentFriendLoaded || state.isLoadingThread,
            threadOffers = threadOffers,
            actions = actions,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        FriendThreadBottomHint()
    }
}

@Composable
private fun FriendSharedOffersThreadBody(
    friend: ConnectedFriend,
    isLoading: Boolean,
    threadOffers: List<SharedOfferMessage>,
    actions: FriendSharedOffersActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            isLoading && threadOffers.isEmpty() -> {
                CircularProgressIndicator(color = SensusPurple, modifier = Modifier.align(Alignment.Center))
            }
            threadOffers.isEmpty() -> {
                EmptySharedOffersNotice(
                    friendName = friend.displayName,
                    onExploreOffers = actions.onExploreOffers,
                    modifier = Modifier.align(Alignment.Center).padding(20.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(threadOffers, key = { it.id }) { item ->
                        SharedOfferBubble(
                            item = item,
                            friend = friend,
                            onClick = { actions.onOfferClick(item.toProductOffer()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendThreadTopBar(friend: ConnectedFriend, onNavigateBack: () -> Unit, onRefresh: () -> Unit) {
    Surface(color = SensusSurface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SensusTextPrimary)
            }
            FriendAvatar(name = friend.displayName, photoUrl = friend.photoUrl, size = 44.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ofertas compartidas entre ustedes",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar ofertas", tint = SensusVioletInk)
            }
        }
    }
}

@Composable
private fun SharedOfferBubble(item: SharedOfferMessage, friend: ConnectedFriend, onClick: () -> Unit) {
    val isSentByMe = item.recipientUserId == friend.userId
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isSentByMe) "Compartiste esta oferta" else "${friend.displayName} te compartió",
            fontSize = 11.sp,
            color = SensusTextMuted,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSentByMe) SensusVioletBg else SensusSurface),
            border = BorderStroke(1.dp, if (isSentByMe) SensusPurple.copy(alpha = 0.25f) else SensusCardBorder),
            modifier = Modifier
                .widthIn(max = BUBBLE_MAX_WIDTH_DP.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
        ) {
            Column {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.offerTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(132.dp)
                    )
                }
                SharedOfferBubbleDetails(item = item)
            }
        }
    }
}

@Composable
private fun SharedOfferBubbleDetails(item: SharedOfferMessage) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = item.storeName,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusVioletInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.offerTitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatPrice(item.discountedPrice),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            if (item.originalPrice > item.discountedPrice) {
                Text(
                    text = formatPrice(item.originalPrice),
                    fontSize = 13.sp,
                    color = SensusTextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            if (item.discountPercentage > 0) {
                Surface(shape = RoundedCornerShape(6.dp), color = SensusDiscountTag) {
                    Text(
                        text = "-${item.discountPercentage}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Text(text = "${formatBubbleTime(item.sharedAtEpochMs)} • Toca para ver oferta", fontSize = 11.sp, color = SensusTextSecondary)
    }
}

@Composable
private fun EmptySharedOffersNotice(friendName: String, onExploreOffers: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(34.dp))
            Text(text = "Sin ofertas compartidas aún", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = "Abre cualquier oferta y pulsa Compartir para enviársela directo a $friendName aquí.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
            Button(
                onClick = onExploreOffers,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = SensusButtonShape
            ) {
                Text(text = "Explorar ofertas", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FriendThreadBottomHint() {
    Surface(color = SensusSurface, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Solo ofertas: comparte productos desde el detalle de cada oferta.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        }
    }
}

private fun formatPrice(amount: Double): String = String.format(Locale.forLanguageTag("es-MX"), "$%.2f", amount)

private fun formatBubbleTime(epochMs: Long): String {
    val formatter = SimpleDateFormat("dd MMM HH:mm", Locale.forLanguageTag("es-MX"))
    return formatter.format(Date(epochMs))
}
