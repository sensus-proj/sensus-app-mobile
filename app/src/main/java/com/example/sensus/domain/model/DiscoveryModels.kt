package com.example.sensus.domain.model

private const val DEFAULT_STOCK_FALLBACK = 1
private const val PERCENT_SCALE = 100.0
private const val MAX_PERCENT = 99
private const val DEFAULT_SEARCH_PAGE_SIZE = 20

data class OfferSearchRequest(
    val query: String?,
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double? = null,
    val page: Int = 1,
    val pageSize: Int = DEFAULT_SEARCH_PAGE_SIZE
)

data class OfferSearchItem(
    val offerId: String,
    val title: String,
    val description: String,
    val categoryCode: String?,
    val categoryDisplayName: String,
    val imageUrl: String?,
    val originalPrice: Double,
    val finalPrice: Double,
    val stock: Int,
    val distanceMeters: Int,
    val expiresAtUtc: String,
    val merchantId: String?,
    val merchantName: String,
    val score: Double
) {
    fun toProductOffer(): ProductOffer {
        val discountPct = if (originalPrice > 0.0 && finalPrice < originalPrice) {
            (((originalPrice - finalPrice) / originalPrice) * PERCENT_SCALE).toInt().coerceIn(0, MAX_PERCENT)
        } else {
            0
        }
        return ProductOffer(
            id = offerId,
            name = title,
            storeName = merchantName,
            category = categoryDisplayName,
            originalPrice = originalPrice,
            discountPrice = finalPrice,
            discountPercent = discountPct,
            unitsLeft = stock.coerceAtLeast(DEFAULT_STOCK_FALLBACK),
            distanceMeters = distanceMeters,
            description = description,
            expirationDate = expiresAtUtc,
            imageUrl = imageUrl,
            merchantId = merchantId
        )
    }
}

data class OfferSearchPage(
    val items: List<OfferSearchItem>,
    val totalCount: Int,
    val pageNumber: Int,
    val pageSize: Int,
    val totalPages: Int,
    val isFromLocalFallback: Boolean = false
)

data class OfferAlternativeItem(
    val offerId: String,
    val title: String,
    val categoryCode: String,
    val categoryDisplayName: String,
    val imageUrl: String?,
    val originalPrice: Double,
    val finalPrice: Double,
    val stock: Int,
    val distanceMeters: Int,
    val expiresAtUtc: String,
    val merchantId: String?,
    val merchantName: String
) {
    fun toProductOffer(): ProductOffer {
        val discountPct = if (originalPrice > 0.0 && finalPrice < originalPrice) {
            (((originalPrice - finalPrice) / originalPrice) * PERCENT_SCALE).toInt().coerceIn(0, MAX_PERCENT)
        } else {
            0
        }
        return ProductOffer(
            id = offerId,
            name = title,
            storeName = merchantName,
            category = categoryDisplayName,
            originalPrice = originalPrice,
            discountPrice = finalPrice,
            discountPercent = discountPct,
            unitsLeft = stock.coerceAtLeast(DEFAULT_STOCK_FALLBACK),
            distanceMeters = distanceMeters,
            expirationDate = expiresAtUtc,
            imageUrl = imageUrl,
            merchantId = merchantId
        )
    }
}
