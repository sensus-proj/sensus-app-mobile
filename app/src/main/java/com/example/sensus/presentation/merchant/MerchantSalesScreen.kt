package com.example.sensus.presentation.merchant

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors

private const val SCANNER_BG_HEX = 0xFF1E293B
private const val CORNER_LEN = 30f
private const val CORNER_STROKE = 6f
private const val CORNER_INSET = 20f
private const val CAMERA_ICON_ALPHA = 0.8f
private const val CAMERA_HINT_ALPHA = 0.7f
private const val SUCCESS_BG_ALPHA = 0.15f

@Composable
fun MerchantSalesScreen(modifier: Modifier = Modifier) {
    var manualCode by remember { mutableStateOf("") }
    var validatedResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MerchantSalesHeader() }
        item {
            QrScannerCard(
                onScanSimulated = {
                    validatedResult = "Código SNS-A7K9-24 validado: Cobrar $90 MXN por Detergente 1 L."
                }
            )
        }
        item {
            ManualCodeEntryCard(
                manualCode = manualCode,
                onCodeChange = { manualCode = it },
                onValidateClick = {
                    if (manualCode.isNotBlank()) {
                        validatedResult = "Código $manualCode validado: Cobrar $90 MXN."
                    }
                }
            )
        }
        if (validatedResult != null) {
            item { ValidationSuccessBanner(message = validatedResult!!) }
        }
        item {
            Text(text = "Canjes completados hoy", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        }
        item { RecentRedemptionsCard() }
    }
}

@Composable
private fun MerchantSalesHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Text(text = "Ventas y Canjes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Valida las reservas de clientes y cobra en caja",
            fontSize = 13.sp,
            color = SensusTextSecondary
        )
    }
}

@Composable
private fun QrScannerCard(onScanSimulated: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrScannerViewfinder()
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanSimulated,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Activar Cámara / Escanear", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun QrScannerViewfinder() {
    Box(
        modifier = Modifier
            .size(190.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(SCANNER_BG_HEX)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawViewfinderBrackets(size.width, size.height)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White.copy(alpha = CAMERA_ICON_ALPHA),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Apunta al código QR", fontSize = 11.sp, color = Color.White.copy(alpha = CAMERA_HINT_ALPHA))
        }
    }
}

private fun DrawScope.drawViewfinderBrackets(width: Float, height: Float) {
    val corners = listOf(
        Offset(CORNER_INSET, CORNER_INSET) to Offset(1f, 1f),
        Offset(width - CORNER_INSET, CORNER_INSET) to Offset(-1f, 1f),
        Offset(CORNER_INSET, height - CORNER_INSET) to Offset(1f, -1f),
        Offset(width - CORNER_INSET, height - CORNER_INSET) to Offset(-1f, -1f)
    )
    corners.forEach { (origin, dir) ->
        drawLine(SensusPurple, origin, Offset(origin.x + dir.x * CORNER_LEN, origin.y), CORNER_STROKE)
        drawLine(SensusPurple, origin, Offset(origin.x, origin.y + dir.y * CORNER_LEN), CORNER_STROKE)
    }
}

@Composable
private fun ManualCodeEntryCard(manualCode: String, onCodeChange: (String) -> Unit, onValidateClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "O ingresa el código del cliente", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = onCodeChange,
                    placeholder = { Text("Ej. SNS-A7K9-24", fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = sensusOutlinedTextFieldColors(),
                    singleLine = true
                )
                Button(
                    onClick = onValidateClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text(text = "Validar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ValidationSuccessBanner(message: String) {
    Surface(
        color = SensusSuccess.copy(alpha = SUCCESS_BG_ALPHA),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = message, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusSuccess)
        }
    }
}

@Composable
private fun RecentRedemptionsCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            RecentRedemptionItem("Detergente - 1 L", "$90.00 MXN", "Hace 15 min", "SNS-A7K9-24")
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SensusBackground)
            Spacer(modifier = Modifier.height(10.dp))
            RecentRedemptionItem("Arroz - 1 kg", "$24.00 MXN", "Hace 45 min", "SNS-ARR-50")
        }
    }
}

@Composable
private fun RecentRedemptionItem(product: String, amount: String, time: String, code: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = product, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = "Código: $code • $time", fontSize = 11.sp, color = SensusTextMuted)
        }
        Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
    }
}
