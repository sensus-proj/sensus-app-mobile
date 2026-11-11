package com.example.sensus.presentation.buyer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AVATAR_PICKER_ALPHA = 0.12f
private const val MIN_FULLNAME_LEN = 2
private const val MAX_FULLNAME_LEN = 100
private const val MAX_AVATAR_DIM = 800
private const val AVATAR_JPEG_QUALITY = 82

@ComposeUiModel
internal data class ProfileDisplaySummary(
    val name: String = "",
    val email: String = "",
    val photoUri: Uri? = null
)

@ComposeUiModel
private data class EditSheetFormState(
    val tempName: String,
    val email: String,
    val tempPhotoUri: Uri?,
    val editError: String?,
    val isSaving: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileBottomSheet(
    currentSummary: ProfileDisplaySummary,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Uri?, (String) -> Unit) -> Unit
) {
    var tempName by remember { mutableStateOf(currentSummary.name) }
    var tempPhotoUri by remember { mutableStateOf(currentSummary.photoUri) }
    var editError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) tempPhotoUri = uri
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        EditProfileSheetBody(
            form = EditSheetFormState(tempName, currentSummary.email, tempPhotoUri, editError, isSaving),
            onNameChange = {
                tempName = it
                editError = null
            },
            onPickPhoto = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onSaveClick = {
                val trimmed = tempName.trim()
                if (trimmed.length !in MIN_FULLNAME_LEN..MAX_FULLNAME_LEN) {
                    editError = "El nombre debe contener entre 2 y 100 caracteres."
                } else {
                    editError = null
                    onSave(trimmed, tempPhotoUri) { err -> editError = err }
                }
            }
        )
    }
}

@Composable
private fun EditProfileSheetBody(form: EditSheetFormState, onNameChange: (String) -> Unit, onPickPhoto: () -> Unit, onSaveClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Editar Perfil", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(20.dp))
        EditableAvatarPicker(photoUri = form.tempPhotoUri, onClick = onPickPhoto)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Toca para cambiar foto de perfil", fontSize = 12.sp, color = SensusPurple, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(20.dp))
        ProfileEditNameField(value = form.tempName, onValueChange = onNameChange)
        Spacer(modifier = Modifier.height(14.dp))
        ProfileReadOnlyEmailField(email = form.email)
        form.editError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = err, fontSize = 12.sp, color = SensusRed, modifier = Modifier.align(Alignment.Start))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSaveClick,
            enabled = !form.isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = if (form.isSaving) "Guardando..." else "Guardar cambios", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun EditableAvatarPicker(photoUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(SensusPurple.copy(alpha = AVATAR_PICKER_ALPHA))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Foto elegida",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(50.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(SensusPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Cambiar foto", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProfileEditNameField(value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Nombre completo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Tu nombre y apellido") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun ProfileReadOnlyEmailField(email: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Correo electrónico (no modificable)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextSecondary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = SensusTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

internal suspend fun persistProfileChanges(
    context: Context,
    viewModel: BuyerProfileViewModel,
    trimmedName: String,
    selectedUri: Uri?,
    onSuccess: (String, Uri?) -> Unit
): String? {
    val photoResolution = resolveUploadedPhotoUrl(context, viewModel, selectedUri)
    if (photoResolution is Result.Failure) return photoResolution.error.message
    val finalPhotoUrl = (photoResolution as Result.Success).data
    return when (val result = viewModel.updateProfileUseCase(trimmedName, finalPhotoUrl)) {
        is Result.Success -> {
            val resolvedUri = result.data.fotoPerfilUrl?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) } ?: selectedUri
            onSuccess(result.data.nombreCompleto, resolvedUri)
            null
        }
        is Result.Failure -> result.error.message
    }
}

private suspend fun resolveUploadedPhotoUrl(context: Context, viewModel: BuyerProfileViewModel, selectedUri: Uri?): Result<String?> {
    val uriText = selectedUri?.toString()
    if (selectedUri == null || uriText?.startsWith("https://", ignoreCase = true) == true) {
        return Result.Success(uriText)
    }
    val compressedBytes = compressAndStripExif(context, selectedUri)
        ?: return Result.Failure(AppError.Validation("No se pudo procesar la imagen seleccionada."))
    return viewModel.profileRepository.uploadProfilePhoto(compressedBytes)
}

private suspend fun compressAndStripExif(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return@withContext null
        val scaledBitmap = scaleBitmapDownIfNeeded(originalBitmap)
        val out = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, out)
        out.toByteArray()
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private fun scaleBitmapDownIfNeeded(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= MAX_AVATAR_DIM && height <= MAX_AVATAR_DIM) return bitmap
    val ratio = width.toFloat() / height.toFloat()
    val (newW, newH) = if (width >= height) {
        MAX_AVATAR_DIM to (MAX_AVATAR_DIM / ratio).toInt().coerceAtLeast(1)
    } else {
        (MAX_AVATAR_DIM * ratio).toInt().coerceAtLeast(1) to MAX_AVATAR_DIM
    }
    return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
}
