package com.example.sensus.data.mapper

import com.example.sensus.data.local.db.entity.FavoriteEntity
import com.example.sensus.data.remote.dto.FavoriteOfferDto
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import java.time.Instant

internal object FavoriteMapper {
    fun FavoriteOfferDto.toDomain(): FavoriteOffer {
        val endsAtMillis = RemoteOfferMapper.parseIsoMillis(endsAt)
        val expirationLabel = endsAtMillis?.let(RemoteOfferMapper::formatExpirationLabel) ?: endsAt.ifBlank { "Vigencia por confirmar" }
        val offer = ProductOffer(
            id = id,
            name = title,
            storeName = merchantName.ifBlank { "Comercio Sensus" },
            category = StoreCategory.displayNameFor(category),
            originalPrice = originalPrice,
            discountPrice = discountedPrice,
            discountPercent = discountPercentage,
            unitsLeft = stock.coerceAtLeast(0),
            distanceMeters = 0,
            description = description,
            expirationDate = expirationLabel,
            imageUrl = imageUrl,
            merchantId = merchantId.ifBlank { null },
            expiresAtMillis = endsAtMillis,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            likesCount = likesCount
        )
        return FavoriteOffer(offer = offer, favoritedAtUtc = favoritedAtUtc, likesCount = likesCount)
    }

    fun FavoriteEntity.toDomain(): FavoriteOffer {
        val resolvedExpiresAt = expiresAtMillis.takeIf { it > 0L } ?: RemoteOfferMapper.parseIsoMillis(endsAtUtc)
        val expirationLabel = resolvedExpiresAt?.let(RemoteOfferMapper::formatExpirationLabel) ?: endsAtUtc.ifBlank { "Vigencia por confirmar" }
        val offer = ProductOffer(
            id = offerId,
            name = title,
            storeName = merchantName.ifBlank { "Comercio Sensus" },
            category = StoreCategory.displayNameFor(category),
            originalPrice = originalPrice,
            discountPrice = discountedPrice,
            discountPercent = discountPercentage,
            unitsLeft = stock.coerceAtLeast(0),
            distanceMeters = 0,
            description = description,
            expirationDate = expirationLabel,
            imageUrl = imageUrl,
            merchantId = merchantId.ifBlank { null },
            expiresAtMillis = resolvedExpiresAt,
            latitude = latitude ?: 0.0,
            longitude = longitude ?: 0.0,
            likesCount = likesCount
        )
        return FavoriteOffer(offer = offer, favoritedAtUtc = favoritedAtUtc, likesCount = likesCount)
    }

    fun FavoriteOffer.toEntity(ownerKey: String, nowMillis: Long): FavoriteEntity {
        val endsAtMillis = offer.expiresAtMillis ?: 0L
        val endsAtUtc = if (endsAtMillis > 0L) RemoteOfferMapper.formatIsoUtc(endsAtMillis) else offer.expirationDate
        return FavoriteEntity(
            offer.id,
            ownerKey,
            offer.merchantId.orEmpty(),
            offer.storeName,
            StoreCategory.fromApiCode(offer.category)?.apiCode ?: offer.category,
            offer.name,
            offer.description,
            offer.originalPrice,
            offer.discountPercent,
            offer.discountPrice,
            offer.unitsLeft.coerceAtLeast(0),
            offer.imageUrl,
            "",
            endsAtUtc,
            endsAtMillis,
            offer.latitude.takeIf { it.isFinite() },
            offer.longitude.takeIf { it.isFinite() },
            likesCount.coerceAtLeast(offer.likesCount),
            favoritedAtUtc.ifBlank { Instant.ofEpochMilli(nowMillis).toString() },
            nowMillis
        )
    }
}
