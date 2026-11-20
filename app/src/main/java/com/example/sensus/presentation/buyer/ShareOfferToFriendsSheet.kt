package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.MAX_OFFER_SHARE_FRIENDS
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusTextButtonColors

private const val FRIEND_LIST_MAX_HEIGHT_DP = 320
private const val LIMIT_ALERT_BG_ALPHA = 0.1f

@ComposeUiModel
internal data class ShareOfferDialogActions(
    val onRefreshFriends: () -> Unit,
    val onDismiss: () -> Unit,
    val onSendToFriends: (List<String>) -> Unit
)

@Composable
internal fun ShareOfferToFriendsDialog(offer: ProductOffer, friends: List<ConnectedFriend>, isSharing: Boolean, actions: ShareOfferDialogActions) {
    val context = LocalContext.current
    val shareableFriends = remember(friends) { friends.filter { it.userId.isNotBlank() } }
    var selectedIds by remember(offer.id) { mutableStateOf(emptySet<String>()) }
    var showLimitWarning by remember(offer.id) { mutableStateOf(false) }

    LaunchedEffect(offer.id) { actions.onRefreshFriends() }

    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = {
            Text(text = "Compartir con amigos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        },
        text = {
            ShareOfferDialogContent(
                offer = offer,
                friends = shareableFriends,
                selectedIds = selectedIds,
                showLimitWarning = showLimitWarning,
                onToggleFriend = { friendId ->
                    val isUnchecking = friendId in selectedIds
                    selectedIds = toggleFriendSelection(selectedIds, friendId) {
                        showLimitWarning = true
                        Toast.makeText(
                            context,
                            "Solo puedes compartir a un máximo de $MAX_OFFER_SHARE_FRIENDS amigos a la vez.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (isUnchecking) showLimitWarning = false
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { actions.onSendToFriends(selectedIds.toList()) },
                enabled = selectedIds.isNotEmpty() && !isSharing,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = SensusButtonShape
            ) {
                if (isSharing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text(text = "Enviar (${selectedIds.size})", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(colors = sensusTextButtonColors(), onClick = actions.onDismiss) { Text(text = "Cancelar", color = SensusTextSecondary) }
        }
    )
}

@Composable
private fun ShareOfferDialogContent(
    offer: ProductOffer,
    friends: List<ConnectedFriend>,
    selectedIds: Set<String>,
    showLimitWarning: Boolean,
    onToggleFriend: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "${offer.name} • ${offer.storeName}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusVioletInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (showLimitWarning) {
            Surface(color = SensusRed.copy(alpha = LIMIT_ALERT_BG_ALPHA), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "Solo puedes compartir esta oferta a un máximo de $MAX_OFFER_SHARE_FRIENDS amigos a la vez.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusRed,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
        if (friends.isEmpty()) {
            Text(
                text = "Aún no tienes amigos conectados en Sensus. Agrégalos desde Comunidad → Amigos y QR.",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        } else {
            Text(
                text = "Elige con qué amigos quieres compartir esta oferta:",
                fontSize = 13.sp,
                color = SensusTextMuted
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = FRIEND_LIST_MAX_HEIGHT_DP.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(friends, key = { it.userId }) { friend ->
                    ShareOfferFriendRow(
                        friend = friend,
                        checked = friend.userId in selectedIds,
                        onToggle = { onToggleFriend(friend.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareOfferFriendRow(friend: ConnectedFriend, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FriendAvatar(name = friend.displayName, photoUrl = friend.photoUrl, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = friend.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = SensusVioletInk)
        )
    }
}

private fun toggleFriendSelection(current: Set<String>, friendId: String, onLimitReached: () -> Unit): Set<String> = when {
    friendId in current -> current - friendId
    current.size >= MAX_OFFER_SHARE_FRIENDS -> {
        onLimitReached()
        current
    }
    else -> current + friendId
}
