package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.OfferSearchRequest
import com.example.sensus.domain.repository.DiscoveryRepository
import com.example.sensus.domain.repository.SessionRepository

private const val MIN_QUERY_LENGTH = 2
private const val MAX_QUERY_LENGTH = 80
private const val DEFAULT_SEARCH_RADIUS_KM = 10.0
private const val REGISTERED_MAX_RADIUS_KM = 50.0
private const val DEFAULT_ALTERNATIVES_LIMIT = 5
private const val MAX_ALTERNATIVES_LIMIT = 10
private const val DEFAULT_SEARCH_PAGE_SIZE = 20

class SearchOffersUseCase(
    private val repository: DiscoveryRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(request: OfferSearchRequest): Result<OfferSearchPage> {
        val rawQuery = request.query.orEmpty()
        val trimmed = rawQuery.trim()
        if (rawQuery.any { it.isISOControl() } || trimmed.length == 1 || trimmed.length > MAX_QUERY_LENGTH) {
            return Result.Failure(
                AppError.Validation("La búsqueda debe estar vacía o tener entre $MIN_QUERY_LENGTH y $MAX_QUERY_LENGTH caracteres.")
            )
        }

        val isGuest = sessionRepository.sessionState.value.isGuest
        val maxRadiusKm = if (isGuest) GuestRadiusPolicy.GUEST_MAX_RADIUS_KM.toDouble() else REGISTERED_MAX_RADIUS_KM
        val safeRadius = (request.radiusKm ?: DEFAULT_SEARCH_RADIUS_KM).coerceIn(1.0, maxRadiusKm)

        val queryToSend = trimmed.ifEmpty { null }
        val result = repository.searchOffers(
            OfferSearchRequest(
                query = queryToSend,
                latitude = request.latitude,
                longitude = request.longitude,
                radiusKm = safeRadius,
                page = request.page.coerceAtLeast(1),
                pageSize = request.pageSize.coerceIn(1, DEFAULT_SEARCH_PAGE_SIZE)
            )
        )

        if (result is Result.Success && queryToSend != null && !isGuest) {
            repository.saveSearchQuery(queryToSend)
        }

        return result
    }

    suspend fun getHistory(): List<String> = if (sessionRepository.sessionState.value.isGuest) emptyList() else repository.getSearchHistory()

    suspend fun clearHistory() {
        repository.clearSearchHistory()
    }
}

class GetOfferAlternativesUseCase(
    private val repository: DiscoveryRepository
) {
    suspend operator fun invoke(offerId: String, limit: Int = DEFAULT_ALTERNATIVES_LIMIT): Result<List<OfferAlternativeItem>> {
        if (offerId.isBlank()) {
            return Result.Failure(AppError.Validation("Identificador de oferta inválido."))
        }
        return repository.getAlternatives(offerId, limit.coerceIn(1, MAX_ALTERNATIVES_LIMIT))
    }
}
