package com.example.sensus.data.repository

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.EncryptedClaimIndex
import com.example.sensus.data.remote.api.ClaimsApiService
import com.example.sensus.data.remote.dto.CartClaimItemDto
import com.example.sensus.data.remote.dto.ClaimDetailDto
import com.example.sensus.data.remote.dto.ClaimOfferResultDto
import com.example.sensus.data.remote.dto.CreateCartClaimRequestDto
import com.example.sensus.data.remote.dto.OfferClaimItemDto
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.ClaimItemSummary
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response

private const val MAX_QR_PAYLOAD_BYTES = 2048
private const val MAX_PARALLEL_CLAIM_FETCHES = 4
private const val HTTP_OK = 200
private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
private const val HTTP_TOO_MANY_REQUESTS = 429

private sealed interface FetchClaimOutcome {
    data class Loaded(val summary: ClaimSummary?) : FetchClaimOutcome
    data object NotFound : FetchClaimOutcome
    data class Failed(val error: AppError) : FetchClaimOutcome
}

class ClaimsRepositoryImpl(
    private val api: ClaimsApiService,
    private val sessions: SessionRepository,
    private val claimIndex: EncryptedClaimIndex? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ClaimsRepository {
    override suspend fun createClaim(offerId: String): Result<OfferClaim> {
        val session = sessions.sessionState.value
        val response = request(false, { api.createClaim(offerId) }, session) {
            require(it.claimId.isNotBlank() && it.offerId == offerId)
            it.toOfferClaim()
        }
        val claim = response.getOrNull()
        val account = session.activeEmail
        if (claim != null && !account.isNullOrBlank()) rememberClaim(account, offerId, claim.claimId, session)
        return if (ClaimsSessionIdentity.from(sessions.sessionState.value) == ClaimsSessionIdentity.from(session)) response else failure(HTTP_UNAUTHORIZED)
    }

    override suspend fun createCartClaim(items: List<CartItem>): Result<OfferClaim> {
        if (items.isEmpty()) return Result.Failure(AppError.Validation("Agrega al menos un producto para apartar."))
        val single = items.singleOrNull()
        if (single != null && single.quantity == 1) return createClaim(single.offer.id)

        val session = sessions.sessionState.value
        val body = CreateCartClaimRequestDto(items.map { CartClaimItemDto(it.offer.id, it.quantity) })
        val response = request(false, { api.createCartClaim(body) }, session) {
            require(it.claimId.isNotBlank() && it.offerId.isNotBlank())
            it.toOfferClaim()
        }
        val claim = response.getOrNull()
        val account = session.activeEmail
        if (claim != null && !account.isNullOrBlank()) {
            items.map { it.offer.id }.distinct().forEach { id ->
                rememberClaim(account, id, claim.claimId, session)
            }
        }
        return if (ClaimsSessionIdentity.from(sessions.sessionState.value) == ClaimsSessionIdentity.from(session)) response else failure(HTTP_UNAUTHORIZED)
    }

    override suspend fun restoreClaim(offerId: String): Result<OfferClaim> {
        val session = sessions.sessionState.value
        val account = session.activeEmail ?: return Result.Failure(AppError.NotFound())
        val claimId = withContext(ioDispatcher) { claimIndex?.findClaimId(account, offerId) }
            ?: return Result.Failure(AppError.NotFound())
        return restoreById(offerId, claimId, session)
    }

    private suspend fun restoreById(offerId: String, claimId: String, session: SessionState): Result<OfferClaim> =
        request(false, { api.getClaim(claimId) }, session) { it.toActiveOfferClaim(offerId, claimId) }

    private suspend fun rememberClaim(account: String, offerId: String, claimId: String, session: SessionState) {
        withContext(ioDispatcher) {
            if (ClaimsSessionIdentity.from(sessions.sessionState.value) == ClaimsSessionIdentity.from(session)) {
                claimIndex?.rememberClaimId(account, offerId, claimId)
            }
        }
    }

    override suspend fun getMyClaims(): Result<List<ClaimSummary>> {
        val session = sessions.sessionState.value
        val identity = ClaimsSessionIdentity.from(session)
        return if (!session.isAuthenticated() || ClaimsSessionIdentity.from(sessions.sessionState.value) != identity) {
            Result.Failure(AppError.Unauthorized())
        } else {
            val account = session.activeEmail?.takeIf { it.isNotBlank() }
            val claimIds = if (account == null) emptyList() else withContext(ioDispatcher) { claimIndex?.getClaimIds(account).orEmpty() }
            if (account == null || claimIds.isEmpty()) {
                Result.Success(emptyList())
            } else {
                aggregateClaimOutcomes(fetchSummariesBounded(claimIds, account, identity))
            }
        }
    }

    private suspend fun fetchSummariesBounded(claimIds: List<String>, account: String, identity: ClaimsSessionIdentity): List<FetchClaimOutcome> =
        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_CLAIM_FETCHES)
            claimIds.map { claimId ->
                async(ioDispatcher) {
                    semaphore.withPermit {
                        fetchClaimSummary(claimId, account, identity)
                    }
                }
            }.awaitAll()
        }

    private suspend fun fetchClaimSummary(claimId: String, account: String, identity: ClaimsSessionIdentity): FetchClaimOutcome =
        when (val result = execute { fetchSingleClaim(claimId, identity) }) {
            is Result.Success -> FetchClaimOutcome.Loaded(result.data)
            is Result.Failure -> {
                if (result.error is AppError.NotFound) {
                    claimIndex?.removeClaimId(account, claimId)
                    FetchClaimOutcome.NotFound
                } else {
                    FetchClaimOutcome.Failed(result.error)
                }
            }
        }

    private suspend fun fetchSingleClaim(claimId: String, identity: ClaimsSessionIdentity): Result<ClaimSummary?> {
        if (ClaimsSessionIdentity.from(sessions.sessionState.value) != identity) {
            return Result.Failure(AppError.Unauthorized())
        }
        val response = api.getClaim(claimId)
        if (ClaimsSessionIdentity.from(sessions.sessionState.value) != identity) {
            return Result.Failure(AppError.Unauthorized())
        }
        val body = response.body()
        return if (response.code() == HTTP_OK && body != null) {
            Result.Success(runCatching { body.toSummary() }.getOrNull())
        } else {
            failure(response.code())
        }
    }

    private fun aggregateClaimOutcomes(outcomes: List<FetchClaimOutcome>): Result<List<ClaimSummary>> {
        val authFailure = outcomes.filterIsInstance<FetchClaimOutcome.Failed>()
            .firstOrNull { it.error is AppError.Unauthorized || it.error is AppError.Forbidden }
        if (authFailure != null) return Result.Failure(authFailure.error)
        val loaded = outcomes.filterIsInstance<FetchClaimOutcome.Loaded>().mapNotNull { it.summary }
        val firstError = outcomes.filterIsInstance<FetchClaimOutcome.Failed>().firstOrNull()?.error
        return if (firstError != null && loaded.isEmpty()) {
            Result.Failure(firstError)
        } else {
            Result.Success(loaded)
        }
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

private fun ClaimOfferResultDto.toOfferClaim(): OfferClaim {
    require(code.isNotBlank() && qrPayload.isNotBlank())
    require(qrPayload.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES)
    val mappedItems = items.toDomainItems()
    val resolvedTotal = totalUnits.coerceAtLeast(mappedItems.sumOf { it.quantity }.coerceAtLeast(1))
    return OfferClaim(
        claimId = claimId,
        offerId = offerId,
        code = code,
        qrPayload = qrPayload,
        expiresAtEpochMs = Instant.parse(expiresAtUtc).toEpochMilli(),
        totalUnits = resolvedTotal,
        items = mappedItems
    )
}

private fun List<OfferClaimItemDto>.toDomainItems(): List<ClaimItemSummary> = map {
    ClaimItemSummary(offerId = it.offerId, quantity = it.quantity.coerceAtLeast(1), offerTitle = it.offerTitle, unitPrice = it.unitPrice)
}

private fun ClaimDetailDto.toActiveOfferClaim(offerId: String, claimId: String): OfferClaim {
    val matchesOffer = this.offerId == offerId || items.any { it.offerId == offerId }
    require(matchesOffer && this.claimId == claimId && ClaimStatus.from(status) == ClaimStatus.ACTIVE)
    val payload = requireNotNull(qrPayload)
    require(payload.isNotBlank() && payload.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES)
    val expiresAt = Instant.parse(expiresAtUtc).toEpochMilli()
    require(expiresAt > System.currentTimeMillis())
    val mappedItems = items.toDomainItems()
    return OfferClaim(
        claimId = claimId,
        offerId = offerId,
        code = payload,
        qrPayload = payload,
        expiresAtEpochMs = expiresAt,
        totalUnits = totalUnits.coerceAtLeast(mappedItems.sumOf { it.quantity }.coerceAtLeast(1)),
        items = mappedItems
    )
}

private fun ClaimDetailDto.toSummary(): ClaimSummary {
    require(claimId.isNotBlank() && offerId.isNotBlank())
    val payload = qrPayload?.takeIf { it.isNotBlank() && it.toByteArray(Charsets.UTF_8).size <= MAX_QR_PAYLOAD_BYTES }
    val mappedItems = items.toDomainItems()
    val resolvedTitle = offerTitle?.trim()?.takeIf { it.isNotEmpty() }
        ?: mappedItems.firstOrNull()?.offerTitle?.trim()?.takeIf { it.isNotEmpty() }
    return ClaimSummary(
        claimId = claimId,
        offerId = offerId,
        offerTitle = resolvedTitle,
        status = ClaimStatus.from(status),
        expiresAtEpochMs = Instant.parse(expiresAtUtc).toEpochMilli(),
        qrPayload = payload,
        totalUnits = totalUnits.coerceAtLeast(mappedItems.sumOf { it.quantity }.coerceAtLeast(1)),
        items = mappedItems
    )
}

private fun SessionState.isAuthenticated(): Boolean = isSessionActive && !isGuest && !activeToken.isNullOrBlank()
