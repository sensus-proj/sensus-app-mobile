package com.example.sensus.domain.model

/**
 * Ciclo de vida de una oferta comercial (FT-16, Specs 035-037).
 */
enum class OfferLifecycleStatus(
    val backendValue: String,
    val displayLabel: String
) {
    ACTIVE("Published", "Activa"),
    PAUSED("Paused", "Pausada"),
    EXPIRED("Expired", "Expirada"),
    ARCHIVED("Archived", "De baja"),
    SOLD_OUT("SoldOut", "Agotada");

    companion object {
        fun fromBackend(raw: String?, unitsLeft: Int = 1): OfferLifecycleStatus = when (raw?.trim()?.lowercase()) {
            "paused" -> PAUSED
            "expired" -> EXPIRED
            "archived" -> ARCHIVED
            "soldout", "sold_out" -> SOLD_OUT
            else -> if (unitsLeft <= 0) SOLD_OUT else ACTIVE
        }
    }
}
