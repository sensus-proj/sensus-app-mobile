package com.example.sensus.domain

import com.example.sensus.domain.model.PushPayload
import com.example.sensus.domain.model.PushTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {
    private val offerId = "3f2a9c1e-5b7d-4e8f-9a0b-1c2d3e4f5a6b"

    @Test
    fun everyBackendTypeMapsToItsDestination() {
        val cases = mapOf(
            "nearby_offer" to PushTarget.Offer(offerId),
            "friend_offer_shared" to PushTarget.Offer(offerId),
            "claim_confirmed" to PushTarget.Claim(offerId),
            "merchant_offer_claimed" to PushTarget.MerchantClaims,
            "friend_request" to PushTarget.Friends,
            "friend_accepted" to PushTarget.Friends,
            "sanction_applied" to PushTarget.Home,
            "sanction_reverted" to PushTarget.Home,
            "something_new" to PushTarget.Home
        )
        cases.forEach { (type, expected) ->
            assertEquals(type, expected, PushPayload.parse(mapOf("type" to type, "offerId" to offerId)))
        }
    }

    @Test
    fun offerTypesWithoutUsableIdFallBackToHome() {
        assertEquals(PushTarget.Home, PushPayload.parse(mapOf("type" to "nearby_offer")))
        assertEquals(PushTarget.Home, PushPayload.parse(mapOf("type" to "claim_confirmed", "offerId" to "../../admin")))
    }

    @Test
    fun missingOrMalformedTypeIsNotASensusNotification() {
        assertNull(PushPayload.parse(emptyMap()))
        assertNull(PushPayload.parse(mapOf("offerId" to offerId)))
        assertNull(PushPayload.parse(mapOf("type" to "Nearby Offer<script>")))
        assertNull(PushPayload.parse(mapOf("type" to null)))
    }

    @Test
    fun sanitizeKeepsOnlyAllowedWellFormedKeys() {
        val clean = PushPayload.sanitize(
            mapOf(
                "type" to "claim_confirmed",
                "offerId" to offerId,
                "claimId" to "c-1",
                "expiresAtUtc" to "2026-11-19T20:00:00Z",
                "senderInviteCode" to "ABC123",
                "requestId" to "bad id with spaces"
            )
        )
        assertEquals(mapOf("type" to "claim_confirmed", "offerId" to offerId, "claimId" to "c-1"), clean)
    }
}
