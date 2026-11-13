package com.example.sensus.presentation.merchant

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferValidationError
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusRed
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CAMERA_JPEG_QUALITY = 95

@ComposeUiModel
private data class NewOfferScreenState(
    val form: NewOfferFormState = NewOfferFormState(),
    val selectedImageUri: Uri? = null,
    val selectedBitmap: Bitmap? = null,
    val imageBytes: ByteArray? = null,
    val isReadingImage: Boolean = false,
    val imageReadError: String? = null
)

@ComposeUiModel
private data class NewOfferScrollActions(
    val onNavigateBack: () -> Unit,
    val onPhotoClick: () -> Unit,
    val onFormChange: (NewOfferFormState) -> Unit,
    val onPublishClick: () -> Unit
)

@Composable
fun MerchantNewOfferScreen(
    viewModel: MerchantNewOfferViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onOfferCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val vmState by viewModel.viewState.collectAsState()
    var uiState by remember { mutableStateOf(NewOfferScreenState()) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.viewEffect.collect { onOfferCreated() } }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            uiState = uiState.copy(
                selectedBitmap = bitmap, selectedImageUri = null, imageBytes = encodeCameraBitmap(bitmap),
                isReadingImage = false, imageReadError = null
            )
            showPhotoPickerSheet = false
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uiState = uiState.copy(selectedImageUri = uri, selectedBitmap = null, imageBytes = null, isReadingImage = true, imageReadError = null)
            scope.launch { loadSelectedOfferImage(context, uri, { uiState }, { uiState = it }) }
            showPhotoPickerSheet = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        NewOfferScrollContent(
            uiState = uiState,
            vmState = vmState,
            actions = NewOfferScrollActions(
                onNavigateBack = onNavigateBack,
                onPhotoClick = { showPhotoPickerSheet = true },
                onFormChange = { updated ->
                    uiState = uiState.copy(form = updated)
                    if (vmState.fieldError != null || vmState.errorMessage != null) {
                        viewModel.dispatchIntent(MerchantNewOfferViewIntent.FieldEdited)
                    }
                },
                onPublishClick = {
                    viewModel.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(uiState.form.toCreateOfferInput(), uiState.imageBytes))
                }
            )
        )
        if (showPhotoPickerSheet) {
            PhotoPickerBottomSheet(
                onDismiss = { showPhotoPickerSheet = false },
                onCameraClick = { cameraLauncher.launch(null) },
                onGalleryClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }
        vmState.moderationMessage?.let { reason ->
            ModerationAlertDialog(reason = reason, onDismiss = { viewModel.dispatchIntent(MerchantNewOfferViewIntent.DismissModeration) })
        }
    }
}

@Composable
private fun NewOfferScrollContent(uiState: NewOfferScreenState, vmState: MerchantNewOfferViewState, actions: NewOfferScrollActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        NewOfferHeader(onNavigateBack = actions.onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))
        OfferPhotoCard(uiState.selectedBitmap, uiState.selectedImageUri, onClick = actions.onPhotoClick)
        Spacer(modifier = Modifier.height(16.dp))
        OfferFormCard(uiState.form, vmState.fieldError.toNewOfferErrors(), onFormChange = actions.onFormChange)
        vmState.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, color = SensusRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        uiState.imageReadError?.let { Text(it, color = SensusRed) }
        PublishOfferButton(
            isLoading = vmState.isPublishing || uiState.isReadingImage,
            isEnabled = uiState.imageReadError == null,
            onClick = actions.onPublishClick
        )
    }
}

private fun NewOfferFormState.toCreateOfferInput(): CreateOfferInput = CreateOfferInput(
    title = productName,
    description = description,
    category = selectedCategory,
    originalPrice = originalPrice.toDoubleOrNull() ?: 0.0,
    discountPrice = discountPrice.toDoubleOrNull() ?: 0.0,
    stock = unitsCount,
    expiresAtMillis = expiresAtMillis
)

private fun OfferValidationError?.toNewOfferErrors(): NewOfferErrors {
    if (this == null) return NewOfferErrors()
    return when (field) {
        OfferField.TITLE -> NewOfferErrors(productNameError = message)
        OfferField.DESCRIPTION -> NewOfferErrors(descriptionError = message)
        OfferField.PRICE -> NewOfferErrors(priceError = message)
        OfferField.STOCK -> NewOfferErrors(stockError = message)
        OfferField.EXPIRATION -> NewOfferErrors(expirationError = message)
    }
}

private fun encodeCameraBitmap(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, CAMERA_JPEG_QUALITY, out)
    out.toByteArray()
}

/** Lee la imagen original; la compresion <= 4.5 MB ocurre en `ImageCompressor` (capa de datos). */
private suspend fun readOfferImageBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private suspend fun loadSelectedOfferImage(context: Context, uri: Uri, state: () -> NewOfferScreenState, onUpdate: (NewOfferScreenState) -> Unit) {
    val bytes = readOfferImageBytes(context, uri)
    val current = state()
    if (current.selectedImageUri == uri) {
        onUpdate(
            current.copy(
                imageBytes = bytes,
                isReadingImage = false,
                imageReadError = if (bytes == null) "No se pudo leer la foto. Selecciónala de nuevo." else null
            )
        )
    }
}
