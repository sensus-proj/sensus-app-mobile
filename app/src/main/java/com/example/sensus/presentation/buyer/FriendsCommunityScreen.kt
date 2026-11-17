package com.example.sensus.presentation.buyer

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.domain.model.ConnectedFriend
import com.example.sensus.domain.model.FriendInviteCodec
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.PendingFriendRequest
import com.example.sensus.domain.model.SentFriendRequest
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.FriendRequestNotifier
import com.example.sensus.presentation.common.QrScannerDialog
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private const val DEFAULT_PREVIEW_CODE = "SNS-VECINO"
private const val MIN_INVITE_LENGTH = 6
private const val TINT_ALPHA = 0.12f
private const val SOFT_ALPHA = 0.1f
private const val SHARE_BUTTON_WEIGHT = 1.3f
private const val BOTTOM_NAV_CLEARANCE_DP = 96
private const val FRIEND_POLL_INTERVAL_MS = 15_000L

@ComposeUiModel
private data class FriendRequestsSectionState(
    val pending: List<PendingFriendRequest> = emptyList(),
    val sent: List<SentFriendRequest> = emptyList(),
    val onAccept: (String) -> Unit = {},
    val onReject: (String) -> Unit = {}
)

@ComposeUiModel
private data class FriendsScreenCallbacks(
    val onCopyCode: () -> Unit,
    val onShareInvite: () -> Unit,
    val onShowQr: () -> Unit,
    val onOpenScanner: () -> Unit,
    val onShowAddFriend: () -> Unit,
    val onRemoveFriend: (String) -> Unit
)

@ComposeUiModel
private data class FriendRowItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val meta: String,
    val removableCode: String? = null
)

@ComposeUiModel
private data class FriendsDialogState(
    val inviteCode: String,
    val inviteUrl: String,
    val showQr: Boolean,
    val showAdd: Boolean,
    val showScanner: Boolean
)

@ComposeUiModel
private data class FriendsDialogActions(
    val onDismissQr: () -> Unit,
    val onDismissAdd: () -> Unit,
    val onDismissScanner: () -> Unit,
    val onOpenScanner: () -> Unit,
    val onSubmitInput: (String, String) -> Unit,
    val onCopyCode: () -> Unit,
    val onShareCode: () -> Unit
)

@Composable
internal fun FriendDeepLinkHandler(isGuest: Boolean, onRequireAuth: () -> Unit, onFriendLinked: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val friendsViewModel: FriendsViewModel = hiltViewModel()
    val friendsState by friendsViewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(friendsState.pendingInviteTrigger, isGuest) {
        friendsViewModel.dispatchIntent(FriendsViewIntent.ConsumePendingDeepLink)
    }

    LaunchedEffect(isGuest) {
        while (!isGuest) {
            friendsViewModel.dispatchIntent(FriendsViewIntent.Refresh)
            delay(FRIEND_POLL_INTERVAL_MS)
        }
    }

    DisposableEffect(lifecycleOwner, isGuest) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && !isGuest) {
                friendsViewModel.dispatchIntent(FriendsViewIntent.Refresh)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(friendsState.newlyArrivedRequest) {
        val incoming = friendsState.newlyArrivedRequest ?: return@LaunchedEffect
        FriendRequestNotifier.showPendingRequestNotification(context, incoming)
        friendsViewModel.dispatchIntent(FriendsViewIntent.NotificationShown)
    }

    LaunchedEffect(friendsViewModel) {
        friendsViewModel.viewEffect.collect { effect ->
            when (effect) {
                is FriendsViewEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    if (effect.openFriendsTab) onFriendLinked()
                }
                FriendsViewEffect.OpenFriendsTab -> onFriendLinked()
                FriendsViewEffect.RequireAuth -> onRequireAuth()
            }
        }
    }
}

