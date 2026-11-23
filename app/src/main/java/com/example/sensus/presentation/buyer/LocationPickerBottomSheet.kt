package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sensus.R

@Composable
internal fun ManualLocationButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) {
        Text(stringResource(R.string.nearby_location_map_center))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationPickerBottomSheet(onApply: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.nearby_location_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.nearby_location_explanation))
            Button(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nearby_location_confirm))
            }
        }
    }
}
