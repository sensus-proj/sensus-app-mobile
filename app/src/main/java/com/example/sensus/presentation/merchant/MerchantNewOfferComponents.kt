package com.example.sensus.presentation.merchant

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val DEFAULT_UNITS_COUNT = 3
private const val DEFAULT_EXPIRATION_LEAD_MILLIS = 3_600_000L
private const val MAX_STOCK_UNITS = 999
private const val DESC_MAX_LINES = 3
private const val PERCENT_SCALE = 100
private const val EDIT_BTN_ALPHA = 0.9f
private const val ICON_BG_ALPHA = 0.1f
private const val SUCCESS_CHIP_ALPHA = 0.12f
private const val UNSELECTED_CHIP_HEX = 0xFFF1F5F9

@ComposeUiModel
internal data class NewOfferFormState(
    val productName: String = "",
    val description: String = "",
    val originalPrice: String = "",
    val discountPrice: String = "",
    val unitsCount: Int = DEFAULT_UNITS_COUNT,
    val selectedCategory: StoreCategory = StoreCategory.GROCERY,
    val expiresAtMillis: Long = System.currentTimeMillis() + DEFAULT_EXPIRATION_LEAD_MILLIS
)

@ComposeUiModel
internal data class NewOfferErrors(
    val productNameError: String? = null,
    val priceError: String? = null,
    val descriptionError: String? = null,
    val stockError: String? = null,
    val expirationError: String? = null
) {
    val hasErrors: Boolean get() = productNameError != null || priceError != null
}

@ComposeUiModel
private data class OfferTextFieldSpec(
    val label: String,
    val value: String,
    val placeholder: String,
    val error: String? = null,
    val maxLines: Int = 1
)

@Composable
internal fun NewOfferHeader(onNavigateBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SensusPurple)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Publicar Nueva Oferta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
    }
}

@Composable
internal fun OfferPhotoCard(selectedBitmap: Bitmap?, selectedImageUri: Uri?, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        if (selectedBitmap != null || selectedImageUri != null) {
            SelectedOfferImageBox(selectedBitmap = selectedBitmap, selectedImageUri = selectedImageUri)
        } else {
            EmptyOfferPhotoPlaceholder()
        }
    }
}

@Composable
private fun SelectedOfferImageBox(selectedBitmap: Bitmap?, selectedImageUri: Uri?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (selectedBitmap != null) {
            Image(
                bitmap = selectedBitmap.asImageBitmap(),
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
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = EDIT_BTN_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Cambiar foto", tint = SensusPurple, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyOfferPhotoPlaceholder() {
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
                .background(SensusPurple.copy(alpha = ICON_BG_ALPHA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Agregar foto del excedente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Text(text = "Captura con la cámara o selecciona desde tu galería", fontSize = 11.sp, color = SensusTextMuted)
    }
}

@Composable
internal fun OfferFormCard(form: NewOfferFormState, errors: NewOfferErrors, onFormChange: (NewOfferFormState) -> Unit) {
    val discountPercent = calculateOfferDiscountPercent(form.originalPrice, form.discountPrice)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OfferTextField(
                spec = OfferTextFieldSpec("Título del producto", form.productName, "Ej. Leche Entera 1 L, Pan artesanal...", errors.productNameError),
                onValueChange = { onFormChange(form.copy(productName = it)) }
            )
            OfferTextField(
                spec = OfferTextFieldSpec("Descripción del producto", form.description, "Detalla el estado, empaque o motivo...", maxLines = DESC_MAX_LINES),
                onValueChange = { onFormChange(form.copy(description = it)) }
            )
            OfferCategoryPicker(selected = form.selectedCategory, onSelect = { onFormChange(form.copy(selectedCategory = it)) })
            OfferPriceRow(form = form, errors = errors, onFormChange = onFormChange)
            if (discountPercent > 0) {
                DiscountPreviewChip(
                    discountPercent = discountPercent,
                    savings = com.example.sensus.domain.model.OfferPublishingPolicy.savedAmount(
                        form.originalPrice.toDoubleOrNull() ?: 0.0,
                        form.discountPrice.toDoubleOrNull() ?: 0.0
                    )
                )
            }
            OfferStockStepperRow(
                unitsCount = form.unitsCount,
                onDecrement = { if (form.unitsCount > 1) onFormChange(form.copy(unitsCount = form.unitsCount - 1)) },
                onIncrement = { if (form.unitsCount < MAX_STOCK_UNITS) onFormChange(form.copy(unitsCount = form.unitsCount + 1)) }
            )
            OfferExpirationField(form.expiresAtMillis, errors.expirationError) {
                onFormChange(form.copy(expiresAtMillis = it))
            }
            errors.stockError?.let { Text(it, color = SensusRed) }
            errors.descriptionError?.let { Text(it, color = SensusRed) }
        }
    }
}

@Composable
private fun OfferTextField(spec: OfferTextFieldSpec, onValueChange: (String) -> Unit) {
    Column {
        Text(text = spec.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = spec.value,
            onValueChange = onValueChange,
            placeholder = { Text(spec.placeholder) },
            isError = spec.error != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = sensusOutlinedTextFieldColors(),
            singleLine = spec.maxLines == 1,
            maxLines = spec.maxLines
        )
        spec.error?.let { err ->
            Text(text = err, color = SensusRed, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
        }
    }
}

@Composable
private fun OfferCategoryPicker(selected: StoreCategory, onSelect: (StoreCategory) -> Unit) {
    Column {
        Text(text = "Categoría", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoreCategory.entries.filter { it != StoreCategory.ALL }.forEach { category ->
                val isSelected = selected == category
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) SensusPurple else Color(UNSELECTED_CHIP_HEX),
                    modifier = Modifier.clickable { onSelect(category) }
                ) {
                    Text(
                        text = category.displayName,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else SensusTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferPriceRow(form: NewOfferFormState, errors: NewOfferErrors, onFormChange: (NewOfferFormState) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PriceInputColumn(
                label = "Precio Original ($)",
                value = form.originalPrice,
                placeholder = "180.00",
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(originalPrice = it)) }
            )
            PriceInputColumn(
                label = "Precio Oferta ($)",
                value = form.discountPrice,
                placeholder = "90.00",
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(discountPrice = it)) }
            )
        }
        errors.priceError?.let { err ->
            Text(text = err, color = SensusRed, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }
    }
}

@Composable
private fun PriceInputColumn(label: String, value: String, placeholder: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = sensusOutlinedTextFieldColors(),
            singleLine = true
        )
    }
}

