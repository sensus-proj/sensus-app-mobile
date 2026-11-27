package com.example.sensus.presentation.buyer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.sensus.R
import com.example.sensus.domain.model.FavoriteFilter
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.theme.SensusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoritesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val sampleOffer = ProductOffer(
        id = "fav-ui-1",
        name = "Canasta de verduras",
        storeName = "Mercado Verde",
        category = "Perecederos",
        originalPrice = 120.0,
        discountPrice = 65.0,
        discountPercent = 45,
        unitsLeft = 3,
        distanceMeters = 180
    )

    @Test
    fun filterChipsAndFavoriteHeartDispatchExpectedIntents() {
        val intents = mutableListOf<FavoritesViewIntent>()
        compose.setContent {
            SensusTheme {
                FavoritesContent(
                    state = FavoritesViewState(favorites = listOf(FavoriteOffer(sampleOffer))),
                    callbacks = FavoritesCallbacks(),
                    onIntent = { intents.add(it) }
                )
            }
        }

        compose.onNodeWithText(compose.activity.getString(R.string.favorites_filter_available)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.favorites_remove_cd)).performClick()

        compose.runOnIdle {
            assertEquals(
                listOf(
                    FavoritesViewIntent.SelectFilter(FavoriteFilter.AVAILABLE_ONLY),
                    FavoritesViewIntent.ToggleFavorite(sampleOffer)
                ),
                intents
            )
        }
    }

    @Test
    fun emptyStateOffersExploreActionAndProductCardSupportsHeartMode() {
        var explored = 0
        var toggledHeart = 0
        compose.setContent {
            SensusTheme {
                FavoritesContent(
                    state = FavoritesViewState(),
                    callbacks = FavoritesCallbacks(onExploreOffers = { explored++ }),
                    onIntent = {}
                )
            }
        }
        compose.onNodeWithText(compose.activity.getString(R.string.favorites_explore)).performClick()
        compose.runOnIdle { assertEquals(1, explored) }

        compose.setContent {
            SensusTheme {
                FavoriteProductOfferCard(
                    offer = sampleOffer,
                    isFavorite = false,
                    onClick = {},
                    onFavoriteClick = { toggledHeart++ }
                )
            }
        }
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.favorites_add_cd)).performClick()
        compose.runOnIdle { assertEquals(1, toggledHeart) }
    }
}
