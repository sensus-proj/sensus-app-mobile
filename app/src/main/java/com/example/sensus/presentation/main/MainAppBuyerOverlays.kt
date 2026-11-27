package com.example.sensus.presentation.main

import androidx.compose.runtime.Composable
import com.example.sensus.presentation.buyer.FavoritesCallbacks
import com.example.sensus.presentation.buyer.FavoritesScreen
import com.example.sensus.presentation.buyer.InterestsPickerCallbacks
import com.example.sensus.presentation.buyer.InterestsPickerScreen
import com.example.sensus.presentation.buyer.SavedInterestsCallbacks
import com.example.sensus.presentation.buyer.SavedInterestsScreen
import com.example.sensus.presentation.buyer.SearchOffersScreen

@Composable
internal fun RenderBuyerUtilityOverlay(state: MainAppState): Boolean {
    when {
        state.showFavorites -> RenderFavoritesOverlay(state)
        state.showSavedInterests -> RenderSavedInterestsOverlay(state)
        state.showInterestsPicker -> InterestsPickerScreen(
            isOnboarding = false,
            callbacks = InterestsPickerCallbacks(
                onCompleted = { state.showInterestsPicker = false },
                onNavigateBack = { state.showInterestsPicker = false },
                onRequireAuth = {
                    state.showInterestsPicker = false
                    state.showGuestPrompt = true
                }
            )
        )
        state.showSearchOffers -> SearchOffersScreen(
            onNavigateBack = { state.showSearchOffers = false },
            onOfferClick = { offer -> state.selectedProductForDetail = offer }
        )
        else -> return false
    }
    return true
}

@Composable
private fun RenderSavedInterestsOverlay(state: MainAppState) {
    SavedInterestsScreen(
        callbacks = SavedInterestsCallbacks(
            onNavigateBack = { state.showSavedInterests = false },
            onOfferClick = { offer -> state.selectedProductForDetail = offer },
            onExploreOffers = {
                state.showSavedInterests = false
                state.buyerTabIndex = 1
            }
        )
    )
}

@Composable
private fun RenderFavoritesOverlay(state: MainAppState) {
    FavoritesScreen(
        callbacks = FavoritesCallbacks(
            onNavigateBack = { state.showFavorites = false },
            onOfferClick = { offer -> state.selectedProductForDetail = offer },
            onExploreOffers = {
                state.showFavorites = false
                state.buyerTabIndex = 1
            },
            onRequireAuth = {
                state.showFavorites = false
                state.showGuestPrompt = true
            }
        )
    )
}
