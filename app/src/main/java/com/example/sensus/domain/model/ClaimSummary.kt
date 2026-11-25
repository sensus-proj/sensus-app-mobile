package com.example.sensus.domain.model

enum class ClaimStatus {
    ACTIVE,
    REDEEMED,
    EXPIRED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun from(raw: String): ClaimStatus = when (raw.trim().lowercase()) {
            "active" -> ACTIVE
            "redeemed" -> REDEEMED
            "expired" -> EXPIRED
            "cancelled", "canceled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

data class ClaimSummary(
    val claimId: String,
    val offerId: String,
    val offerTitle: String?,
    val status: ClaimStatus,
    val expiresAtEpochMs: Long,
    val qrPayload: String?
) {
    fun isActiveAt(nowEpochMs: Long): Boolean = status == ClaimStatus.ACTIVE && expiresAtEpochMs > nowEpochMs

    fun toOfferClaim(): OfferClaim? = qrPayload?.takeIf { it.isNotBlank() }?.let { OfferClaim(claimId, offerId, it, it, expiresAtEpochMs) }
}
