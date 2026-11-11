package com.example.sensus.presentation.buyer

import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import kotlinx.coroutines.launch

private const val AVATAR_BG_ALPHA = 0.15f
private const val GUEST_BADGE_ALPHA = 0.12f
private const val EDIT_BTN_ALPHA = 0.1f
private const val MERCHANT_CARD_ALPHA = 0.08f
private const val SWITCH_UNCHECKED_HEX = 0xFFCBD5E1
private const val LOCKED_BADGE_HEX = 0xFFF1F5F9

@ComposeUiModel
data class BuyerProfileCallbacks(
    val isMerchantMode: Boolean = false,
    val onToggleMerchantMode: (Boolean) -> Unit = {},
    val onLogoutClick: () -> Unit = {},
    val onDeleteAccountClick: () -> Unit = {},
    val onTermsClick: () -> Unit = {},
    val onHistoryClick: () -> Unit = {},
    val onGuestActionRequired: () -> Unit = {}
)

@ComposeUiModel
private data class BuyerProfileListUi(
    val summary: ProfileDisplaySummary,
    val isGuest: Boolean,
    val profileUpdatedNotice: Boolean
)

@Composable
fun BuyerProfileScreen(viewModel: BuyerProfileViewModel = hiltViewModel(), callbacks: BuyerProfileCallbacks, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.viewState.collectAsState()
    val isGuest = state.isGuest

    var summary by remember { mutableStateOf(ProfileDisplaySummary()) }
    var isSaving by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var profileUpdatedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.dispatchIntent(BuyerProfileViewIntent.LoadProfile) }
    LaunchedEffect(state.profile, isGuest, state.isLoading) {
        summary = resolveProfileDisplay(state.profile, isGuest, state.isLoading)
    }

    BuyerProfileListContent(
        ui = BuyerProfileListUi(summary, isGuest, profileUpdatedNotice),
        callbacks = callbacks,
        onOpenEditSheet = { showEditProfileSheet = true },
        modifier = modifier
    )

    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            currentSummary = summary,
            isSaving = isSaving,
            onDismiss = { showEditProfileSheet = false },
            onSave = { trimmedName, tempPhotoUri, onError ->
                scope.launch {
                    isSaving = true
                    val error = persistProfileChanges(context, viewModel, trimmedName, tempPhotoUri) { savedName, savedUri ->
                        summary = summary.copy(name = savedName, photoUri = savedUri)
                        showEditProfileSheet = false
                        profileUpdatedNotice = true
                    }
                    isSaving = false
                    if (error != null) onError(error)
                }
            }
        )
    }
}

@Composable
private fun BuyerProfileListContent(ui: BuyerProfileListUi, callbacks: BuyerProfileCallbacks, onOpenEditSheet: () -> Unit, modifier: Modifier = Modifier) {
    val isGuest = ui.isGuest
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (ui.profileUpdatedNotice) item { ProfileUpdatedSuccessBanner() }
        item {
            BuyerProfileHeaderCard(
                summary = ui.summary,
                isGuest = isGuest,
                onClick = { if (isGuest) callbacks.onGuestActionRequired() else onOpenEditSheet() }
            )
        }
        item {
            MerchantModeToggleCard(
                isGuest = isGuest,
                isMerchantMode = callbacks.isMerchantMode,
                onToggle = { enable -> if (isGuest) callbacks.onGuestActionRequired() else callbacks.onToggleMerchantMode(enable) }
            )
        }
        item {
            BuyerSettingsOptionsCard(
                isGuest = isGuest,
                onEditClick = { if (isGuest) callbacks.onGuestActionRequired() else onOpenEditSheet() },
                onHistoryClick = { if (isGuest) callbacks.onGuestActionRequired() else callbacks.onHistoryClick() },
                onTermsClick = callbacks.onTermsClick
            )
        }
        item {
            BuyerAccountActionsCard(
                isGuest = isGuest,
                onLogoutClick = callbacks.onLogoutClick,
                onDeleteAccountClick = { if (isGuest) callbacks.onGuestActionRequired() else callbacks.onDeleteAccountClick() }
            )
        }
    }
}

@Composable
private fun ProfileUpdatedSuccessBanner() {
    Surface(shape = RoundedCornerShape(12.dp), color = SensusSuccessBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Perfil actualizado exitosamente", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusSuccess)
        }
    }
}

