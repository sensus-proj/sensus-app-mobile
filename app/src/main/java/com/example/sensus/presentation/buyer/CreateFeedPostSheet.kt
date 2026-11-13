package com.example.sensus.presentation.buyer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateFeedPostSheet(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit) {
    var isReadingImage by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            isReadingImage = true
            onIntent(CommunityViewIntent.ImageSelected(null))
            scope.launch {
                val bytes = readPostImage(context, uri)
                isReadingImage = false
                if (bytes == null) onIntent(CommunityViewIntent.ImageReadFailed) else onIntent(CommunityViewIntent.ImageSelected(bytes))
            }
        }
    }
    ModalBottomSheet(onDismissRequest = { onIntent(CommunityViewIntent.CloseComposer) }) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.community_create_post), color = SensusPurple)
            OutlinedTextField(
                value = state.draftText,
                onValueChange = { onIntent(CommunityViewIntent.TextChanged(it)) },
                label = { Text(stringResource(R.string.community_share_label)) },
                enabled = !state.isPublishing,
                supportingText = { Text("${state.draftText.length}/${FeedPolicy.MAX_TEXT_LENGTH}") },
                modifier = Modifier.fillMaxWidth()
            )
            FeedAttachmentControls(state.copy(isPublishing = state.isPublishing || isReadingImage), onIntent) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            state.errorMessage?.let { Text(it, color = SensusRed) }
            Button(
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
private fun FeedAttachmentControls(state: CommunityViewState, onIntent: (CommunityViewIntent) -> Unit, onPickImage: () -> Unit) {
    TextButton(onClick = { onPickImage() }, enabled = !state.isPublishing) {
        Text(if (state.hasImage) stringResource(R.string.community_change_photo) else stringResource(R.string.community_add_photo))
    }
    if (state.hasImage) {
        TextButton(
            onClick = { onIntent(CommunityViewIntent.ImageSelected(null)) },
            enabled = !state.isPublishing
        ) { Text(stringResource(R.string.community_remove_photo)) }
    }
    Text(stringResource(R.string.community_tag_merchant))
    TextButton(onClick = {
        onIntent(CommunityViewIntent.MerchantSelected(null))
    }, enabled = !state.isPublishing) { Text(stringResource(R.string.community_no_merchant)) }
    state.nearbyMerchants.forEach { offer ->
        TextButton(onClick = { onIntent(CommunityViewIntent.MerchantSelected(offer.merchantId)) }, enabled = !state.isPublishing) {
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
