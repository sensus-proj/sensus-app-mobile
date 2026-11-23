package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ToggleOfferLikeResult

interface OfferDetailsRepository {
    suspend fun getDetail(offerId: String): Result<OfferDetails>
    suspend fun registerView(offerId: String): Result<Unit>
    suspend fun toggleLike(offerId: String): Result<ToggleOfferLikeResult>
}
