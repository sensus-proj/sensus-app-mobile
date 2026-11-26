package com.example.sensus.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.InterestCatalog
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.presentation.auth.TermsScreen
import com.example.sensus.presentation.buyer.InterestsPickerContent
import com.example.sensus.presentation.buyer.InterestsPickerUiActions
import com.example.sensus.presentation.buyer.InterestsPickerViewState
import com.example.sensus.presentation.buyer.OfferFeedCallbacks
import com.example.sensus.presentation.buyer.OfferFeedContent
import com.example.sensus.presentation.buyer.OfferFeedViewState
import com.example.sensus.presentation.buyer.SearchOffersContent
import com.example.sensus.presentation.buyer.SearchOffersViewState
import com.example.sensus.presentation.common.GuestModeBanner
import com.example.sensus.presentation.support.FaqCatalog
import com.example.sensus.presentation.support.HelpFaqCallbacks
import com.example.sensus.presentation.support.HelpFaqContent
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Ft30Ft20Ft23ComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun offerFeedAndInterestsPicker_renderStatesCleanly() {
        composeTestRule.setContent {
            SensusTheme {
                OfferFeedContent(
                    state = OfferFeedViewState(
                        items = listOf(
                            OfferFeedItem(
                                offerId = "1",
                                title = "Bolsa sorpresa",
                                categoryCode = "Bakery",
                                categoryDisplayName = "Panadería",
                                imageUrl = null,
                                originalPrice = 100.0,
                                finalPrice = 50.0,
                                stock = 4,
                                distanceMeters = 250,
                                expiresAtUtc = "2026-11-21",
                                merchantId = "m1",
                                merchantName = "La Espiga",
                                slot = FeedSlot.RANKED
                            )
                        )
                    ),
                    callbacks = OfferFeedCallbacks(),
                    onIntent = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Promociones para ti").assertExists()
        composeTestRule.onNodeWithText("Bolsa sorpresa").assertExists()

        composeTestRule.setContent {
            SensusTheme {
                InterestsPickerContent(
                    state = InterestsPickerViewState(availableCategories = InterestCatalog.selectableCategories, selectedCodes = listOf("Bakery")),
                    isOnboarding = true,
                    actions = InterestsPickerUiActions(onToggle = {}, onSave = {}, onSkipOrBack = {})
                )
            }
        }
        composeTestRule.onNodeWithText("Guardar").assertExists()
        composeTestRule.onNodeWithText("Omitir").assertExists()
    }

    @Test
    fun searchOffersAndSupportAndGuestBanner_renderExpectedCopyAndToggleAccordion() {
        composeTestRule.setContent {
            SensusTheme {
                SearchOffersContent(
                    state = SearchOffersViewState(
                        query = "Croissant",
                        lastCompletedQuery = "Croissant",
                        hasSearched = true,
                        alternatives = listOf(
                            OfferAlternativeItem("a1", "Bolillos", "Bakery", "Panadería", null, 30.0, 20.0, 2, 150, "2026-11-21", "m2", "Horno")
                        )
                    ),
                    onNavigateBack = {},
                    onIntent = {},
                    onOfferClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("No encontramos Croissant, pero te recomendamos estas ofertas similares cerca de ti").assertExists()

        composeTestRule.setContent {
            SensusTheme {
                HelpFaqContent(
                    items = FaqCatalog.offlineItems,
                    callbacks = HelpFaqCallbacks(onNavigateBack = {}, onContactSupport = {}, onOpenPrivacyWeb = {}),
                    initialExpandedId = null
                )
            }
        }
        val privacyQuestion = "¿Cómo usa Sensus mis intereses e interacciones en el feed?"
        composeTestRule.onNodeWithText(privacyQuestion).performClick()
        composeTestRule.onNodeWithText("180 días", substring = true).assertExists()

        var clicked = false
        composeTestRule.setContent {
            SensusTheme {
                GuestModeBanner(onRegisterClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Crear cuenta").performClick()
        assertTrue(clicked)

        composeTestRule.setContent {
            SensusTheme { TermsScreen(onTermsAccepted = {}) }
        }
        composeTestRule.onNodeWithText("Intereses y personalización del feed").assertExists()
    }
}
