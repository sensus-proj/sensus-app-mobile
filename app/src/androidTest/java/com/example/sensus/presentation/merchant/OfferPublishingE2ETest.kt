package com.example.sensus.presentation.merchant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OfferPublishingE2ETest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun invalidPriceAppearsNextToOfferForm() {
        compose.setContent {
            SensusTheme {
                OfferFormCard(
                    NewOfferFormState(originalPrice = "50", discountPrice = "60"),
                    NewOfferErrors(priceError = "El precio de oferta debe ser menor que el original"),
                    {}
                )
            }
        }
        compose.onNodeWithText("El precio de oferta debe ser menor que el original").assertExists()
    }

    @Test
    fun moderationDialogBlocksAndCanBeAcknowledged() {
        var dismissed = false
        compose.setContent { SensusTheme { ModerationAlertDialog("Imagen rechazada") { dismissed = true } } }
        compose.onNodeWithText("Imagen rechazada").assertExists()
        compose.onNodeWithText("Entendido").performClick()
        assertTrue(dismissed)
    }
}
