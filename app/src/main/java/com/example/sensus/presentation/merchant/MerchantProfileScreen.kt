package com.example.sensus.presentation.merchant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.common.AppearanceSettings
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.StatusGreen
import kotlinx.coroutines.launch

private const val STORE_AVATAR_ALPHA = 0.12f
private const val EDIT_BTN_ALPHA = 0.1f
private const val ERROR_BG_ALPHA = 0.1f
private const val MIN_ADDR_LEN = 5
private const val MAX_ADDR_LEN = 250
private const val POSTAL_LEN = 5

@ComposeUiModel
private data class MerchantEditDraft(
    val name: String,
    val address: String,
    val postalCode: String,
    val schedules: List<HorarioDia>,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@ComposeUiModel
private data class MerchantProfileDisplayState(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePostalCode: String = "",
    val storeScheduleText: String = "",
    val schedules: List<HorarioDia> = defaultScheduleList(),
    val isOpen: Boolean = true,
    val updateSuccessMessage: String? = null,
    val storeLatitude: Double? = null,
    val storeLongitude: Double? = null,
    val isUpdatingPhoto: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val photoErrorMessage: String? = null
)

@ComposeUiModel
private data class MerchantProfileActions(
    val onToggleOpen: (Boolean) -> Unit,
    val onPhotoClick: () -> Unit,
    val onEditClick: () -> Unit,
    val onSwitchToBuyer: () -> Unit
)

@ComposeUiModel
private data class MerchantEditSheetActions(
    val onDraftChange: (MerchantEditDraft) -> Unit,
    val onPickLocationClick: () -> Unit,
    val onSaveClick: () -> Unit
)

private enum class MerchantProfileSheet {
    NONE,
    FACADE_PICKER,
    EDIT
}

@Composable
fun MerchantProfileScreen(viewModel: MerchantProfileViewModel = hiltViewModel(), onSwitchToBuyer: () -> Unit, modifier: Modifier = Modifier) {
    val profileState by viewModel.viewState.collectAsState()
    val store = profileState.store
    var activeSheet by remember { mutableStateOf(MerchantProfileSheet.NONE) }
    var display by remember { mutableStateOf(buildMerchantDisplayState(store, profileState.schedules, MerchantProfileDisplayState())) }

    LaunchedEffect(Unit) { viewModel.dispatchIntent(MerchantProfileViewIntent.LoadMerchant) }
    LaunchedEffect(store, profileState.schedules) {
        display = buildMerchantDisplayState(store, profileState.schedules, display)
    }

    MerchantProfileListContent(
        display = display.copy(
            isOpen = store.isOpen,
            updateSuccessMessage = profileState.statusMessage ?: display.updateSuccessMessage,
            isUpdatingPhoto = profileState.isUpdatingPhoto,
            isUpdatingStatus = profileState.isUpdatingStatus,
            photoErrorMessage = profileState.errorMessage
        ),
        store = store,
        actions = MerchantProfileActions(
            onToggleOpen = { viewModel.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(it)) },
            onPhotoClick = { activeSheet = MerchantProfileSheet.FACADE_PICKER },
            onEditClick = { activeSheet = MerchantProfileSheet.EDIT },
            onSwitchToBuyer = onSwitchToBuyer
        ),
        modifier = modifier
    )

    MerchantProfileSheets(
        viewModel = viewModel,
        display = display,
        activeSheet = activeSheet,
        onDismiss = { activeSheet = MerchantProfileSheet.NONE },
        onDisplayUpdated = { display = it }
    )
}

