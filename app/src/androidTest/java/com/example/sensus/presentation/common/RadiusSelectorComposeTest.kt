package com.example.sensus.presentation.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RadiusSelectorComposeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buyerCanChooseMetersInsteadOfKilometers() {
        var selected: Float? = null
        compose.setContent {
            RadiusSelectorBottomSheet(currentRadiusKm = 2f, onRadiusSelected = { selected = it }, onDismiss = {})
        }

        compose.onNodeWithText("Metros").performClick()
        compose.onNodeWithText("300 m").performClick()
        compose.onNodeWithText("Aplicar distancia").performClick()

        compose.runOnIdle { assertEquals(0.3f, selected) }
    }

    @Test
    fun guestSeesFixedRadiusWithoutEditableOptions() {
        compose.setContent {
            RadiusSelectorBottomSheet(currentRadiusKm = 0.3f, isGuest = true, onRadiusSelected = {}, onDismiss = {})
        }

        compose.onNodeWithText("Radio fijo de 2 km en modo invitado").assertExists()
        compose.onNodeWithText("Metros").assertDoesNotExist()
        compose.onNodeWithText("Aplicar distancia").assertDoesNotExist()
    }
}
