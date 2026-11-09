package com.example.sensus.data.repository

import com.example.sensus.data.cart.CartManager
import com.example.sensus.domain.model.AddToCartResult
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import com.example.sensus.domain.repository.CartRepository
import kotlinx.coroutines.flow.StateFlow

class CartRepositoryImpl : CartRepository {
    override val cart: StateFlow<StoreCart> = CartManager.cart

    override fun addToCart(offer: ProductOffer, quantity: Int): AddToCartResult {
        return CartManager.addToCart(offer, quantity)
    }

    override fun replaceCart(offer: ProductOffer, quantity: Int) {
        CartManager.replaceCart(offer, quantity)
    }

    override fun updateQuantity(offerId: String, newQuantity: Int) {
        CartManager.updateQuantity(offerId, newQuantity)
    }

    override fun removeItem(offerId: String) {
        CartManager.removeItem(offerId)
    }

    override fun clearCart() {
        CartManager.clearCart()
    }

    override fun createApartadoOrder(): ApartadoOrder? {
        val current = cart.value
        return if (!current.isEmpty) ApartadoOrder.fromCart(current) else null
    }
}
