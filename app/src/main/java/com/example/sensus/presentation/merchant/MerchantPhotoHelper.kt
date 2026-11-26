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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusVioletInk
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MERCHANT_CAMERA_JPEG_QUALITY = 90
private const val FACADE_PREVIEW_HEIGHT_DP = 152
private const val FACADE_ICON_ALPHA = 0.12f
private const val FACADE_EDIT_ALPHA = 0.9f

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

@Composable
internal fun StoreFacadePickerField(selection: StoreFacadePhotoSelection, error: String?, onClick: () -> Unit) {
    Column {
        Text(text = "Foto de la fachada de tu comercio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SensusBackground,
            border = BorderStroke(1.dp, if (error != null) SensusRed else SensusCardBorder),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            if (selection.bitmap != null || selection.uri != null) {
                Box(modifier = Modifier.fillMaxWidth().height(FACADE_PREVIEW_HEIGHT_DP.dp)) {
                    if (selection.bitmap != null) {
                        Image(
                            bitmap = selection.bitmap.asImageBitmap(),
                            contentDescription = "Foto de fachada del comercio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (selection.uri != null) {
                        AsyncImage(
                            model = selection.uri,
                            contentDescription = "Foto de fachada del comercio",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = FACADE_EDIT_ALPHA),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = "Cambiar foto de fachada", tint = SensusVioletInk, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(SensusPurple.copy(alpha = FACADE_ICON_ALPHA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Tomar o elegir foto de fachada", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                    Text(text = "Obligatoria para registrar tu comercio", fontSize = 11.sp, color = SensusTextMuted)
                }
            }
        }
        error?.let { err ->
            Text(text = err, color = SensusRed, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
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
