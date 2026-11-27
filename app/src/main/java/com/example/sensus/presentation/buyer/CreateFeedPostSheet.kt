package com.example.sensus.presentation.buyer

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.presentation.merchant.rememberSafeMerchantCameraLauncher
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusTextButtonColors
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CAMERA_JPEG_QUALITY = 90

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateFeedPostSheet(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit) {
    var isReadingImage by remember { mutableStateOf(false) }
    val pickGallery = rememberFeedGalleryLauncher(onReadingChange = { isReadingImage = it }, onIntent = onIntent)
    val launchCamera = rememberFeedCameraLauncher(onReadingChange = { isReadingImage = it }, onIntent = onIntent)
    ModalBottomSheet(onDismissRequest = { onIntent(CommunityViewIntent.CloseComposer) }) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(stringResource(R.string.community_create_post), color = SensusVioletInk)
            OutlinedTextField(
                value = state.draftText,
                onValueChange = { onIntent(CommunityViewIntent.TextChanged(it)) },
                label = { Text(stringResource(R.string.community_share_label)) },
                enabled = !state.isPublishing,
                supportingText = { Text("${state.draftText.length}/${FeedPolicy.MAX_TEXT_LENGTH}") },
                modifier = Modifier.fillMaxWidth()
            )
            FeedAttachmentControls(
                state = state.copy(isPublishing = state.isPublishing || isReadingImage),
                onIntent = onIntent,
                onPickGallery = pickGallery,
                onTakePhoto = launchCamera
            )
            state.publishError?.let {
                val message = if (it is AppError.NotFound) stringResource(R.string.community_publish_unavailable) else it.message
                Text(message, color = SensusRed)
            }
            Button(
                shape = SensusButtonShape,
                onClick = { onIntent(CommunityViewIntent.Submit) },
                enabled = !state.isPublishing && !isReadingImage && state.draftText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
            ) {
                Text(if (state.isPublishing) stringResource(R.string.community_publishing) else stringResource(R.string.community_publish))
            }
        }
    }
}

@Composable
private fun rememberFeedGalleryLauncher(onReadingChange: (Boolean) -> Unit, onIntent: (CommunityViewIntent) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onReadingChange(true)
        onIntent(CommunityViewIntent.ImageSelected(null))
        scope.launch {
            val bytes = readPostImage(context, uri)
            onReadingChange(false)
            onIntent(if (bytes == null) CommunityViewIntent.ImageReadFailed else CommunityViewIntent.ImageSelected(bytes))
        }
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
private fun rememberFeedCameraLauncher(onReadingChange: (Boolean) -> Unit, onIntent: (CommunityViewIntent) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return rememberSafeMerchantCameraLauncher { bitmap ->
        onReadingChange(true)
        scope.launch {
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, CAMERA_JPEG_QUALITY, out)
                    out.toByteArray()
                }
            }
            onReadingChange(false)
            onIntent(if (bytes.isEmpty()) CommunityViewIntent.ImageReadFailed else CommunityViewIntent.ImageSelected(bytes))
        }
    }
}

@Composable
private fun FeedAttachmentControls(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit, onPickGallery: () -> Unit, onTakePhoto: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(colors = sensusTextButtonColors(), onClick = onPickGallery, enabled = !state.isPublishing) {
            Text(if (state.hasImage) stringResource(R.string.community_change_photo) else stringResource(R.string.community_add_photo))
        }
        TextButton(colors = sensusTextButtonColors(), onClick = onTakePhoto, enabled = !state.isPublishing) {
            Text(stringResource(R.string.community_take_photo))
        }
    }
    Text(
        text = stringResource(R.string.community_photo_moderated_hint),
        style = MaterialTheme.typography.bodySmall,
        color = SensusTextSecondary
    )
    if (state.hasImage) {
        TextButton(
            colors = sensusTextButtonColors(),
            onClick = { onIntent(CommunityViewIntent.ImageSelected(null)) },
            enabled = !state.isPublishing
        ) { Text(stringResource(R.string.community_remove_photo)) }
    }
    Text(stringResource(R.string.community_tag_merchant))
    TextButton(colors = sensusTextButtonColors(), onClick = {
        onIntent(CommunityViewIntent.MerchantSelected(null))
    }, enabled = !state.isPublishing) {
        val noMerchantLabel = stringResource(R.string.community_no_merchant)
        Text(if (state.merchantId == null) "$noMerchantLabel (seleccionado)" else noMerchantLabel)
    }
    state.nearbyMerchants
        .filter { !it.merchantId.isNullOrBlank() }
        .distinctBy { it.merchantId }
        .forEach { offer ->
            TextButton(
                colors = sensusTextButtonColors(),
                onClick = { onIntent(CommunityViewIntent.MerchantSelected(offer.merchantId)) },
                enabled = !state.isPublishing
            ) {
                Text(if (state.merchantId == offer.merchantId) "${offer.storeName} (seleccionado)" else offer.storeName)
            }
        }
}

private suspend fun readPostImage(context: android.content.Context, uri: android.net.Uri): ByteArray? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
