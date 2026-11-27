package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FavoriteOffer
import com.example.sensus.domain.repository.FavoritesRepository
import com.example.sensus.domain.repository.SessionRepository
import javax.inject.Inject

private const val DEFAULT_FAVORITES_PAGE_SIZE = 20
private const val MAX_FAVORITES_PAGE_SIZE = 100

class GetFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = DEFAULT_FAVORITES_PAGE_SIZE): Result<List<FavoriteOffer>> {
        val session = sessionRepository.sessionState.value
        return when {
            session.isGuest -> Result.Failure(AppError.Forbidden("Regístrate para ver tus favoritos"))
            !session.isSessionActive || session.activeToken.isNullOrBlank() -> Result.Failure(AppError.Unauthorized("Sesión expirada"))
            else -> repository.syncFavorites(
                page = page.coerceAtLeast(1),
                pageSize = pageSize.coerceIn(1, MAX_FAVORITES_PAGE_SIZE)
            )
        }
    }
}
