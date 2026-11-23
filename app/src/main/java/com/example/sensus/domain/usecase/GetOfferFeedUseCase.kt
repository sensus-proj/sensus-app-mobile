package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FeedLocationPoint
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.model.haversineMeters
import com.example.sensus.domain.repository.OfferFeedRepository
import com.example.sensus.domain.repository.SessionRepository

private const val MAX_RADIUS_KM = 50
private const val SIGNIFICANT_MOVE_METERS = 500.0

class GetOfferFeedUseCase(
    private val repository: OfferFeedRepository,
    private val sessionRepository: SessionRepository
) {
    private var activeSnapshotId: String? = null
    private var lastLocation: FeedLocationPoint? = null
    private val seenOfferIds = LinkedHashSet<String>()

    suspend operator fun invoke(request: OfferFeedRequest): Result<OfferFeedPage> {
        if (sessionRepository.sessionState.value.isGuest) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para explorar promociones personalizadas."))
        }

        val currentPoint = FeedLocationPoint(request.latitude, request.longitude)
        val resetSnapshot = shouldResetSnapshot(request, currentPoint)
        val outboundRequest = buildOutboundRequest(request, currentPoint, resetSnapshot)

        return when (val result = repository.getFeed(outboundRequest)) {
            is Result.Success -> Result.Success(applySnapshotAndDedup(result.data, outboundRequest.page, currentPoint, resetSnapshot))
            is Result.Failure -> result
        }
    }

    suspend fun loadCachedFirstPage(): OfferFeedPage? = if (sessionRepository.sessionState.value.isGuest) null else repository.getCachedFirstPage()

    private fun shouldResetSnapshot(request: OfferFeedRequest, currentPoint: FeedLocationPoint): Boolean {
        val movedTooFar = lastLocation?.let { haversineMeters(it, currentPoint) > SIGNIFICANT_MOVE_METERS } == true
        return request.forceRefresh || request.page <= 1 || movedTooFar
    }

    private fun buildOutboundRequest(request: OfferFeedRequest, currentPoint: FeedLocationPoint, resetSnapshot: Boolean): OfferFeedRequest {
        val movedTooFar = lastLocation?.let { haversineMeters(it, currentPoint) > SIGNIFICANT_MOVE_METERS } == true
        val targetPage = if (movedTooFar && request.page > 1) 1 else request.page.coerceAtLeast(1)
        return request.copy(
            radiusKm = request.radiusKm.coerceIn(1, MAX_RADIUS_KM),
            page = targetPage,
            snapshotId = if (resetSnapshot) null else activeSnapshotId
        )
    }

    private fun applySnapshotAndDedup(pageData: OfferFeedPage, requestedPage: Int, point: FeedLocationPoint, wasReset: Boolean): OfferFeedPage {
        lastLocation = point
        if (wasReset || requestedPage == 1) {
            seenOfferIds.clear()
            activeSnapshotId = pageData.snapshotId
            seenOfferIds.addAll(pageData.items.map { it.offerId })
            return pageData
        }

        if (pageData.snapshotId == null) {
            val deduplicated = pageData.items.filter { seenOfferIds.add(it.offerId) }
            activeSnapshotId = null
            return pageData.copy(items = deduplicated)
        }

        activeSnapshotId = pageData.snapshotId
        seenOfferIds.addAll(pageData.items.map { it.offerId })
        return pageData
    }
}
