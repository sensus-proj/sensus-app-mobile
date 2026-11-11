package com.example.sensus.presentation.merchant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_NAME_LEN = 3
private const val JPEG_QUALITY = 85
private const val MAX_IMAGE_DIM = 800
private const val DEFAULT_DISTANCE_METERS = 150
private const val DEFAULT_COLLECT_MINUTES = 60
private const val DEFAULT_OFFER_RATING = 4.9
private const val COUPON_MIN_NUM = 1000
private const val COUPON_MAX_NUM = 9999

@ComposeUiModel
private data class NewOfferScreenState(
    val form: NewOfferFormState = NewOfferFormState(),
    val errors: NewOfferErrors = NewOfferErrors(),
    val selectedImageUri: Uri? = null,
    val selectedBitmap: Bitmap? = null,
    val imageBytes: ByteArray? = null,
    val isCheckingModeration: Boolean = false
)

@ComposeUiModel
private data class NewOfferScrollActions(
    val onNavigateBack: () -> Unit,
    val onPhotoClick: () -> Unit,
    val onFormChange: (NewOfferFormState, NewOfferErrors) -> Unit,
    val onPublishClick: () -> Unit
)

@ComposeUiModel
private data class OfferSubmissionHandlers(
    val onStateUpdate: (NewOfferScreenState) -> Unit,
    val onModerationRejected: (String) -> Unit,
    val onOfferCreated: () -> Unit,
    val launchTask: (suspend () -> Unit) -> Unit
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
    var uiState by remember { mutableStateOf(NewOfferScreenState()) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var moderationAlertMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            uiState = uiState.copy(selectedBitmap = bitmap, selectedImageUri = null, imageBytes = compressOfferBitmapToBytes(bitmap))
            showPhotoPickerSheet = false
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uiState = uiState.copy(selectedImageUri = uri, selectedBitmap = null)
            scope.launch { uiState = uiState.copy(imageBytes = compressOfferUriToBytes(context, uri)) }
            showPhotoPickerSheet = false
        }
    }

    Box(modifier = modifier.fillMaxSize().background(SensusBackground)) {
        NewOfferScrollContent(
            uiState = uiState,
            actions = NewOfferScrollActions(
                onNavigateBack = onNavigateBack,
                onPhotoClick = { showPhotoPickerSheet = true },
                onFormChange = { updated, cleared -> uiState = uiState.copy(form = updated, errors = cleared) },
                onPublishClick = {
                    submitOfferPublication(
                        uiState = uiState,
                        viewModel = viewModel,
                        handlers = OfferSubmissionHandlers(
                            onStateUpdate = { uiState = it },
                            onModerationRejected = { moderationAlertMessage = it },
                            onOfferCreated = onOfferCreated,
                            launchTask = { block -> scope.launch { block() } }
                        )
                    )
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
        moderationAlertMessage?.let { reason -> ModerationAlertDialog(reason = reason, onDismiss = { moderationAlertMessage = null }) }
    }
}

@Composable
private fun NewOfferScrollContent(uiState: NewOfferScreenState, actions: NewOfferScrollActions) {
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
        OfferFormCard(uiState.form, uiState.errors, onFormChange = actions.onFormChange)
        Spacer(modifier = Modifier.height(24.dp))
        PublishOfferButton(isLoading = uiState.isCheckingModeration, onClick = actions.onPublishClick)
    }
}

private fun submitOfferPublication(uiState: NewOfferScreenState, viewModel: MerchantNewOfferViewModel, handlers: OfferSubmissionHandlers) {
    val validation = validateNewOfferForm(uiState.form)
    handlers.onStateUpdate(uiState.copy(errors = validation))
    if (validation.hasErrors) return

    handlers.launchTask {
        handlers.onStateUpdate(uiState.copy(errors = validation, isCheckingModeration = true))
        when (val modRes = evaluateImageModeration(uiState.imageBytes)) {
            is ModerationResult.Rejected -> {
                handlers.onStateUpdate(uiState.copy(errors = validation, isCheckingModeration = false))
                handlers.onModerationRejected(modRes.reason)
            }
            is ModerationResult.Approved -> {
                viewModel.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(buildNewOffer(uiState.form)))
                handlers.onStateUpdate(uiState.copy(errors = validation, isCheckingModeration = false))
                handlers.onOfferCreated()
            }
        }
    }
}

private fun validateNewOfferForm(form: NewOfferFormState): NewOfferErrors {
    val origPriceVal = form.originalPrice.toDoubleOrNull() ?: 0.0
    val discPriceVal = form.discountPrice.toDoubleOrNull() ?: 0.0
    val nameError = if (form.productName.trim().length < MIN_NAME_LEN) "Ingresa el nombre del producto (mínimo 3 caracteres)" else null
    val priceError = when {
        origPriceVal <= 0.0 -> "Ingresa un precio original válido"
        discPriceVal <= 0.0 -> "Ingresa un precio de oferta válido"
        discPriceVal >= origPriceVal -> "El precio de oferta debe ser menor que el original"
        else -> null
    }
    return NewOfferErrors(productNameError = nameError, priceError = priceError)
}

private fun evaluateImageModeration(imageBytes: ByteArray?): ModerationResult {
    if (imageBytes == null || imageBytes.isEmpty()) return ModerationResult.Approved
    return ModerationResult.Approved
}

private fun buildNewOffer(form: NewOfferFormState): ProductOffer {
    val origPriceVal = form.originalPrice.toDoubleOrNull() ?: 0.0
    val discPriceVal = form.discountPrice.toDoubleOrNull() ?: 0.0
    return ProductOffer(
        id = "off_${System.currentTimeMillis()}",
        name = form.productName.trim(),
        storeName = "Comercio Local",
        category = form.selectedCategory.displayName,
        originalPrice = origPriceVal,
        discountPrice = discPriceVal,
        discountPercent = calculateOfferDiscountPercent(form.originalPrice, form.discountPrice),
        unitsLeft = form.unitsCount,
        distanceMeters = DEFAULT_DISTANCE_METERS,
        timeToCollectMinutes = DEFAULT_COLLECT_MINUTES,
        rating = DEFAULT_OFFER_RATING,
        reviewCount = 1,
        address = "Av. Principal 123",
        couponCode = "SNS-OFF-${(COUPON_MIN_NUM..COUPON_MAX_NUM).random()}",
        description = form.description.trim().ifEmpty { "Excedente garantizado en óptimo estado." },
        expirationDate = form.expirationDate.trim().ifEmpty { "Hoy 20:00 hrs" }
    )
}

private fun compressOfferBitmapToBytes(bitmap: Bitmap): ByteArray {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    return out.toByteArray()
}

private suspend fun compressOfferUriToBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null
        compressOfferBitmapToBytes(scaleOfferBitmapIfNeeded(originalBitmap))
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private fun scaleOfferBitmapIfNeeded(originalBitmap: Bitmap): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height
    if (width <= MAX_IMAGE_DIM && height <= MAX_IMAGE_DIM) return originalBitmap
    val ratio = width.toFloat() / height.toFloat()
    val (newW, newH) = if (width >= height) {
        MAX_IMAGE_DIM to (MAX_IMAGE_DIM / ratio).toInt().coerceAtLeast(1)
    } else {
        (MAX_IMAGE_DIM * ratio).toInt().coerceAtLeast(1) to MAX_IMAGE_DIM
    }
    return Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
}
