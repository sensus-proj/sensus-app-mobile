package com.example.sensus.presentation.buyer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val DEFAULT_INVITE_CODE = "SENSUS-8492"
private const val MIN_INVITE_LENGTH = 6
private const val TINT_ALPHA = 0.12f
private const val SOFT_ALPHA = 0.1f
private const val SHARE_BUTTON_WEIGHT = 1.3f
private const val BOTTOM_NAV_CLEARANCE_DP = 96

@ComposeUiModel
private data class CommunityCallbacks(
    val onCopyCode: () -> Unit,
    val onShareCode: () -> Unit,
    val onShowQr: () -> Unit,
    val onShowAddFriend: () -> Unit
)

@Composable
fun FriendsCommunityScreen(friendsList: List<FriendShare>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val friends = remember(friendsList) { mutableStateListOf<FriendShare>().apply { addAll(friendsList) } }
    var showQrDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }

    CommunityContent(
        friends = friends,
        inviteCode = DEFAULT_INVITE_CODE,
        modifier = modifier,
        callbacks = CommunityCallbacks(
            onCopyCode = { copyInviteToClipboard(context, DEFAULT_INVITE_CODE) },
            onShareCode = { shareInviteCode(context, DEFAULT_INVITE_CODE) },
            onShowQr = { showQrDialog = true },
            onShowAddFriend = { showAddFriendDialog = true }
        )
    )

    if (showQrDialog) {
        QrInviteDialog(
            inviteCode = DEFAULT_INVITE_CODE,
            onDismiss = { showQrDialog = false },
            onCopyCode = { copyInviteToClipboard(context, DEFAULT_INVITE_CODE) },
            onShareCode = {
                showQrDialog = false
                shareInviteCode(context, DEFAULT_INVITE_CODE)
            }
        )
    }

    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onFriendAdded = { friend, message ->
                friends.add(0, friend)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                showAddFriendDialog = false
            }
        )
    }
}

private fun copyInviteToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Código Sensus", code)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Código $code copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun shareInviteCode(context: Context, code: String) {
    val shareMessage =
        "Te invito a unirte a Sensus para ahorrar en ofertas locales cerca de ti. " +
            "Ingresa mi código de vecino: $code o conoce más en https://sensus.app/invitacion"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Invitar a un vecino a Sensus"))
    copyInviteToClipboard(context, code)
}

@Composable
private fun CommunityContent(friends: List<FriendShare>, inviteCode: String, modifier: Modifier, callbacks: CommunityCallbacks) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = BOTTOM_NAV_CLEARANCE_DP.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { CommunityHeader() }
        item {
            CommunityInviteCard(
                inviteCode = inviteCode,
                onCopyCode = callbacks.onCopyCode,
                onShareCode = callbacks.onShareCode,
                onShowQr = callbacks.onShowQr,
                onShowAddFriend = callbacks.onShowAddFriend
            )
        }
        item { CommunityActivityHeader(friendsCount = friends.size) }
        items(friends) { friend -> FriendShareCard(friend = friend) }
    }
}

@Composable
private fun CommunityHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SensusPurple.copy(alpha = TINT_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    tint = SensusPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Comunidad Sensus",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ahorros y compras recientes de vecinos cercanos",
            fontSize = 13.sp,
            color = SensusTextSecondary
        )
    }
}

@Composable
private fun CommunityInviteCard(inviteCode: String, onCopyCode: () -> Unit, onShareCode: () -> Unit, onShowQr: () -> Unit, onShowAddFriend: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InviteCardTitle()
            InviteCodePill(inviteCode = inviteCode, onCopyCode = onCopyCode)
            InviteCardActions(onShareCode = onShareCode, onShowQr = onShowQr)
            TextButton(onClick = onShowAddFriend, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = SensusPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Agregar a un vecino con su código o QR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusPurple
                )
            }
        }
    }
}

@Composable
private fun InviteCardTitle() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SensusPurple.copy(alpha = TINT_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = null,
                tint = SensusPurple,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Crece tu red vecinal",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = "Comparte tu código de invitación o muestra tu QR",
                fontSize = 11.sp,
                color = SensusTextSecondary
            )
        }
    }
}

@Composable
private fun InviteCodePill(inviteCode: String, onCopyCode: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SensusBackground,
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tu código de invitado",
                    fontSize = 10.sp,
                    color = SensusTextSecondary
                )
                Text(
                    text = inviteCode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple,
                    letterSpacing = 1.sp
                )
            }
            IconButton(
                onClick = onCopyCode,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar código",
                    tint = SensusPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun InviteCardActions(onShareCode: () -> Unit, onShowQr: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onShareCode,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.weight(SHARE_BUTTON_WEIGHT),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Compartir invitación",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        OutlinedButton(
            onClick = onShowQr,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SensusPurple),
            border = BorderStroke(1.dp, SensusPurple),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = SensusPurple,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Mi QR",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CommunityActivityHeader(friendsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Actividad reciente en tu zona",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SensusPurple.copy(alpha = SOFT_ALPHA)
        ) {
            Text(
                text = "$friendsCount vecinos conectados",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusPurple,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun FriendShareCard(friend: FriendShare) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SensusPurple.copy(alpha = SOFT_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SensusPurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary
                )
                Text(
                    text = "Apartó ${friend.activeOffersCount} ofertas locales • ${friend.distance}",
                    fontSize = 12.sp,
                    color = SensusTextSecondary
                )
            }
            Text(
                text = friend.timeAgo,
                fontSize = 11.sp,
                color = SensusTextMuted
            )
        }
    }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onFriendAdded: (FriendShare, String) -> Unit) {
    var friendInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agregar a un vecino o amigo",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
        },
        text = {
            AddFriendDialogBody(
                friendInput = friendInput,
                inputError = inputError,
                onInputChanged = { next ->
                    friendInput = next.uppercase()
                    if (inputError != null) inputError = null
                },
                onScanDemoFriend = {
                    val selectedName = listOf("Mariana Flores", "Roberto Vega", "Alejandra Ruiz", "Diego Morales").random()
                    onFriendAdded(
                        FriendShare(selectedName, "250 m • Conectado por QR", 2, "Recién agregado"),
                        "$selectedName escaneado y agregado con éxito"
                    )
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = friendInput.trim()
                    if (clean.length < MIN_INVITE_LENGTH) {
                        inputError = "Ingresa un código válido de al menos $MIN_INVITE_LENGTH caracteres"
                    } else {
                        onFriendAdded(
                            FriendShare("Vecino ($clean)", "Cerca de ti • En línea", 1, "Ahora"),
                            "$clean agregado a tu red comunitaria"
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
            ) {
                Text("Agregar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = SensusTextSecondary)
            }
        }
    )
}

@Composable
private fun AddFriendDialogBody(friendInput: String, inputError: String?, onInputChanged: (String) -> Unit, onScanDemoFriend: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Escribe el código de invitación o escanea su código QR:",
            fontSize = 12.sp,
            color = SensusTextSecondary
        )
        OutlinedTextField(
            value = friendInput,
            onValueChange = onInputChanged,
            label = { Text("Código de invitación") },
            placeholder = { Text("Ej. SENSUS-1234") },
            isError = inputError != null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = sensusOutlinedTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (inputError != null) {
            Text(text = inputError, color = SensusRed, fontSize = 11.sp)
        }
        Button(
            onClick = onScanDemoFriend,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple.copy(alpha = TINT_ALPHA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = SensusPurple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Escanear QR de un amigo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SensusPurple
            )
        }
    }
}
