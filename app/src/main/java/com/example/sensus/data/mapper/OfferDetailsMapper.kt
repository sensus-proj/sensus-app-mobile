package com.example.sensus.data.mapper

import com.example.sensus.data.mapper.RemoteOfferMapper.toDomain
import com.example.sensus.data.remote.dto.OfferDetailsDto
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.domain.model.OfferDetails
import java.time.Instant

object OfferDetailsMapper {
    fun OfferDetailsDto.toDomain(): OfferDetails {
        val startsMillis = Instant.parse(startsAt).toEpochMilli()
        val endsMillis = Instant.parse(endsAt).toEpochMilli()
        val offer = OfferDto(
            id = id,
            title = title,
            description = description,
            category = category,
            originalPrice = originalPrice,
            discountedPrice = discountedPrice,
            discountPercentage = discountPercentage,
            stock = stock,
            startsAt = startsAt,
            endsAt = endsAt,
            status = status,
            imageUrl = imageUrl,
            merchantId = merchantId,
            merchantName = merchantName,
            address = merchantAddress,
            latitude = latitude,
            longitude = longitude
        ).toDomain()
        val publicOffer = offer.copy(
            couponCode = "",
            rating = 0.0,
            reviewCount = 0,
            isVerified = false,
            latitude = latitude ?: Double.NaN,
            longitude = longitude ?: Double.NaN,
            expiresAtMillis = endsMillis,
            expirationDate = RemoteOfferMapper.formatExpirationLabel(endsMillis)
        )
        return OfferDetails(publicOffer, viewCount, hasActiveClaim, startsMillis)
    }
}
