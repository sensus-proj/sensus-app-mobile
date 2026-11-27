package com.example.sensus.presentation.common

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
fun SecureScreenEffect() {
    val window = LocalActivity.current?.window
    DisposableEffect(window) {
        WindowSecureFlagRegistry.acquire(window)
        onDispose { WindowSecureFlagRegistry.release(window) }
    }
}
