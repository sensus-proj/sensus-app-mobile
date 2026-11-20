package com.example.sensus.domain.model

import java.util.Locale

private const val DEFAULT_COLLECT_MINUTES = 45
private const val DEFAULT_RATING = 4.8
private const val DEFAULT_MERCHANT_RATING = 4.9
private const val DEFAULT_REVIEW_COUNT = 120
private const val DEFAULT_LAT = 20.0536
private const val DEFAULT_LNG = -99.3436
private const val METERS_PER_KM = 1000
private const val METERS_PER_KM_DOUBLE = 1000.0
private const val COUPON_SUFFIX_LENGTH = 4
private const val RANDOM_CART_MIN = 1000
private const val RANDOM_CART_MAX = 9999
private const val DEFAULT_ACTIVE_OFFERS = 0
private const val DEFAULT_RECOVERED_TODAY = 0.0
private const val DEFAULT_REDEEMED_TODAY = 0

enum class StoreCategory(val displayName: String, val apiCode: String?) {
    ALL("Todo", null),
    GROCERY("Abarrotes", "Grocery"),
    CLEANING("Limpieza y Hogar", "CleaningHome"),
    PERISHABLES("Perecederos", "Perishables"),
    DAIRY("Lácteos", "Dairy"),
    FOOD("Comida", "Food"),
    BAKERY("Panadería", "Bakery"),
    BEVERAGES("Bebidas", "Beverages"),
    PETS("Mascotas", "Pets"),
    PHARMACY("Farmacia", "Pharmacy");

    companion object {
        private val byCode = entries.mapNotNull { entry ->
            entry.apiCode?.lowercase(Locale.ROOT)?.let { it to entry }
        }.toMap()

        fun fromApiCode(raw: String?): StoreCategory? = raw?.trim()?.takeIf { it.isNotEmpty() }?.lowercase(Locale.ROOT)?.let { byCode[it] }

        fun displayNameFor(raw: String?, fallback: String = "Todo"): String {
            val trimmed = raw?.trim().orEmpty()
            return if (trimmed.isEmpty()) fallback else fromApiCode(trimmed)?.displayName ?: trimmed
        }
    }
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
    val timeToCollectMinutes: Int = DEFAULT_COLLECT_MINUTES,
    val rating: Double = DEFAULT_RATING,
    val reviewCount: Int = DEFAULT_REVIEW_COUNT,
    val address: String = "Av. Juárez 120, Centro",
    val couponCode: String = "SNS-A7K9-24",
    val isVerified: Boolean = true,
    val latitude: Double = DEFAULT_LAT,
    val longitude: Double = DEFAULT_LNG,
    val postalCode: String = "",
    val description: String =
        "Excedente de inventario en perfecto estado con empaque sellado de fábrica. Calidad y ahorro garantizados.",
    val expirationDate: String = "Hoy 20:00 hrs",
    val imageUrl: String? = null,
    val merchantId: String? = null,
    val status: OfferLifecycleStatus = OfferLifecycleStatus.ACTIVE,
    val version: Int = 1,
    val expiresAtMillis: Long? = null
) {
    val isPubliclyVisible: Boolean
        get() = status == OfferLifecycleStatus.ACTIVE && unitsLeft > 0

    val formattedDistance: String
        get() = if (distanceMeters < METERS_PER_KM) {
            "$distanceMeters m"
        } else {
            String.format(Locale.US, "%.1f km", distanceMeters / METERS_PER_KM_DOUBLE)
        }

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
    val timeToCollectMinutes: Int = DEFAULT_COLLECT_MINUTES
) {
    companion object {
        fun fromSingleOffer(offer: ProductOffer, quantity: Int): ApartadoOrder {
            val item = CartItem(offer, quantity)
            val coupon = if (quantity > 1) {
                "SNS-${quantity}X-${offer.couponCode.takeLast(COUPON_SUFFIX_LENGTH)}"
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
            val randomSuffix = (RANDOM_CART_MIN..RANDOM_CART_MAX).random()
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
                timeToCollectMinutes = DEFAULT_COLLECT_MINUTES
            )
        }
    }
}

data class MerchantStore(
    val name: String = "Mi Comercio",
    val category: String = "Comercio Local",
    val isOpen: Boolean = true,
    val rating: Double = DEFAULT_MERCHANT_RATING,
    val reviewCount: Int = DEFAULT_REVIEW_COUNT,
    val activeOffersCount: Int = DEFAULT_ACTIVE_OFFERS,
    val recoveredMoneyToday: Double = DEFAULT_RECOVERED_TODAY,
    val redeemedTodayCount: Int = DEFAULT_REDEEMED_TODAY,
    val address: String = "",
    val schedule: String = "Lun - Sáb 09:00 - 20:00",
    val phone: String = "",
    val latitude: Double = DEFAULT_LAT,
    val longitude: Double = DEFAULT_LNG,
    val postalCode: String = "",
    val photoUrl: String? = null
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeAgo: String,
    val category: String,
    val actionText: String? = null
)

data class FriendShare(
    val name: String,
    val distance: String,
    val activeOffersCount: Int,
    val timeAgo: String
)

sealed interface AddToCartResult {
    data object Success : AddToCartResult
    data class StoreConflict(val currentStoreName: String, val newStoreName: String) : AddToCartResult
}

sealed interface ModerationResult {
    data object Approved : ModerationResult
    data class Rejected(val reason: String) : ModerationResult
    data class Quarantine(val reason: String, val scores: Map<String, Double> = emptyMap()) : ModerationResult
}
