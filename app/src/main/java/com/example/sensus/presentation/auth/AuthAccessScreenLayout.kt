package com.example.sensus.presentation.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.theme.BrandOrange
import com.example.sensus.theme.BrandOrangeSoft
import com.example.sensus.theme.LocalSensusDark
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk

internal val AuthActionPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
private const val AUTH_ENTRANCE_MS = 260
private const val AUTH_WAVE_MS = 5200
private const val WAVE_ACCENT_ALPHA = 0.19f

@Composable
internal fun AuthAccessScreenLayout(modifier: Modifier = Modifier, header: @Composable () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(AUTH_ENTRANCE_MS)) }
    Box(
        modifier = modifier.fillMaxSize().background(SensusBackground).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        AuthAnimatedWave()
        Column(
            modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            header()
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .graphicsLayer { translationY = (1f - entrance.value) * 12.dp.toPx() },
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

@Composable
private fun AuthAnimatedWave() {
    val waveColor = if (LocalSensusDark.current) SensusVioletBg else BrandOrangeSoft
    val wave = painterResource(R.drawable.auth_access_wave)
    val motion = rememberInfiniteTransition(label = "authWave").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(AUTH_WAVE_MS), RepeatMode.Reverse),
        label = "waveDrift"
    )
    Image(
        painter = wave,
        contentDescription = null,
        colorFilter = ColorFilter.tint(BrandOrange.copy(alpha = WAVE_ACCENT_ALPHA)),
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxWidth().height(376.dp).graphicsLayer { translationY = motion.value * 10.dp.toPx() }
    )
    Image(
        painter = wave,
        contentDescription = null,
        colorFilter = ColorFilter.tint(waveColor),
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxWidth().height(356.dp)
    )
}

@Composable
internal fun AuthFormTitle(title: String) {
    Text(title, color = SensusVioletInk, style = MaterialTheme.typography.displayLarge, modifier = Modifier.semantics { heading() })
}

@Composable
internal fun AuthDataFrame(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SensusSurface,
        border = BorderStroke(1.dp, SensusCardBorder)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
internal fun AuthAccessBrand(showBackSpace: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(
            start = 20.dp,
            top = if (showBackSpace) 40.dp else 16.dp,
            end = 12.dp
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.login_brand),
                style = MaterialTheme.typography.displayLarge,
                color = SensusTextPrimary
            )
            Text(
                stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = SensusTextSecondary
            )
        }
        Image(
            painter = painterResource(R.drawable.sensus_cat),
            contentDescription = null,
            modifier = Modifier.size(128.dp)
        )
    }
}
