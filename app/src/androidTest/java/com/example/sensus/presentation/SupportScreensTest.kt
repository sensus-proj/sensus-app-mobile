package com.example.sensus.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.sensus.presentation.auth.TermsScreen
import com.example.sensus.presentation.common.GuestModeBannerIfVisible
import com.example.sensus.presentation.support.FaqCatalog
import com.example.sensus.presentation.support.HelpFaqScreen
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SupportScreensTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun helpExpandsTheOfflineAnswer() {
        val question = FaqCatalog.offlineItems.first()
        compose.setContent { SensusTheme { HelpFaqScreen(onNavigateBack = {}) } }

        compose.onNodeWithText(question.answer).assertDoesNotExist()
        compose.onNodeWithText(question.question).performClick()
        compose.onNodeWithText(question.answer).assertExists()
    }

    @Test
    fun guestBannerRequestsAccountAccess() {
        var clicks = 0
        val visible = mutableStateOf(false)
        compose.setContent { SensusTheme { GuestModeBannerIfVisible(visible.value, onRegisterClick = { clicks++ }) } }

        compose.onNodeWithText("Modo invitado · 2 km").assertDoesNotExist()
        compose.runOnIdle { visible.value = true }

        compose.onNodeWithText("Modo invitado · 2 km").assertExists()
        compose.onNodeWithText("Crear cuenta").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun termsShowFeedPrivacyWithoutOpeningTheWeb() {
        var webVisits = 0
        compose.setContent {
            SensusTheme { TermsScreen(onTermsAccepted = {}, onPrivacyPolicyClick = { webVisits++ }) }
        }

        compose.onNodeWithText("Intereses y personalización del feed").performScrollTo().assertExists()
        compose.onNodeWithText("180 días", substring = true).assertExists()
        compose.onNodeWithText("Tus derechos ARCO").performScrollTo().assertExists()
        compose.onNodeWithText("Eliminación de cuenta").performScrollTo().assertExists()
        compose.runOnIdle { assertEquals(0, webVisits) }
    }
}
