package com.example.sensus.domain.model

/** New schedule and inventory for the published republish contract. */
data class RepublishOfferInput(
    val offerId: String,
    val newStartsAtMillis: Long,
    val newEndsAtMillis: Long,
    val newStock: Int
) {
    fun isValid(nowMillis: Long): Boolean = offerId.isNotBlank() && newStock > 0 &&
        newStartsAtMillis > nowMillis && newEndsAtMillis > newStartsAtMillis
}
