package com.example.sensus.domain.model

/**
 * Destino al que lleva una notificacion push al abrirla (FT-39).
 *
 * Solo transporta identificadores. Los datos del apartado y el QR nunca viajan en la notificacion:
 * se cargan desde el backend en la pantalla correspondiente, ya con la sesion validada.
 */
sealed interface PushTarget {
    data class Offer(val offerId: String) : PushTarget
    data class Claim(val offerId: String) : PushTarget
    data object MerchantClaims : PushTarget
    data object Friends : PushTarget
    data object Home : PushTarget
}

/** Contrato del bloque `data` que envia el backend por FCM HTTP v1. */
object PushPayload {
    const val KEY_TYPE = "type"
    const val KEY_OFFER_ID = "offerId"
    const val KEY_CLAIM_ID = "claimId"
    const val KEY_REQUEST_ID = "requestId"

    const val TYPE_NEARBY_OFFER = "nearby_offer"
    const val TYPE_FRIEND_OFFER_SHARED = "friend_offer_shared"
    const val TYPE_CLAIM_CONFIRMED = "claim_confirmed"
    const val TYPE_MERCHANT_OFFER_CLAIMED = "merchant_offer_claimed"
    const val TYPE_FRIEND_REQUEST = "friend_request"
    const val TYPE_FRIEND_ACCEPTED = "friend_accepted"

    /** Claves que la app acepta del payload. Cualquier otra (fechas, codigos de invitacion, etc.) se descarta. */
    val ALLOWED_KEYS: Set<String> = setOf(KEY_TYPE, KEY_OFFER_ID, KEY_CLAIM_ID, KEY_REQUEST_ID)

    private const val MAX_TYPE_LENGTH = 40
    private val TYPE_PATTERN = Regex("^[a-z_]{1,$MAX_TYPE_LENGTH}$")
    private val ID_PATTERN = Regex("^[A-Za-z0-9-]{1,64}$")

    /** Conserva solo las claves permitidas con valores bien formados; asi un payload malicioso no inyecta extras. */
    fun sanitize(data: Map<String, String?>): Map<String, String> = buildMap {
        for ((key, value) in data) {
            if (key in ALLOWED_KEYS && value != null && isWellFormed(key, value)) put(key, value)
        }
    }

    private val OFFER_TYPES = setOf(TYPE_NEARBY_OFFER, TYPE_FRIEND_OFFER_SHARED)
    private val FRIEND_TYPES = setOf(TYPE_FRIEND_REQUEST, TYPE_FRIEND_ACCEPTED)

    /** Devuelve `null` si el mapa no trae un `type` valido (no es una notificacion de Sensus). */
    fun parse(data: Map<String, String?>): PushTarget? {
        val clean = sanitize(data)
        val type = clean[KEY_TYPE] ?: return null
        return targetFor(type, clean[KEY_OFFER_ID])
    }

    private fun targetFor(type: String, offerId: String?): PushTarget = when (type) {
        in OFFER_TYPES -> offerTarget(offerId) { PushTarget.Offer(it) }
        TYPE_CLAIM_CONFIRMED -> offerTarget(offerId) { PushTarget.Claim(it) }
        TYPE_MERCHANT_OFFER_CLAIMED -> PushTarget.MerchantClaims
        in FRIEND_TYPES -> PushTarget.Friends
        else -> PushTarget.Home
    }

    private fun offerTarget(offerId: String?, build: (String) -> PushTarget): PushTarget = offerId?.let(build) ?: PushTarget.Home

    private fun isWellFormed(key: String, value: String): Boolean = if (key == KEY_TYPE) TYPE_PATTERN.matches(value) else ID_PATTERN.matches(value)
}