@Composable
fun FriendsCommunityTab(onRequireAuth: () -> Unit, modifier: Modifier = Modifier, viewModel: FriendsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    var showQrDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }

    RequestFriendNotificationPermission(isGuest = state.isGuest) { viewModel.dispatchIntent(FriendsViewIntent.Refresh) }
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is FriendsViewEffect.ShowMessage -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                FriendsViewEffect.OpenFriendsTab -> Unit
                FriendsViewEffect.RequireAuth -> onRequireAuth()
            }
        }
    }

    val inviteCode = state.myInviteCode.ifBlank { DEFAULT_PREVIEW_CODE }
    val inviteUrl = state.myInviteUrl.ifBlank { FriendInviteCodec.buildInviteUrl(inviteCode, null) }
    val items = remember(state.friends) { state.friends.map { it.toRowItem() } }

    FriendsCommunityBody(
        friends = items,
        inviteCode = inviteCode,
        requestsState = FriendRequestsSectionState(
            pending = state.pendingRequests,
            sent = state.sentRequests,
            onAccept = { id -> viewModel.dispatchIntent(FriendsViewIntent.AcceptPendingRequest(id)) },
            onReject = { id -> viewModel.dispatchIntent(FriendsViewIntent.RejectPendingRequest(id)) }
        ),
        callbacks = FriendsScreenCallbacks(
            onCopyCode = { copyInviteToClipboard(context, inviteCode) },
            onShareInvite = { shareInviteLink(context, inviteCode, inviteUrl) },
            onShowQr = { showQrDialog = true },
            onOpenScanner = { showScannerDialog = true },
            onShowAddFriend = { showAddFriendDialog = true },
            onRemoveFriend = { code -> viewModel.dispatchIntent(FriendsViewIntent.RemoveFriend(code)) }
        ),
        modifier = modifier
    )

    FriendsOverlayDialogs(
        dialogState = FriendsDialogState(inviteCode, inviteUrl, showQrDialog, showAddFriendDialog, showScannerDialog),
        actions = FriendsDialogActions(
            onDismissQr = { showQrDialog = false },
            onDismissAdd = { showAddFriendDialog = false },
            onDismissScanner = { showScannerDialog = false },
            onOpenScanner = {
                showAddFriendDialog = false
                showScannerDialog = true
            },
            onSubmitInput = { raw, via -> viewModel.dispatchIntent(FriendsViewIntent.AddFromInput(raw, via)) },
            onCopyCode = { copyInviteToClipboard(context, inviteCode) },
            onShareCode = { shareInviteLink(context, inviteCode, inviteUrl) }
        )
    )
}

