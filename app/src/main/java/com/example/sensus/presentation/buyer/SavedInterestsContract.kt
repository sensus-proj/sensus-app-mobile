package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

enum class SavedInterestFilterTab {
    ALL,
    OFFERS,
    POSTS
}

data class SavedInterestsViewState(
    val savedOffers: List<ProductOffer> = emptyList(),
    val savedPosts: List<FeedPost> = emptyList(),
    val selectedTab: SavedInterestFilterTab = SavedInterestFilterTab.ALL
) : ViewState {
    val savedOfferIds: Set<String>
        get() = savedOffers.mapTo(mutableSetOf()) { it.id }

    val savedPostIds: Set<String>
        get() = savedPosts.mapTo(mutableSetOf()) { it.id }

    val totalCount: Int
        get() = savedOffers.size + savedPosts.size
}

sealed interface SavedInterestsViewIntent : ViewIntent {
    data class ToggleOffer(val offer: ProductOffer) : SavedInterestsViewIntent
    data class RemoveOffer(val offerId: String) : SavedInterestsViewIntent
    data class TogglePost(val post: FeedPost) : SavedInterestsViewIntent
    data class RemovePost(val postId: String) : SavedInterestsViewIntent
    data class SelectTab(val tab: SavedInterestFilterTab) : SavedInterestsViewIntent
}

sealed interface SavedInterestsViewEffect : ViewEffect {
    data class SavedStateChanged(val isSaved: Boolean) : SavedInterestsViewEffect
    data object RequireAuth : SavedInterestsViewEffect
}
