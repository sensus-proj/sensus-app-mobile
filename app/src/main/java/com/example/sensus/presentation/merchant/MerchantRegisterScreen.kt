package com.example.sensus.presentation.merchant

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val HERO_ICON_BG_ALPHA = 0.20f
private const val HERO_SUBTITLE_ALPHA = 0.85f
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
private data class MerchantRegisterErrors(
    val businessNameError: String? = null,
    val addressError: String? = null,
    val postalCodeError: String? = null
) {
    val hasErrors: Boolean
        get() = businessNameError != null || addressError != null || postalCodeError != null
}

@ComposeUiModel
private data class MerchantFieldSpec(
    val label: String,
    val value: String,
    val placeholder: String,
    val error: String?,
    val keyboardOptions: KeyboardOptions
)

@Composable
fun MerchantRegisterScreen(
    viewModel: MerchantRegisterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    var form by remember { mutableStateOf(MerchantRegisterForm()) }
    var errors by remember { mutableStateOf(MerchantRegisterErrors()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MerchantRegisterTopBar(onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))
        MerchantBenefitsCard()
        Spacer(modifier = Modifier.height(20.dp))
        MerchantRegisterFormCard(
            form = form,
            errors = errors,
            onFormChange = { updated, cleared ->
                form = updated
                errors = cleared
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        MerchantRegisterSubmitButton(
            onClick = {
                val result = validateMerchantForm(form)
                errors = result
                if (!result.hasErrors) {
                    viewModel.dispatchIntent(
                        MerchantRegisterViewIntent.ActivateMerchant(
                            storeName = form.businessName.trim(),
                            address = form.address.trim(),
                            postalCode = form.postalCode.trim()
                        )
                    )
                    Toast.makeText(context, "¡Comercio registrado con éxito!", Toast.LENGTH_SHORT).show()
                    onRegisterSuccess()
                }
            }
        )
    }
}

@Composable
private fun MerchantRegisterTopBar(onNavigateBack: () -> Unit) {
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
        Text(text = "¿Cambiarte a vendedor?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
    }
}

@Composable
private fun MerchantBenefitsCard() {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SensusPurple), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = HERO_ICON_BG_ALPHA), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = "Convierte excedentes en ganancias", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = "Publica excedentes en segundos y liquida stock antes del cierre",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = HERO_SUBTITLE_ALPHA)
                )
            }
        }
    }
}

@Composable
private fun MerchantRegisterFormCard(
    form: MerchantRegisterForm,
    errors: MerchantRegisterErrors,
    onFormChange: (MerchantRegisterForm, MerchantRegisterErrors) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = "Datos de tu Establecimiento", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            MerchantLabeledField(
                spec = MerchantFieldSpec(
                    "Nombre comercial del negocio",
                    form.businessName,
                    "Ej. Bodega Aurrera, Panadería El Sol",
                    errors.businessNameError,
                    KeyboardOptions(imeAction = ImeAction.Next)
                ),
                onValueChange = { onFormChange(form.copy(businessName = it), errors.copy(businessNameError = null)) }
            )
            MerchantLabeledField(
                spec = MerchantFieldSpec(
                    "Dirección completa del local",
                    form.address,
                    "Calle, número y colonia",
                    errors.addressError,
                    KeyboardOptions(imeAction = ImeAction.Next)
                ),
                onValueChange = { onFormChange(form.copy(address = it), errors.copy(addressError = null)) }
            )
            MerchantLabeledField(
                spec = MerchantFieldSpec(
                    "Código postal",
                    form.postalCode,
                    "Ej. 42800",
                    errors.postalCodeError,
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                ),
                onValueChange = { input ->
                    val filtered = input.filter { it.isLetterOrDigit() || it == '-' || it == ' ' }
                    if (filtered.length <= MAX_POSTAL_LENGTH) {
                        onFormChange(form.copy(postalCode = filtered), errors.copy(postalCodeError = null))
                    }
                }
            )
        }
    }
}

@Composable
private fun MerchantLabeledField(spec: MerchantFieldSpec, onValueChange: (String) -> Unit) {
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
            singleLine = true,
            keyboardOptions = spec.keyboardOptions,
            colors = sensusOutlinedTextFieldColors()
        )
        spec.error?.let { err ->
            Text(text = err, color = SensusRed, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
        }
    }
}

@Composable
private fun MerchantRegisterSubmitButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(text = "Comenzar a vender en Sensus", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun validateMerchantForm(form: MerchantRegisterForm): MerchantRegisterErrors {
    val cleanBusiness = form.businessName.trim()
    val cleanAddress = form.address.trim()
    val cleanPostal = form.postalCode.trim()
    val businessErr = when {
        cleanBusiness.length < MIN_BUSINESS_LENGTH -> "El nombre comercial debe tener al menos 2 caracteres"
        cleanBusiness.length > MAX_BUSINESS_LENGTH -> "El nombre comercial no puede exceder 150 caracteres"
        else -> null
    }
    val addressErr = when {
        cleanAddress.length < MIN_ADDRESS_LENGTH -> "La dirección debe contener al menos 5 caracteres"
        cleanAddress.length > MAX_ADDRESS_LENGTH -> "La dirección no puede exceder 250 caracteres"
        else -> null
    }
    val postalErr = if (cleanPostal.isBlank() || !POSTAL_CODE_REGEX.matches(cleanPostal)) {
        "Ingresa un código postal válido (4 a 10 caracteres)"
    } else {
        null
    }
    return MerchantRegisterErrors(businessNameError = businessErr, addressError = addressErr, postalCodeError = postalErr)
}
