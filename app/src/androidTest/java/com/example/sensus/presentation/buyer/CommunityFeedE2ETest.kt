package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CommunityFeedE2ETest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun feedRendersNearbyPost_andOpensTaggedMerchant() {
        var selected = ""
        val post = FeedPost("p1", "Vecina", "Pan disponible", "2026-11-12", merchantId = "shop-1", merchantName = "Panadería local")
        compose.setContent {
            SensusTheme { CommunityFeedContent(CommunityViewState(posts = listOf(post)), {}, { selected = it }) }
        }
        compose.onNodeWithText("Pan disponible").assertExists()
        compose.onNodeWithText("Panadería local").performClick()
        assertEquals("shop-1", selected)
    }

    @Test
    fun composerSendsTextAndRetainsModerationError() {
        var text by mutableStateOf("")
        var submitted = false
        compose.setContent {
            SensusTheme {
                CreateFeedPostSheet(CommunityViewState(draftText = text, errorMessage = "Contenido bloqueado")) { intent ->
                    when (intent) {
                        is CommunityViewIntent.TextChanged -> text = intent.text
                        CommunityViewIntent.Submit -> submitted = true
                        else -> Unit
                    }
                }
            }
        }
        compose.onNodeWithText("Comparte con tu comunidad").performTextInput("Oferta vecinal")
        compose.onNodeWithText("Contenido bloqueado").assertExists()
        compose.onNodeWithText("Publicar").performClick()
        assertTrue(submitted)
        assertEquals("Oferta vecinal", text)
    }

    @Test
    fun newPostAndRefreshDispatchExpectedIntents() {
        val intents = mutableListOf<CommunityViewIntent>()
        compose.setContent { SensusTheme { CommunityFeedContent(CommunityViewState(), { intents.add(it) }, {}) } }
        compose.onNodeWithText("Nueva publicación").performClick()
        compose.onNodeWithText("Actualizar comunidad").performScrollTo().performClick()
        assertEquals(listOf(CommunityViewIntent.NewPost, CommunityViewIntent.Refresh), intents)
    }
}
