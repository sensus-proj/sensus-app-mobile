package com.example.sensus.presentation

import com.example.sensus.presentation.common.OfferThumbnailSize
import com.example.sensus.presentation.common.publicOfferThumbnailUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicOfferThumbnailTest {
    @Test
    fun publicCloudinaryImageKeepsVersionAndUsesSurfaceWidth() {
        val prefix = "https://res.cloudinary.com/sensus-test/image/upload/"
        val resource = "v123/offers/pan.jpg"
        assertEquals(prefix + "c_limit,w_288,q_auto/" + resource, publicOfferThumbnailUrl(prefix + resource, OfferThumbnailSize.PREVIEW))
        assertEquals(prefix + "c_limit,w_640,q_auto/" + resource, publicOfferThumbnailUrl(prefix + resource, OfferThumbnailSize.CATALOG))
    }

    @Test
    fun preservesExternalPrivateSignedAndAlreadyTransformedResources() {
        val urls = listOf(
            "https://example.com/v123/offer.jpg",
            "https://res.cloudinary.com/demo/image/authenticated/v123/offer.jpg",
            "https://res.cloudinary.com/demo/image/upload/v123/offer.jpg?auth=sample",
            "https://res.cloudinary.com/demo/image/upload/s--sample--/v123/offer.jpg",
            "https://res.cloudinary.com/demo/image/upload/c_fill,w_200/v123/offer.jpg"
        )
        urls.forEach { url -> assertEquals(url, publicOfferThumbnailUrl(url, OfferThumbnailSize.PREVIEW)) }
    }
}
