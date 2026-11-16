package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response

private const val MAX_QR_PAYLOAD_BYTES = 2048
private const val HTTP_OK = 200
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val HTTP_TOO_MANY_REQUESTS = 429

class ClaimsRepositoryImpl(
    private val api: ClaimsApiService,
    private val sessions: SessionRepository
) : ClaimsRepository {
    override suspend fun createClaim(offerId: String): Result<OfferClaim> = request(false, { api.createClaim(offerId) }) {
        require(it.claimId.isNotBlank() && it.offerId == offerId)
        require(it.code.isNotBlank() && it.qrPayload.isNotBlank())
        require(it.qrPayload.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES)
        OfferClaim(it.claimId, it.offerId, it.code, it.qrPayload, Instant.parse(it.expiresAtUtc).toEpochMilli())
    }

    override suspend fun redeemClaim(code: String): Result<ClaimRedemption> = request(true, { api.redeemClaim(code) }) {
        require(it.claimId.isNotBlank() && it.offerId.isNotBlank() && it.merchantId.isNotBlank())
        ClaimRedemption(it.claimId, it.offerId, it.merchantId, Instant.parse(it.redeemedAtUtc).toEpochMilli())
    }

    private suspend fun <T, R> request(merchant: Boolean, call: suspend () -> Response<T>, map: (T) -> R): Result<R> {
        val session = sessions.sessionState.value
        val identity = ClaimsSessionIdentity.from(session)
        if (!session.isSessionActive || session.isGuest || session.activeToken.isNullOrBlank()) {
            return Result.Failure(AppError.Unauthorized())
        }
        if (merchant && !session.isMerchantRegistered) return Result.Failure(AppError.Forbidden())
        return execute {
            val response = call()
            if (ClaimsSessionIdentity.from(sessions.sessionState.value) != identity) return@execute Result.Failure(AppError.Unauthorized())
            val body = response.body()
            if (response.code() == HTTP_OK && body != null) Result.Success(map(body)) else failure(response.code())
        }
    }

    private suspend fun <T> execute(block: suspend () -> Result<T>): Result<T> = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: SerializationException) {
        Result.Failure(AppError.Unprocessable())
    } catch (http: HttpException) {
        failure(http.code())
    } catch (_: IOException) {
        Result.Failure(AppError.Network())
    } catch (_: DateTimeParseException) {
        Result.Failure(AppError.Unprocessable())
    } catch (_: IllegalArgumentException) {
        Result.Failure(AppError.Unprocessable())
    }

    private fun failure(status: Int): Result<Nothing> = Result.Failure(
        when (status) {
            HTTP_UNAUTHORIZED -> AppError.Unauthorized()
            HTTP_FORBIDDEN -> AppError.Forbidden()
            HTTP_NOT_FOUND -> AppError.NotFound()
            HTTP_CONFLICT, HTTP_TOO_MANY_REQUESTS -> AppError.Unprocessable()
            else -> AppError.Generic()
        }
    )
}
