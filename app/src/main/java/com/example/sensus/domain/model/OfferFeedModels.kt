package com.example.sensus.domain.model

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val HALF_DIVISOR = 2.0
private const val DEFAULT_STOCK_FALLBACK = 1
private const val PERCENT_SCALE = 100.0
private const val MAX_DISCOUNT_PERCENT = 99
private const val DEFAULT_FEED_RADIUS_KM = 10
private const val DEFAULT_FEED_PAGE_SIZE = 20

enum class FeedSlot {
    RANKED,
    EXPLORATION;

    companion object {
        fun fromApi(raw: String?): FeedSlot = if (raw.equals("exploration", ignoreCase = true)) EXPLORATION else RANKED
    }
}

data class OfferFeedRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Int = DEFAULT_FEED_RADIUS_KM,
    val page: Int = 1,
    val pageSize: Int = DEFAULT_FEED_PAGE_SIZE,
    val snapshotId: String? = null,
    val forceRefresh: Boolean = false
)

data class OfferFeedItem(
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
    val merchantName: String,
    val slot: FeedSlot
) {
    fun toProductOffer(): ProductOffer {
        val discountPct = if (originalPrice > 0.0 && finalPrice < originalPrice) {
            (((originalPrice - finalPrice) / originalPrice) * PERCENT_SCALE).toInt().coerceIn(0, MAX_DISCOUNT_PERCENT)
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

data class OfferFeedPage(
    val items: List<OfferFeedItem>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int,
    val snapshotId: String?,
    val rankingVersion: String,
    val isCached: Boolean = false
)

data class FeedLocationPoint(
    val latitude: Double,
    val longitude: Double
)

object InterestCatalog {
    const val MAX_SELECTED = 5

    val selectableCategories: List<StoreCategory> =
        StoreCategory.entries.filter { it.apiCode != null }

    fun normalizeAndValidate(codes: List<String>): List<String>? {
        val canonical = LinkedHashSet<String>()
        for (raw in codes) {
            val category = StoreCategory.fromApiCode(raw) ?: return null
            val apiCode = category.apiCode ?: return null
            canonical.add(apiCode)
        }
        return if (canonical.size <= MAX_SELECTED) canonical.toList() else null
    }
}

internal fun haversineMeters(a: FeedLocationPoint, b: FeedLocationPoint): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLng = Math.toRadians(b.longitude - a.longitude)
    val lat1 = Math.toRadians(a.latitude)
    val lat2 = Math.toRadians(b.latitude)
    val sinDLat = kotlin.math.sin(dLat / HALF_DIVISOR)
    val sinDLng = kotlin.math.sin(dLng / HALF_DIVISOR)
    val h = (sinDLat * sinDLat) + (kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinDLng * sinDLng)
    val c = HALF_DIVISOR * kotlin.math.asin(kotlin.math.min(1.0, kotlin.math.sqrt(h)))
    return EARTH_RADIUS_METERS * c
}
