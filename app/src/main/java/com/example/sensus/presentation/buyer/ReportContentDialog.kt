package com.example.sensus.presentation.buyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.domain.model.ReportPolicy
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val MAX_DIALOG_BODY_DP = 420
private const val MIN_REASON_ROW_DP = 48

@ComposeUiModel
data class ReportDialogModel(
    val targetLabel: String,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

@Composable
internal fun ReportContentDialog(model: ReportDialogModel, onDismiss: () -> Unit, onSubmit: (ReportReason, String) -> Unit) {
    var selectedReason by remember { mutableStateOf(ReportReason.SPAM) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!model.isSubmitting) onDismiss() },
        icon = { Icon(imageVector = Icons.Outlined.Flag, contentDescription = null, tint = SensusPurple) },
        title = { Text(text = stringResource(R.string.ft19_report_dialog_title), color = SensusTextPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = MAX_DIALOG_BODY_DP.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ft19_report_target_hint, model.targetLabel),
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
                ReportReasonList(
                    selected = selectedReason,
                    onSelect = { selectedReason = it }
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(ReportPolicy.MAX_DETAILS_LENGTH) },
                    label = { Text(stringResource(R.string.ft19_report_details_label)) },
                    supportingText = {
                        Text("${details.length}/${ReportPolicy.MAX_DETAILS_LENGTH}", color = SensusTextMuted)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !model.isSubmitting
                )
                model.errorMessage?.let {
                    Text(text = it, color = SensusRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason, details) },
                enabled = !model.isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple)
            ) {
                Text(stringResource(if (model.isSubmitting) R.string.ft19_report_sending else R.string.ft19_report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !model.isSubmitting) {
                Text(stringResource(R.string.ft19_cancel))
            }
        }
    )
}

@Composable
private fun ReportReasonList(selected: ReportReason, onSelect: (ReportReason) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ReportReason.entries.forEach { reason ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MIN_REASON_ROW_DP.dp)
                    .clickable { onSelect(reason) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = reason == selected,
                    onClick = { onSelect(reason) },
                    colors = RadioButtonDefaults.colors(selectedColor = SensusPurple)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reason.displayLabel,
                    fontSize = 14.sp,
                    fontWeight = if (reason == selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = SensusTextPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun ReportContentDialogPreview() {
    SensusTheme {
        ReportContentDialog(
            model = ReportDialogModel(targetLabel = "Pan artesano 2x1"),
            onDismiss = {},
            onSubmit = { _, _ -> }
        )
    }
}
