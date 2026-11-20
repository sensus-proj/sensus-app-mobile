package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.db.dao.OfferDao
import com.example.sensus.data.mapper.OfferDetailsMapper.toDomain
import com.example.sensus.data.mapper.OfferMapper.toEntity
import com.example.sensus.data.remote.api.OfferDetailsApiService
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ToggleOfferLikeResult
import com.example.sensus.domain.repository.OfferDetailsRepository
import java.time.format.DateTimeParseException
import retrofit2.Response

class OfferDetailsRepositoryImpl(private val api: OfferDetailsApiService, private val dao: OfferDao) : OfferDetailsRepository {
    override suspend fun getDetail(offerId: String): Result<OfferDetails> = safeNetworkCall("No se pudo consultar la oferta.") {
        val response = api.getDetail(offerId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            try {
                val details = body.toDomain()
                dao.insertOffer(details.offer.toEntity())
                Result.Success(details)
            } catch (_: DateTimeParseException) {
                Result.Failure(AppError.Unprocessable("La oferta contiene fechas inválidas."))
            }
        } else {
            Result.Failure(error(response))
        }
    }

    override suspend fun registerView(offerId: String): Result<Unit> = safeNetworkCall("No se pudo registrar la vista.") {
        val response = api.registerView(offerId)
        if (response.code() == HTTP_NO_CONTENT) Result.Success(Unit) else Result.Failure(error(response))
    }

    override suspend fun toggleLike(offerId: String): Result<ToggleOfferLikeResult> = safeNetworkCall("No se pudo actualizar tu me gusta.") {
        val response = api.toggleLike(offerId)
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.Success(ToggleOfferLikeResult(body.offerId, body.likesCount, body.isLikedByMe))
        } else {
            Result.Failure(error(response))
        }
    }

    private fun error(response: Response<*>): AppError = mapCommonHttpStatus(
        response.code(),
        parseBackendErrorMessage(response.errorBody()?.string()),
        "Inicia sesión para consultar la oferta.",
        "La oferta ya no está disponible."
    )
}

private const val HTTP_NO_CONTENT = 204
