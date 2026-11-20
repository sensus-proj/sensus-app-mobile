package com.example.sensus.presentation.splash

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sensus.R

@Composable
fun SplashScreen(modifier: Modifier = Modifier, onSplashFinished: () -> Unit) {
    val onFinished by rememberUpdatedState(onSplashFinished)
    var hasFinished by remember { mutableStateOf(false) }

    fun finishOnce() {
        if (!hasFinished) {
            hasFinished = true
            onFinished()
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(colorResource(R.color.sensus_video_matte)).clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { finishOnce() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.sensus_splash}"))
                    setOnPreparedListener { player ->
                        player.setVolume(0f, 0f)
                        start()
                    }
                    setOnCompletionListener { finishOnce() }
                    setOnErrorListener { _, _, _ ->
                        finishOnce()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            onReset = null,
            onRelease = { it.stopPlayback() }
        )
    }
}
