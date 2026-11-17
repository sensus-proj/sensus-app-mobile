package com.example.sensus.presentation.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private const val DIALOG_CORNER_DP = 20
private const val PREVIEW_HEIGHT_DP = 260
private const val FRAME_GUIDE_SIZE_DP = 176
private const val SCAN_WIDTH = 640
private const val SCAN_HEIGHT = 480
private const val IMAGE_MAX_IMAGES = 2

@ComposeUiModel
private data class QrScannerCallbacks(
    val onQrDecoded: (String) -> Unit,
    val onRequestPermission: () -> Unit,
    val onPickGallery: () -> Unit,
    val onCameraUnavailable: (String) -> Unit,
    val onDismiss: () -> Unit
)

@Composable
fun QrScannerDialog(onQrDecoded: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanError by remember { mutableStateOf<String?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) scanError = "Sin permiso de cámara, puedes seleccionar una imagen QR de tu galería."
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val decoded = decodeQrFromUri(context, uri)
            if (decoded != null) {
                onQrDecoded(decoded)
            } else {
                scanError = "No se detectó un código QR válido en la imagen seleccionada."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    QrScannerDialogContent(
        hasCameraPermission = hasCameraPermission,
        scanError = scanError,
        callbacks = QrScannerCallbacks(
            onQrDecoded = onQrDecoded,
            onRequestPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            onPickGallery = { galleryLauncher.launch("image/*") },
            onCameraUnavailable = { msg -> scanError = msg },
            onDismiss = onDismiss
        )
    )
}

@Composable
private fun QrScannerDialogContent(hasCameraPermission: Boolean, scanError: String?, callbacks: QrScannerCallbacks) {
    Dialog(onDismissRequest = callbacks.onDismiss) {
        Surface(
            shape = RoundedCornerShape(DIALOG_CORNER_DP.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                QrScannerHeader(onDismiss = callbacks.onDismiss)
                Text(
                    text = "Apunta la cámara al código QR de tu amigo o elige una captura de galería.",
                    fontSize = 12.sp,
                    color = SensusTextSecondary
                )
                QrScannerPreviewBox(
                    hasCameraPermission = hasCameraPermission,
                    onQrDecoded = callbacks.onQrDecoded,
                    onRequestPermission = callbacks.onRequestPermission,
                    onCameraUnavailable = callbacks.onCameraUnavailable
                )
                if (!scanError.isNullOrBlank()) {
                    Text(text = scanError, fontSize = 11.sp, color = SensusRed)
                }
                OutlinedButton(
                    onClick = callbacks.onPickGallery,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SensusPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Elegir imagen QR de galería", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
                }
            }
        }
    }
}

@Composable
private fun QrScannerHeader(onDismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = SensusPurple)
            Text(text = "Escanear QR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = SensusTextSecondary)
        }
    }
}

