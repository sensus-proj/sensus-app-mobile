package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.InterestCatalog
import com.example.sensus.domain.repository.InterestsRepository
import com.example.sensus.domain.repository.SessionRepository

class GetMyInterestsUseCase(
    private val repository: InterestsRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        if (sessionRepository.sessionState.value.isGuest) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para gestionar tus intereses."))
        }
        return repository.getMyInterests()
    }
}

class UpdateMyInterestsUseCase(
    private val repository: InterestsRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(categories: List<String>): Result<List<String>> {
        if (sessionRepository.sessionState.value.isGuest) {
            return Result.Failure(AppError.Unauthorized("Inicia sesión para guardar tus intereses."))
        }
        val validated = InterestCatalog.normalizeAndValidate(categories)
            ?: return Result.Failure(
                AppError.Validation("Selecciona entre 0 y ${InterestCatalog.MAX_SELECTED} categorías válidas.")
            )
        return repository.updateMyInterests(validated)
    }
}
