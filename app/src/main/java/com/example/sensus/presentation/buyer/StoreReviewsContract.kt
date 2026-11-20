package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.StoreReviewsSummary
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

private const val DEFAULT_DRAFT_RATING = 5

data class StoreReviewsViewState(
    val storeName: String = "",
    val storeAddress: String = "",
    val storePhotoUrl: String? = null,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val summary: StoreReviewsSummary? = null,
    val draftRating: Int = DEFAULT_DRAFT_RATING,
    val draftComment: String = "",
    val replyingToReviewId: String? = null,
    val replyDraft: String = "",
    val isSendingReply: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface StoreReviewsViewIntent : ViewIntent {
    data class Load(
        val merchantId: String,
        val storeName: String,
        val storeAddress: String,
        val storePhotoUrl: String?
    ) : StoreReviewsViewIntent
    data class SelectRating(val stars: Int) : StoreReviewsViewIntent
    data class CommentChanged(val text: String) : StoreReviewsViewIntent
    data class SubmitReview(val merchantId: String) : StoreReviewsViewIntent
    data class ToggleLikeReview(val reviewId: String) : StoreReviewsViewIntent
    data class OpenReply(val reviewId: String?) : StoreReviewsViewIntent
    data class ReplyDraftChanged(val text: String) : StoreReviewsViewIntent
    data class SubmitReply(val reviewId: String) : StoreReviewsViewIntent
    data object ClearError : StoreReviewsViewIntent
}

sealed interface StoreReviewsViewEffect : ViewEffect {
    data object RequireAuth : StoreReviewsViewEffect
    data class ShowMessage(val message: String) : StoreReviewsViewEffect
}
