package com.example.sensus.data.remote.dto

import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.StoreCategory
import kotlinx.serialization.Serializable

@Serializable
data class OfferFeedItemDto(
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
    val merchantName: String = "",
    val slot: String = "ranked"
) {
    fun toDomain(): OfferFeedItem {
        val resolvedCode = StoreCategory.fromApiCode(category)?.apiCode ?: category
        val display = StoreCategory.displayNameFor(category, fallback = category)
        return OfferFeedItem(
            offerId = offerId,
            title = title,
            categoryCode = resolvedCode,
            categoryDisplayName = display,
            imageUrl = imageUrl,
            originalPrice = originalPrice,
            finalPrice = finalPrice,
            stock = stock,
            distanceMeters = distanceMeters,
            expiresAtUtc = expiresAtUtc,
            merchantId = merchantId,
            merchantName = merchantName,
            slot = FeedSlot.fromApi(slot)
        )
    }
}

@Serializable
data class OfferFeedPageDto(
    val items: List<OfferFeedItemDto> = emptyList(),
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalCount: Int = 0,
    val totalPages: Int = 0,
    val snapshotId: String? = null,
    val rankingVersion: String = "feed-v1"
) {
    fun toDomain(isCached: Boolean = false): OfferFeedPage = OfferFeedPage(
        items = items.map { it.toDomain() },
        page = page,
        pageSize = pageSize,
        totalCount = totalCount,
        totalPages = totalPages,
        snapshotId = snapshotId,
        rankingVersion = rankingVersion,
        isCached = isCached
    )
}

@Serializable
data class UserInterestsResponseDto(
    val categories: List<String> = emptyList(),
    val updatedAtUtc: String? = null
)

@Serializable
data class UpdateInterestsRequestDto(
    val categories: List<String>
)
