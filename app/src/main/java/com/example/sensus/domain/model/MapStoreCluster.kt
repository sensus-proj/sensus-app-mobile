package com.example.sensus.domain.model

data class MapStoreCluster(
    val storeKey: String,
    val merchantId: String?,
    val storeName: String,
    val address: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val merchantProfile: MerchantProfile?,
    val matchingOffers: List<ProductOffer>,
    val allStoreOffers: List<ProductOffer>,
    val initialCategory: StoreCategory = StoreCategory.ALL
) {
    val featuredOffer: ProductOffer?
        get() = matchingOffers.minByOrNull { it.discountPrice } ?: allStoreOffers.minByOrNull { it.discountPrice }

    val formattedDistance: String
        get() = if (distanceMeters < METERS_PER_KM) {
            "$distanceMeters m"
        } else {
            String.format(java.util.Locale.US, "%.1f km", distanceMeters / METERS_PER_KM_DOUBLE)
        }
}

private const val METERS_PER_KM = 1000
private const val METERS_PER_KM_DOUBLE = 1000.0
