package com.example.sensus.presentation.splash

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sensus.R
import com.example.sensus.theme.SensusSplashYellow

private const val VIDEO_FALLBACK_DELAY_MS = 300L
private const val FADE_OUT_MS = 150

/**
 * Splash Screen Sensus con animación de video real (1080x1920, 30fps).
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier, onSplashFinished: () -> Unit) {
    var isVideoReady by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }

    fun finishOnce() {
        if (!hasFinished) {
            hasFinished = true
            onSplashFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusSplashYellow)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { finishOnce() },
        contentAlignment = Alignment.Center
    ) {
        SplashVideoPlayer(
            isVideoReady = isVideoReady,
            onVideoReady = { isVideoReady = true },
            onFinished = ::finishOnce
        )

        AnimatedVisibility(
            visible = !isVideoReady,
            exit = fadeOut(animationSpec = tween(FADE_OUT_MS)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SensusSplashYellow)
            )
        }
    }
}

@Composable
private fun SplashVideoPlayer(isVideoReady: Boolean, onVideoReady: () -> Unit, onFinished: () -> Unit) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                alpha = 0f
                setVideoURI(Uri.parse("android.resource://${ctx.packageName}/${R.raw.sensus_splash}"))
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = false
                    mediaPlayer.setVolume(0f, 0f)
                    mediaPlayer.setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            alpha = 1f
                            onVideoReady()
                        }
                        false
                    }
                    start()
                    postDelayed({
                        if (!isVideoReady) {
                            alpha = 1f
                            onVideoReady()
                        }
                    }, VIDEO_FALLBACK_DELAY_MS)
                }
                setOnCompletionListener { onFinished() }
                setOnErrorListener { _, _, _ ->
                    onFinished()
                    true
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
