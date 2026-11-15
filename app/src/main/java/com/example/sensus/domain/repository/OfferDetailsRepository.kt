package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails

interface OfferDetailsRepository {
    suspend fun getDetail(offerId: String): Result<OfferDetails>
    suspend fun registerView(offerId: String): Result<Unit>
}
