package com.example.sensus.presentation.merchant

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.sensus.presentation.common.ComposeUiModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MERCHANT_CAMERA_JPEG_QUALITY = 90

@ComposeUiModel
internal data class StoreFacadePhotoSelection(
    val bitmap: Bitmap? = null,
    val uri: Uri? = null,
    val bytes: ByteArray? = null,
    val isReading: Boolean = false
) {
    val hasPhoto: Boolean
        get() = bytes != null && bytes.isNotEmpty()
}

@Composable
internal fun rememberSafeMerchantCameraLauncher(onBitmapCaptured: (Bitmap) -> Unit): () -> Unit {
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) onBitmapCaptured(bitmap)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchCameraPreviewSafely(context) { cameraLauncher.launch(null) }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para tomar la foto.", Toast.LENGTH_SHORT).show()
        }
    }
    return {
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasCamera) {
            launchCameraPreviewSafely(context) { cameraLauncher.launch(null) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

private fun launchCameraPreviewSafely(context: Context, launchAction: () -> Unit) {
    try {
        launchAction()
    } catch (_: SecurityException) {
        Toast.makeText(context, "No se pudo abrir la cámara. Verifica el permiso en Ajustes.", Toast.LENGTH_SHORT).show()
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No se encontró una aplicación de cámara disponible.", Toast.LENGTH_SHORT).show()
    }
}

internal fun encodeMerchantCameraBitmap(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { out ->
    bitmap.compress(Bitmap.CompressFormat.JPEG, MERCHANT_CAMERA_JPEG_QUALITY, out)
    out.toByteArray()
}

internal suspend fun readMerchantImageBytes(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }?.takeIf { it.isNotEmpty() }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
