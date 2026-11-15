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
import com.example.sensus.R
import com.example.sensus.common.AppError
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
                CreateFeedPostSheet(CommunityViewState(draftText = text, publishError = AppError.ContentRejected("Contenido bloqueado"))) { intent ->
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

    @Test
    fun unavailableFeedDoesNotClaimThereAreNoPostsOrRetryPaginationAutomatically() {
        val intents = mutableListOf<CommunityViewIntent>()
        val post = FeedPost("p1", "Vecina", "Pan disponible", "2026-11-12")
        compose.setContent {
            SensusTheme {
                CommunityFeedContent(
                    CommunityViewState(posts = listOf(post), hasMore = true, hasLoadedFeed = true, feedError = AppError.NotFound()),
                    { intents.add(it) },
                    {}
                )
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.community_feed_unavailable)).assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.community_empty)).assertDoesNotExist()
        compose.runOnIdle { assertTrue(intents.isEmpty()) }
    }

    @Test
    fun failedInitialLoadDoesNotShowEmptyFeedButConfirmedEmptyLoadDoes() {
        var state by mutableStateOf(CommunityViewState(feedError = AppError.NotFound()))
        compose.setContent { SensusTheme { CommunityFeedContent(state, {}, {}) } }
        val emptyText = compose.activity.getString(R.string.community_empty)
        compose.onNodeWithText(emptyText).assertDoesNotExist()
        compose.runOnIdle { state = CommunityViewState(hasLoadedFeed = true) }
        compose.onNodeWithText(emptyText).assertExists()
    }

    @Test
    fun composerShowsOnlyItsOwnFailureAndRetainsText() {
        var publishError by mutableStateOf<AppError?>(null)
        compose.setContent {
            SensusTheme {
                CreateFeedPostSheet(
                    CommunityViewState(draftText = "Hola comunidad", feedError = AppError.Network("Fallo solo de lectura"), publishError = publishError)
                ) {}
            }
        }
        compose.onNodeWithText("Fallo solo de lectura").assertDoesNotExist()
        compose.runOnIdle { publishError = AppError.NotFound() }
        compose.onNodeWithText(compose.activity.getString(R.string.community_publish_unavailable)).assertExists()
        compose.onNodeWithText("Hola comunidad").assertExists()
    }

    @Test
    fun publishFailureDoesNotAppearAsFeedFailure() {
        compose.setContent {
            SensusTheme {
                CommunityFeedContent(CommunityViewState(hasLoadedFeed = true, publishError = AppError.Network("Fallo solo de publicar")), {}, {})
            }
        }
        compose.onNodeWithText("Fallo solo de publicar").assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.community_empty)).assertExists()
    }
}
