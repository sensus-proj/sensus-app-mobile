package com.example.sensus.data.mapper

import com.example.sensus.data.local.db.entity.OfferEntity
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import kotlin.math.roundToInt

object OfferMapper {

    fun OfferEntity.toDomain(defaultDistanceMeters: Int = 350): ProductOffer {
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
            unitsLeft = unitsLeft,
            distanceMeters = defaultDistanceMeters,
            latitude = latitude,
            longitude = longitude,
            description = description ?: "",
            imageUrl = imageUrl,
            expirationDate = expirationDate ?: "Sin fecha límite",
            merchantId = merchantId,
            status = OfferLifecycleStatus.fromBackend(status, unitsLeft),
            version = version.coerceAtLeast(1),
            expiresAtMillis = expiresAtMillis.takeIf { it > 0L }
        )
    }

    fun ProductOffer.toEntity(merchantId: String? = this.merchantId, cachedAt: Long = System.currentTimeMillis()): OfferEntity {
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
        ).also { entity ->
            entity.unitsLeft = unitsLeft
            entity.expirationDate = expirationDate
            entity.status = status.backendValue
            entity.version = version.coerceAtLeast(1)
            entity.expiresAtMillis = expiresAtMillis ?: 0L
        }
    }
}
