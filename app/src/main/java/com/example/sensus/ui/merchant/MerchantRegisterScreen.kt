package com.example.sensus.ui.merchant

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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.sensus.data.SensusAuthManager
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

@Composable
fun MerchantRegisterScreen(
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }

    var businessName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    var businessNameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var postalCodeError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    fun validateAndSubmit() {
        var hasError = false
        generalError = null

        val cleanBusiness = businessName.trim()
        if (cleanBusiness.length < 2) {
            businessNameError = "El nombre comercial debe tener al menos 2 caracteres"
            hasError = true
        } else if (cleanBusiness.length > 150) {
            businessNameError = "El nombre comercial no puede exceder 150 caracteres"
            hasError = true
        } else {
            businessNameError = null
        }

        val cleanAddr = address.trim()
        if (cleanAddr.length < 5) {
            addressError = "La dirección debe contener al menos 5 caracteres"
            hasError = true
        } else if (cleanAddr.length > 250) {
            addressError = "La dirección no puede exceder 250 caracteres"
            hasError = true
        } else {
            addressError = null
        }

        val cleanPostal = postalCode.trim()
        val cpRegex = Regex("^[0-9a-zA-Z\\s\\-]{4,10}$")
        if (cleanPostal.isBlank() || !cpRegex.matches(cleanPostal)) {
            postalCodeError = "Ingresa un código postal válido (4 a 10 caracteres)"
            hasError = true
        } else {
            postalCodeError = null
        }

        if (!hasError) {
            val (success, err) = SensusAuthManager.activateMerchant(
                businessName = cleanBusiness,
                address = cleanAddr,
                postalCode = cleanPostal
            )
            if (success) {
                Toast.makeText(context, "¡Comercio registrado con éxito!", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
            } else {
                generalError = err ?: "Error al registrar el comercio"
            }
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
                    text = "¿Cambiarte a vendedor?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de beneficios
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Convierte mermas en ganancias",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Publica excedentes en segundos y liquida stock antes del cierre",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Formulario
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Datos de tu Establecimiento",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )

                    if (generalError != null) {
                        Text(
                            text = generalError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column {
                        Text(
                            text = "Nombre comercial del negocio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = {
                                businessName = it
                                if (businessNameError != null) businessNameError = null
                            },
                            placeholder = { Text("Ej. Bodega Aurrera, Panadería El Sol") },
                            isError = businessNameError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = sensusOutlinedTextFieldColors()
                        )
                        if (businessNameError != null) {
                            Text(
                                text = businessNameError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Dirección completa del local",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = address,
                            onValueChange = {
                                address = it
                                if (addressError != null) addressError = null
                            },
                            placeholder = { Text("Calle, número y colonia") },
                            isError = addressError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = sensusOutlinedTextFieldColors()
                        )
                        if (addressError != null) {
                            Text(
                                text = addressError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Código postal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isLetterOrDigit() || it == '-' || it == ' ' }
                                if (filtered.length <= 10) {
                                    postalCode = filtered
                                    if (postalCodeError != null) postalCodeError = null
                                }
                            },
                            placeholder = { Text("Ej. 42800") },
                            isError = postalCodeError != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            colors = sensusOutlinedTextFieldColors()
                        )
                        if (postalCodeError != null) {
                            Text(
                                text = postalCodeError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 6.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de confirmación
            Button(
                onClick = { validateAndSubmit() },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Comenzar a vender en Sensus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
