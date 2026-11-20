package com.example.sensus.presentation.buyer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedButtonColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

private const val QR_BITMAP_SIZE = 512
private const val MAX_QR_PAYLOAD_BYTES = 2048
private const val QR_DARK_COLOR = 0xFF1E1B4B.toInt()
private const val QR_LIGHT_COLOR = 0xFFFFFFFF.toInt()

@Composable
internal fun QrInviteDialog(inviteCode: String, onDismiss: () -> Unit, onCopyCode: () -> Unit, onShareCode: () -> Unit, qrPayload: String = inviteCode) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SensusSurface,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                QrDialogHeader(onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Muestra este código a tus amigos para que lo escaneen desde la app o cámara y se agreguen a tu red.",
                    fontSize = 13.sp,
                    color = SensusTextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                QrPreviewBox(payload = qrPayload)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = SensusBackground) {
                    Text(
                        text = inviteCode,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusVioletInk,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(colors = sensusOutlinedButtonColors(), onClick = onCopyCode, shape = SensusButtonShape, modifier = Modifier.weight(1f)) {
                        Text("Copiar código", fontSize = 13.sp, color = SensusVioletInk)
                    }
                    Button(
                        onClick = onShareCode,
                        shape = SensusButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Compartir", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QrDialogHeader(onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Tu Código QR de Usuario", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = SensusTextSecondary)
        }
    }
}

@Composable
private fun QrPreviewBox(payload: String) {
    val qrBitmap = remember(payload) { encodeInviteQr(payload) }
    Box(
        modifier = Modifier
            .size(208.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap,
                contentDescription = "Código QR de invitación Sensus",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(imageVector = Icons.Default.QrCode2, contentDescription = "QR", tint = SensusVioletInk, modifier = Modifier.size(96.dp))
        }
    }
}

private fun encodeInviteQr(payload: String): ImageBitmap? {
    if (payload.isBlank() || payload.toByteArray(Charsets.UTF_8).size > MAX_QR_PAYLOAD_BYTES) return null
    return try {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_BITMAP_SIZE, QR_BITMAP_SIZE, hints)
        val pixels = IntArray(QR_BITMAP_SIZE * QR_BITMAP_SIZE) { idx ->
            if (matrix[idx % QR_BITMAP_SIZE, idx / QR_BITMAP_SIZE]) QR_DARK_COLOR else QR_LIGHT_COLOR
        }
        Bitmap.createBitmap(pixels, QR_BITMAP_SIZE, QR_BITMAP_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (_: WriterException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
