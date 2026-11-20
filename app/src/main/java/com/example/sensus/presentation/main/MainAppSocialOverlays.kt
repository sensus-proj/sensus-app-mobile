package com.example.sensus.presentation.main

import androidx.compose.runtime.Composable
import com.example.sensus.presentation.buyer.FriendSharedOffersActions
import com.example.sensus.presentation.buyer.FriendSharedOffersScreen
import com.example.sensus.presentation.buyer.StoreCatalogCallbacks
import com.example.sensus.presentation.buyer.StoreCatalogScreen
import com.example.sensus.presentation.buyer.StoreReviewsScreen

@Composable
internal fun renderStoreAndFriendOverlays(state: MainAppState): Boolean {
    val storeReviews = state.selectedStoreForReviews
    val storeCatalog = state.selectedStoreCatalog
    val friend = state.selectedFriendForOffers
    return when {
        storeReviews != null -> {
            StoreReviewsScreen(
                cluster = storeReviews,
                onNavigateBack = { state.selectedStoreForReviews = null },
                onRequireAuth = { state.showGuestPrompt = true }
            )
            true
        }
        storeCatalog != null -> {
            StoreCatalogScreen(
                cluster = storeCatalog,
                callbacks = StoreCatalogCallbacks(
                    onNavigateBack = { state.selectedStoreCatalog = null },
                    onProductClick = { state.selectedProductForDetail = it },
                    onRequireAuth = { state.showGuestPrompt = true },
                    onStoreHeaderClick = { clicked ->
                        if (!clicked.merchantId.isNullOrBlank()) {
                            state.selectedStoreForReviews = clicked
                        }
                    }
                )
            )
            true
        }
        friend != null -> {
            FriendSharedOffersScreen(
                friend = friend,
                actions = FriendSharedOffersActions(
                    onNavigateBack = { state.selectedFriendForOffers = null },
                    onOfferClick = { state.selectedProductForDetail = it },
                    onExploreOffers = {
                        state.selectedFriendForOffers = null
                        state.buyerTabIndex = 1
                    }
                )
            )
            true
        }
        else -> false
    }
}
