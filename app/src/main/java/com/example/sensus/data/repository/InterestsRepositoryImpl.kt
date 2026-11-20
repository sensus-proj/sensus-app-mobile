package com.example.sensus.data.repository

import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.InterestsApiService
import com.example.sensus.data.remote.dto.UpdateInterestsRequestDto
import com.example.sensus.domain.repository.InterestsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InterestsRepositoryImpl(
    private val api: InterestsApiService
) : InterestsRepository {

    override suspend fun getMyInterests(): Result<List<String>> = withContext(Dispatchers.IO) {
        safeNetworkCall("No se pudieron cargar tus intereses.") {
            val response = api.getMyInterests()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.categories)
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        default401 = "Inicia sesión para consultar tus intereses.",
                        default404 = "Servicio de intereses no disponible."
                    )
                )
            }
        }
    }

    override suspend fun updateMyInterests(categories: List<String>): Result<List<String>> = withContext(Dispatchers.IO) {
        safeNetworkCall("No se pudieron guardar tus intereses.") {
            val response = api.updateMyInterests(UpdateInterestsRequestDto(categories))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.Success(body.categories)
            } else {
                Result.Failure(
                    mapCommonHttpStatus(
                        response.code(),
                        parseBackendErrorMessage(response.errorBody()?.string()),
                        default401 = "Inicia sesión para guardar tus intereses.",
                        default404 = "Servicio de intereses no disponible."
                    )
                )
            }
        }
    }
}
