package com.example.sensus.presentation.merchant

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk

private const val FACADE_PREVIEW_HEIGHT_DP = 152
private const val FACADE_EDIT_ALPHA = 0.9f

@Composable
internal fun StoreFacadePickerField(selection: StoreFacadePhotoSelection, error: String?, onClick: () -> Unit) {
    Column {
        MerchantFieldLabel(stringResource(R.string.merchant_register_photo_label))
        if (selection.bitmap != null || selection.uri != null) {
            FacadePhotoPreview(selection, onClick)
            Text(
                stringResource(R.string.merchant_register_photo_main_hint),
                fontSize = 11.sp,
                color = SensusTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            MerchantPhotoUploadRow(
                title = stringResource(R.string.merchant_new_offer_photo_title),
                subtitle = stringResource(R.string.merchant_register_photo_hint),
                onClick = onClick
            )
        }
        error?.let { MerchantErrorText(it) }
    }
}

@Composable
private fun FacadePhotoPreview(selection: StoreFacadePhotoSelection, onClick: () -> Unit) {
    val description = stringResource(R.string.merchant_register_photo_description)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FACADE_PREVIEW_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        if (selection.bitmap != null) {
            Image(selection.bitmap.asImageBitmap(), description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (selection.uri != null) {
            AsyncImage(selection.uri, description, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Surface(color = SensusPurple, shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
            Text(
                stringResource(R.string.merchant_register_photo_main),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = FACADE_EDIT_ALPHA),
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val changeDescription = stringResource(R.string.merchant_register_photo_change)
                Icon(Icons.Default.Edit, contentDescription = changeDescription, tint = SensusVioletInk, modifier = Modifier.size(16.dp))
            }
        }
    }
}
