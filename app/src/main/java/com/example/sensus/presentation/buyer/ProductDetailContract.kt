package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class ProductDetailViewState(
    val details: OfferDetails? = null,
    val alternatives: List<OfferAlternativeItem> = emptyList(),
    val isLoading: Boolean = false,
    val quantity: Int = 1,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isTogglingLike: Boolean = false,
    val error: AppError? = null
) : ViewState

sealed interface ProductDetailViewIntent : ViewIntent {
    data class Load(val offerId: String, val initialLikesCount: Int = 0, val initialLiked: Boolean = false) : ProductDetailViewIntent
    data class ChangeQuantity(val quantity: Int) : ProductDetailViewIntent
    data class ToggleLike(val offerId: String) : ProductDetailViewIntent
}

sealed interface ProductDetailViewEffect : ViewEffect {
    data object RequireAuth : ProductDetailViewEffect
    data class ShowMessage(val message: String) : ProductDetailViewEffect
}