@Composable
private fun QrScannerPreviewBox(
    hasCameraPermission: Boolean,
    onQrDecoded: (String) -> Unit,
    onRequestPermission: () -> Unit,
    onCameraUnavailable: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PREVIEW_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SensusVioletBg),
        contentAlignment = Alignment.Center
    ) {
        if (hasCameraPermission) {
            CameraQrTexturePreview(onQrDecoded = onQrDecoded, onError = onCameraUnavailable)
            Box(
                modifier = Modifier
                    .size(FRAME_GUIDE_SIZE_DP.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(42.dp))
                Text(text = "Permiso de cámara requerido", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = "Permitir cámara", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CameraQrTexturePreview(onQrDecoded: (String) -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    val controller = remember { Camera2QrController(context, onQrDecoded, onError) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = controller.surfaceListener(this)
            }
        }
    )
}

private class Camera2QrController(
    private val context: Context,
    private val onQrDecoded: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val decodedOnce = AtomicBoolean(false)
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    fun surfaceListener(textureView: TextureView): TextureView.SurfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera(textureView)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            release()
            return true
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(textureView: TextureView) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        val thread = HandlerThread("SensusQrCamera").also { it.start() }
        backgroundThread = thread
        val handler = Handler(thread.looper)
        backgroundHandler = handler

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cameraId = findBackCameraId(manager)
        if (cameraId == null) {
            onError("No se encontró cámara disponible. Usa una imagen QR de galería.")
            return
        }

        val reader = ImageReader.newInstance(SCAN_WIDTH, SCAN_HEIGHT, ImageFormat.YUV_420_888, IMAGE_MAX_IMAGES)
        reader.setOnImageAvailableListener({ r -> processLatestFrame(r) }, handler)
        imageReader = reader

        try {
            manager.openCamera(cameraId, cameraStateCallback(textureView, reader, handler), handler)
        } catch (_: CameraAccessException) {
            onError("No se pudo abrir la cámara. Intenta con una imagen QR de galería.")
        } catch (_: SecurityException) {
            onError("Permiso de cámara denegado.")
        }
    }

    private fun cameraStateCallback(textureView: TextureView, reader: ImageReader, handler: Handler): CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreviewSession(camera, textureView, reader, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                onError("Error al iniciar cámara ($error). Puedes usar galería.")
            }
        }

    @Suppress("DEPRECATION")
    private fun startPreviewSession(camera: CameraDevice, textureView: TextureView, reader: ImageReader, handler: Handler) {
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(SCAN_WIDTH, SCAN_HEIGHT)
        val previewSurface = Surface(texture)
        val targets = listOf(previewSurface, reader.surface)
        try {
            camera.createCaptureSession(
                targets,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                            addTarget(previewSurface)
                            addTarget(reader.surface)
                        }.build()
                        try {
                            session.setRepeatingRequest(request, null, handler)
                        } catch (_: CameraAccessException) {
                            onError("Error al capturar vista de cámara.")
                        } catch (_: IllegalStateException) {
                            // Sesión cerrada durante liberación.
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        onError("No se pudo configurar la cámara para escaneo QR.")
                    }
                },
                handler
            )
        } catch (_: CameraAccessException) {
            onError("Error de acceso a cámara.")
        } catch (_: IllegalStateException) {
            // Cámara cerrada.
        }
    }

    private fun processLatestFrame(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (_: IllegalStateException) {
            null
        } ?: return
        try {
            val payload = if (decodedOnce.get()) null else decodeQrFromYuvImage(image)
            if (payload != null && decodedOnce.compareAndSet(false, true)) {
                Handler(context.mainLooper).post { onQrDecoded(payload) }
            }
        } finally {
            image.close()
        }
    }

    fun release() {
        try {
            captureSession?.close()
        } catch (_: IllegalStateException) {
            // Ignorar si ya fue cerrada.
        }
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        backgroundThread?.quitSafely()
        backgroundThread = null
        backgroundHandler = null
    }
}

private fun findBackCameraId(manager: CameraManager): String? = try {
    manager.cameraIdList.firstOrNull { id ->
        manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    } ?: manager.cameraIdList.firstOrNull()
} catch (_: CameraAccessException) {
    null
}

private fun decodeQrFromYuvImage(image: Image): String? {
    val yPlane = image.planes.firstOrNull() ?: return null
    val width = image.width
    val height = image.height
    val rowStride = yPlane.rowStride
    val pixelStride = yPlane.pixelStride
    val buffer = yPlane.buffer
    val data = ByteArray(width * height)
    for (row in 0 until height) {
        val rowOffset = row * rowStride
        for (col in 0 until width) {
            val idx = rowOffset + col * pixelStride
            if (idx < buffer.limit()) {
                data[row * width + col] = buffer.get(idx)
            }
        }
    }
    return decodeQrYBytes(data, width, height) ?: decodeQrYBytes(rotate90(data, width, height), height, width)
}

private fun rotate90(input: ByteArray, width: Int, height: Int): ByteArray {
    val rotated = ByteArray(input.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            rotated[x * height + (height - 1 - y)] = input[y * width + x]
        }
    }
    return rotated
}

private fun decodeQrYBytes(data: ByteArray, width: Int, height: Int): String? {
    val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return tryDecodeBitmap(bitmap)
}

internal fun decodeQrFromUri(context: Context, uri: Uri): String? {
    val bitmap: Bitmap = try {
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    } ?: return null

    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
    return tryDecodeBitmap(BinaryBitmap(HybridBinarizer(source)))
}

private fun tryDecodeBitmap(bitmap: BinaryBitmap): String? {
    val hints = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )
    return try {
        QRCodeReader().decode(bitmap, hints).text?.takeIf { it.isNotBlank() }
    } catch (_: ReaderException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
