package com.example.sensus.data.mapper

import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.domain.model.ProductOffer
import kotlin.math.roundToInt

object OfferMapper {

    fun OfferEntity.toDomain(defaultUnitsLeft: Int = 5, defaultDistanceMeters: Int = 350): ProductOffer {
        val computedDiscountPercent = if (originalPrice > 0.0 && originalPrice >= price) {
            (((originalPrice - price) / originalPrice) * 100.0).roundToInt()
        } else {
            0
        }
        return ProductOffer(
            id = id,
            name = title ?: "",
            storeName = merchantName ?: "",
            category = category ?: "",
            originalPrice = originalPrice,
            discountPrice = price,
            discountPercent = computedDiscountPercent,
            unitsLeft = defaultUnitsLeft,
            distanceMeters = defaultDistanceMeters,
            latitude = latitude,
            longitude = longitude,
            description = description ?: "",
            imageUrl = imageUrl
        )
    }

    fun ProductOffer.toEntity(merchantId: String = storeName, cachedAt: Long = System.currentTimeMillis()): OfferEntity {
        return OfferEntity(
            id,
            name,
            description,
            discountPrice,
            originalPrice,
            category,
            imageUrl,
            latitude,
            longitude,
            merchantId,
            storeName,
            cachedAt
        )
    }
}
