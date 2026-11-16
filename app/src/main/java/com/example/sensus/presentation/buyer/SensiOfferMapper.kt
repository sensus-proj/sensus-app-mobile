package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.ProductOffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val METERS_PER_KM = 1000
private const val PERCENT_BASE = 100.0
private const val MAX_OFFER_CARDS = 5

internal fun formatOfferDay(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale("es", "MX")).format(Date(millis))

/**
 * Ofertas que se muestran como tarjeta: solo las que el texto de la respuesta menciona por título, con un tope.
 * El backend ya filtra así (FR-046-18); esta defensa evita decenas de tarjetas con versiones anteriores del API.
 */
internal fun visibleOfferCards(reply: String, offers: List<CitedOffer>, max: Int = MAX_OFFER_CARDS): List<CitedOffer> =
    offers
        .filter { it.title.isNotBlank() && reply.contains(it.title.trim(), ignoreCase = true) }
        .take(max)

/**
 * Convierte la oferta citada en el modelo que ya usan el detalle, el apartado y la ruta. Las coordenadas solo
 * se asignan si el backend las envió; sin ellas no se ofrece "Cómo llegar".
 */
internal fun CitedOffer.toProductOffer(): ProductOffer {
    val percent = discountPercent ?: if (regularPriceMxn > 0.0 && regularPriceMxn >= offerPriceMxn) {
        (((regularPriceMxn - offerPriceMxn) / regularPriceMxn) * PERCENT_BASE).roundToInt()
    } else {
        0
    }
    val base = ProductOffer(
        id = offerId,
        name = title,
        storeName = merchantName.ifBlank { "Comercio Sensus" },
        category = category.orEmpty().ifBlank { "Todo" },
        originalPrice = regularPriceMxn,
        discountPrice = offerPriceMxn,
        discountPercent = percent,
        unitsLeft = availableStock,
        distanceMeters = distanceKm?.let { (it * METERS_PER_KM).roundToInt() } ?: 0,
        address = merchantAddress.orEmpty(),
        description = description.orEmpty(),
        expirationDate = endsAtMillis?.let { "${formatOfferDay(it)} hrs" } ?: "Sin fecha límite",
        imageUrl = imageUrl,
        merchantId = merchantId.ifBlank { null },
        expiresAtMillis = endsAtMillis
    )
    return if (latitude != null && longitude != null) base.copy(latitude = latitude, longitude = longitude) else base
}
