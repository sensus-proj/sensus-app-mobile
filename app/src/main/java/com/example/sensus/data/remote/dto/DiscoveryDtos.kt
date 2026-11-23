package com.example.sensus.data.remote.dto

import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.StoreCategory
import kotlinx.serialization.Serializable

@Serializable
data class OfferSearchItemDto(
    val offerId: String,
    val title: String,
    val description: String = "",
    val category: String? = null,
    val imageUrl: String? = null,
    val originalPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val stock: Int = 1,
    val distanceMeters: Int = 0,
    val expiresAtUtc: String = "",
    val merchantId: String? = null,
    val merchantName: String = "",
    val score: Double = 0.0
) {
    fun toDomain(): OfferSearchItem {
        val code = StoreCategory.fromApiCode(category)?.apiCode ?: category
        val display = StoreCategory.displayNameFor(category, fallback = category ?: "Todo")
        return OfferSearchItem(
            offerId = offerId,
            title = title,
            description = description,
            categoryCode = code,
            categoryDisplayName = display,
            imageUrl = imageUrl,
            originalPrice = originalPrice,
            finalPrice = finalPrice,
            stock = stock,
            distanceMeters = distanceMeters,
            expiresAtUtc = expiresAtUtc,
            merchantId = merchantId,
            merchantName = merchantName,
            score = score
        )
    }
}

@Serializable
data class OfferSearchPageDto(
    val items: List<OfferSearchItemDto> = emptyList(),
    val totalCount: Int = 0,
    val pageNumber: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 0
) {
    fun toDomain(): OfferSearchPage = OfferSearchPage(
        items = items.map { it.toDomain() },
        totalCount = totalCount,
        pageNumber = pageNumber,
        pageSize = pageSize,
        totalPages = totalPages,
        isFromLocalFallback = false
    )
}

@Serializable
data class OfferAlternativeItemDto(
    val offerId: String,
    val title: String,
    val category: String,
    val imageUrl: String? = null,
    val originalPrice: Double = 0.0,
    val finalPrice: Double = 0.0,
    val stock: Int = 1,
    val distanceMeters: Int = 0,
    val expiresAtUtc: String = "",
    val merchantId: String? = null,
    val merchantName: String = ""
) {
    fun toDomain(): OfferAlternativeItem {
        val code = StoreCategory.fromApiCode(category)?.apiCode ?: category
        val display = StoreCategory.displayNameFor(category, fallback = category)
        return OfferAlternativeItem(
            offerId = offerId,
            title = title,
            categoryCode = code,
            categoryDisplayName = display,
            imageUrl = imageUrl,
            originalPrice = originalPrice,
            finalPrice = finalPrice,
            stock = stock,
            distanceMeters = distanceMeters,
            expiresAtUtc = expiresAtUtc,
            merchantId = merchantId,
            merchantName = merchantName
        )
    }
}
