package com.example.sensus.presentation.common

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

private const val QR_SIZE = 512
private const val QR_DISPLAY_SIZE = 240
private const val MAX_QR_PAYLOAD_BYTES = 2048

@Composable
fun ConfirmedClaimQr(payload: String) {
    val bitmap = remember(payload) { encodeClaimQr(payload) }
    if (bitmap == null) {
        Text(stringResource(R.string.claims_qr_unavailable))
    } else {
        Image(bitmap, stringResource(R.string.claims_qr_description), Modifier.size(QR_DISPLAY_SIZE.dp))
    }
}

private fun encodeClaimQr(payload: String): ImageBitmap? {
    if (payload.isBlank() || payload.toByteArray(Charsets.UTF_8).size > MAX_QR_PAYLOAD_BYTES) return null
    return try {
        val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints)
        val pixels = IntArray(QR_SIZE * QR_SIZE) { index ->
            if (matrix[index % QR_SIZE, index / QR_SIZE]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        Bitmap.createBitmap(pixels, QR_SIZE, QR_SIZE, Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (_: WriterException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
