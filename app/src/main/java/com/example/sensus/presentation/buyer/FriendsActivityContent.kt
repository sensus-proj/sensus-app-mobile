package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.CommunityActivityItem
import com.example.sensus.domain.model.CommunityActivityType
import com.example.sensus.presentation.components.states.ShimmerFeedList
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg

private const val MAX_STARS = 5
private const val ACTIVITY_NAV_CLEARANCE_DP = 96

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FriendsActivityFeedContent(state: FriendsActivityViewState, onRefresh: () -> Unit, onRequireAuth: () -> Unit, modifier: Modifier = Modifier) {
    if (state.isGuest) {
        FriendsActivityGuestPrompt(onRequireAuth = onRequireAuth, modifier = modifier)
        return
    }
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = ACTIVITY_NAV_CLEARANCE_DP.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Actividad de tus amigos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Text(
                        text = "Comentarios en tiendas y me gusta de tus amigos conectados",
                        fontSize = 12.sp,
                        color = SensusTextSecondary
                    )
                }
            }
            state.errorMessage?.let { error ->
                item { Text(text = error, fontSize = 12.sp, color = SensusRed) }
            }
            if (state.isLoading && state.items.isEmpty()) {
                item { ShimmerFeedList() }
            } else if (!state.isLoading && state.items.isEmpty() && state.errorMessage == null) {
                item { FriendsActivityEmptyCard() }
            } else {
                items(state.items, key = { it.id }) { item ->
                    FriendsActivityCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun FriendsActivityGuestPrompt(onRequireAuth: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(48.dp))
            Text(
                text = "Inicia sesión para ver los comentarios y me gusta de tus amigos",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusTextPrimary,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRequireAuth, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
                Text("Iniciar sesión")
            }
        }
    }
}

@Composable
private fun FriendsActivityEmptyCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(36.dp))
            Text(
                text = "Aún no hay actividad reciente",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = "Cuando tú o tus amigos den me gusta a un producto o comenten una tienda, aparecerá aquí.",
                fontSize = 12.sp,
                color = SensusTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FriendsActivityCard(item: CommunityActivityItem) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityActorHeader(item = item)
            ActivityBadgeRow(item = item)
            item.commentText?.takeIf { it.isNotBlank() }?.let { comment ->
                Surface(color = SensusVioletBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "\"$comment\"",
                        fontSize = 13.sp,
                        color = SensusTextPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityActorHeader(item: CommunityActivityItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorCircleAvatar(name = item.actorName, photoUrl = item.actorPhotoUrl, sizeDp = 38)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.actorName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = formatReviewDateWithYear(item.createdAtUtc), fontSize = 11.sp, color = SensusTextMuted)
        }
    }
}

@Composable
private fun ActivityBadgeRow(item: CommunityActivityItem) {
    val (icon, tint, actionText) = when (item.type) {
        CommunityActivityType.OFFER_LIKE -> Triple(Icons.Default.Favorite, SensusRed, "Le gustó la publicación: ${item.targetTitle}")
        CommunityActivityType.STORE_REVIEW -> Triple(Icons.Default.RateReview, SensusPurple, "Comentó sobre ${item.targetTitle}")
        CommunityActivityType.REVIEW_REPLY -> Triple(Icons.AutoMirrored.Filled.Reply, SensusPurple, "Respondió en ${item.targetTitle}")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text = actionText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary, modifier = Modifier.weight(1f))
    }
    item.targetSubtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
        Text(text = subtitle, fontSize = 11.sp, color = SensusTextMuted)
    }
    item.rating?.takeIf { it in 1..MAX_STARS }?.let { stars ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (idx in 1..MAX_STARS) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (idx <= stars) SensusPurple else Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "$stars/$MAX_STARS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SensusTextSecondary)
        }
    }
}
