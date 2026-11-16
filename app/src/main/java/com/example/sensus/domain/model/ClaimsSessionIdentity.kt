package com.example.sensus.domain.model

import com.example.sensus.domain.repository.SessionState

class ClaimsSessionIdentity private constructor(
    private val email: String?,
    private val token: String?,
    private val active: Boolean,
    private val guest: Boolean,
    private val merchant: Boolean
) {
    private val identityFields = listOf(email, token, active, guest, merchant)

    override fun equals(other: Any?): Boolean = other is ClaimsSessionIdentity && identityFields == other.identityFields

    override fun hashCode(): Int = identityFields.hashCode()

    companion object {
        fun from(session: SessionState): ClaimsSessionIdentity = ClaimsSessionIdentity(
            session.activeEmail,
            session.activeToken,
            session.isSessionActive,
            session.isGuest,
            session.isMerchantRegistered
        )
    }
}
