package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.GetOfferAlternativesUseCase
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.domain.usecase.ToggleOfferLikeUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getDetail: GetOfferDetailUseCase,
    private val registerView: RegisterOfferViewUseCase,
    private val getAlternatives: GetOfferAlternativesUseCase? = null,
    private val toggleLikeUseCase: ToggleOfferLikeUseCase? = null
) : MviViewModel<ProductDetailViewState, ProductDetailViewIntent, ProductDetailViewEffect>(ProductDetailViewState()) {
    private var loadJob: Job? = null
    private val viewedOffers = mutableSetOf<String>()

    override fun processIntent(intent: ProductDetailViewIntent) {
        when (intent) {
            is ProductDetailViewIntent.Load -> load(intent.offerId, intent.initialLikesCount, intent.initialLiked)
            is ProductDetailViewIntent.ChangeQuantity -> updateState {
                copy(quantity = intent.quantity.coerceIn(1, (details?.offer?.unitsLeft ?: 1).coerceAtLeast(1)))
            }
            is ProductDetailViewIntent.ToggleLike -> toggleLike(intent.offerId)
        }
    }

    private fun load(offerId: String, initialLikesCount: Int, initialLiked: Boolean) {
        loadJob?.cancel()
        updateState {
            copy(
                isLoading = true,
                details = null,
                alternatives = emptyList(),
                quantity = 1,
                likesCount = initialLikesCount,
                isLiked = initialLiked,
                error = null
            )
        }
        loadJob = viewModelScope.launch {
            when (val result = getDetail(offerId)) {
                is Result.Success -> {
                    updateState {
                        copy(
                            isLoading = false,
                            details = result.data,
                            likesCount = result.data.likesCount,
                            isLiked = result.data.isLikedByMe
                        )
                    }
                    if (viewedOffers.add(offerId)) {
                        viewModelScope.launch { registerView(offerId) }
                    }
                    if (result.data.offer.unitsLeft <= 0 && getAlternatives != null) {
                        val altRes = getAlternatives(offerId)
                        if (altRes is Result.Success) {
                            updateState { copy(alternatives = altRes.data) }
                        }
                    }
                }
                is Result.Failure -> updateState { copy(isLoading = false, error = result.error) }
            }
        }
    }

    private fun toggleLike(offerId: String) {
        val useCase = toggleLikeUseCase ?: return
        val current = viewState.value
        if (current.isTogglingLike) return
        val previousLiked = current.isLiked
        val previousCount = current.likesCount
        val optimisticLiked = !previousLiked
        val optimisticCount = (previousCount + if (optimisticLiked) 1 else -1).coerceAtLeast(0)

        updateState {
            copy(isTogglingLike = true, isLiked = optimisticLiked, likesCount = optimisticCount)
        }
        viewModelScope.launch {
            when (val result = useCase(offerId)) {
                is Result.Success -> updateState {
                    copy(
                        isTogglingLike = false,
                        isLiked = result.data.isLikedByMe,
                        likesCount = result.data.likesCount
                    )
                }
                is Result.Failure -> {
                    updateState {
                        copy(isTogglingLike = false, isLiked = previousLiked, likesCount = previousCount)
                    }
                    if (result.error is AppError.Unauthorized) {
                        emitEffect(ProductDetailViewEffect.RequireAuth)
                    } else {
                        emitEffect(ProductDetailViewEffect.ShowMessage(result.error.message))
                    }
                }
            }
        }
    }
}
