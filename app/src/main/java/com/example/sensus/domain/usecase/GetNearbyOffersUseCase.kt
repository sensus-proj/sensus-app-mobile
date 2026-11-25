package com.example.sensus.domain.usecase

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.NEARBY_DEFAULT_PAGE_SIZE
import com.example.sensus.domain.model.NEARBY_MAX_PAGE_SIZE
import com.example.sensus.domain.model.NearbyOffersQuery
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.OffersRepository
import javax.inject.Inject
import kotlin.math.roundToInt

private const val MAX_LATITUDE = 90.0
private const val MAX_LONGITUDE = 180.0
private const val BACKEND_MIN_RADIUS_KM = 1.0
private const val METERS_PER_KM = 1000f

class GetNearbyOffersUseCase @Inject constructor(
    private val repository: OffersRepository,
    private val preferences: MapPreferencesRepository?
) {

    constructor(repository: OffersRepository) : this(repository, null)

    suspend fun getPreferredRadiusKm(): Float? = preferences?.getPreferredRadiusMeters()?.let { it / METERS_PER_KM }

    suspend fun savePreferredRadiusKm(radiusKm: Float) {
        preferences?.savePreferredRadiusMeters((radiusKm * METERS_PER_KM).roundToInt())
    }

    suspend fun readCachedOffers(): List<ProductOffer> = repository.readCachedOffers()

    suspend operator fun invoke(location: UserLocation, radiusKm: Float, isGuest: Boolean): Result<List<ProductOffer>> {
        if (!isValidSearch(location, radiusKm)) {
            return Result.Failure(AppError.Validation())
        }
        return repository.fetchNearbyOffers(location.latitude, location.longitude, backendRadiusKm(radiusKm, isGuest))
    }

    suspend fun page(location: UserLocation, radiusKm: Float, isGuest: Boolean, page: Int, pageSize: Int = NEARBY_DEFAULT_PAGE_SIZE): Result<PagedOffers> {
        if (!isValidSearch(location, radiusKm) || page < 1 || pageSize !in 1..NEARBY_MAX_PAGE_SIZE) return Result.Failure(AppError.Validation())
        return repository.fetchNearbyOffersPage(
            NearbyOffersQuery(location, backendRadiusKm(radiusKm, isGuest), page = page, pageSize = pageSize)
        )
    }

    // The nearby endpoint rejects sub-kilometer queries for registered users.
    // The map and list apply the selected distance to the returned offers.
    private fun backendRadiusKm(radiusKm: Float, isGuest: Boolean): Double = nearbyRadiusKm(radiusKm, isGuest).toDouble().coerceAtLeast(BACKEND_MIN_RADIUS_KM)

    private fun isValidSearch(location: UserLocation, radiusKm: Float): Boolean =
        location.latitude in -MAX_LATITUDE..MAX_LATITUDE && location.longitude in -MAX_LONGITUDE..MAX_LONGITUDE && radiusKm.isFinite()
}

fun nearbyRadiusKm(radiusKm: Float, isGuest: Boolean): Float =
    if (isGuest) GuestRadiusPolicy.GUEST_MAX_RADIUS_KM else GuestRadiusPolicy.clampRadius(radiusKm, isGuest = false)
