package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : MviViewModel<CartViewState, CartViewIntent, CartViewEffect>(CartViewState()) {

    init {
        viewModelScope.launch {
            cartRepository.cart.collect { cart ->
                updateState { copy(storeCart = cart) }
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
                val order = cartRepository.createApartadoOrder()
                if (order != null) {
                    updateState { copy(activeOrder = order) }
                    emitEffect(CartViewEffect.OrderCreated(order))
                }
            }
        }
    }
}
