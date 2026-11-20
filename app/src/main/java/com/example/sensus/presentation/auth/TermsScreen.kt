package com.example.sensus.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedButtonColors

const val PRIVACY_POLICY_URL = "https://rodnix.com.mx/sensus/privacidad.html"

private const val HEADER_BADGE_ALPHA = 0.12f
private const val MUTED_BUTTON_ALPHA = 0.3f

@Composable
fun TermsScreen(modifier: Modifier = Modifier, onTermsAccepted: () -> Unit, onNavigateBack: (() -> Unit)? = null, onPrivacyPolicyClick: (() -> Unit)? = null) {
    val uriHandler = LocalUriHandler.current
    val openPrivacyPolicy: () -> Unit = onPrivacyPolicyClick ?: { uriHandler.openUri(PRIVACY_POLICY_URL) }
    var termsAccepted by remember { mutableStateOf(true) }
    var locationAccepted by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TermsTopBar(onNavigateBack = onNavigateBack)
            TermsHeader()
            Spacer(modifier = Modifier.height(20.dp))
            TermsGuidelinesCard()
            Spacer(modifier = Modifier.height(18.dp))
            TermsConsentCheckbox(
                checked = termsAccepted,
                label = "He leído y acepto los Términos y Condiciones de Sensus",
                onCheckedChange = { termsAccepted = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TermsConsentCheckbox(
                checked = locationAccepted,
                label = "Autorizo el permiso de ubicación para descubrir ofertas en mi zona",
                onCheckedChange = { locationAccepted = it }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TermsActions(
                canProceed = termsAccepted && locationAccepted,
                onPrivacyPolicyClick = openPrivacyPolicy,
                onAcceptClick = onTermsAccepted
            )
        }
    }
}

@Composable
private fun TermsTopBar(onNavigateBack: (() -> Unit)?) {
    if (onNavigateBack == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SensusSurface)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Regresar",
                tint = SensusVioletInk
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Volver",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = SensusTextSecondary
        )
    }
}

@Composable
private fun TermsHeader() {
    Surface(
        shape = CircleShape,
        color = SensusPurple.copy(alpha = HEADER_BADGE_ALPHA),
        modifier = Modifier.size(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Términos y Privacidad",
                tint = SensusVioletInk,
                modifier = Modifier.size(38.dp)
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Términos del Servicio",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SensusVioletInk
    )
    Text(
        text = "Para comenzar a ahorrar, revisa los lineamientos de Sensus",
        fontSize = 13.sp,
        color = SensusTextSecondary,
        lineHeight = 18.sp
    )
}

@Composable
private fun TermsGuidelinesCard() {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            TermItem(
                icon = Icons.Default.LocationOn,
                title = "Uso de Geolocalización",
                description = "Sensus utiliza tu ubicación en primer plano para calcular distancias " +
                    "exactas y mostrarte los locales más cercanos con promociones activas."
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SensusCardBorder)
            TermItem(
                icon = Icons.Default.ConfirmationNumber,
                title = "Validez de Cupones",
                description = "Los descuentos son provistos por los comercios afiliados y son " +
                    "válidos al presentar el código en pantalla al momento de pagar."
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = SensusCardBorder)
            TermItem(
                icon = Icons.Default.Security,
                title = "Seguridad y Datos",
                description = "No compartimos tu información personal ni tus coordenadas con " +
                    "redes de publicidad externa. Tus datos están seguros con nosotros."
            )
        }
    }
}

@Composable
private fun TermsConsentCheckbox(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = SensusVioletInk,
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = SensusTextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun TermsActions(canProceed: Boolean, onPrivacyPolicyClick: () -> Unit, onAcceptClick: () -> Unit) {
    OutlinedButton(
        colors = sensusOutlinedButtonColors(),
        onClick = onPrivacyPolicyClick,
        shape = SensusButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Abrir Aviso de Privacidad",
            tint = SensusVioletInk,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Consultar Aviso de Privacidad",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusVioletInk
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = onAcceptClick,
        enabled = canProceed,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SensusPurple,
            disabledContainerColor = SensusTextMuted.copy(alpha = MUTED_BUTTON_ALPHA)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Aceptar y Continuar",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun TermItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SensusPurple.copy(alpha = 0.08f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SensusVioletInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = SensusTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true, name = "TermsDefault")
@Composable
internal fun TermsDefaultPreview() {
    SensusTheme {
        TermsScreen(
            onTermsAccepted = {},
            onNavigateBack = {}
        )
    }
}
