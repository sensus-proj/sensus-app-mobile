package com.example.sensus.domain

import com.example.sensus.domain.model.MapStoreClusterBuilder
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.StoreClusterQuery
import com.example.sensus.domain.model.UserLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStoreClusterBuilderTest {

    private val userLoc = UserLocation(20.0536, -99.3436)

    @Test
    fun buildClusters_groupsMultipleOffersIntoSingleStorePin_usingMerchantCoordinates() {
        val merchant = MerchantProfile(
            id = "m_1",
            usuarioId = "u_1",
            nombreComercial = "Panadería La Espiga",
            direccion = "Calle Hidalgo 10",
            codigoPostal = "42800",
            latitud = 20.0540,
            longitud = -99.3440
        )
        val offer1 = ProductOffer(
            id = "o_1",
            name = "Bolillo artesanal",
            storeName = "Panadería La Espiga",
            category = "Panadería",
            originalPrice = 20.0,
            discountPrice = 10.0,
            discountPercent = 50,
            unitsLeft = 6,
            distanceMeters = 500,
            latitude = 20.0536,
            longitude = -99.3436,
            merchantId = "m_1",
            status = OfferLifecycleStatus.ACTIVE
        )
        val offer2 = ProductOffer(
            id = "o_2",
            name = "Leche entera 1L",
            storeName = "Panadería La Espiga",
            category = "Lácteos",
            originalPrice = 30.0,
            discountPrice = 21.0,
            discountPercent = 30,
            unitsLeft = 4,
            distanceMeters = 500,
            latitude = 20.0536,
            longitude = -99.3436,
            merchantId = "m_1",
            status = OfferLifecycleStatus.ACTIVE
        )

        val clusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(
                userLocation = userLoc,
                offers = listOf(offer1, offer2),
                nearbyMerchants = listOf(merchant),
                searchQuery = "",
                selectedCategory = StoreCategory.ALL,
                effectiveRadiusKm = 2f
            )
        )

        assertEquals(1, clusters.size)
        val cluster = clusters.first()
        assertEquals(20.0540, cluster.latitude, 0.00001)
        assertEquals(-99.3440, cluster.longitude, 0.00001)
        assertEquals(2, cluster.allStoreOffers.size)
        assertEquals(2, cluster.matchingOffers.size)
        assertEquals("o_1", cluster.featuredOffer?.id)
    }

    @Test
    fun buildClusters_filtersStoresByProductCategory_includingAccentedAndUnaccentedNames() {
        val bakeryOffer = sampleOffer("o_bak", "Concha de vainilla", "Tienda Mixta", "Panaderia", "m_mix")
        val dairyOffer = sampleOffer("o_lac", "Yogurt natural", "Tienda Mixta", "Lácteos", "m_mix")
        val petOffer = sampleOffer("o_pet", "Croquetas 2kg", "Pet Shop", "Mascotas", "m_pet")
        val offers = listOf(bakeryOffer, dairyOffer, petOffer)

        val bakeryClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(userLoc, offers, emptyList(), "", StoreCategory.BAKERY, 2f)
        )
        assertEquals(1, bakeryClusters.size)
        assertEquals("Tienda Mixta", bakeryClusters.first().storeName)
        assertEquals(listOf("o_bak"), bakeryClusters.first().matchingOffers.map { it.id })
        assertEquals(2, bakeryClusters.first().allStoreOffers.size)

        val dairyClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(userLoc, offers, emptyList(), "", StoreCategory.DAIRY, 2f)
        )
        assertEquals(listOf("o_lac"), dairyClusters.first().matchingOffers.map { it.id })
        assertFalse(MapStoreClusterBuilder.matchesCategory("Mascotas", StoreCategory.BAKERY))
        assertTrue(MapStoreClusterBuilder.matchesCategory("Lácteos", StoreCategory.DAIRY))
    }

    private fun sampleOffer(id: String, name: String, storeName: String, category: String, merchantId: String) = ProductOffer(
        id = id,
        name = name,
        storeName = storeName,
        category = category,
        originalPrice = 30.0,
        discountPrice = 15.0,
        discountPercent = 50,
        unitsLeft = 4,
        distanceMeters = 100,
        latitude = 20.0540,
        longitude = -99.3440,
        merchantId = merchantId
    )

    @Test
    fun buildClusters_excludesPausedZeroStockOrOutOfRadiusStores_andIncludesEmptyStoresOnlyOnAll() {
        val merchantNoOffers = MerchantProfile(
            id = "m_empty",
            usuarioId = "u_2",
            nombreComercial = "Miscelánea Centro",
            direccion = "Av. Centro 5",
            codigoPostal = "42800",
            latitud = 20.0539,
            longitud = -99.3439
        )
        val pausedOffer = ProductOffer(
            id = "o_paused",
            name = "Pan pausado",
            storeName = "Miscelánea Centro",
            category = "Panadería",
            originalPrice = 20.0,
            discountPrice = 10.0,
            discountPercent = 50,
            unitsLeft = 0,
            distanceMeters = 80,
            latitude = 20.0539,
            longitude = -99.3439,
            merchantId = "m_empty",
            status = OfferLifecycleStatus.PAUSED
        )
        val farMerchant = MerchantProfile(
            id = "m_far",
            usuarioId = "u_3",
            nombreComercial = "Tienda Lejana",
            direccion = "Carretera 99",
            codigoPostal = "42899",
            latitud = 20.2000,
            longitud = -99.5000
        )

        val allClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(
                userLocation = userLoc,
                offers = listOf(pausedOffer),
                nearbyMerchants = listOf(merchantNoOffers, farMerchant),
                searchQuery = "",
                selectedCategory = StoreCategory.ALL,
                effectiveRadiusKm = 2f
            )
        )
        assertEquals(1, allClusters.size)
        assertEquals("Miscelánea Centro", allClusters.first().storeName)
        assertTrue(allClusters.first().allStoreOffers.isEmpty())

        val filteredClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(
                userLocation = userLoc,
                offers = listOf(pausedOffer),
                nearbyMerchants = listOf(merchantNoOffers, farMerchant),
                searchQuery = "",
                selectedCategory = StoreCategory.BAKERY,
                effectiveRadiusKm = 2f
            )
        )
        assertTrue(filteredClusters.isEmpty())
    }

    @Test
    fun buildClusters_matchesNormalizedMerchantIdsCaseInsensitively_andSetsInitialCategory() {
        val merchant = MerchantProfile(
            id = "482D39D8-2AC6-4976-AD10-5EFDD6DF87F6",
            usuarioId = "u_case",
            nombreComercial = "El Santo",
            direccion = "Ignacio Ramírez 19",
            codigoPostal = "42804",
            latitud = 20.0434,
            longitud = -99.3428
        )
        val offer = ProductOffer(
            id = "o_milk",
            name = "Queso fresco",
            storeName = "",
            category = "Lácteos",
            originalPrice = 50.0,
            discountPrice = 30.0,
            discountPercent = 40,
            unitsLeft = 3,
            distanceMeters = 300,
            latitude = 20.0536,
            longitude = -99.3436,
            merchantId = "482d39d8-2ac6-4976-ad10-5efdd6df87f6",
            status = OfferLifecycleStatus.ACTIVE
        )

        val allClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(userLoc, listOf(offer), listOf(merchant), "", StoreCategory.ALL, 3f)
        )
        assertEquals(1, allClusters.size)
        assertEquals(StoreCategory.ALL, allClusters.first().initialCategory)
        assertEquals("El Santo", allClusters.first().storeName)
        assertEquals(20.0434, allClusters.first().latitude, 0.0001)
        assertEquals(-99.3428, allClusters.first().longitude, 0.0001)

        val dairyClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(userLoc, listOf(offer), listOf(merchant), "", StoreCategory.DAIRY, 3f)
        )
        assertEquals(1, dairyClusters.size)
        assertEquals(StoreCategory.DAIRY, dairyClusters.first().initialCategory)
    }
}
