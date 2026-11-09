package com.example.sensus.domain.repository

import com.example.sensus.domain.model.AddToCartResult
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCart
import kotlinx.coroutines.flow.StateFlow

interface CartRepository {
    val cart: StateFlow<StoreCart>
    fun addToCart(offer: ProductOffer, quantity: Int = 1): AddToCartResult
    fun replaceCart(offer: ProductOffer, quantity: Int = 1)
    fun updateQuantity(offerId: String, newQuantity: Int)
    fun removeItem(offerId: String)
    fun clearCart()
    fun createApartadoOrder(): ApartadoOrder?
}
