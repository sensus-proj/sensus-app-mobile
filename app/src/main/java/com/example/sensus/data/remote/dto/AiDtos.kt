package com.example.sensus.data.remote.dto

import com.example.sensus.data.mapper.RemoteOfferMapper
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.AiConversation
import com.example.sensus.domain.model.AiTurn
import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.RecommendedPackage
import com.example.sensus.domain.model.ScreenContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ScreenContextDto {
    @SerialName("home")
    HOME,

    @SerialName("map")
    MAP,

    @SerialName("offer_detail")
    OFFER_DETAIL,

    @SerialName("my_claims")
    MY_CLAIMS,

    @SerialName("merchant_inventory")
    MERCHANT_INVENTORY,

    @SerialName("merchant_profile")
    MERCHANT_PROFILE,

    @SerialName("user_profile")
    USER_PROFILE,

    @SerialName("settings")
    SETTINGS
}

fun ScreenContext.toDto(): ScreenContextDto = when (this) {
    ScreenContext.HOME -> ScreenContextDto.HOME
    ScreenContext.MAP -> ScreenContextDto.MAP
    ScreenContext.OFFER_DETAIL -> ScreenContextDto.OFFER_DETAIL
    ScreenContext.MY_CLAIMS -> ScreenContextDto.MY_CLAIMS
    ScreenContext.MERCHANT_INVENTORY -> ScreenContextDto.MERCHANT_INVENTORY
    ScreenContext.MERCHANT_PROFILE -> ScreenContextDto.MERCHANT_PROFILE
    ScreenContext.USER_PROFILE -> ScreenContextDto.USER_PROFILE
    ScreenContext.SETTINGS -> ScreenContextDto.SETTINGS
}

fun ScreenContextDto.toDomain(): ScreenContext = when (this) {
    ScreenContextDto.HOME -> ScreenContext.HOME
    ScreenContextDto.MAP -> ScreenContext.MAP
    ScreenContextDto.OFFER_DETAIL -> ScreenContext.OFFER_DETAIL
    ScreenContextDto.MY_CLAIMS -> ScreenContext.MY_CLAIMS
    ScreenContextDto.MERCHANT_INVENTORY -> ScreenContext.MERCHANT_INVENTORY
    ScreenContextDto.MERCHANT_PROFILE -> ScreenContext.MERCHANT_PROFILE
    ScreenContextDto.USER_PROFILE -> ScreenContext.USER_PROFILE
    ScreenContextDto.SETTINGS -> ScreenContext.SETTINGS
}

@Serializable
data class CreateConversationRequest(
    val forceNew: Boolean = false,
    val screenContext: ScreenContextDto? = null
)

@Serializable
data class SendAiMessageRequest(
    val conversationId: String?,
    val message: String,
    val screenContext: ScreenContextDto? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double? = null
)

@Serializable
data class ConversationSummaryDto(
    val conversationId: String,
    val status: String = "Active",
    val routingTier: String = "Fast",
    val messageCount: Int = 0,
    val lastScreenContext: String? = null,
    val markdownVersion: String = "1.0.0",
    val createdAtUtc: String = "",
    val updatedAtUtc: String? = null
)

fun ConversationSummaryDto.toDomain(): AiConversation = AiConversation(
    conversationId = conversationId,
    status = status,
    routingTier = routingTier,
    messageCount = messageCount,
    lastScreenContext = lastScreenContext,
    markdownVersion = markdownVersion,
    createdAtUtc = createdAtUtc,
    updatedAtUtc = updatedAtUtc
)

@Serializable
data class CitedOfferDto(
    val offerId: String,
    val title: String,
    val merchantId: String,
    val merchantName: String,
    val offerPriceMxn: Double,
    val regularPriceMxn: Double,
    val availableStock: Int,
    val isOpenNow: Boolean,
    val distanceKm: Double? = null,
    val description: String? = null,
    val discountPercentage: Int? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val endsAtUtc: String? = null,
    val merchantAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

fun CitedOfferDto.toDomain(): CitedOffer = CitedOffer(
    offerId = offerId,
    title = title,
    merchantId = merchantId,
    merchantName = merchantName,
    offerPriceMxn = offerPriceMxn,
    regularPriceMxn = regularPriceMxn,
    availableStock = availableStock,
    isOpenNow = isOpenNow,
    distanceKm = distanceKm,
    description = description,
    discountPercent = discountPercentage,
    imageUrl = imageUrl,
    category = category,
    endsAtMillis = RemoteOfferMapper.parseIsoMillis(endsAtUtc),
    merchantAddress = merchantAddress,
    latitude = latitude,
    longitude = longitude
)

@Serializable
data class RecommendedPackageDto(
    val budgetMxn: Double,
    val totalPriceMxn: Double,
    val savingsMxn: Double,
    val items: List<CitedOfferDto>
)

fun RecommendedPackageDto.toDomain(): RecommendedPackage = RecommendedPackage(
    budgetMxn = budgetMxn,
    totalPriceMxn = totalPriceMxn,
    savingsMxn = savingsMxn,
    items = items.map { it.toDomain() }
)

@Serializable
data class AiTurnResponseDto(
    val conversationId: String,
    val messageId: String,
    val reply: String,
    val recommendedPackage: RecommendedPackageDto? = null,
    val offersCited: List<CitedOfferDto>? = null,
    val degraded: Boolean = false,
    val degradationReason: String? = null,
    val correlationId: String? = null
)

fun AiTurnResponseDto.toDomain(): AiTurn = AiTurn(
    conversationId = conversationId,
    messageId = messageId,
    reply = reply,
    recommendedPackage = recommendedPackage?.toDomain(),
    offersCited = offersCited?.map { it.toDomain() } ?: emptyList(),
    degraded = degraded,
    degradationReason = degradationReason,
    correlationId = correlationId ?: ""
)

@Serializable
data class ConversationMessageDto(
    val messageId: String,
    val role: String,
    val content: String,
    val recommendedPackage: RecommendedPackageDto? = null,
    val offersCited: List<CitedOfferDto>? = null,
    val degraded: Boolean = false,
    val createdAtUtc: String = ""
)

fun ConversationMessageDto.toDomain(conversationId: String = ""): AiChatMessage = AiChatMessage(
    id = messageId,
    conversationId = conversationId,
    role = role,
    content = content,
    recommendedPackage = recommendedPackage?.toDomain(),
    offersCited = offersCited?.map { it.toDomain() } ?: emptyList(),
    degraded = degraded,
    createdAtUtc = createdAtUtc
)

@Serializable
data class PaginationDto(
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int
)

@Serializable
data class ConversationDetailDto(
    val conversation: ConversationSummaryDto,
    val messages: List<ConversationMessageDto> = emptyList(),
    val pagination: PaginationDto? = null
)

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val detail: String? = null,
    val status: Int? = null,
    val instance: String? = null,
    val correlationId: String? = null
)
