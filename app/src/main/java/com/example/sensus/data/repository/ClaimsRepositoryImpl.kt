package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.EncryptedClaimIndex
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.domain.model.ClaimRedemption
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val sessions: SessionRepository,
    private val claimIndex: EncryptedClaimIndex? = null
) : ClaimsRepository {
    override suspend fun createClaim(offerId: String): Result<OfferClaim> {
        val session = sessions.sessionState.value
        val response = request(false, { api.createClaim(offerId) }, session) {
            require(it.claimId.isNotBlank() && it.offerId == offerId)
            require(it.code.isNotBlank() && it.qrPayload.isNotBlank())
            require(it.qrPayload.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES)
            OfferClaim(it.claimId, it.offerId, it.code, it.qrPayload, Instant.parse(it.expiresAtUtc).toEpochMilli())
        }
        val claim = response.getOrNull()
        val account = session.activeEmail
        if (claim != null && !account.isNullOrBlank()) rememberClaim(account, offerId, claim.claimId, session)
        return if (ClaimsSessionIdentity.from(sessions.sessionState.value) == ClaimsSessionIdentity.from(session)) response else failure(HTTP_UNAUTHORIZED)
    }

    override suspend fun restoreClaim(offerId: String): Result<OfferClaim> {
        val session = sessions.sessionState.value
        val account = session.activeEmail ?: return Result.Failure(AppError.NotFound())
        val claimId = withContext(Dispatchers.IO) { claimIndex?.findClaimId(account, offerId) }
            ?: return restoreFromClaimList(offerId, account, session)
        return restoreById(offerId, claimId, session)
    }

    private suspend fun restoreById(offerId: String, claimId: String, session: SessionState): Result<OfferClaim> =
        request(false, { api.getClaim(claimId) }, session) { it.toActiveOfferClaim(offerId, claimId) }

    /** FT-13: recupera el QR cuando la instalacion desconoce el ID de la reserva activa. */
    private suspend fun restoreFromClaimList(offerId: String, account: String, session: SessionState): Result<OfferClaim> {
        val listed = request(false, { api.getMyClaims() }, session) { claims ->
            claims.firstOrNull { it.offerId == offerId && ClaimStatus.from(it.status) == ClaimStatus.ACTIVE }
        }
        val match = when (listed) {
            is Result.Failure -> return listed
            is Result.Success -> listed.data ?: return Result.Failure(AppError.NotFound())
        }
        val restored = if (match.qrPayload.isNullOrBlank()) {
            restoreById(offerId, match.claimId, session)
        } else {
            execute { Result.Success(match.toActiveOfferClaim(offerId, match.claimId)) }
        }
        if (restored is Result.Success) rememberClaim(account, offerId, restored.data.claimId, session)
        return restored
    }

    private suspend fun rememberClaim(account: String, offerId: String, claimId: String, session: SessionState) {
        withContext(Dispatchers.IO) {
            if (ClaimsSessionIdentity.from(sessions.sessionState.value) == ClaimsSessionIdentity.from(session)) {
                claimIndex?.rememberClaimId(account, offerId, claimId)
            }
        }
    }

    override suspend fun getMyClaims(): Result<List<ClaimSummary>> = request(false, { api.getMyClaims() }) { claims ->
        claims.mapNotNull { dto -> runCatching { dto.toSummary() }.getOrNull() }
    }

    override suspend fun cancelClaim(claimId: String): Result<ClaimSummary> = request(false, { api.cancelClaim(claimId) }) {
        val summary = it.toSummary()
        require(summary.claimId == claimId && summary.status == ClaimStatus.CANCELLED)
        summary
    }

    override suspend fun redeemClaim(code: String): Result<ClaimRedemption> = request(true, { api.redeemClaim(code) }) {
        require(it.claimId.isNotBlank() && it.offerId.isNotBlank() && it.merchantId.isNotBlank())
        ClaimRedemption(it.claimId, it.offerId, it.merchantId, Instant.parse(it.redeemedAtUtc).toEpochMilli())
    }

    private suspend fun <T, R> request(
        merchant: Boolean,
        call: suspend () -> Response<T>,
        session: SessionState = sessions.sessionState.value,
        map: (T) -> R
    ): Result<R> {
        val identity = ClaimsSessionIdentity.from(session)
        if (!session.isAuthenticated() || ClaimsSessionIdentity.from(sessions.sessionState.value) != identity) {
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

private fun ClaimDetailDto.toActiveOfferClaim(offerId: String, claimId: String): OfferClaim {
    require(this.offerId == offerId && this.claimId == claimId && ClaimStatus.from(status) == ClaimStatus.ACTIVE)
    val payload = requireNotNull(qrPayload)
    require(payload.isNotBlank() && payload.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES)
    val expiresAt = Instant.parse(expiresAtUtc).toEpochMilli()
    require(expiresAt > System.currentTimeMillis())
    return OfferClaim(claimId, offerId, payload, payload, expiresAt)
}

private fun ClaimDetailDto.toSummary(): ClaimSummary {
    require(claimId.isNotBlank() && offerId.isNotBlank())
    val payload = qrPayload?.takeIf { it.isNotBlank() && it.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES }
    return ClaimSummary(
        claimId = claimId,
        offerId = offerId,
        offerTitle = offerTitle?.trim()?.takeIf { it.isNotEmpty() },
        status = ClaimStatus.from(status),
        expiresAtEpochMs = Instant.parse(expiresAtUtc).toEpochMilli(),
        qrPayload = payload
    )
}

private fun SessionState.isAuthenticated(): Boolean = isSessionActive && !isGuest && !activeToken.isNullOrBlank()
