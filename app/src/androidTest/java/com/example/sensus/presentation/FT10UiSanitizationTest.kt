package com.example.sensus.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.auth.OtpVerificationScreen
import com.example.sensus.presentation.auth.PRIVACY_POLICY_URL
import com.example.sensus.presentation.auth.TermsScreen
import com.example.sensus.presentation.buyer.FriendsCommunityScreen
import com.example.sensus.theme.SensusTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FT10UiSanitizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun termsScreen_rendersPrivacyPolicyLinkAndTriggersCallback() {
        var privacyClicked = false
        assertEquals("https://rodnix.com.mx/sensus/privacidad.html", PRIVACY_POLICY_URL)

        composeTestRule.setContent {
            SensusTheme {
                TermsScreen(
                    onTermsAccepted = {},
                    onPrivacyPolicyClick = { privacyClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Consultar Aviso de Privacidad")
            .assertExists()
            .performClick()

        assertTrue(privacyClicked)
    }

    @Test
    fun communityScreen_omitsWhatsAppAndRendersGenericInvite() {
        composeTestRule.setContent {
            SensusTheme {
                FriendsCommunityScreen(friendsList = FakeStoreRepository().getSampleFriends())
            }
        }

        composeTestRule
            .onAllNodesWithText("WhatsApp", substring = true, ignoreCase = true)
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("Compartir invitación").assertExists()
        composeTestRule.onNodeWithText("Compartir").assertExists()
    }

    @Test
    fun otpVerificationScreen_startsEmptyWithoutHardcodedCode_andEnablesAfterFourDigits() {
        var verified = false

        composeTestRule.setContent {
            SensusTheme {
                OtpVerificationScreen(
                    onVerifySuccess = { verified = true },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("7294").assertDoesNotExist()
        composeTestRule.onNodeWithText("Verificar y Continuar").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("otp_code_input").performTextInput("1234")
        composeTestRule.onNodeWithText("Verificar y Continuar")
            .assertIsEnabled()
            .performClick()

        assertTrue(verified)
    }

    private class FakeStoreRepository : StoreRepository {
        override fun getSampleStore(): MerchantStore = MerchantStore(
            name = "Tienda Test",
            category = "Abarrotes",
            address = "Centro",
            phone = "5500000000",
            schedule = "08:00 - 20:00",
            rating = 4.8,
            reviewCount = 10,
            isOpen = true,
            activeOffersCount = 2,
            redeemedTodayCount = 1
        )

        override fun getSampleNotifications() = emptyList<com.example.sensus.domain.model.NotificationItem>()
        override fun getSampleFriends(): List<FriendShare> = emptyList()
        override fun getSampleChatMessages(): List<ChatMessage> = emptyList()
    }

    private class FakeSessionRepository : SessionRepository {
        override val sessionState: StateFlow<SessionState> = MutableStateFlow(
            SessionState(isSessionActive = true, activeName = "Usuario Sensus")
        )

        override suspend fun saveGuestSession(token: String, maxRadiusMeters: Int) = Unit
        override suspend fun clearSession() = Unit
        override suspend fun setTermsAccepted(accepted: Boolean) = Unit
        override suspend fun setLocationPermission(granted: Boolean) = Unit
        override suspend fun setSplashCompleted(completed: Boolean) = Unit
    }
}
