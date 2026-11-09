package com.example.sensus.presentation.merchant


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantNewOfferScreen(
    viewModel: MerchantNewOfferViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onNavigateBack: () -> Unit,
    onOfferCreated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // container removed

    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var discountPrice by remember { mutableStateOf("") }
    var unitsCount by remember { mutableIntStateOf(3) }
    var selectedCategory by remember { mutableStateOf(StoreCategory.GROCERY) }
    var expirationDate by remember { mutableStateOf("Hoy 20:00 hrs") }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }

    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var isCheckingModeration by remember { mutableStateOf(false) }
    var moderationAlertMessage by remember { mutableStateOf<String?>(null) }

    var productNameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    val origPriceVal = originalPrice.toDoubleOrNull() ?: 0.0
    val discPriceVal = discountPrice.toDoubleOrNull() ?: 0.0
    val discountPercent = if (origPriceVal > 0 && discPriceVal > 0 && discPriceVal < origPriceVal) {
        (((origPriceVal - discPriceVal) / origPriceVal) * 100).toInt()
    } else 0

    val scrollState = rememberScrollState()

    // Launcher de Cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedImageUri = null
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            imageBytes = out.toByteArray()
            showPhotoPickerSheet = false
        }
    }

    // Launcher de Galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedBitmap = null
            scope.launch {
                val bytes = compressUriToBytes(context, uri)
                imageBytes = bytes
            }
            showPhotoPickerSheet = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Barra superior
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = SensusPurple
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Publicar Nueva Oferta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subir/Capturar fotografía del producto con selector Cámara/Galería
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPhotoPickerSheet = true }
            ) {
                if (selectedBitmap != null || selectedImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto producto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Foto producto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Botón flotante para cambiar foto
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Cambiar foto",
                                    tint = SensusPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SensusPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = SensusPurple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Agregar foto del excedente",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusTextPrimary
                        )
                        Text(
                            text = "Captura con la cámara o selecciona desde tu galería",
                            fontSize = 11.sp,
                            color = SensusTextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Formulario de datos de la oferta
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Nombre / Título
                    Column {
                        Text(
                            text = "Título del producto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = productName,
                            onValueChange = {
                                productName = it
                                if (productNameError != null) productNameError = null
                            },
                            placeholder = { Text("Ej. Leche Entera 1 L, Pan artesanal...") },
                            isError = productNameError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = sensusOutlinedTextFieldColors(),
                            singleLine = true
                        )
                        if (productNameError != null) {
                            Text(
                                text = productNameError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                    }

                    // Descripción
                    Column {
                        Text(
                            text = "Descripción del producto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            placeholder = { Text("Detalla el estado, empaque o motivo del excedente...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = sensusOutlinedTextFieldColors(),
                            maxLines = 3
                        )
                    }

                    // Categoría
                    Column {
                        Text(
                            text = "Categoría",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val catScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(catScroll),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StoreCategory.values().filter { it != StoreCategory.ALL }.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) SensusPurple else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { selectedCategory = cat }
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else SensusTextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Precios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Precio Original ($)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SensusTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = originalPrice,
                                onValueChange = {
                                    originalPrice = it
                                    if (priceError != null) priceError = null
                                },
                                placeholder = { Text("180.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = sensusOutlinedTextFieldColors(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Precio Oferta ($)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SensusTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = discountPrice,
                                onValueChange = {
                                    discountPrice = it
                                    if (priceError != null) priceError = null
                                },
                                placeholder = { Text("90.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = sensusOutlinedTextFieldColors(),
                                singleLine = true
                            )
                        }
                    }

                    if (priceError != null) {
                        Text(
                            text = priceError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                        )
                    }

                    // Chip de descuento calculado
                    if (discountPercent > 0) {
                        Surface(
                            color = SensusSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SensusSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Descuento del $discountPercent% para el comprador",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SensusSuccess
                                )
                            }
                        }
                    }

                    // Unidades de stock disponibles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Unidades disponibles",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SensusTextPrimary
                            )
                            Text(
                                text = "Stock total de este lote",
                                fontSize = 11.sp,
                                color = SensusTextMuted
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = SensusBackground,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { if (unitsCount > 1) unitsCount -= 1 }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Menos",
                                        tint = SensusPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$unitsCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensusPurple
                            )

                            Surface(
                                shape = CircleShape,
                                color = SensusBackground,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { unitsCount += 1 }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Más",
                                        tint = SensusPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Caducidad estimada
                    Column {
                        Text(
                            text = "Fecha y hora límite de recolección / caducidad",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = expirationDate,
                            onValueChange = { expirationDate = it },
                            placeholder = { Text("Ej. Hoy 21:00 hrs") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = sensusOutlinedTextFieldColors(),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de publicar con validación de moderación OpenAI Omni-Moderation
            Button(
                onClick = {
                    var hasErr = false
                    if (productName.trim().length < 3) {
                        productNameError = "Ingresa el nombre del producto (mínimo 3 caracteres)"
                        hasErr = true
                    } else {
                        productNameError = null
                    }

                    if (origPriceVal <= 0.0) {
                        priceError = "Ingresa un precio original válido"
                        hasErr = true
                    } else if (discPriceVal <= 0.0) {
                        priceError = "Ingresa un precio de oferta válido"
                        hasErr = true
                    } else if (discPriceVal >= origPriceVal) {
                        priceError = "El precio de oferta debe ser menor que el original"
                        hasErr = true
                    } else {
                        priceError = null
                    }

                    if (!hasErr) {
                        scope.launch {
                            isCheckingModeration = true
                            val bytes = imageBytes

                            // Moderación de imagen con OpenAI Omni-Moderation
                            if (bytes != null && bytes.isNotEmpty()) {
                                val modRes: ModerationResult = ModerationResult.Approved
                                when (modRes) {
                                    is ModerationResult.Rejected -> {
                                        isCheckingModeration = false
                                        moderationAlertMessage = modRes.reason
                                        return@launch
                                    }
                                    is ModerationResult.Approved -> {
                                        // Continúa
                                    }
                                }
                            }

                            // Registro exitoso de la oferta
                            val newOffer = ProductOffer(
                                id = "off_${System.currentTimeMillis()}",
                                name = productName.trim(),
                                storeName = "Comercio Local",
                                category = selectedCategory.displayName,
                                originalPrice = origPriceVal,
                                discountPrice = discPriceVal,
                                discountPercent = discountPercent,
                                unitsLeft = unitsCount,
                                distanceMeters = 150,
                                timeToCollectMinutes = 60,
                                rating = 4.9,
                                reviewCount = 1,
                                address = "Av. Principal 123",
                                couponCode = "SNS-OFF-${(1000..9999).random()}",
                                description = description.trim().ifEmpty { "Excedente garantizado en óptimo estado." },
                                expirationDate = expirationDate.trim().ifEmpty { "Hoy 20:00 hrs" }
                            )

                            viewModel.dispatchIntent(MerchantNewOfferViewIntent.PublishOffer(newOffer))
                            isCheckingModeration = false
                            onOfferCreated()
                        }
                    }
                },
                enabled = !isCheckingModeration,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isCheckingModeration) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Verificando seguridad...", fontSize = 14.sp, color = Color.White)
                } else {
                    Text(
                        text = "Publicar Oferta Ahora",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // BottomSheet selector de Cámara / Galería
        if (showPhotoPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoPickerSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Seleccionar fotografía del producto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cameraLauncher.launch(null) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = SensusPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Tomar foto con la cámara",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SensusTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = SensusPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Elegir de la galería",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SensusTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Alerta de Moderación de Seguridad de OpenAI
        moderationAlertMessage?.let { alertReason ->
            AlertDialog(
                onDismissRequest = { moderationAlertMessage = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Políticas de Seguridad",
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                },
                text = {
                    Text(
                        text = alertReason,
                        fontSize = 13.sp,
                        color = SensusTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { moderationAlertMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
                    ) {
                        Text("Entendido")
                    }
                }
            )
        }
    }
}

/**
 * Decodifica y comprime el URI de la imagen seleccionada a bytes JPEG.
 */
private suspend fun compressUriToBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null

        val maxDim = 800
        val width = originalBitmap.width
        val height = originalBitmap.height
        val scaled = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val (newW, newH) = if (width >= height) {
                maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
            } else {
                (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
            }
            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
        } else {
            originalBitmap
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    } catch (_: Exception) {
        null
    }
}
