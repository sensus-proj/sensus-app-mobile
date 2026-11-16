package com.example.sensus.domain.model

import java.text.Normalizer
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6371000.0
private const val METERS_PER_KM_INT = 1000
private const val COORDINATE_EPSILON = 0.00001
private const val DIACRITIC_REGEX = "\\p{InCombiningDiacriticalMarks}+"

private val categoryFilterKeywords = mapOf(
    StoreCategory.GROCERY to "abarrotes",
    StoreCategory.CLEANING to "limpieza",
    StoreCategory.PERISHABLES to "perecederos",
    StoreCategory.DAIRY to "lacteos",
    StoreCategory.FOOD to "comida",
    StoreCategory.BAKERY to "panaderia",
    StoreCategory.BEVERAGES to "bebidas",
    StoreCategory.PETS to "mascotas",
    StoreCategory.PHARMACY to "farmacia"
)

data class StoreClusterQuery(
    val userLocation: UserLocation,
    val offers: List<ProductOffer>,
    val nearbyMerchants: List<MerchantProfile>,
    val searchQuery: String,
    val selectedCategory: StoreCategory,
    val effectiveRadiusKm: Float
)

object MapStoreClusterBuilder {

    fun buildClusters(query: StoreClusterQuery): List<MapStoreCluster> {
        val merchantsById = query.nearbyMerchants.filter { it.id.isNotBlank() }.associateBy { normalizeId(it.id) }
        val merchantsByName = query.nearbyMerchants
            .filter { it.nombreComercial.isNotBlank() }
            .associateBy { normalizeText(it.nombreComercial) }

        val alignedOffers = query.offers
            .filter { it.isPubliclyVisible }
            .map { offer -> alignOfferWithMerchant(offer, merchantsById, merchantsByName) }

        val offersByStoreKey = alignedOffers.groupBy { resolveStoreKey(it.merchantId, it.storeName) }
        val maxRadiusMeters = (query.effectiveRadiusKm * METERS_PER_KM_INT).toInt()

        val offerClusters = offersByStoreKey.mapNotNull { (storeKey, storeOffers) ->
            val rep = storeOffers.first()
            val merchant = findMatchingMerchant(rep.merchantId, rep.storeName, merchantsById, merchantsByName)
            buildOfferStoreCluster(
                input = StoreClusterInput(
                    storeKey = storeKey,
                    merchantId = rep.merchantId ?: merchant?.id,
                    storeName = merchant?.nombreComercial?.ifBlank { rep.storeName } ?: rep.storeName,
                    address = merchant?.direccion?.ifBlank { rep.address } ?: rep.address,
                    postalCode = merchant?.codigoPostal?.ifBlank { rep.postalCode } ?: rep.postalCode,
                    latitude = merchant?.takeIf { hasValidCoords(it.latitud, it.longitud) }?.latitud ?: rep.latitude,
                    longitude = merchant?.takeIf { hasValidCoords(it.latitud, it.longitud) }?.longitud ?: rep.longitude,
                    merchantProfile = merchant,
                    storeOffers = storeOffers
                ),
                query = query,
                maxRadiusMeters = maxRadiusMeters
            )
        }

        val coveredMerchantIds = offerClusters.mapNotNull { it.merchantId?.takeIf(String::isNotBlank)?.let(::normalizeId) }.toSet()
        val coveredNames = offerClusters.map { normalizeText(it.storeName) }.toSet()
        val merchantOnlyClusters = collectMerchantOnlyClusters(
            query = query,
            coveredMerchantIds = coveredMerchantIds,
            coveredNames = coveredNames,
            maxRadiusMeters = maxRadiusMeters
        )

        return (offerClusters + merchantOnlyClusters).sortedBy { it.distanceMeters }
    }

    private fun collectMerchantOnlyClusters(
        query: StoreClusterQuery,
        coveredMerchantIds: Set<String>,
        coveredNames: Set<String>,
        maxRadiusMeters: Int
    ): List<MapStoreCluster> {
        if (query.selectedCategory != StoreCategory.ALL) return emptyList()
        return query.nearbyMerchants
            .filter { merchant ->
                val idCovered = merchant.id.isNotBlank() && normalizeId(merchant.id) in coveredMerchantIds
                val nameCovered = normalizeText(merchant.nombreComercial) in coveredNames
                !idCovered && !nameCovered
            }
            .mapNotNull { merchant -> buildEmptyStoreCluster(merchant, query, maxRadiusMeters) }
    }

    fun matchesCategory(rawCategory: String, category: StoreCategory): Boolean {
        if (category == StoreCategory.ALL) return true
        val keyword = categoryFilterKeywords[category] ?: return true
        val normalized = normalizeText(rawCategory)
        return normalized.contains(keyword)
    }

    private data class StoreClusterInput(
        val storeKey: String,
        val merchantId: String?,
        val storeName: String,
        val address: String,
        val postalCode: String,
        val latitude: Double,
        val longitude: Double,
        val merchantProfile: MerchantProfile?,
        val storeOffers: List<ProductOffer>
    )

