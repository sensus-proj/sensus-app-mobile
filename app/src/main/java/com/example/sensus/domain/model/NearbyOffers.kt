package com.example.sensus.domain.model

const val NEARBY_DEFAULT_PAGE_SIZE = 20
const val NEARBY_MAX_PAGE_SIZE = 100

data class NearbyOffersQuery(
    val location: UserLocation,
    val radiusKm: Double?,
    val category: String? = null,
    val page: Int = 1,
    val pageSize: Int = NEARBY_DEFAULT_PAGE_SIZE
)

data class PagedOffers(
    val items: List<ProductOffer>,
    val nextPage: Int? = null
)
