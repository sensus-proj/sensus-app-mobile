package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class CommunityViewState(
    val friends: List<com.example.sensus.domain.model.FriendShare> = emptyList(),
    val posts: List<FeedPost> = emptyList(),
    val currentUserName: String = "Usuario Sensus",
    val isGuest: Boolean = true,
    val isLoading: Boolean = false,
    val isPublishing: Boolean = false,
    val isCached: Boolean = false,
    val hasMore: Boolean = false,
    val nextPage: Int = 1,
    val errorMessage: String? = null,
    val showComposer: Boolean = false,
    val draftText: String = "",
    val merchantId: String? = null,
    val hasImage: Boolean = false,
    val nearbyMerchants: List<ProductOffer> = emptyList()
) : ViewState

sealed interface CommunityViewIntent : ViewIntent {
    data object Refresh : CommunityViewIntent
    data object LoadMore : CommunityViewIntent
    data object NewPost : CommunityViewIntent
    data object CloseComposer : CommunityViewIntent
    data class TextChanged(val text: String) : CommunityViewIntent
    data class MerchantSelected(val id: String?) : CommunityViewIntent
    class ImageSelected(val bytes: ByteArray?) : CommunityViewIntent
    data object ImageReadFailed : CommunityViewIntent
    data object Submit : CommunityViewIntent
}

sealed interface CommunityViewEffect : ViewEffect {
    data object RequireAuth : CommunityViewEffect
}