@Composable
private fun MerchantProfileSheets(
    viewModel: MerchantProfileViewModel,
    display: MerchantProfileDisplayState,
    activeSheet: MerchantProfileSheet,
    onDismiss: () -> Unit,
    onDisplayUpdated: (MerchantProfileDisplayState) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    val launchCamera = rememberSafeMerchantCameraLauncher {
        onDismiss()
        viewModel.dispatchIntent(MerchantProfileViewIntent.UpdateFacadePhoto(encodeMerchantCameraBitmap(it)))
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onDismiss()
            scope.launch {
                val bytes = readMerchantImageBytes(context, uri)
                if (bytes != null) viewModel.dispatchIntent(MerchantProfileViewIntent.UpdateFacadePhoto(bytes))
            }
        }
    }

    when (activeSheet) {
        MerchantProfileSheet.NONE -> Unit
        MerchantProfileSheet.FACADE_PICKER -> PhotoPickerBottomSheet(
            title = "Foto de la fachada de tu tienda",
            onDismiss = onDismiss,
            onCameraClick = launchCamera,
            onGalleryClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
        MerchantProfileSheet.EDIT -> {
            val draft = MerchantEditDraft(
                display.storeName,
                display.storeAddress,
                display.storePostalCode,
                display.schedules,
                display.storeLatitude,
                display.storeLongitude
            )
            MerchantEditBottomSheet(
                initialDraft = draft,
                isSaving = isSaving,
                onDismiss = onDismiss,
                onSave = { edited, onError ->
                    scope.launch {
                        isSaving = true
                        saveMerchantEdit(viewModel, edited, onError) { updatedDisplay ->
                            onDisplayUpdated(updatedDisplay.copy(isOpen = display.isOpen))
                            onDismiss()
                        }
                        isSaving = false
                    }
                }
            )
        }
    }
}

private fun buildMerchantDisplayState(store: MerchantStore, schedules: List<HorarioDia>, current: MerchantProfileDisplayState): MerchantProfileDisplayState =
    current.copy(
        storeName = store.name.ifBlank { current.storeName },
        storeAddress = store.address.removePrefix("Dirección: ").trim().ifBlank { current.storeAddress },
        storePostalCode = store.postalCode.ifBlank { current.storePostalCode.ifBlank { "42800" } },
        storeScheduleText = store.schedule.removePrefix("Horarios: ").trim().ifBlank { current.storeScheduleText },
        schedules = schedules.ifEmpty { current.schedules },
        isOpen = store.isOpen,
        storeLatitude = store.latitude.takeIf { it != 0.0 } ?: current.storeLatitude,
        storeLongitude = store.longitude.takeIf { it != 0.0 } ?: current.storeLongitude
    )

@Composable
private fun MerchantProfileListContent(
    display: MerchantProfileDisplayState,
    store: MerchantStore,
    actions: MerchantProfileActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        display.updateSuccessMessage?.let { msg -> item { ProfileSuccessBanner(message = msg) } }
        display.photoErrorMessage?.let { err ->
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = SensusRed.copy(alpha = ERROR_BG_ALPHA), modifier = Modifier.fillMaxWidth()) {
                    Text(text = err, fontSize = 13.sp, color = SensusRed, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp))
                }
            }
        }
        item {
            MerchantProfileHeaderCard(
                storeName = display.storeName,
                store = store,
                isUpdatingPhoto = display.isUpdatingPhoto,
                onPhotoClick = actions.onPhotoClick,
                onEditClick = actions.onEditClick
            )
        }
        item { MerchantOpenToggleCard(isOpen = display.isOpen, isUpdating = display.isUpdatingStatus, onToggle = actions.onToggleOpen) }
        item {
            MerchantDetailsCard(
                storeAddress = display.storeAddress,
                storePostalCode = display.storePostalCode,
                storeScheduleText = display.storeScheduleText,
                onEditClick = actions.onEditClick
            )
        }
        item { AppearanceSettings() }
        item { SwitchToBuyerModeButton(onClick = actions.onSwitchToBuyer) }
    }
}

private suspend fun saveMerchantEdit(
    viewModel: MerchantProfileViewModel,
    draft: MerchantEditDraft,
    onError: (String) -> Unit,
    onSaved: (MerchantProfileDisplayState) -> Unit
) {
    val coords = draft.latitude?.let { lat -> draft.longitude?.let { lng -> MerchantCoordinates(lat, lng) } }
    when (
        val result = viewModel.updateMerchantProfileUseCase(
            nombreComercial = draft.name.trim(),
            direccion = draft.address.trim(),
            codigoPostal = draft.postalCode,
            horarios = draft.schedules,
            coordinates = coords
        )
    ) {
        is Result.Success -> {
            val profile = result.data
            onSaved(
                MerchantProfileDisplayState(
                    storeName = profile.nombreComercial,
                    storeAddress = profile.direccion,
                    storePostalCode = profile.codigoPostal,
                    schedules = profile.horarios,
                    storeScheduleText = "Lun-Vie: 08:00 - 20:00 / Sáb: 09:00 - 18:00",
                    updateSuccessMessage = "¡Datos del local actualizados exitosamente! Coordenadas espaciales actualizadas.",
                    isOpen = profile.activo,
                    storeLatitude = profile.latitud,
                    storeLongitude = profile.longitud
                )
            )
        }
        is Result.Failure -> onError(result.error.message)
    }
}

