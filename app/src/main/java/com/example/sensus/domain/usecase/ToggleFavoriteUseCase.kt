package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.FavoriteToggleResult
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.FavoritesRepository
import com.example.sensus.domain.repository.SessionRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: FavoritesRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(offer: ProductOffer): Result<FavoriteToggleResult> {
        val session = sessionRepository.sessionState.value
        val error = when {
            session.isGuest -> AppError.Forbidden("Regístrate para guardar tus ofertas favoritas")
            !session.isSessionActive || session.activeToken.isNullOrBlank() -> AppError.Unauthorized("Inicia sesión para guardar favoritos")
            offer.id.isBlank() -> AppError.Validation("Oferta inválida")
            else -> null
        }
        return if (error != null) Result.Failure(error) else repository.toggleFavorite(offer)
    }
}
