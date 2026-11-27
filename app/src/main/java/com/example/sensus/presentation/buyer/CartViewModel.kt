package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.usecase.PrepareCartClaimUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val prepareClaim: PrepareCartClaimUseCase
) : MviViewModel<CartViewState, CartViewIntent, CartViewEffect>(CartViewState()) {

    init {
        viewModelScope.launch {
            cartRepository.cart.collect { cart ->
                updateState { copy(storeCart = cart, checkoutError = null) }
            }
        }
    }

    override fun processIntent(intent: CartViewIntent) {
        when (intent) {
            is CartViewIntent.AddOffer -> cartRepository.addToCart(intent.offer, intent.quantity)
            is CartViewIntent.UpdateQuantity -> cartRepository.updateQuantity(intent.offerId, intent.newQuantity)
            is CartViewIntent.RemoveOffer -> cartRepository.removeItem(intent.offerId)
            is CartViewIntent.ClearCart -> cartRepository.clearCart()
            is CartViewIntent.CheckoutApartado -> {
                when (val result = prepareClaim(cartRepository.cart.value)) {
                    is Result.Success -> emitEffect(CartViewEffect.OpenClaim(result.data.first().offer, result.data))
                    is Result.Failure -> updateState { copy(checkoutError = result.error) }
                }
            }
        }
    }
}
