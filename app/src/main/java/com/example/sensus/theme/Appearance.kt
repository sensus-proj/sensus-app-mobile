package com.example.sensus.theme

import android.content.Context
import android.view.Window
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.sensus.R

internal enum class SensusAppearance(@StringRes val label: Int) {
    SYSTEM(R.string.appearance_system),
    LIGHT(R.string.appearance_light),
    DARK(R.string.appearance_dark);

    fun usesDarkTheme(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromPreference(value: String?): SensusAppearance = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

internal class AppearancePreference(initial: SensusAppearance, private val save: (SensusAppearance) -> Unit) {
    var selected by mutableStateOf(initial)
        private set

    fun select(appearance: SensusAppearance) {
        selected = appearance
        save(appearance)
    }
}

internal val LocalAppearancePreference = staticCompositionLocalOf<AppearancePreference?> { null }

@Composable
internal fun SensusAppTheme(window: Window, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preference = remember(context.applicationContext) {
        val settings = context.applicationContext.getSharedPreferences("sensus_appearance", Context.MODE_PRIVATE)
        AppearancePreference(SensusAppearance.fromPreference(settings.getString("mode", null))) {
            settings.edit().putString("mode", it.name).apply()
        }
    }
    val darkTheme = preference.selected.usesDarkTheme(isSystemInDarkTheme())

    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalAppearancePreference provides preference) {
        SensusTheme(darkTheme = darkTheme, content = content)
    }
}
