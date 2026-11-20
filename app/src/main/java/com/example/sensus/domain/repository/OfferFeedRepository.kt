package com.example.sensus.domain.repository

import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest

interface OfferFeedRepository {
    suspend fun getFeed(request: OfferFeedRequest): Result<OfferFeedPage>

    suspend fun getCachedFirstPage(): OfferFeedPage? = null
}
