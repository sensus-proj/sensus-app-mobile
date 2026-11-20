package com.example.sensus.presentation.buyer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk

private const val BADGE_ALPHA = 0.14f
private const val AVATAR_BG_ALPHA = 0.12f
private const val AVATAR_ICON_RATIO = 0.55f

@Composable
internal fun FriendAvatar(name: String, photoUrl: String?, size: Dp = 42.dp) {
    val cleanUrl = photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
        if (raw.startsWith("http://", ignoreCase = true)) "https://${raw.drop("http://".length)}" else raw
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(SensusPurple.copy(alpha = AVATAR_BG_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = name,
            tint = SensusVioletInk,
            modifier = Modifier.size(size * AVATAR_ICON_RATIO)
        )
        if (cleanUrl != null) {
            AsyncImage(
                model = cleanUrl,
                contentDescription = "Foto de perfil de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
    }
}

@Composable
internal fun PendingRequestsSectionCard(pendingRequests: List<PendingFriendRequest>, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PendingRequestsHeaderRow(
                count = pendingRequests.size,
                expanded = expanded,
                onToggle = { expanded = !expanded }
            )
            AnimatedVisibility(visible = expanded) {
                PendingRequestsBody(
                    pendingRequests = pendingRequests,
                    onAccept = onAccept,
                    onReject = onReject
                )
            }
        }
    }
}

@Composable
private fun PendingRequestsHeaderRow(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SensusPurple.copy(alpha = BADGE_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Solicitudes pendientes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = if (count == 0) "No tienes invitaciones por revisar" else "Pulsa para aceptar o rechazar",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        }
        Surface(
            color = if (count > 0) SensusPurple else SensusPurple.copy(alpha = BADGE_ALPHA),
            shape = CircleShape
        ) {
            Text(
                text = count.toString(),
                color = if (count > 0) Color.White else SensusVioletInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Contraer solicitudes" else "Expandir solicitudes",
            tint = SensusTextMuted
        )
    }
}

@Composable
private fun PendingRequestsBody(pendingRequests: List<PendingFriendRequest>, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pendingRequests.isEmpty()) {
            Text(
                text = "Cuando un vecino escanee tu QR o abra tu enlace, aparecerá aquí para que aceptes o canceles.",
                fontSize = 13.sp,
                color = SensusTextMuted
            )
        } else {
            pendingRequests.forEach { request ->
                PendingRequestItemRow(
                    request = request,
                    onAccept = { onAccept(request.requestId) },
                    onReject = { onReject(request.requestId) }
                )
            }
        }
    }
}

@Composable
private fun PendingRequestItemRow(request: PendingFriendRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    Surface(
        color = SensusBackground,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FriendAvatar(name = request.senderName, photoUrl = request.senderPhotoUrl, size = 38.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.senderName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Código ${request.senderInviteCode} • Vía ${request.addedVia}",
                    fontSize = 11.sp,
                    color = SensusTextSecondary
                )
            }
            IconButton(
                onClick = onAccept,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SensusSuccess.copy(alpha = BADGE_ALPHA),
                    contentColor = SensusSuccess
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Aceptar invitación")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onReject,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = SensusRed.copy(alpha = BADGE_ALPHA),
                    contentColor = SensusRed
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar invitación")
            }
        }
    }
}

@Composable
internal fun SentRequestsSummaryCard(sentRequests: List<SentFriendRequest>) {
    if (sentRequests.isEmpty()) return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "Solicitudes enviadas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            sentRequests.forEach { sent ->
                Text(
                    text = "• ${sent.targetName} (${sent.targetInviteCode}) — esperando confirmación",
                    fontSize = 13.sp,
                    color = SensusTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

internal fun copyInviteToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Código Sensus", code)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Código $code copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

internal fun shareInviteLink(context: Context, code: String, inviteUrl: String) {
    val shareMessage =
        "Te invito a unirte a Sensus para ahorrar en ofertas locales cerca de ti. " +
            "Abre este enlace para descargar la app o agregarnos como amigos: $inviteUrl " +
            "(Código: $code)"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Invitar a un vecino a Sensus"))
    copyInviteToClipboard(context, code)
}
