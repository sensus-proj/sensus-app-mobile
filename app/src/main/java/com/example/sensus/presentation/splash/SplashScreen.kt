package com.example.sensus.presentation.splash

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sensus.R
import com.example.sensus.theme.SensusSplashYellow
import java.io.IOException
import kotlinx.coroutines.delay

internal const val SPLASH_SAFETY_TIMEOUT_MS = 4_500L
private const val VIDEO_FALLBACK_DELAY_MS = 300L
private const val FADE_OUT_MS = 150
private const val CENTER_PIVOT_DIVISOR = 2f
private const val ZERO_VOLUME = 0f

private class SplashMediaHolder {
    var player: MediaPlayer? = null
    var surface: Surface? = null
    var videoWidth: Int = 0
    var videoHeight: Int = 0
    var viewWidth: Int = 0
    var viewHeight: Int = 0
}

private data class SplashPlaybackCallbacks(
    val isVideoReady: () -> Boolean,
    val onVideoReady: () -> Unit,
    val onFinished: () -> Unit
)

/**
 * Calcula los factores de escala (X, Y) en modo fit-center para preservar la relación de aspecto en un [TextureView].
 */
internal fun computeFitCenterScale(viewWidth: Int, viewHeight: Int, videoWidth: Int, videoHeight: Int): Pair<Float, Float> {
    val hasValidView = viewWidth > 0 && viewHeight > 0
    val hasValidVideo = videoWidth > 0 && videoHeight > 0
    if (!hasValidView || !hasValidVideo) {
        return 1f to 1f
    }
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    return if (videoAspect > viewAspect) {
        1f to (viewAspect / videoAspect)
    } else {
        (videoAspect / viewAspect) to 1f
    }
}

/**
 * Splash Screen Sensus con animación de video real (1080x1920, 3 s) a velocidad nativa sin solicitar Audio Focus.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier, onSplashFinished: () -> Unit) {
    var isVideoReady by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }
    val currentOnSplashFinished by rememberUpdatedState(onSplashFinished)

    fun finishOnce() {
        if (!hasFinished) {
            hasFinished = true
            currentOnSplashFinished()
        }
    }

    LaunchedEffect(Unit) {
        delay(SPLASH_SAFETY_TIMEOUT_MS)
        finishOnce()
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
    val holder = remember { SplashMediaHolder() }
    val currentIsReady by rememberUpdatedState(isVideoReady)
    val currentOnReady by rememberUpdatedState(onVideoReady)
    val currentOnFinished by rememberUpdatedState(onFinished)

    DisposableEffect(Unit) {
        onDispose { releaseMediaHolder(holder) }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                alpha = 0f
                val callbacks = SplashPlaybackCallbacks(
                    isVideoReady = { currentIsReady },
                    onVideoReady = { currentOnReady() },
                    onFinished = { currentOnFinished() }
                )
                surfaceTextureListener = buildSurfaceListener(
                    textureView = this,
                    holder = holder,
                    callbacks = callbacks
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun buildSurfaceListener(textureView: TextureView, holder: SplashMediaHolder, callbacks: SplashPlaybackCallbacks): TextureView.SurfaceTextureListener {
    return object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            holder.viewWidth = width
            holder.viewHeight = height
            startSplashPlayer(
                textureView = textureView,
                surfaceTex = surfaceTexture,
                holder = holder,
                callbacks = callbacks
            )
        }

        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            holder.viewWidth = width
            holder.viewHeight = height
            applyFitCenterTransform(textureView, holder)
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            releaseMediaHolder(holder)
            return true
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit
    }
}

private fun startSplashPlayer(textureView: TextureView, surfaceTex: SurfaceTexture, holder: SplashMediaHolder, callbacks: SplashPlaybackCallbacks) {
    val context = textureView.context
    releaseMediaHolder(holder)
    val surface = Surface(surfaceTex)
    val player = MediaPlayer()
    holder.surface = surface
    holder.player = player

    try {
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.sensus_splash}")
        player.setDataSource(context, uri)
        player.setSurface(surface)
        player.isLooping = false
        player.setVolume(ZERO_VOLUME, ZERO_VOLUME)
        player.setOnVideoSizeChangedListener { _, width, height ->
            holder.videoWidth = width
            holder.videoHeight = height
            applyFitCenterTransform(textureView, holder)
        }
        player.setOnPreparedListener { prepared ->
            configurePreparedMediaPlayer(prepared, textureView, callbacks)
        }
        player.setOnCompletionListener { callbacks.onFinished() }
        player.setOnErrorListener { _, _, _ ->
            releaseMediaHolder(holder)
            callbacks.onFinished()
            true
        }
        player.prepareAsync()
    } catch (_: IOException) {
        releaseMediaHolder(holder)
        callbacks.onFinished()
    } catch (_: IllegalStateException) {
        releaseMediaHolder(holder)
        callbacks.onFinished()
    } catch (_: IllegalArgumentException) {
        releaseMediaHolder(holder)
        callbacks.onFinished()
    } catch (_: SecurityException) {
        releaseMediaHolder(holder)
        callbacks.onFinished()
    }
}

private fun configurePreparedMediaPlayer(mediaPlayer: MediaPlayer, textureView: TextureView, callbacks: SplashPlaybackCallbacks) {
    mediaPlayer.isLooping = false
    mediaPlayer.setVolume(ZERO_VOLUME, ZERO_VOLUME)
    mediaPlayer.setOnInfoListener { _, what, _ ->
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            textureView.alpha = 1f
            callbacks.onVideoReady()
        }
        false
    }
    mediaPlayer.start()
    textureView.postDelayed({
        if (!callbacks.isVideoReady()) {
            textureView.alpha = 1f
            callbacks.onVideoReady()
        }
    }, VIDEO_FALLBACK_DELAY_MS)
}

private fun applyFitCenterTransform(textureView: TextureView, holder: SplashMediaHolder) {
    val targetWidth = if (textureView.width > 0) textureView.width else holder.viewWidth
    val targetHeight = if (textureView.height > 0) textureView.height else holder.viewHeight
    val (scaleX, scaleY) = computeFitCenterScale(targetWidth, targetHeight, holder.videoWidth, holder.videoHeight)
    val matrix = Matrix().apply {
        setScale(
            scaleX,
            scaleY,
            targetWidth / CENTER_PIVOT_DIVISOR,
            targetHeight / CENTER_PIVOT_DIVISOR
        )
    }
    textureView.setTransform(matrix)
}

private fun releaseMediaHolder(holder: SplashMediaHolder) {
    holder.player?.let { player ->
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: IllegalStateException) {
            // Ya liberado o en estado inválido.
        }
        try {
            player.release()
        } catch (_: IllegalStateException) {
            // Ignorar si ya fue liberado.
        }
    }
    holder.player = null
    holder.surface?.release()
    holder.surface = null
}