@Composable
private fun DiscountPreviewChip(discountPercent: Int, savings: Double) {
    Surface(color = SensusSuccess.copy(alpha = SUCCESS_CHIP_ALPHA), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Descuento del $discountPercent% · Ahorro: $" + java.lang.String.format(java.util.Locale.getDefault(), "%.2f", savings),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SensusSuccess
            )
        }
    }
}

@Composable
private fun OfferStockStepperRow(unitsCount: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(text = "Unidades disponibles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
            Text(text = "Stock total de este lote", fontSize = 11.sp, color = SensusTextMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepperCircleButton(Icons.Default.Remove, "Menos", onDecrement)
            Text(text = "$unitsCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
            StepperCircleButton(Icons.Default.Add, "Más", onIncrement)
        }
    }
}

@Composable
private fun StepperCircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = SensusBackground, modifier = Modifier.size(32.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = description, tint = SensusPurple, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun PublishOfferButton(isLoading: Boolean, isEnabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading && isEnabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Verificando seguridad...", fontSize = 14.sp, color = Color.White)
        } else {
            Text(text = "Publicar Oferta Ahora", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoPickerBottomSheet(onDismiss: () -> Unit, onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(text = "Seleccionar fotografía del producto", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            PickerOptionRow(Icons.Default.CameraAlt, "Tomar foto con la cámara", onCameraClick)
            PickerOptionRow(Icons.Default.PhotoLibrary, "Elegir de la galería", onGalleryClick)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PickerOptionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
    }
}

@Composable
internal fun ModerationAlertDialog(reason: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = SensusRed, modifier = Modifier.size(32.dp)) },
        title = { Text(text = "Políticas de Seguridad", fontWeight = FontWeight.Bold, color = SensusTextPrimary) },
        text = { Text(text = reason, fontSize = 13.sp, color = SensusTextSecondary) },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
                Text("Entendido")
            }
        }
    )
}

internal fun calculateOfferDiscountPercent(originalPrice: String, discountPrice: String): Int {
    val orig = originalPrice.toDoubleOrNull() ?: 0.0
    val disc = discountPrice.toDoubleOrNull() ?: 0.0
    return if (orig > 0.0 && disc > 0.0 && disc < orig) {
        (((orig - disc) / orig) * PERCENT_SCALE).toInt()
    } else {
        0
    }
}

@Composable
private fun OfferExpirationField(value: Long, error: String?, onChange: (Long) -> Unit) {
    OfferExpirationPicker(value, error, onChange)
}
