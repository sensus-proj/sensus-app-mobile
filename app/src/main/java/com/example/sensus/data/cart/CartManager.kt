package com.example.sensus.data.cart

import com.example.sensus.data.CartItem
import com.example.sensus.data.ProductOffer
import com.example.sensus.data.StoreCart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.example.sensus.domain.model.AddToCartResult

object CartManager {
    private val _cart = MutableStateFlow(StoreCart())
    val cart: StateFlow<StoreCart> = _cart.asStateFlow()

    fun addToCart(offer: ProductOffer, quantity: Int = 1): AddToCartResult {
        val current = _cart.value
        if (!current.isEmpty && current.storeName != null && !current.storeName.equals(offer.storeName, ignoreCase = true)) {
            return AddToCartResult.StoreConflict(current.storeName, offer.storeName)
        }

        val existingIndex = current.items.indexOfFirst { it.offer.id == offer.id }
        val updatedItems = if (existingIndex >= 0) {
            val oldItem = current.items[existingIndex]
            val newQty = (oldItem.quantity + quantity).coerceAtMost(offer.unitsLeft)
            current.items.toMutableList().apply {
                this[existingIndex] = oldItem.copy(quantity = newQty)
            }
        } else {
            current.items + CartItem(offer, quantity.coerceAtMost(offer.unitsLeft))
        }

        _cart.value = StoreCart(
            storeName = offer.storeName,
            storeAddress = offer.address,
            latitude = offer.latitude,
            longitude = offer.longitude,
            items = updatedItems
        )
        return AddToCartResult.Success
    }

    fun replaceCart(offer: ProductOffer, quantity: Int = 1) {
        _cart.value = StoreCart(
            storeName = offer.storeName,
            storeAddress = offer.address,
            latitude = offer.latitude,
            longitude = offer.longitude,
            items = listOf(CartItem(offer, quantity.coerceAtMost(offer.unitsLeft)))
        )
    }

    fun updateQuantity(offerId: String, newQuantity: Int) {
        val current = _cart.value
        if (newQuantity <= 0) {
            removeItem(offerId)
            return
        }
        val updatedItems = current.items.map { item ->
            if (item.offer.id == offerId) {
                item.copy(quantity = newQuantity.coerceAtMost(item.offer.unitsLeft))
            } else {
                item
            }
        }
        _cart.value = if (updatedItems.isEmpty()) StoreCart() else current.copy(items = updatedItems)
    }

    fun removeItem(offerId: String) {
        val current = _cart.value
        val updatedItems = current.items.filterNot { it.offer.id == offerId }
        _cart.value = if (updatedItems.isEmpty()) StoreCart() else current.copy(items = updatedItems)
    }

    fun clearCart() {
        _cart.value = StoreCart()
    }
}
