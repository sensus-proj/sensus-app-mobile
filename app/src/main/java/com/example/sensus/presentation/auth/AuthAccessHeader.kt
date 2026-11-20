package com.example.sensus.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusWhite
import kotlinx.coroutines.launch

private const val TAB_SLIDE_DURATION_MS = 180
private const val TAB_MIDPOINT = 0.5f

@Composable
internal fun AuthAccessSelector(isRegister: Boolean, onSwitch: () -> Unit) {
    val initialPosition = authTabPosition(isRegister)
    val position = remember(isRegister) { Animatable(initialPosition) }
    val scope = rememberCoroutineScope()
    val switch: () -> Unit = {
        scope.launch { switchAuthTab(isRegister, position, onSwitch) }
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(CircleShape)
            .background(SensusVioletBg)
            .padding(4.dp)
            .drawBehind { drawAuthIndicator(position.value) }
            .selectableGroup()
    ) {
        val highlightsRegister = authHighlightsRegister(position.value)
        AuthAccessOption(R.string.auth_sign_in_tab, !isRegister, !highlightsRegister, Modifier.weight(1f), switch)
        AuthAccessOption(R.string.auth_create_account_tab, isRegister, highlightsRegister, Modifier.weight(1f), switch)
    }
}

internal fun DrawScope.drawAuthIndicator(position: Float) {
    val indicator = authIndicatorGeometry(size, position, layoutDirection)
    drawRoundRect(SensusPurple, indicator.offset, indicator.size, indicator.radius)
}

internal data class AuthIndicatorGeometry(val offset: Offset, val size: Size, val radius: CornerRadius)

internal fun authIndicatorGeometry(size: Size, position: Float, direction: LayoutDirection): AuthIndicatorGeometry {
    val width = size.width / 2
    val offset = if (direction == LayoutDirection.Ltr) position else 1f - position
    return AuthIndicatorGeometry(
        offset = Offset(width * offset, 0f),
        size = Size(width, size.height),
        radius = CornerRadius(size.height / 2)
    )
}

internal fun authTabPosition(isRegister: Boolean): Float = if (isRegister) 1f else 0f

internal fun authHighlightsRegister(position: Float): Boolean = position >= TAB_MIDPOINT

internal suspend fun switchAuthTab(isRegister: Boolean, position: Animatable<Float, *>, onSwitch: () -> Unit) {
    if (position.isRunning) return
    position.animateTo(authTabPosition(!isRegister), tween(TAB_SLIDE_DURATION_MS))
    onSwitch()
}

@Composable
private fun AuthAccessOption(label: Int, selected: Boolean, highlighted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = { if (!selected) onClick() })
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = if (highlighted) SensusWhite else SensusTextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
