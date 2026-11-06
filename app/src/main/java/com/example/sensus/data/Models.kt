package com.example.sensus.data

import com.example.sensus.R
import java.util.Locale

enum class StoreCategory(val displayName: String) {
    ALL("Todo"),
    GROCERY("Abarrotes"),
    CLEANING("Limpieza y Hogar"),
    PERISHABLES("Perecederos"),
    DAIRY("Lácteos"),
    FOOD("Comida"),
    BAKERY("Panadería"),
    BEVERAGES("Bebidas"),
    PETS("Mascotas"),
    PHARMACY("Farmacia")
}

data class ProductOffer(
    val id: String,
    val name: String,
    val storeName: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val discountPercent: Int,
    val unitsLeft: Int,
    val distanceMeters: Int,
    val timeToCollectMinutes: Int = 45,
    val rating: Double = 4.8,
    val reviewCount: Int = 120,
    val address: String = "Av. Juárez 120, Centro",
    val couponCode: String = "SNS-A7K9-24",
    val isVerified: Boolean = true,
    val latitude: Double = 20.0545,
    val longitude: Double = -99.3420,
    val postalCode: String = "",
    val description: String = "Excedente de inventario en perfecto estado con empaque sellado de fábrica. Rescate de merma garantizado.",
    val expirationDate: String = "Hoy 20:00 hrs",
    val imageUrl: String? = null
) {
    val formattedDistance: String
        get() = if (distanceMeters < 1000) "${distanceMeters} m" else String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)

    val savedAmount: Double
        get() = originalPrice - discountPrice
}

data class CartItem(
    val offer: ProductOffer,
    val quantity: Int
) {
    val totalOriginalPrice: Double
        get() = offer.originalPrice * quantity

    val totalDiscountPrice: Double
        get() = offer.discountPrice * quantity

    val totalSavedAmount: Double
        get() = totalOriginalPrice - totalDiscountPrice
}

data class StoreCart(
    val storeName: String? = null,
    val storeAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val items: List<CartItem> = emptyList()
) {
    val totalItemsCount: Int
        get() = items.sumOf { it.quantity }

    val totalOriginalPrice: Double
        get() = items.sumOf { it.totalOriginalPrice }

    val totalDiscountPrice: Double
        get() = items.sumOf { it.totalDiscountPrice }

    val totalSavedAmount: Double
        get() = totalOriginalPrice - totalDiscountPrice

    val isEmpty: Boolean
        get() = items.isEmpty()
}

data class ApartadoOrder(
    val id: String,
    val storeName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val items: List<CartItem>,
    val couponCode: String,
    val totalOriginalPrice: Double,
    val totalDiscountPrice: Double,
    val totalSavedAmount: Double,
    val timeToCollectMinutes: Int = 45
) {
    companion object {
        fun fromSingleOffer(offer: ProductOffer, quantity: Int): ApartadoOrder {
            val item = CartItem(offer, quantity)
            val coupon = if (quantity > 1) {
                "SNS-${quantity}X-${offer.couponCode.takeLast(4)}"
            } else {
                offer.couponCode
            }
            return ApartadoOrder(
                id = "order_${System.currentTimeMillis()}",
                storeName = offer.storeName,
                address = offer.address,
                latitude = offer.latitude,
                longitude = offer.longitude,
                items = listOf(item),
                couponCode = coupon,
                totalOriginalPrice = item.totalOriginalPrice,
                totalDiscountPrice = item.totalDiscountPrice,
                totalSavedAmount = item.totalSavedAmount,
                timeToCollectMinutes = offer.timeToCollectMinutes
            )
        }

        fun fromCart(cart: StoreCart): ApartadoOrder {
            require(!cart.isEmpty) { "El carrito no puede estar vacío" }
            val firstOffer = cart.items.first().offer
            val randomSuffix = (1000..9999).random()
            return ApartadoOrder(
                id = "order_${System.currentTimeMillis()}",
                storeName = cart.storeName ?: firstOffer.storeName,
                address = cart.storeAddress ?: firstOffer.address,
                latitude = cart.latitude ?: firstOffer.latitude,
                longitude = cart.longitude ?: firstOffer.longitude,
                items = cart.items,
                couponCode = "SNS-CART-$randomSuffix",
                totalOriginalPrice = cart.totalOriginalPrice,
                totalDiscountPrice = cart.totalDiscountPrice,
                totalSavedAmount = cart.totalSavedAmount,
                timeToCollectMinutes = 45
            )
        }
    }
}

data class MerchantStore(
    val name: String = "Bodega A. - Centro",
    val category: String = "Abarrotes y Limpieza",
    val isOpen: Boolean = true,
    val rating: Double = 4.9,
    val reviewCount: Int = 120,
    val activeOffersCount: Int = 12,
    val recoveredMoneyToday: Double = 480.0,
    val redeemedTodayCount: Int = 12,
    val address: String = "Dirección: Av. Universidad 412, Tula de Allende, Hgo.",
    val schedule: String = "Horarios: Lun - Sáb 09:00 - 20:00",
    val phone: String = "+52 55 1234 5678",
    val latitude: Double = 20.0545,
    val longitude: Double = -99.3420,
    val postalCode: String = ""
)

data class ChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val text: String,
    val comboTitle: String? = null,
    val comboPrice: Double? = null,
    val comboItems: List<String> = emptyList()
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val category: String, // "Ofertas", "Reservas", "Seguridad"
    val actionText: String? = null
)

data class FriendShare(
    val name: String,
    val distance: String,
    val activeOffersCount: Int,
    val timeAgo: String
)