@Composable
private fun ProfileSuccessBanner(message: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = SensusSuccessBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = message, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        }
    }
}

@Composable
private fun MerchantProfileHeaderCard(storeName: String, store: MerchantStore, isUpdatingPhoto: Boolean, onPhotoClick: () -> Unit, onEditClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            StoreProfileFacadeAvatar(
                photoUrl = store.photoUrl,
                isUpdating = isUpdatingPhoto,
                onClick = onPhotoClick
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = storeName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(text = store.category, fontSize = 13.sp, color = SensusTextSecondary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Calificación: ${store.rating} (${store.reviewCount} opiniones)",
                    fontSize = 11.sp,
                    color = SensusVioletInk,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(36.dp).clip(CircleShape).background(SensusPurple.copy(alpha = EDIT_BTN_ALPHA))
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar datos del local", tint = SensusVioletInk, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StoreProfileFacadeAvatar(photoUrl: String?, isUpdating: Boolean, onClick: () -> Unit) {
    val cleanUrl = photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { raw ->
        if (raw.startsWith("http://", ignoreCase = true)) "https://${raw.drop("http://".length)}" else raw
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SensusPurple.copy(alpha = STORE_AVATAR_ALPHA))
            .clickable(enabled = !isUpdating, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (cleanUrl != null) {
            AsyncImage(
                model = cleanUrl,
                contentDescription = "Foto de fachada del comercio",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
            )
        } else {
            Icon(imageVector = Icons.Default.Storefront, contentDescription = "Cambiar foto de tienda", tint = SensusVioletInk, modifier = Modifier.size(32.dp))
        }
        if (isUpdating) {
            CircularProgressIndicator(color = SensusPurple, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(11.dp))
                }
            }
        }
    }
}

@Composable
private fun MerchantOpenToggleCard(isOpen: Boolean, isUpdating: Boolean, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SensusSurface), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOpen) "Tienda Abierta" else "Tienda Cerrada",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOpen) SensusSuccess else SensusRed
                )
                Text(
                    text = if (isOpen) "Visible en el mapa y aceptando apartados" else "Oculta del mapa · No se recibirán apartados",
                    fontSize = 11.sp,
                    color = SensusTextSecondary
                )
            }
            Switch(
                checked = isOpen,
                enabled = !isUpdating,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = StatusGreen)
            )
        }
    }
}

@Composable
private fun MerchantDetailsCard(storeAddress: String, storePostalCode: String, storeScheduleText: String, onEditClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SensusSurface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Información del establecimiento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(
                    text = "Modificar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusVioletInk,
                    modifier = Modifier.clickable(onClick = onEditClick)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = storeAddress, fontSize = 13.sp, color = SensusTextPrimary)
                    Text(text = "C.P. $storePostalCode (Coordenadas sincronizadas)", fontSize = 11.sp, color = SensusTextSecondary)
                }
            }
            ProfileSimpleInfoRow(icon = Icons.Default.AccessTime, text = storeScheduleText)
        }
    }
}

@Composable
private fun ProfileSimpleInfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = SensusTextSecondary)
    }
}

@Composable
private fun SwitchToBuyerModeButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Regresar al modo comprador", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MerchantEditBottomSheet(
    initialDraft: MerchantEditDraft,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (MerchantEditDraft, (String) -> Unit) -> Unit
) {
    var draft by remember { mutableStateOf(initialDraft) }
    var editError by remember { mutableStateOf<String?>(null) }
    var showPinPicker by remember { mutableStateOf(false) }
    var pickerRecenterSignal by remember { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SensusSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        MerchantEditSheetLazyBody(
            draft = draft,
            editError = editError,
            isSaving = isSaving,
            actions = MerchantEditSheetActions(
                onDraftChange = {
                    draft = it
                    editError = null
                },
                onPickLocationClick = { showPinPicker = true },
                onSaveClick = {
                    val err = validateMerchantDraft(draft)
                    if (err != null) editError = err else onSave(draft) { serverErr -> editError = serverErr }
                }
            )
        )
    }

    if (showPinPicker) {
        val baseLoc = UserLocation(draft.latitude ?: UserLocation().latitude, draft.longitude ?: UserLocation().longitude)
        val pickerState = MerchantRegisterViewState(
            userLocation = baseLoc,
            pickerLatitude = baseLoc.latitude,
            pickerLongitude = baseLoc.longitude,
            selectedLatitude = baseLoc.latitude,
            selectedLongitude = baseLoc.longitude,
            recenterSignal = pickerRecenterSignal,
            resolvedAddressPreview = draft.address.ifBlank { null },
            resolvedPostalPreview = draft.postalCode.ifBlank { null }
        )
        MerchantLocationPickerModal(
            state = pickerState,
            onDismiss = { showPinPicker = false },
            onRecenterGps = { pickerRecenterSignal++ },
            onPinMoved = { lat, lng -> draft = draft.copy(latitude = lat, longitude = lng) },
            onConfirm = { lat, lng ->
                draft = draft.copy(latitude = lat, longitude = lng)
                showPinPicker = false
            }
        )
    }
}

