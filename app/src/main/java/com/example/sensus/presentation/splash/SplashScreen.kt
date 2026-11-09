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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sensus.R
import com.example.sensus.theme.SensusSplashYellow

/**
 * Splash Screen Sensus con animacin de video real (1080x1920, 30fps).
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit
) {
    LocalContext.current
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
            ) {
                finishOnce()
            },
        contentAlignment = Alignment.Center
    ) {
        // Reproductor de video nativo con aceleracin por hardware
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    alpha = 0f
                    val videoUri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.sensus_splash}")
                    setVideoURI(videoUri)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = false
                        mediaPlayer.setVolume(0f, 0f)
                        mediaPlayer.setOnInfoListener { _, what, _ ->
                            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                alpha = 1f
                                isVideoReady = true
                            }
                            false
                        }
                        start()
                        postDelayed({
                            if (!isVideoReady) {
                                alpha = 1f
                                isVideoReady = true
                            }
                        }, 300)
                    }
                    setOnCompletionListener {
                        finishOnce()
                    }
                    setOnErrorListener { _, _, _ ->
                        finishOnce()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = !isVideoReady,
            exit = fadeOut(animationSpec = tween(150)),
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
