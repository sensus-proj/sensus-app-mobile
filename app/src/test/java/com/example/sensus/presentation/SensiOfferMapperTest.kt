package com.example.sensus.presentation

import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.presentation.buyer.toProductOffer
import com.example.sensus.presentation.buyer.visibleOfferCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensiOfferMapperTest {

    private fun cited(
        latitude: Double? = null,
        longitude: Double? = null,
        discountPercent: Int? = null,
        endsAtMillis: Long? = null,
        distanceKm: Double? = null
    ) = CitedOffer(
        offerId = "off_1",
        title = "Teclado Gaming",
        merchantId = "m_1",
        merchantName = "Tienda 3B",
        offerPriceMxn = 150.0,
        regularPriceMxn = 200.0,
        availableStock = 1,
        isOpenNow = true,
        distanceKm = distanceKm,
        description = "Teclado con RGB",
        discountPercent = discountPercent,
        imageUrl = "https://img/teclado.jpg",
        category = "Tecnologia",
        endsAtMillis = endsAtMillis,
        merchantAddress = "Calle Falsa 123",
        latitude = latitude,
        longitude = longitude
    )

    @Test
    fun toProductOffer_copiesAllCardDataIntoTheExistingOfferModel() {
        val offer = cited(latitude = 19.43, longitude = -99.13, discountPercent = 25, distanceKm = 0.4)
            .toProductOffer()

        assertEquals("off_1", offer.id)
        assertEquals("Teclado Gaming", offer.name)
        assertEquals("Tienda 3B", offer.storeName)
        assertEquals("Tecnologia", offer.category)
        assertEquals(200.0, offer.originalPrice, 0.0)
        assertEquals(150.0, offer.discountPrice, 0.0)
        assertEquals(25, offer.discountPercent)
        assertEquals(1, offer.unitsLeft)
        assertEquals(400, offer.distanceMeters)
        assertEquals("Calle Falsa 123", offer.address)
        assertEquals("Teclado con RGB", offer.description)
        assertEquals("https://img/teclado.jpg", offer.imageUrl)
        assertEquals("m_1", offer.merchantId)
        assertEquals(19.43, offer.latitude, 0.0)
        assertEquals(-99.13, offer.longitude, 0.0)
    }

    @Test
    fun toProductOffer_computesDiscountWhenBackendDoesNotSendIt() {
        assertEquals(25, cited().toProductOffer().discountPercent)
    }

    @Test
    fun toProductOffer_withoutCoordinates_keepsDefaultsAndTheCardHidesRoute() {
        val withoutCoords = cited()
        val offer = withoutCoords.toProductOffer()

        assertNull(withoutCoords.latitude)
        assertFalse(withoutCoords.latitude != null && withoutCoords.longitude != null)
        assertEquals(com.example.sensus.domain.model.ProductOffer("x", "x", "x", "x", 0.0, 0.0, 0, 0, 0).latitude, offer.latitude, 0.0)
    }

    @Test
    fun toProductOffer_withExpiry_setsMillisAndLabel() {
        val offer = cited(endsAtMillis = 1_795_000_000_000L).toProductOffer()

        assertEquals(1_795_000_000_000L, offer.expiresAtMillis)
        assertTrue(offer.expirationDate.endsWith("hrs"))
    }

    @Test
    fun toProductOffer_withoutExpiry_showsNoDeadline() {
        val offer = cited().toProductOffer()

        assertNull(offer.expiresAtMillis)
        assertEquals("Sin fecha límite", offer.expirationDate)
    }

    @Test
    fun toProductOffer_blankMerchantAndCategory_useReadableDefaults() {
        val offer = CitedOffer("o", "Pan", "", "", 10.0, 10.0, 5, true).toProductOffer()

        assertEquals("Comercio Sensus", offer.storeName)
        assertEquals("Todo", offer.category)
        assertNull(offer.merchantId)
        assertEquals(0, offer.discountPercent)
    }

    private fun offer(id: String, title: String) = CitedOffer(id, title, "m", "T", 1.0, 2.0, 1, true)

    @Test
    fun visibleOfferCards_keepsOnlyOffersMentionedInTheReply() {
        val offers = listOf(offer("1", "Teclado Gaming"), offer("2", "Agua"), offer("3", "Pan"))

        val visible = visibleOfferCards("Encontré el teclado gaming por \$150.", offers)

        assertEquals(listOf("1"), visible.map { it.offerId })
    }

    @Test
    fun visibleOfferCards_limitsTheNumberOfCards() {
        val offers = (1..8).map { offer("$it", "Oferta $it") }
        val reply = offers.joinToString(", ") { it.title }

        assertEquals(5, visibleOfferCards(reply, offers).size)
        assertEquals(2, visibleOfferCards(reply, offers, max = 2).size)
    }

    @Test
    fun visibleOfferCards_ignoresBlankTitlesAndUnrelatedReplies() {
        assertTrue(visibleOfferCards("Hola", listOf(offer("1", "  "), offer("2", "Teclado"))).isEmpty())
    }
}
