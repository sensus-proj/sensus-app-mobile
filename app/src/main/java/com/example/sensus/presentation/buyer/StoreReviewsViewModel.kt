package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.StoreReview
import com.example.sensus.domain.usecase.CreateStoreReviewUseCase
import com.example.sensus.domain.usecase.GetStoreReviewsUseCase
import com.example.sensus.domain.usecase.ReplyStoreReviewUseCase
import com.example.sensus.domain.usecase.ToggleStoreReviewLikeUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

private const val DEFAULT_STARS = 5

@HiltViewModel
class StoreReviewsViewModel @Inject constructor(
    private val getReviews: GetStoreReviewsUseCase,
    private val createReview: CreateStoreReviewUseCase,
    private val toggleReviewLike: ToggleStoreReviewLikeUseCase,
    private val replyReview: ReplyStoreReviewUseCase
) : MviViewModel<StoreReviewsViewState, StoreReviewsViewIntent, StoreReviewsViewEffect>(StoreReviewsViewState()) {

    override fun processIntent(intent: StoreReviewsViewIntent) {
        when (intent) {
            is StoreReviewsViewIntent.Load -> loadReviews(intent)
            is StoreReviewsViewIntent.SubmitReview -> submitReview(intent.merchantId)
            is StoreReviewsViewIntent.ToggleLikeReview -> toggleLike(intent.reviewId)
            is StoreReviewsViewIntent.SubmitReply -> submitReply(intent.reviewId)
            else -> handleFormIntent(intent)
        }
    }

    private fun handleFormIntent(intent: StoreReviewsViewIntent) {
        when (intent) {
            is StoreReviewsViewIntent.SelectRating -> updateState { copy(draftRating = intent.stars) }
            is StoreReviewsViewIntent.CommentChanged -> updateState { copy(draftComment = intent.text, errorMessage = null) }
            is StoreReviewsViewIntent.OpenReply -> updateState { copy(replyingToReviewId = intent.reviewId, replyDraft = "", errorMessage = null) }
            is StoreReviewsViewIntent.ReplyDraftChanged -> updateState { copy(replyDraft = intent.text, errorMessage = null) }
            StoreReviewsViewIntent.ClearError -> updateState { copy(errorMessage = null) }
            else -> Unit
        }
    }

    private fun loadReviews(intent: StoreReviewsViewIntent.Load) {
        updateState {
            copy(
                storeName = intent.storeName,
                storeAddress = intent.storeAddress,
                storePhotoUrl = intent.storePhotoUrl,
                isLoading = true,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            when (val res = getReviews(intent.merchantId)) {
                is Result.Success -> updateState { copy(isLoading = false, summary = res.data) }
                is Result.Failure -> updateState { copy(isLoading = false, errorMessage = res.error.message) }
            }
        }
    }

    private fun submitReview(merchantId: String) {
        val state = viewState.value
        if (state.isSubmitting) return
        updateState { copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = createReview(merchantId, state.draftRating, state.draftComment)) {
                is Result.Success -> {
                    updateState {
                        copy(
                            isSubmitting = false,
                            summary = res.data,
                            draftRating = DEFAULT_STARS,
                            draftComment = ""
                        )
                    }
                    emitEffect(StoreReviewsViewEffect.ShowMessage("¡Opinión publicada!"))
                }
                is Result.Failure -> handleFailure(res.error) { copy(isSubmitting = false, errorMessage = it) }
            }
        }
    }

    private fun toggleLike(reviewId: String) {
        viewModelScope.launch {
            when (val res = toggleReviewLike(reviewId)) {
                is Result.Success -> updateReviewInSummary(reviewId) { review ->
                    review.copy(likesCount = res.data.likesCount, isLikedByMe = res.data.isLikedByMe)
                }
                is Result.Failure -> handleFailure(res.error) { copy(errorMessage = it) }
            }
        }
    }

    private fun submitReply(reviewId: String) {
        val state = viewState.value
        if (state.isSendingReply) return
        updateState { copy(isSendingReply = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = replyReview(reviewId, state.replyDraft)) {
                is Result.Success -> {
                    updateReviewInSummary(reviewId) { res.data }
                    updateState { copy(isSendingReply = false, replyingToReviewId = null, replyDraft = "") }
                    emitEffect(StoreReviewsViewEffect.ShowMessage("Respuesta publicada"))
                }
                is Result.Failure -> handleFailure(res.error) { copy(isSendingReply = false, errorMessage = it) }
            }
        }
    }

    private fun handleFailure(error: AppError, onError: StoreReviewsViewState.(String) -> StoreReviewsViewState) {
        if (error is AppError.Unauthorized) {
            updateState { onError(error.message) }
            emitEffect(StoreReviewsViewEffect.RequireAuth)
        } else {
            updateState { onError(error.message) }
        }
    }

    private fun updateReviewInSummary(reviewId: String, transform: (StoreReview) -> StoreReview) {
        val currentSummary = viewState.value.summary ?: return
        val updated = currentSummary.reviews.map { if (it.id == reviewId) transform(it) else it }
        updateState { copy(summary = currentSummary.copy(reviews = updated)) }
    }
}
