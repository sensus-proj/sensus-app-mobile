package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedPolicy
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.SessionRepository

class GetNearbyFeedUseCase(private val repository: FeedRepository, private val session: SessionRepository) {
    suspend operator fun invoke(query: FeedQuery): Result<PagedFeed> {
        if (!FeedPolicy.validCoordinates(query.latitude, query.longitude) || query.page < 1 || query.radiusMeters < 1) {
            return Result.Failure(AppError.Validation("La ubicación o página no es válida."))
        }
        val maxRadius = if (session.sessionState.value.isGuest) FeedPolicy.GUEST_RADIUS else FeedPolicy.MAX_RADIUS
        return repository.nearby(
            query.copy(
                radiusMeters = query.radiusMeters.coerceAtMost(maxRadius),
                pageSize = query.pageSize.coerceIn(1, FeedPolicy.MAX_PAGE_SIZE)
            )
        )
    }
}
