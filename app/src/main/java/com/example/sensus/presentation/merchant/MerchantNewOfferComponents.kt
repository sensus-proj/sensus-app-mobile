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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.BrandOrangeDark
import com.example.sensus.theme.BrandOrangeSoft
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val DEFAULT_UNITS_COUNT = 3
private const val DEFAULT_EXPIRATION_LEAD_MILLIS = 3_600_000L
private const val MAX_STOCK_UNITS = 999
private const val DESC_MAX_LINES = 3
private const val PERCENT_SCALE = 100
private const val EDIT_BTN_ALPHA = 0.9f
private const val DASH_LENGTH = 10f
private const val DASH_GAP = 8f

internal val MerchantFieldShape = RoundedCornerShape(12.dp)

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
    val categoryError: String? = null,
    val stockError: String? = null,
    val expirationError: String? = null,
    val photoError: String? = null
) {
    val hasErrors: Boolean
        get() = productNameError != null || priceError != null || descriptionError != null || categoryError != null || photoError != null
}

@Composable
internal fun NewOfferHeader(onNavigateBack: () -> Unit) {
    MerchantScreenHeader(
        title = stringResource(R.string.merchant_new_offer_title),
        subtitle = stringResource(R.string.merchant_new_offer_subtitle),
        onNavigateBack = onNavigateBack
    )
}

/** Borde punteado violeta de la maqueta para las zonas de carga de foto. */
internal fun Modifier.merchantDashedBorder(color: Color = SensusPurple): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_LENGTH, DASH_GAP))),
        cornerRadius = CornerRadius(16.dp.toPx())
    )
}

@Composable
internal fun OfferPhotoCard(selectedBitmap: Bitmap?, selectedImageUri: Uri?, error: String? = null, onClick: () -> Unit) {
    Column {
        if (selectedBitmap != null || selectedImageUri != null) {
            SelectedOfferImageBox(selectedBitmap, selectedImageUri, onClick)
        } else {
            MerchantPhotoUploadRow(
                title = stringResource(R.string.merchant_new_offer_photo_title),
                subtitle = stringResource(R.string.merchant_new_offer_photo_hint),
                onClick = onClick
            )
        }
        error?.let { MerchantErrorText(it) }
    }
}

@Composable
internal fun MerchantPhotoUploadRow(title: String, subtitle: String, onClick: () -> Unit, icon: ImageVector = Icons.Default.PhotoCamera) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SensusSurface)
            .merchantDashedBorder()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MerchantIconBadge(icon, size = 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusVioletInk)
            Text(subtitle, fontSize = 11.sp, color = SensusTextSecondary)
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = SensusVioletInk)
    }
}

@Composable
private fun SelectedOfferImageBox(selectedBitmap: Bitmap?, selectedImageUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        val description = stringResource(R.string.merchant_new_offer_photo_preview)
        if (selectedBitmap != null) {
            Image(selectedBitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (selectedImageUri != null) {
            AsyncImage(selectedImageUri, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = EDIT_BTN_ALPHA),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.merchant_new_offer_change_photo), fontSize = 12.sp, color = SensusVioletInk)
            }
        }
    }
}

@Composable
internal fun OfferFormCard(form: NewOfferFormState, errors: NewOfferErrors, onFormChange: (NewOfferFormState) -> Unit) {
    val discountPercent = calculateOfferDiscountPercent(form.originalPrice, form.discountPrice)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MerchantTextField(
            label = stringResource(R.string.merchant_new_offer_name),
            value = form.productName,
            options = MerchantFieldOptions(placeholder = stringResource(R.string.merchant_new_offer_name_hint), error = errors.productNameError),
            onValueChange = { onFormChange(form.copy(productName = it)) }
        )
        MerchantCategoryPicker(selected = form.selectedCategory, onSelect = { onFormChange(form.copy(selectedCategory = it)) })
        errors.categoryError?.let { MerchantErrorText(it) }
        MerchantTextField(
            label = stringResource(R.string.merchant_new_offer_description),
            value = form.description,
            options = MerchantFieldOptions(
                placeholder = stringResource(R.string.merchant_new_offer_description_hint),
                error = errors.descriptionError,
                maxLines = DESC_MAX_LINES
            ),
            onValueChange = { onFormChange(form.copy(description = it)) }
        )
        OfferPriceRow(form = form, discountPercent = discountPercent, error = errors.priceError, onFormChange = onFormChange)
        OfferStockStepperRow(
            unitsCount = form.unitsCount,
            onDecrement = { if (form.unitsCount > 1) onFormChange(form.copy(unitsCount = form.unitsCount - 1)) },
            onIncrement = { if (form.unitsCount < MAX_STOCK_UNITS) onFormChange(form.copy(unitsCount = form.unitsCount + 1)) }
        )
        errors.stockError?.let { MerchantErrorText(it) }
        OfferExpirationPicker(form.expiresAtMillis, errors.expirationError) { onFormChange(form.copy(expiresAtMillis = it)) }
        MerchantNoticeCard(text = stringResource(R.string.merchant_new_offer_notice), icon = Icons.Default.Timer)
    }
}

