package com.example.sensus.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusWhite
import com.example.sensus.theme.sensusTextButtonColors

private const val HERO_ENTER_MS = 440
private const val HERO_OFFSET_DIVISOR = 5

@Composable
internal fun CommerceHero(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = {}) {
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = entrance,
        modifier = modifier,
        enter = fadeIn(tween(HERO_ENTER_MS)) +
            slideInVertically(tween(HERO_ENTER_MS)) { height -> height / HERO_OFFSET_DIVISOR }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(SensusPurple)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                color = SensusWhite,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.semantics { heading() }
            )
            ProvideTextStyle(MaterialTheme.typography.bodyLarge.copy(color = SensusWhite)) {
                content()
            }
        }
    }
}

@Composable
internal fun CommerceFeedFeedback(isLoading: Boolean, message: String?, onRetry: (() -> Unit)? = null) {
    AnimatedVisibility(visible = isLoading) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (!message.isNullOrBlank()) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                if (onRetry != null) TextButton(colors = sensusTextButtonColors(), onClick = onRetry) { Text(stringResource(R.string.commerce_retry)) }
            }
        }
    }
}
