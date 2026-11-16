package com.example.sensus.presentation.buyer

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sensus.R
import com.example.sensus.domain.model.NotificationsPreferences
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NotificationPreferencesComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test
    fun loadedPreferencesEmitEditsAndActivationWithoutFakeInbox() {
        val preferences = NotificationsPreferences(true, false, true, 5)
        val intents = mutableListOf<NotificationsViewIntent>()
        var activations = 0
        compose.setContent {
            MaterialTheme {
                NotificationsPreferencesContent(
                    NotificationsViewState(preferences, preferences, isLoading = false),
                    { intents.add(it) },
                    {},
                    { activations++ }
                )
            }
        }
        compose.onNodeWithText(text(R.string.notification_preferences_save)).assertIsNotEnabled()
        compose.onNodeWithContentDescription(text(R.string.notification_preferences_offers)).performClick()
        assertEquals(listOf(NotificationsViewIntent.Edit(preferences.copy(offerAlerts = true))), intents)
        compose.onNodeWithText(text(R.string.notification_preferences_activate)).performScrollTo().performClick()
        assertEquals(1, activations)
        compose.onNodeWithText("Marcar leídas").assertDoesNotExist()
    }

    @Test
    fun failedLoadOffersRetryWithoutEditableDefaults() {
        val intents = mutableListOf<NotificationsViewIntent>()
        compose.setContent {
            MaterialTheme {
                NotificationsPreferencesContent(
                    NotificationsViewState(isLoading = false, error = NotificationsError.SESSION),
                    { intents.add(it) },
                    {},
                    {}
                )
            }
        }
        compose.onNodeWithText(text(R.string.notification_preferences_session)).assertExists()
        compose.onNodeWithText(text(R.string.notification_preferences_retry)).assertIsEnabled().performClick()
        assertEquals(listOf(NotificationsViewIntent.Retry), intents)
        compose.onNodeWithContentDescription(text(R.string.notification_preferences_push)).assertDoesNotExist()
    }
}