    private fun buildOfferStoreCluster(input: StoreClusterInput, query: StoreClusterQuery, maxRadiusMeters: Int): MapStoreCluster? {
        val distanceMeters = distanceBetweenMeters(
            query.userLocation.latitude,
            query.userLocation.longitude,
            input.latitude,
            input.longitude
        )
        if (distanceMeters > maxRadiusMeters) return null

        val storeMatchesSearch = query.searchQuery.isBlank() ||
            input.storeName.contains(query.searchQuery, ignoreCase = true) ||
            input.address.contains(query.searchQuery, ignoreCase = true)

        val localizedStoreOffers = input.storeOffers.map {
            it.copy(latitude = input.latitude, longitude = input.longitude, distanceMeters = distanceMeters)
        }

        val matchingOffers = localizedStoreOffers.filter { offer ->
            val categoryOk = matchesCategory(offer.category, query.selectedCategory)
            val queryOk = storeMatchesSearch ||
                offer.name.contains(query.searchQuery, ignoreCase = true) ||
                offer.category.contains(query.searchQuery, ignoreCase = true)
            categoryOk && queryOk
        }

        if (matchingOffers.isEmpty()) return null

        return MapStoreCluster(
            storeKey = input.storeKey,
            merchantId = input.merchantId,
            storeName = input.storeName,
            address = input.address,
            postalCode = input.postalCode,
            latitude = input.latitude,
            longitude = input.longitude,
            distanceMeters = distanceMeters,
            merchantProfile = input.merchantProfile,
            matchingOffers = matchingOffers,
            allStoreOffers = localizedStoreOffers,
            initialCategory = query.selectedCategory
        )
    }

    private fun buildEmptyStoreCluster(merchant: MerchantProfile, query: StoreClusterQuery, maxRadiusMeters: Int): MapStoreCluster? {
        val validCoords = hasValidCoords(merchant.latitud, merchant.longitud)
        val distanceMeters = if (validCoords) {
            distanceBetweenMeters(
                query.userLocation.latitude,
                query.userLocation.longitude,
                merchant.latitud,
                merchant.longitud
            )
        } else {
            Int.MAX_VALUE
        }
        val matchesSearch = query.searchQuery.isBlank() ||
            merchant.nombreComercial.contains(query.searchQuery, ignoreCase = true) ||
            merchant.direccion.contains(query.searchQuery, ignoreCase = true)

        return if (validCoords && distanceMeters <= maxRadiusMeters && matchesSearch) {
            MapStoreCluster(
                storeKey = resolveStoreKey(merchant.id, merchant.nombreComercial),
                merchantId = merchant.id.takeIf(String::isNotBlank),
                storeName = merchant.nombreComercial,
                address = merchant.direccion,
                postalCode = merchant.codigoPostal,
                latitude = merchant.latitud,
                longitude = merchant.longitud,
                distanceMeters = distanceMeters,
                merchantProfile = merchant,
                matchingOffers = emptyList(),
                allStoreOffers = emptyList(),
                initialCategory = StoreCategory.ALL
            )
        } else {
            null
        }
    }

    private fun alignOfferWithMerchant(
        offer: ProductOffer,
        merchantsById: Map<String, MerchantProfile>,
        merchantsByName: Map<String, MerchantProfile>
    ): ProductOffer {
        val merchant = findMatchingMerchant(offer.merchantId, offer.storeName, merchantsById, merchantsByName) ?: return offer
        return if (hasValidCoords(merchant.latitud, merchant.longitud)) {
            offer.copy(
                latitude = merchant.latitud,
                longitude = merchant.longitud,
                storeName = merchant.nombreComercial.ifBlank { offer.storeName },
                address = merchant.direccion.ifBlank { offer.address },
                postalCode = merchant.codigoPostal.ifBlank { offer.postalCode }
            )
        } else {
            offer
        }
    }

    private fun findMatchingMerchant(
        merchantId: String?,
        storeName: String,
        merchantsById: Map<String, MerchantProfile>,
        merchantsByName: Map<String, MerchantProfile>
    ): MerchantProfile? {
        if (!merchantId.isNullOrBlank()) {
            merchantsById[normalizeId(merchantId)]?.let { return it }
        }
        val normalized = normalizeText(storeName)
        return if (normalized.isNotBlank()) merchantsByName[normalized] else null
    }
}

private fun distanceBetweenMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * asin(sqrt(a))
    return (EARTH_RADIUS_METERS * c).toInt()
}

private fun resolveStoreKey(merchantId: String?, storeName: String): String =
    merchantId?.trim()?.takeIf { it.isNotBlank() }?.let(::normalizeId) ?: "name:${normalizeText(storeName)}"

private fun normalizeId(value: String): String = value.trim().lowercase()

private fun hasValidCoords(lat: Double, lng: Double): Boolean = kotlin.math.abs(lat) > COORDINATE_EPSILON || kotlin.math.abs(lng) > COORDINATE_EPSILON

private fun normalizeText(value: String): String = Normalizer
    .normalize(value.trim(), Normalizer.Form.NFD)
    .replace(Regex(DIACRITIC_REGEX), "")
    .lowercase()
