package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.OfferClaim

interface ClaimsRepository {
    suspend fun createClaim(offerId: String): Result<OfferClaim>
    suspend fun redeemClaim(code: String): Result<ClaimRedemption>
}