@Composable
private fun BuyerProfileHeaderCard(summary: ProfileDisplaySummary, isGuest: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatarCircle(photoUri = summary.photoUri)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = summary.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = summary.email, fontSize = 13.sp, color = SensusTextSecondary)
            }
            ProfileHeaderActionControl(isGuest = isGuest, onClick = onClick)
        }
    }
}

@Composable
private fun ProfileAvatarCircle(photoUri: Uri?) {
    Box(
        modifier = Modifier.size(64.dp).clip(CircleShape).background(SensusPurple.copy(alpha = AVATAR_BG_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp).clip(CircleShape)
            )
        } else {
            Icon(imageVector = Icons.Default.Person, contentDescription = "Avatar de usuario", tint = SensusPurple, modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun ProfileHeaderActionControl(isGuest: Boolean, onClick: () -> Unit) {
    if (isGuest) {
        Surface(shape = RoundedCornerShape(8.dp), color = SensusPurple.copy(alpha = GUEST_BADGE_ALPHA), modifier = Modifier.clickable(onClick = onClick)) {
            Text(
                text = "Ingresar",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SensusPurple,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp).clip(CircleShape).background(SensusPurple.copy(alpha = EDIT_BTN_ALPHA))) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar perfil", tint = SensusPurple, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MerchantModeToggleCard(isGuest: Boolean, isMerchantMode: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusPurple.copy(alpha = MERCHANT_CARD_ALPHA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(SensusPurple), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "Soy Comercio / Vendedor", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
                    Text(text = "Publica excedentes y recupera dinero", fontSize = 11.sp, color = SensusTextSecondary)
                }
            }
            Switch(
                checked = !isGuest && isMerchantMode,
                enabled = !isGuest,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SensusPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(SWITCH_UNCHECKED_HEX)
                )
            )
        }
    }
}

@Composable
private fun BuyerSettingsOptionsCard(isGuest: Boolean, onEditClick: () -> Unit, onHistoryClick: () -> Unit, onTermsClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ProfileMenuOption(icon = Icons.Default.Edit, title = "Editar información personal", isLocked = isGuest, onClick = onEditClick)
            ProfileMenuOption(icon = Icons.Default.History, title = "Mis apartados y pedidos", isLocked = isGuest, onClick = onHistoryClick)
            ProfileMenuOption(icon = Icons.Default.NotificationsNone, title = "Preferencias de notificaciones", onClick = {})
            ProfileMenuOption(icon = Icons.AutoMirrored.Filled.HelpOutline, title = "Centro de ayuda y soporte", onClick = {})
            ProfileMenuOption(icon = Icons.Default.Description, title = "Términos y condiciones legales", onClick = onTermsClick)
        }
    }
}

@Composable
private fun BuyerAccountActionsCard(isGuest: Boolean, onLogoutClick: () -> Unit, onDeleteAccountClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ProfileMenuOption(icon = Icons.AutoMirrored.Filled.Logout, title = "Cerrar sesión", tintColor = SensusRed, onClick = onLogoutClick)
            ProfileMenuOption(
                icon = Icons.Default.DeleteOutline,
                title = "Eliminar cuenta",
                tintColor = SensusRed,
                isLocked = isGuest,
                onClick = onDeleteAccountClick
            )
        }
    }
}

@Composable
private fun ProfileMenuOption(icon: ImageVector, title: String, tintColor: Color = SensusTextPrimary, isLocked: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isLocked) SensusTextMuted else if (tintColor == SensusTextPrimary) SensusPurple else tintColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isLocked) SensusTextMuted else tintColor)
        }
        if (isLocked) {
            Surface(shape = RoundedCornerShape(6.dp), color = Color(LOCKED_BADGE_HEX), modifier = Modifier.padding(end = 4.dp)) {
                Text(
                    text = "Requiere cuenta",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusTextMuted,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = SensusTextMuted, modifier = Modifier.size(14.dp))
    }
}

private fun resolveProfileDisplay(profile: UserProfile?, isGuest: Boolean, isLoading: Boolean): ProfileDisplaySummary = when {
    isGuest -> ProfileDisplaySummary("Invitado Sensus", "Modo Invitado (Radio máx. 2 km)", null)
    profile != null -> ProfileDisplaySummary(
        name = profile.nombreCompleto.ifBlank { "Usuario Sensus" },
        email = profile.correo.ifBlank { "" },
        photoUri = profile.fotoPerfilUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
    )
    isLoading -> ProfileDisplaySummary("Cargando perfil...", "Verificando sesión activa", null)
    else -> ProfileDisplaySummary("Sesión no iniciada", "Inicia sesión para ver tu cuenta", null)
}