@ComposeUiModel
internal data class MerchantFieldOptions(
    val placeholder: String = "",
    val error: String? = null,
    val maxLines: Int = 1,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val leadingIcon: ImageVector? = null
)

@Composable
internal fun MerchantTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: MerchantFieldOptions = MerchantFieldOptions()
) {
    Column(modifier) {
        MerchantFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(options.placeholder) },
            isError = options.error != null,
            leadingIcon = options.leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
            keyboardOptions = KeyboardOptions(keyboardType = options.keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = MerchantFieldShape,
            colors = sensusOutlinedTextFieldColors(),
            singleLine = options.maxLines == 1,
            maxLines = options.maxLines
        )
        options.error?.let { MerchantErrorText(it) }
    }
}

@Composable
private fun MerchantCategoryPicker(selected: StoreCategory, onSelect: (StoreCategory) -> Unit) {
    Column {
        MerchantFieldLabel(stringResource(R.string.merchant_new_offer_category))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoreCategory.entries.filter { it != StoreCategory.ALL }.forEach { category ->
                val isSelected = selected == category
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) SensusPurple else SensusVioletBg,
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
private fun OfferPriceRow(form: NewOfferFormState, discountPercent: Int, error: String?, onFormChange: (NewOfferFormState) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MerchantTextField(
                label = stringResource(R.string.merchant_new_offer_original_price),
                value = form.originalPrice,
                options = MerchantFieldOptions(stringResource(R.string.merchant_new_offer_original_price_hint), keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(originalPrice = it)) }
            )
            MerchantTextField(
                label = stringResource(R.string.merchant_new_offer_sensus_price),
                value = form.discountPrice,
                options = MerchantFieldOptions(stringResource(R.string.merchant_new_offer_sensus_price_hint), keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(discountPrice = it)) }
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.merchant_new_offer_currency_hint), fontSize = 11.sp, color = SensusTextSecondary, modifier = Modifier.weight(1f))
            if (discountPercent > 0) {
                MerchantStatusChip(stringResource(R.string.merchant_new_offer_discount, discountPercent), BrandOrangeDark, BrandOrangeSoft)
            }
        }
        error?.let { MerchantErrorText(it) }
    }
}

@Composable
private fun OfferStockStepperRow(unitsCount: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    MerchantCard {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.merchant_new_offer_units), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
                Text(stringResource(R.string.merchant_new_offer_units_hint), fontSize = 11.sp, color = SensusTextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StepperCircleButton(Icons.Default.Remove, stringResource(R.string.merchant_new_offer_less), onDecrement)
                Text("$unitsCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
                StepperCircleButton(Icons.Default.Add, stringResource(R.string.merchant_new_offer_more), onIncrement)
            }
        }
    }
}

@Composable
private fun StepperCircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = SensusVioletBg, modifier = Modifier.size(36.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = SensusVioletInk, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PublishOfferButton(isLoading: Boolean, isEnabled: Boolean = true, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MerchantPrimaryButton(
            text = stringResource(if (isLoading) R.string.merchant_new_offer_publishing else R.string.merchant_new_offer_publish),
            onClick = onClick,
            enabled = isEnabled,
            loading = isLoading
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = SensusTextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.merchant_new_offer_map_hint), fontSize = 11.sp, color = SensusTextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoPickerBottomSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    title: String = "Seleccionar fotografía del producto"
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SensusSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
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
        MerchantIconBadge(icon)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
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
            Button(shape = SensusButtonShape, onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)) {
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
