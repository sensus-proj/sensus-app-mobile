package com.example.sensus.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val OTP_LENGTH = 4
private const val SHIELD_BG_ALPHA = 0.12f

@ComposeUiModel
data class OtpScreenConfig(
    val phoneNumber: String = "+52 55 •••• 4821",
    val initialCode: String = "",
    val initialError: String? = null
)

@Composable
fun OtpVerificationScreen(modifier: Modifier = Modifier, config: OtpScreenConfig = OtpScreenConfig(), onVerifySuccess: () -> Unit, onNavigateBack: () -> Unit) {
    var otpCode by remember { mutableStateOf(config.initialCode.filter(Char::isDigit).take(OTP_LENGTH)) }
    var errorMessage by remember { mutableStateOf(config.initialError) }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OtpBodySection(
                phoneNumber = config.phoneNumber,
                otpCode = otpCode,
                errorMessage = errorMessage,
                onNavigateBack = onNavigateBack,
                onCodeChange = { next ->
                    otpCode = next.filter(Char::isDigit).take(OTP_LENGTH)
                    if (errorMessage != null) errorMessage = null
                }
            )
            OtpVerifyButton(
                enabled = otpCode.length == OTP_LENGTH,
                onClick = {
                    if (otpCode.length == OTP_LENGTH && otpCode.all(Char::isDigit)) {
                        onVerifySuccess()
                    } else {
                        errorMessage = "Ingresa los 4 dígitos de tu código de verificación."
                    }
                }
            )
        }
    }
}

@Composable
private fun OtpBodySection(phoneNumber: String, otpCode: String, errorMessage: String?, onNavigateBack: () -> Unit, onCodeChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusPurple)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Surface(shape = CircleShape, color = SensusPurple.copy(alpha = SHIELD_BG_ALPHA), modifier = Modifier.size(90.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.Shield, contentDescription = "Seguridad", tint = SensusPurple, modifier = Modifier.size(46.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Verificación de Seguridad", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ingresa el código de 4 dígitos enviado a\n$phoneNumber",
            fontSize = 14.sp,
            color = SensusTextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(36.dp))
        OtpCodeInput(otpCode = otpCode, hasError = errorMessage != null, onCodeChange = onCodeChange)
        errorMessage?.let { err ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = err, fontSize = 13.sp, color = SensusRed, textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(28.dp))
        OtpResendRow()
    }
}

@Composable
private fun OtpResendRow() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(text = "¿No recibiste el código? ", fontSize = 13.sp, color = SensusTextSecondary)
        Text(text = "Reenviar en 00:45", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
    }
}

@Composable
private fun OtpVerifyButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Verificar y Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun OtpCodeInput(otpCode: String, hasError: Boolean, onCodeChange: (String) -> Unit) {
    BasicTextField(
        value = otpCode,
        onValueChange = onCodeChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.testTag("otp_code_input"),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                for (index in 0 until OTP_LENGTH) {
                    OtpDigitCell(
                        char = if (index < otpCode.length) otpCode[index].toString() else "",
                        isFocused = index == otpCode.length,
                        hasError = hasError
                    )
                }
            }
        }
    )
}

@Composable
private fun OtpDigitCell(char: String, isFocused: Boolean, hasError: Boolean) {
    val borderColor = when {
        hasError -> SensusRed
        isFocused -> SensusPurple
        else -> SensusCardBorder
    }
    Box(
        modifier = Modifier
            .size(62.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(width = if (isFocused || hasError) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = char, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = SensusPurple, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, name = "OtpEmpty")
@Composable
internal fun OtpEmptyPreview() {
    SensusTheme {
        OtpVerificationScreen(onVerifySuccess = {}, onNavigateBack = {})
    }
}

@Preview(showBackground = true, name = "OtpError")
@Composable
internal fun OtpErrorPreview() {
    SensusTheme {
        OtpVerificationScreen(
            config = OtpScreenConfig(initialCode = "12", initialError = "Código inválido o expirado."),
            onVerifySuccess = {},
            onNavigateBack = {}
        )
    }
}
