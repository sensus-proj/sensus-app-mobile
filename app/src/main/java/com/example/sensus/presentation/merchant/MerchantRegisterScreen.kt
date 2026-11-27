package com.example.sensus.presentation.merchant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.R
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import kotlinx.coroutines.launch

private const val MIN_BUSINESS_LENGTH = 2
private const val MAX_BUSINESS_LENGTH = 150
private const val MIN_ADDRESS_LENGTH = 5
private const val MAX_ADDRESS_LENGTH = 250
private const val MAX_POSTAL_LENGTH = 10
private val POSTAL_CODE_REGEX = Regex("^[0-9a-zA-Z\\s\\-]{4,10}$")

@ComposeUiModel
private data class MerchantRegisterForm(
    val businessName: String = "",
    val address: String = "",
    val postalCode: String = ""
)

@ComposeUiModel
private data class MerchantRegisterUiBundle(
    val state: MerchantRegisterViewState,
    val form: MerchantRegisterForm,
    val errors: MerchantRegisterErrors,
    val facadePhoto: StoreFacadePhotoSelection
)

@ComposeUiModel
private data class MerchantRegisterErrors(
    val businessNameError: String? = null,
    val addressError: String? = null,
    val postalCodeError: String? = null,
    val facadePhotoError: String? = null
) {
    val hasErrors: Boolean
        get() = businessNameError != null || addressError != null || postalCodeError != null || facadePhotoError != null
}

@ComposeUiModel
private data class MerchantFieldSpec(
    val label: String,
    val value: String,
    val placeholder: String,
    val error: String?,
    val keyboardOptions: KeyboardOptions
)

@ComposeUiModel
private data class MerchantRegisterUiActions(
    val onNavigateBack: () -> Unit,
    val onFormChange: (MerchantRegisterForm, MerchantRegisterErrors) -> Unit,
    val onFacadePhotoClick: () -> Unit,
    val onUseLocationClick: () -> Unit,
    val onSubmitClick: () -> Unit
)

@Composable
fun MerchantRegisterScreen(
    viewModel: MerchantRegisterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsState()
    val formState = remember { mutableStateOf(MerchantRegisterForm()) }
    val errorsState = remember { mutableStateOf(MerchantRegisterErrors()) }
    val facadePhotoState = remember { mutableStateOf(StoreFacadePhotoSelection()) }
    val showFacadePicker = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLocationPickerPermissionLauncher(viewModel)

    BackHandler {
        if (state.showMapPicker) viewModel.dispatchIntent(MerchantRegisterViewIntent.DismissMapPicker) else onNavigateBack()
    }
    CollectMerchantRegisterEffects(viewModel, context, onApplyResolvedAddress = { addr, cp ->
        formState.value = formState.value.copy(address = addr.ifBlank { formState.value.address }, postalCode = cp.ifBlank { formState.value.postalCode })
        errorsState.value = errorsState.value.copy(
            addressError = addr.ifBlank { null }?.let { null } ?: errorsState.value.addressError,
            postalCodeError = cp.ifBlank { null }?.let { null } ?: errorsState.value.postalCodeError
        )
    }, onRegisterSuccess = onRegisterSuccess)
    SyncAutoDetectedAddressIfBlank(state, formState.value) { addr, cp ->
        formState.value = formState.value.copy(address = addr, postalCode = cp)
        errorsState.value = errorsState.value.copy(addressError = null, postalCodeError = null)
    }

    val actions = MerchantRegisterUiActions(
        onNavigateBack = onNavigateBack,
        onFormChange = { updated, cleared ->
            formState.value = updated
            errorsState.value = cleared
            if (state.errorMessage != null) viewModel.dispatchIntent(MerchantRegisterViewIntent.ClearError)
        },
        onFacadePhotoClick = { showFacadePicker.value = true },
        onUseLocationClick = { launchLocationPicker(context, viewModel, permissionLauncher::launch) },
        onSubmitClick = { submitMerchantRegistration(formState.value, facadePhotoState.value, { errorsState.value = it }, viewModel) }
    )
    val (launchCamera, launchGallery) = rememberRegisterFacadeLaunchers(
        onDismiss = { showFacadePicker.value = false },
        onSelectionUpdated = { photo, err ->
            facadePhotoState.value = photo
            errorsState.value = errorsState.value.copy(facadePhotoError = err)
        }
    )
    MerchantRegisterScaffoldContent(
        bundle = MerchantRegisterUiBundle(state, formState.value, errorsState.value, facadePhotoState.value),
        actions = actions,
        modifier = modifier
    )
    RenderLocationPickerIfOpen(state = state, viewModel = viewModel)
    if (showFacadePicker.value) {
        PhotoPickerBottomSheet(
            title = "Foto de la fachada de tu comercio",
            onDismiss = { showFacadePicker.value = false },
            onCameraClick = launchCamera,
            onGalleryClick = launchGallery
        )
    }
}

