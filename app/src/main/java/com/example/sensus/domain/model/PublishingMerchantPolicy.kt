package com.example.sensus.domain.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object PublishingMerchantPolicy {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    fun nearby(offers: List<ProductOffer>, location: UserLocation, radiusMeters: Int): List<ProductOffer> = offers
        .filter { !it.merchantId.isNullOrBlank() }
        .filter { distanceMeters(it, location) <= radiusMeters }
        .distinctBy { it.merchantId }

    private fun distanceMeters(offer: ProductOffer, location: UserLocation): Double {
        val lat = Math.toRadians(offer.latitude - location.latitude)
        val lng = Math.toRadians(offer.longitude - location.longitude)
        val a = sin(lat / 2).pow(2) + cos(Math.toRadians(location.latitude)) *
            cos(Math.toRadians(offer.latitude)) * sin(lng / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
