package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.CreateOfferRequest
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.data.remote.dto.UpdateOfferRequestDto
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
private const val LABEL_PATTERN = "dd/MM HH:mm"
private const val DEFAULT_DISTANCE_METERS = 0
private const val MIN_BACKEND_DESC_LEN = 5
private const val MIN_DISCOUNT_PCT = 1
private const val MAX_DISCOUNT_PCT = 90
private const val ONE_SECOND_MILLIS = 1000L
private const val CLOCK_SKEW_BUFFER_MS = 60_000L
private val ISO_PARSE_PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", ISO_UTC_PATTERN)

/** Mapeo entre los contratos remotos de ofertas (FT-15 / Spec 007 / Spec 034-037) y el dominio. */
object RemoteOfferMapper {

    fun CreateOfferInput.toRequest(media: UploadedMedia?): CreateOfferRequest {
        val nowMillis = (System.currentTimeMillis() - CLOCK_SKEW_BUFFER_MS).coerceAtMost(expiresAtMillis - ONE_SECOND_MILLIS)
        val endsIso = formatIsoUtc(expiresAtMillis)
        val startsIso = formatIsoUtc(nowMillis)
        val safeDescription = normalizeDescription(description, title)
        val pct = OfferPublishingPolicy.discountPercent(originalPrice, discountPrice)
            .coerceIn(MIN_DISCOUNT_PCT, MAX_DISCOUNT_PCT)
        return CreateOfferRequest(
            title = title,
            description = safeDescription,
            category = category.displayName,
            originalPrice = originalPrice,
            discountPrice = discountPrice,
            discountPercentage = pct,
            stock = stock,
            startsAt = startsIso,
            endsAt = endsIso,
            expiresAt = endsIso,
            imageUrl = media?.url,
            mediaId = media?.mediaId
        )
    }

    fun UpdateOfferInput.toRequest(): UpdateOfferRequestDto {
        val nowMillis = (System.currentTimeMillis() - CLOCK_SKEW_BUFFER_MS).coerceAtMost(expiresAtMillis - ONE_SECOND_MILLIS)
        val pct = OfferPublishingPolicy.discountPercent(originalPrice, discountPrice)
            .coerceIn(MIN_DISCOUNT_PCT, MAX_DISCOUNT_PCT)
        return UpdateOfferRequestDto(
            title = title.trim(),
            description = normalizeDescription(description, title),
            originalPrice = originalPrice,
            discountPercentage = pct,
            stock = stock,
            startsAt = formatIsoUtc(nowMillis),
            endsAt = formatIsoUtc(expiresAtMillis),
            imageUrl = imageUrl,
            expectedVersion = expectedVersion.coerceAtLeast(1)
        )
    }

    fun OfferDto.toDomain(fallbackExpiresAtMillis: Long? = null, fallbackImageUrl: String? = null): ProductOffer {
        val expiresMillis = resolveOfferExpirationMillis(fallbackExpiresAtMillis)
        val resolvedDiscountPrice = resolveOfferDiscountPrice()
        val resolvedDiscountPct = discountPercentage
            ?: OfferPublishingPolicy.discountPercent(originalPrice, resolvedDiscountPrice)
        val resolvedStatus = OfferLifecycleStatus.fromBackend(status, stock)
        val offer = ProductOffer(
            id = id,
            name = title,
            storeName = merchantName.orEmpty().ifBlank { "Comercio Sensus" },
            category = category.orEmpty().ifBlank { "Todo" },
            originalPrice = originalPrice,
            discountPrice = resolvedDiscountPrice,
            discountPercent = resolvedDiscountPct,
            unitsLeft = stock,
            distanceMeters = distanceMeters?.toInt() ?: DEFAULT_DISTANCE_METERS,
            address = address.orEmpty(),
            description = description.orEmpty(),
            expirationDate = expiresMillis?.let(::formatExpirationLabel) ?: "Sin fecha límite",
            imageUrl = imageUrl ?: fallbackImageUrl,
            merchantId = merchantId,
            status = resolvedStatus,
            version = version.coerceAtLeast(1),
            expiresAtMillis = expiresMillis,
            likesCount = likesCount,
            isLikedByMe = isLikedByMe
        )
        return applyOfferCoordinates(offer)
    }

    fun formatIsoUtc(millis: Long): String = utcFormat(ISO_UTC_PATTERN).format(Date(millis))

    fun formatExpirationLabel(millis: Long): String = SimpleDateFormat(LABEL_PATTERN, Locale.US).format(Date(millis)) + " hrs"

    fun parseIsoMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return ISO_PARSE_PATTERNS.firstNotNullOfOrNull { pattern ->
            try {
                utcFormat(pattern).parse(raw)?.time
            } catch (_: ParseException) {
                null
            }
        }
    }
}

private fun normalizeDescription(description: String, title: String): String = description.trim().takeIf { it.length >= MIN_BACKEND_DESC_LEN }
    ?: "Producto en promoción disponible en tienda: ${title.trim()}"

private fun OfferDto.resolveOfferExpirationMillis(fallbackExpiresAtMillis: Long?): Long? {
    val rawExpires = endsAt?.takeIf { it.isNotBlank() } ?: expiresAt
    return RemoteOfferMapper.parseIsoMillis(rawExpires) ?: fallbackExpiresAtMillis
}

private fun OfferDto.resolveOfferDiscountPrice(): Double = discountedPrice ?: discountPrice ?: originalPrice

private fun OfferDto.applyOfferCoordinates(offer: ProductOffer): ProductOffer {
    val lat = latitude ?: return offer
    val lng = longitude ?: return offer
    return offer.copy(latitude = lat, longitude = lng)
}

private fun utcFormat(pattern: String): SimpleDateFormat = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