@Composable
private fun RequestFriendNotificationPermission(isGuest: Boolean, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(isGuest) {
        onRefresh()
        val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (!isGuest && needsPerm) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun FriendsCommunityScreen(friendsList: List<FriendShare> = emptyList(), modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val friends = remember(friendsList) { mutableStateListOf<FriendShare>().apply { addAll(friendsList) } }
    var showQrDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showScannerDialog by remember { mutableStateOf(false) }
    val inviteUrl = remember { FriendInviteCodec.buildInviteUrl(DEFAULT_PREVIEW_CODE, null) }

    val rowItems = friends.mapIndexed { idx, f ->
        FriendRowItem(
            id = "legacy_${idx}_${f.name}",
            name = f.name,
            subtitle = "Apartó ${f.activeOffersCount} ofertas locales • ${f.distance}",
            meta = f.timeAgo
        )
    }

    FriendsCommunityBody(
        friends = rowItems,
        inviteCode = DEFAULT_PREVIEW_CODE,
        callbacks = FriendsScreenCallbacks(
            onCopyCode = { copyInviteToClipboard(context, DEFAULT_PREVIEW_CODE) },
            onShareInvite = { shareInviteLink(context, DEFAULT_PREVIEW_CODE, inviteUrl) },
            onShowQr = { showQrDialog = true },
            onOpenScanner = { showScannerDialog = true },
            onShowAddFriend = { showAddFriendDialog = true },
            onRemoveFriend = {}
        ),
        modifier = modifier
    )

    FriendsOverlayDialogs(
        dialogState = FriendsDialogState(
            inviteCode = DEFAULT_PREVIEW_CODE,
            inviteUrl = inviteUrl,
            showQr = showQrDialog,
            showAdd = showAddFriendDialog,
            showScanner = showScannerDialog
        ),
        actions = FriendsDialogActions(
            onDismissQr = { showQrDialog = false },
            onDismissAdd = { showAddFriendDialog = false },
            onDismissScanner = { showScannerDialog = false },
            onOpenScanner = {
                showAddFriendDialog = false
                showScannerDialog = true
            },
            onSubmitInput = { raw, via ->
                val parsed = FriendInviteCodec.parseInvite(raw)
                if (parsed != null) {
                    val label = parsed.displayName ?: "Vecino (${parsed.inviteCode})"
                    friends.add(0, FriendShare(label, "Conectado por $via", 0, "Ahora"))
                    Toast.makeText(context, "$label agregado a tus amigos", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Código o QR inválido", Toast.LENGTH_SHORT).show()
                }
            },
            onCopyCode = { copyInviteToClipboard(context, DEFAULT_PREVIEW_CODE) },
            onShareCode = { shareInviteLink(context, DEFAULT_PREVIEW_CODE, inviteUrl) }
        )
    )
}

@Composable
private fun FriendsOverlayDialogs(dialogState: FriendsDialogState, actions: FriendsDialogActions) {
    if (dialogState.showQr) {
        QrInviteDialog(
            inviteCode = dialogState.inviteCode,
            qrPayload = dialogState.inviteUrl,
            onDismiss = actions.onDismissQr,
            onCopyCode = actions.onCopyCode,
            onShareCode = {
                actions.onDismissQr()
                actions.onShareCode()
            }
        )
    }

    if (dialogState.showAdd) {
        AddFriendDialog(
            onDismiss = actions.onDismissAdd,
            onOpenScanner = actions.onOpenScanner,
            onAddCode = { raw ->
                actions.onSubmitInput(raw, "Código")
                actions.onDismissAdd()
            }
        )
    }

    if (dialogState.showScanner) {
        QrScannerDialog(
            onQrDecoded = { payload ->
                actions.onDismissScanner()
                actions.onSubmitInput(payload, "QR")
            },
            onDismiss = actions.onDismissScanner
        )
    }
}

private fun copyInviteToClipboard(context: Context, code: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Código Sensus", code)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Código $code copiado al portapapeles", Toast.LENGTH_SHORT).show()
}

private fun shareInviteLink(context: Context, code: String, inviteUrl: String) {
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

@Composable
private fun FriendsCommunityBody(
    friends: List<FriendRowItem>,
    inviteCode: String,
    callbacks: FriendsScreenCallbacks,
    modifier: Modifier = Modifier,
    requestsState: FriendRequestsSectionState = FriendRequestsSectionState()
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = BOTTOM_NAV_CLEARANCE_DP.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { CommunityHeader() }
        item { CommunityInviteCard(inviteCode = inviteCode, callbacks = callbacks) }
        item {
            PendingRequestsSectionCard(
                pendingRequests = requestsState.pending,
                onAccept = requestsState.onAccept,
                onReject = requestsState.onReject
            )
        }
        item { SentRequestsSummaryCard(sentRequests = requestsState.sent) }
        item { CommunityActivityHeader(friendsCount = friends.size) }
        if (friends.isEmpty()) {
            item { EmptyFriendsCard(onOpenScanner = callbacks.onOpenScanner) }
        } else {
            items(friends, key = { it.id }) { friend ->
                FriendShareCard(
                    friend = friend,
                    onRemove = friend.removableCode?.let { code -> { callbacks.onRemoveFriend(code) } }
                )
            }
        }
    }
}

@Composable
private fun CommunityHeader() {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SensusPurple.copy(alpha = TINT_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Amigos y QR", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Conecta con tus vecinos escaneando su QR o compartiendo tu enlace de invitación",
            fontSize = 13.sp,
            color = SensusTextSecondary
        )
    }
}

@Composable
private fun CommunityInviteCard(inviteCode: String, callbacks: FriendsScreenCallbacks) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InviteCardTitle()
            InviteCodePill(inviteCode = inviteCode, onCopyCode = callbacks.onCopyCode)
            InviteCardActions(onShareCode = callbacks.onShareInvite, onShowQr = callbacks.onShowQr)
            OutlinedButton(
                onClick = callbacks.onOpenScanner,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SensusPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Escanear QR de un amigo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
            }
            TextButton(onClick = callbacks.onShowAddFriend, modifier = Modifier.fillMaxWidth()) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Agregar con código o enlace",
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SensusPurple.copy(alpha = TINT_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.GroupAdd, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Crece tu red vecinal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = "Comparte tu invitación o muestra tu código QR", fontSize = 11.sp, color = SensusTextSecondary)
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Tu código de invitación", fontSize = 10.sp, color = SensusTextSecondary)
                Text(
                    text = inviteCode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple,
                    letterSpacing = 1.sp
                )
            }
            IconButton(onClick = onCopyCode, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar código", tint = SensusPurple, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun InviteCardActions(onShareCode: () -> Unit, onShowQr: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onShareCode,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.weight(SHARE_BUTTON_WEIGHT),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Compartir invitación", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        OutlinedButton(
            onClick = onShowQr,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SensusPurple),
            border = BorderStroke(1.dp, SensusPurple),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Mi QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        Text(text = "Tus amigos en Sensus", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Surface(shape = RoundedCornerShape(6.dp), color = SensusPurple.copy(alpha = SOFT_ALPHA)) {
            Text(
                text = "$friendsCount conectados",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusPurple,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun EmptyFriendsCard(onOpenScanner: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(32.dp))
            Text(text = "Aún no tienes vecinos conectados", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                text = "Escanea el QR de un amigo o compártele tu enlace de invitación para conectarse aquí.",
                fontSize = 12.sp,
                color = SensusTextSecondary
            )
            TextButton(onClick = onOpenScanner) {
                Text(text = "Escanear primer QR", color = SensusPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FriendShareCard(friend: FriendRowItem, onRemove: (() -> Unit)? = null) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SensusPurple.copy(alpha = SOFT_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = friend.subtitle,
                    fontSize = 12.sp,
                    color = SensusTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = friend.meta, fontSize = 11.sp, color = SensusTextMuted)
                if (onRemove != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar amigo",
                            tint = SensusTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendDialog(onDismiss: () -> Unit, onOpenScanner: () -> Unit, onAddCode: (String) -> Unit) {
    var friendInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Agregar a un vecino o amigo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary) },
        text = {
            AddFriendDialogBody(
                friendInput = friendInput,
                inputError = inputError,
                onInputChanged = { next ->
                    friendInput = next
                    if (inputError != null) inputError = null
                },
                onOpenScanner = onOpenScanner
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = friendInput.trim()
                    if (FriendInviteCodec.parseInvite(clean) == null && clean.length < MIN_INVITE_LENGTH) {
                        inputError = "Ingresa un código válido o enlace de invitación"
                    } else {
                        onAddCode(clean)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
            ) {
                Text("Agregar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = SensusTextSecondary) } }
    )
}

@Composable
private fun AddFriendDialogBody(friendInput: String, inputError: String?, onInputChanged: (String) -> Unit, onOpenScanner: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = "Pega el enlace o código de invitación, o escanea su código QR:", fontSize = 12.sp, color = SensusTextSecondary)
        OutlinedTextField(
            value = friendInput,
            onValueChange = onInputChanged,
            label = { Text("Código o enlace") },
            placeholder = { Text("Ej. SNS-A4F9C2") },
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
            onClick = onOpenScanner,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple.copy(alpha = TINT_ALPHA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Abrir cámara para escanear QR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
        }
    }
}

private fun ConnectedFriend.toRowItem(): FriendRowItem {
    val formatter = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-MX"))
    val dateLabel = formatter.format(Date(addedAtEpochMs))
    return FriendRowItem(
        id = inviteCode,
        name = displayName,
        subtitle = "Código: $inviteCode • Vía $addedVia",
        meta = dateLabel,
        removableCode = inviteCode
    )
}
