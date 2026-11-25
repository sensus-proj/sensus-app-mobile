package com.example.sensus.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.theme.LocalAppearancePreference
import com.example.sensus.theme.SensusAppearance
import com.example.sensus.theme.SensusVioletInk

@Composable
internal fun AppearanceSettings() {
    val preference = LocalAppearancePreference.current ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(vertical = 8.dp).selectableGroup()) {
            Text(
                text = stringResource(R.string.appearance_title),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() },
                style = MaterialTheme.typography.titleMedium
            )
            SensusAppearance.entries.forEach { appearance ->
                val selected = preference.selected == appearance
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = selected, role = Role.RadioButton) { preference.select(appearance) }
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(appearance.label), modifier = Modifier.weight(1f))
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = SensusVioletInk)
                    )
                }
            }
        }
    }
}
