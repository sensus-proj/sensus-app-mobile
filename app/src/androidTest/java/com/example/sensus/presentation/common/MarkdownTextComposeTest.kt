package com.example.sensus.presentation.common

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.theme.SensusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** FT-27 / FT-24: las respuestas de Sensi se pintan sin los marcadores Markdown. */
@RunWith(AndroidJUnit4::class)
class MarkdownTextComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun boldAndBullets_areRenderedWithoutMarkdownMarkers() {
        composeTestRule.setContent {
            SensusTheme {
                MarkdownText(text = "**Producto:** Teclado Gaming\n- Precio: *150*\n1. Apartalo", color = Color.Black)
            }
        }

        composeTestRule.onNodeWithText("Producto: Teclado Gaming").assertExists()
        composeTestRule.onNodeWithText("Precio: 150").assertExists()
        composeTestRule.onNodeWithText("Apartalo").assertExists()
        composeTestRule.onNodeWithText("**", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("*150*", substring = true).assertDoesNotExist()
    }

    @Test
    fun unclosedMarkers_remainLiteralText() {
        composeTestRule.setContent {
            SensusTheme { MarkdownText(text = "2 ** 3 y *a", color = Color.Black) }
        }

        composeTestRule.onNodeWithText("2 ** 3 y *a").assertExists()
    }
}
