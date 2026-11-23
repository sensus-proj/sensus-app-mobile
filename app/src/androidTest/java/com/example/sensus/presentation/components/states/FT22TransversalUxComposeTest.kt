package com.example.sensus.presentation.components.states

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FT22TransversalUxComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun offlineErrorState_rendersAndRetryClickTriggersCallback() {
        var retried = false
        composeTestRule.setContent {
            SensusTheme {
                OfflineErrorState(
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("offline_error_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_error_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeTestRule.onNodeWithTag("offline_retry_button").assertIsDisplayed().performClick()

        assertTrue("Expected onRetry callback to be invoked", retried)
    }

    @Test
    fun locationDisabledState_rendersStepsAndButtonsTriggerCallbacks() {
        var settingsOpened = false
        var manualLocationClicked = false

        composeTestRule.setContent {
            SensusTheme {
                LocationDisabledState(
                    onOpenSettings = { settingsOpened = true },
                    onChooseManualLocation = { manualLocationClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("location_disabled_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_disabled_icon").assertIsDisplayed()
        composeTestRule.onNodeWithText("Activa tu ubicación").assertIsDisplayed()

        composeTestRule.onNodeWithTag("location_open_settings_button").assertIsDisplayed().performClick()
        assertTrue("Expected onOpenSettings callback to be invoked", settingsOpened)

        composeTestRule.onNodeWithTag("location_choose_manual_button").assertIsDisplayed().performClick()
        assertTrue("Expected onChooseManualLocation callback to be invoked", manualLocationClicked)
    }

    @Test
    fun offlineBanner_rendersAndRetryClickWorks() {
        var retried = false
        composeTestRule.setContent {
            SensusTheme {
                OfflineBanner(
                    message = "Modo sin conexión",
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Modo sin conexión").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reintentar").assertIsDisplayed().performClick()
        assertTrue("Expected onRetry callback to be invoked from OfflineBanner", retried)
    }

    @Test
    fun shimmerPlaceholders_renderCleanlyInDarkAndLightTheme() {
        composeTestRule.setContent {
            SensusTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ShimmerOfferList()
                }
            }
        }

        composeTestRule.onNodeWithTag("shimmer_offer_list").assertIsDisplayed()

        composeTestRule.setContent {
            SensusTheme(darkTheme = false) {
                Box(modifier = Modifier.fillMaxSize()) {
                    ShimmerFeedList()
                }
            }
        }

        composeTestRule.onNodeWithTag("shimmer_feed_list").assertIsDisplayed()
    }
}
