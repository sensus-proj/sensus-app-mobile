package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class CartViewState(
    val storeCart: StoreCart = StoreCart(),
    val checkoutError: AppError? = null
) : ViewState

sealed interface CartViewIntent : ViewIntent {
    data class AddOffer(val offer: ProductOffer, val quantity: Int = 1) : CartViewIntent
    data class UpdateQuantity(val offerId: String, val newQuantity: Int) : CartViewIntent
    data class RemoveOffer(val offerId: String) : CartViewIntent
    data object ClearCart : CartViewIntent
    data object CheckoutApartado : CartViewIntent
}

sealed interface CartViewEffect : ViewEffect {
    data class OpenClaim(val offer: ProductOffer) : CartViewEffect
}