@Composable
private fun MerchantEditSheetLazyBody(draft: MerchantEditDraft, editError: String?, isSaving: Boolean, actions: MerchantEditSheetActions) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(text = "Editar datos del local comercial", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(
                    text = "Al modificar la dirección física se recalcularán automáticamente las coordenadas espaciales.",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
            }
        }
        item { EditSheetField("Nombre comercial", draft.name, "Nombre de la tienda") { actions.onDraftChange(draft.copy(name = it)) } }
        item {
            PeekingCatLocationButton(
                isResolving = false,
                hasCoordinates = draft.latitude != null && draft.longitude != null,
                onClick = actions.onPickLocationClick
            )
        }
        item { EditSheetField("Dirección física del local", draft.address, "Calle, número y colonia") { actions.onDraftChange(draft.copy(address = it)) } }
        item {
            EditSheetField("Código postal (5 dígitos)", draft.postalCode, "06000") { input ->
                if (input.length <= POSTAL_LEN && input.all(Char::isDigit)) actions.onDraftChange(draft.copy(postalCode = input))
            }
        }
        item {
            HorizontalDivider(color = SensusVioletBg)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Horarios de atención por día", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        }
        items(draft.schedules) { schedule ->
            MerchantScheduleDayCard(schedule) { updated ->
                actions.onDraftChange(draft.copy(schedules = draft.schedules.map { if (it.dia == updated.dia) updated else it }))
            }
        }
        editError?.let { err -> item { Text(text = err, fontSize = 13.sp, color = SensusRed) } }
        item {
            Button(
                onClick = actions.onSaveClick,
                enabled = !isSaving,
                shape = SensusButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Text(text = if (isSaving) "Guardando..." else "Guardar cambios", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun EditSheetField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun MerchantScheduleDayCard(schedule: HorarioDia, onUpdate: (HorarioDia) -> Unit) {
    val apertura = schedule.horaApertura ?: "08:00"
    val cierre = schedule.horaCierre ?: "20:00"
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SensusBackground), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = schedule.dia, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = schedule.estaCerrado,
                        onCheckedChange = { onUpdate(schedule.copy(estaCerrado = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = SensusVioletInk)
                    )
                    Text(text = "Cerrado", fontSize = 13.sp, color = SensusTextSecondary)
                }
            }
            if (!schedule.estaCerrado) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = apertura,
                        onValueChange = { onUpdate(schedule.copy(horaApertura = it)) },
                        label = { Text("Abre", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cierre,
                        onValueChange = { onUpdate(schedule.copy(horaCierre = it)) },
                        label = { Text("Cierra", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}

private fun validateMerchantDraft(draft: MerchantEditDraft): String? = when {
    draft.address.trim().length !in MIN_ADDR_LEN..MAX_ADDR_LEN -> "La dirección debe tener entre 5 y 250 caracteres."
    draft.postalCode.length != POSTAL_LEN -> "El código postal debe tener exactamente 5 dígitos."
    else -> null
}

private fun defaultScheduleList(): List<HorarioDia> = listOf(
    HorarioDia("Lunes", "08:00", "20:00", false),
    HorarioDia("Martes", "08:00", "20:00", false),
    HorarioDia("Miércoles", "08:00", "20:00", false),
    HorarioDia("Jueves", "08:00", "20:00", false),
    HorarioDia("Viernes", "08:00", "20:00", false),
    HorarioDia("Sábado", "09:00", "18:00", false),
    HorarioDia("Domingo", "00:00", "00:00", true)
)
