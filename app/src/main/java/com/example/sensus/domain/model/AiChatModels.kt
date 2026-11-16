package com.example.sensus.domain.model

enum class ScreenContext {
    HOME,
    MAP,
    OFFER_DETAIL,
    MY_CLAIMS,
    MERCHANT_INVENTORY,
    MERCHANT_PROFILE,
    USER_PROFILE,
    SETTINGS
}

data class CitedOffer(
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
    val discountPercent: Int? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val endsAtMillis: Long? = null,
    val merchantAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RecommendedPackage(
    val budgetMxn: Double,
    val totalPriceMxn: Double,
    val savingsMxn: Double,
    val items: List<CitedOffer>
)

data class AiConversation(
    val conversationId: String,
    val status: String,
    val routingTier: String,
    val messageCount: Int,
    val lastScreenContext: String? = null,
    val markdownVersion: String = "1.0.0",
    val createdAtUtc: String = "",
    val updatedAtUtc: String? = null
)

/** Ubicación y radio de búsqueda que acompañan a un mensaje para que Sensi encuentre ofertas cercanas. */
data class SensiLocation(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double
)

/** [role] es `"User"` o `"Assistant"`. */
data class AiChatMessage(
    val id: String,
    val conversationId: String = "",
    val role: String,
    val content: String,
    val recommendedPackage: RecommendedPackage? = null,
    val offersCited: List<CitedOffer> = emptyList(),
    val degraded: Boolean = false,
    val createdAtUtc: String = ""
) {
    val isFromUser: Boolean get() = role.equals("User", ignoreCase = true)
    val text: String get() = content
}

data class AiTurn(
    val conversationId: String,
    val messageId: String,
    val reply: String,
    val recommendedPackage: RecommendedPackage? = null,
    val offersCited: List<CitedOffer> = emptyList(),
    val degraded: Boolean = false,
    val degradationReason: String? = null,
    val correlationId: String
)

sealed interface SensiResult {
    data class Success(val turn: AiTurn) : SensiResult
    data class Rejected(val code: String, val message: String) : SensiResult
    data class RateLimited(val retryAfterSeconds: Int) : SensiResult
    data object GuestBlocked : SensiResult
    data object ConversationGone : SensiResult
    data object Unavailable : SensiResult
}
