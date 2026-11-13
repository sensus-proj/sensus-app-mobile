package com.example.sensus.data.mapper

import com.example.sensus.data.remote.dto.CreateOfferRequest
import com.example.sensus.data.remote.dto.OfferDto
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UploadedMedia
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"
private const val LABEL_PATTERN = "dd/MM HH:mm"
private const val DEFAULT_DISTANCE_METERS = 0
private val ISO_PARSE_PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX", ISO_UTC_PATTERN)

/** Mapeo entre los contratos remotos de ofertas (FT-15) y el dominio. */
object RemoteOfferMapper {

    fun CreateOfferInput.toRequest(media: UploadedMedia?): CreateOfferRequest = CreateOfferRequest(
        title = title,
        description = description,
        category = category.displayName,
        originalPrice = originalPrice,
        discountPrice = discountPrice,
        stock = stock,
        expiresAt = formatIsoUtc(expiresAtMillis),
        mediaId = media?.mediaId
    )

    fun OfferDto.toDomain(fallbackExpiresAtMillis: Long? = null, fallbackImageUrl: String? = null): ProductOffer {
        val expiresMillis = parseIsoMillis(expiresAt) ?: fallbackExpiresAtMillis
        val offer = ProductOffer(
            id = id,
            name = title,
            storeName = merchantName.orEmpty(),
            category = category.orEmpty(),
            originalPrice = originalPrice,
            discountPrice = discountPrice,
            discountPercent = OfferPublishingPolicy.discountPercent(originalPrice, discountPrice),
            unitsLeft = stock,
            distanceMeters = DEFAULT_DISTANCE_METERS,
            address = address.orEmpty(),
            description = description.orEmpty(),
            expirationDate = expiresMillis?.let(::formatExpirationLabel) ?: "Sin fecha límite",
            imageUrl = imageUrl ?: fallbackImageUrl,
            merchantId = merchantId
        )
        val lat = latitude
        val lng = longitude
        return if (lat != null && lng != null) offer.copy(latitude = lat, longitude = lng) else offer
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

    private fun utcFormat(pattern: String): SimpleDateFormat = SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
}