@Composable
private fun rememberRegisterFacadeLaunchers(
    onDismiss: () -> Unit,
    onSelectionUpdated: (StoreFacadePhotoSelection, String?) -> Unit
): Pair<() -> Unit, () -> Unit> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launchCamera = rememberSafeMerchantCameraLauncher { bitmap ->
        onSelectionUpdated(StoreFacadePhotoSelection(bitmap = bitmap, bytes = encodeMerchantCameraBitmap(bitmap)), null)
        onDismiss()
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onSelectionUpdated(StoreFacadePhotoSelection(uri = uri, isReading = true), null)
            onDismiss()
            scope.launch {
                val bytes = readMerchantImageBytes(context, uri)
                val err = if (bytes == null) "No se pudo leer la foto de fachada." else null
                onSelectionUpdated(StoreFacadePhotoSelection(uri = uri, bytes = bytes, isReading = false), err)
            }
        }
    }
    return launchCamera to { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

private fun launchLocationPicker(context: Context, viewModel: MerchantRegisterViewModel, launchPermission: (Array<String>) -> Unit) {
    if (hasAnyLocationPermission(context)) {
        viewModel.dispatchIntent(MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation)
    } else {
        launchPermission(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

private fun submitMerchantRegistration(
    form: MerchantRegisterForm,
    facadePhoto: StoreFacadePhotoSelection,
    onErrors: (MerchantRegisterErrors) -> Unit,
    viewModel: MerchantRegisterViewModel
) {
    val validation = validateMerchantForm(form, facadePhoto)
    onErrors(validation)
    if (!validation.hasErrors && !facadePhoto.isReading) {
        viewModel.dispatchIntent(
            MerchantRegisterViewIntent.ActivateMerchant(
                storeName = form.businessName.trim(),
                address = form.address.trim(),
                postalCode = form.postalCode.trim(),
                facadePhotoBytes = facadePhoto.bytes
            )
        )
    }
}

@Composable
private fun RenderLocationPickerIfOpen(state: MerchantRegisterViewState, viewModel: MerchantRegisterViewModel) {
    if (state.showMapPicker) {
        MerchantLocationPickerModal(
            state = state,
            onDismiss = { viewModel.dispatchIntent(MerchantRegisterViewIntent.DismissMapPicker) },
            onRecenterGps = { viewModel.dispatchIntent(MerchantRegisterViewIntent.RecenterPickerOnCurrentLocation) },
            onPinMoved = { lat, lng -> viewModel.dispatchIntent(MerchantRegisterViewIntent.MoveMapPin(lat, lng)) },
            onConfirm = { lat, lng -> viewModel.dispatchIntent(MerchantRegisterViewIntent.ConfirmMapLocation(lat, lng)) }
        )
    }
}

@Composable
private fun rememberLocationPickerPermissionLauncher(viewModel: MerchantRegisterViewModel) =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val intent = if (granted) {
            MerchantRegisterViewIntent.OpenMapPickerWithCurrentLocation
        } else {
            MerchantRegisterViewIntent.OpenMapPickerManualFallback
        }
        viewModel.dispatchIntent(intent)
    }

@Composable
private fun CollectMerchantRegisterEffects(
    viewModel: MerchantRegisterViewModel,
    context: Context,
    onApplyResolvedAddress: (String, String) -> Unit,
    onRegisterSuccess: () -> Unit
) {
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is MerchantRegisterViewEffect.ActivationSuccess -> {
                    Toast.makeText(context, "¡Comercio registrado con éxito!", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess()
                }
                is MerchantRegisterViewEffect.ApplyResolvedAddress -> onApplyResolvedAddress(effect.address, effect.postalCode)
            }
        }
    }
}

@Composable
private fun SyncAutoDetectedAddressIfBlank(state: MerchantRegisterViewState, form: MerchantRegisterForm, onAutoFill: (String, String) -> Unit) {
    LaunchedEffect(state.resolvedAddressPreview, state.resolvedPostalPreview, state.selectedLatitude, state.selectedLongitude) {
        val addr = state.resolvedAddressPreview.orEmpty()
        val cp = state.resolvedPostalPreview.orEmpty()
        val hasResolvedText = addr.isNotBlank() || cp.isNotBlank()
        if (state.selectedLatitude != null && form.address.isBlank() && hasResolvedText) {
            onAutoFill(addr.ifBlank { form.address }, cp.ifBlank { form.postalCode })
        }
    }
}

private fun hasAnyLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@Composable
private fun MerchantRegisterScaffoldContent(bundle: MerchantRegisterUiBundle, actions: MerchantRegisterUiActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusVioletBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(MerchantScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MerchantScreenHeader(
            title = stringResource(R.string.merchant_register_title),
            subtitle = stringResource(R.string.merchant_register_subtitle),
            onNavigateBack = actions.onNavigateBack
        )
        MerchantRegisterFormFields(
            state = bundle.state,
            form = bundle.form,
            errors = bundle.errors,
            facadePhoto = bundle.facadePhoto,
            actions = actions
        )
        bundle.state.errorMessage?.let { apiError ->
            Surface(shape = MerchantCardShape, color = SensusRedBg, modifier = Modifier.fillMaxWidth()) {
                Text(text = apiError, color = SensusRed, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp))
            }
        }
        MerchantOrangeButton(
            text = stringResource(R.string.merchant_register_submit),
            onClick = actions.onSubmitClick,
            loading = bundle.state.isRegistering
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = SensusTextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.merchant_register_map_hint), fontSize = 11.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun MerchantRegisterFormFields(
    state: MerchantRegisterViewState,
    form: MerchantRegisterForm,
    errors: MerchantRegisterErrors,
    facadePhoto: StoreFacadePhotoSelection,
    actions: MerchantRegisterUiActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StoreFacadePickerField(selection = facadePhoto, error = errors.facadePhotoError, onClick = actions.onFacadePhotoClick)
        MerchantLabeledField(
            spec = MerchantFieldSpec(
                stringResource(R.string.merchant_register_name),
                form.businessName,
                stringResource(R.string.merchant_register_name_hint),
                errors.businessNameError,
                KeyboardOptions(imeAction = ImeAction.Next)
            ),
            onValueChange = { actions.onFormChange(form.copy(businessName = it), errors.copy(businessNameError = null)) }
        )
        Column {
            MerchantFieldLabel(stringResource(R.string.merchant_register_location))
            PeekingCatLocationButton(
                isResolving = state.isResolvingLocation && !state.showMapPicker,
                hasCoordinates = state.selectedLatitude != null && state.selectedLongitude != null,
                onClick = actions.onUseLocationClick
            )
        }
        MerchantLabeledField(
            spec = MerchantFieldSpec(
                stringResource(R.string.merchant_register_address),
                form.address,
                stringResource(R.string.merchant_register_address_hint),
                errors.addressError,
                KeyboardOptions(imeAction = ImeAction.Next)
            ),
            onValueChange = { actions.onFormChange(form.copy(address = it), errors.copy(addressError = null)) }
        )
        MerchantLabeledField(
            spec = MerchantFieldSpec(
                stringResource(R.string.merchant_register_postal),
                form.postalCode,
                stringResource(R.string.merchant_register_postal_hint),
                errors.postalCodeError,
                KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            ),
            onValueChange = { input ->
                val filtered = input.filter { it.isLetterOrDigit() || it == '-' || it == ' ' }
                if (filtered.length <= MAX_POSTAL_LENGTH) {
                    actions.onFormChange(form.copy(postalCode = filtered), errors.copy(postalCodeError = null))
                }
            }
        )
    }
}

@Composable
private fun MerchantLabeledField(spec: MerchantFieldSpec, onValueChange: (String) -> Unit) {
    Column {
        MerchantFieldLabel(spec.label)
        OutlinedTextField(
            value = spec.value,
            onValueChange = onValueChange,
            placeholder = { Text(spec.placeholder) },
            isError = spec.error != null,
            modifier = Modifier.fillMaxWidth(),
            shape = MerchantFieldShape,
            singleLine = true,
            keyboardOptions = spec.keyboardOptions,
            colors = sensusOutlinedTextFieldColors()
        )
        spec.error?.let { MerchantErrorText(it) }
    }
}

private fun validateMerchantForm(form: MerchantRegisterForm, facadePhoto: StoreFacadePhotoSelection): MerchantRegisterErrors = MerchantRegisterErrors(
    businessNameError = validateBusinessName(form.businessName.trim()),
    addressError = validateStoreAddress(form.address.trim()),
    postalCodeError = if (POSTAL_CODE_REGEX.matches(form.postalCode.trim())) null else "Ingresa un código postal válido (4 a 10 caracteres)",
    facadePhotoError = when {
        facadePhoto.hasPhoto -> null
        facadePhoto.isReading -> "Cargando la foto de fachada, espera un momento..."
        else -> "Debes tomar o seleccionar una foto de la fachada de tu comercio"
    }
)

private fun validateBusinessName(clean: String): String? = when {
    clean.length < MIN_BUSINESS_LENGTH -> "El nombre comercial debe tener al menos 2 caracteres"
    clean.length > MAX_BUSINESS_LENGTH -> "El nombre comercial no puede exceder 150 caracteres"
    else -> null
}

private fun validateStoreAddress(clean: String): String? = when {
    clean.length < MIN_ADDRESS_LENGTH -> "La dirección debe contener al menos 5 caracteres"
    clean.length > MAX_ADDRESS_LENGTH -> "La dirección no puede exceder 250 caracteres"
    else -> null
}
