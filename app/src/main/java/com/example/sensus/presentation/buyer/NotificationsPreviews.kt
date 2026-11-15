package com.example.sensus.presentation.buyer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.sensus.domain.model.NotificationsPreferences

private const val PREVIEW_RADIUS_KM = 5

@Preview
@Composable
internal fun NotificationsPreferencesLoadedPreview() {
    val preferences = NotificationsPreferences(true, true, false, PREVIEW_RADIUS_KM)
    NotificationsPreferencesContent(NotificationsViewState(preferences = preferences, isLoading = false), {}, {}, {})
}

@Preview
@Composable
internal fun NotificationsPreferencesLoadingPreview() {
    NotificationsPreferencesContent(NotificationsViewState(), {}, {}, {})
}

@Preview
@Composable
internal fun NotificationsPreferencesErrorPreview() {
    NotificationsPreferencesContent(NotificationsViewState(isLoading = false, error = NotificationsError.SESSION), {}, {}, {})
}
