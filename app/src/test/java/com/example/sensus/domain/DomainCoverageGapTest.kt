package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.FeedSlot
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.MapStoreClusterBuilder
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.model.OfferAlternativeItem
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.OfferFeedItem
import com.example.sensus.domain.model.OfferFeedPage
import com.example.sensus.domain.model.OfferFeedRequest
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferSearchItem
import com.example.sensus.domain.model.OfferSearchPage
import com.example.sensus.domain.model.OfferSearchRequest
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.StoreClusterQuery
import com.example.sensus.domain.model.ToggleOfferLikeResult
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.ClaimsRepository
import com.example.sensus.domain.repository.DiscoveryRepository
import com.example.sensus.domain.repository.LocationRepository
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.NotificationPermissionChecker
import com.example.sensus.domain.repository.OfferDetailsRepository
import com.example.sensus.domain.repository.OfferFeedRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.sync.SyncManager
import com.example.sensus.domain.sync.SyncResult
import com.example.sensus.domain.usecase.ChangeOfferStatusUseCase
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.GetMerchantInventoryUseCase
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.GetOfferFeedUseCase
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.domain.usecase.ToggleOfferLikeUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.domain.usecase.uploadMediaIfPresent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainCoverageGapTest {

    private val baseOffer = ProductOffer(
        id = "offer-1",
        name = "Pan dulce",
        storeName = "Panadería Sol",
        address = "Centro 12",
        postalCode = "42800",
        category = "Panadería",
        originalPrice = 40.0,
        discountPrice = 20.0,
        discountPercent = 50,
        unitsLeft = 3,
        distanceMeters = 1200,
        merchantId = "m-1"
    )

    private val baseProfile = MerchantProfile(
        id = "m-1",
        usuarioId = "u-1",
        nombreComercial = "Panadería Sol",
        direccion = "Centro 12",
        codigoPostal = "42800",
        latitud = 20.0,
        longitud = -99.3,
        activo = true
    )

    @Test
    fun feedDiscoveryLocationAndPrefsDefaultImplsWork() = runTest {
        val offerFeedRepo = object : OfferFeedRepository {
            override suspend fun getFeed(request: OfferFeedRequest): Result<OfferFeedPage> = Result.Failure(AppError.Network())
        }
        assertNull(offerFeedRepo.getCachedFirstPage())

        val discoveryRepo = object : DiscoveryRepository {
            override suspend fun searchOffers(request: OfferSearchRequest): Result<OfferSearchPage> = Result.Failure(AppError.Network())
            override suspend fun getAlternatives(offerId: String, limit: Int): Result<List<OfferAlternativeItem>> = Result.Success(emptyList())
        }
        assertTrue(discoveryRepo.getSearchHistory().isEmpty())
        discoveryRepo.saveSearchQuery("pan")
        discoveryRepo.clearSearchHistory()

        val locationRepo = object : LocationRepository {
            override suspend fun getCurrentLocation(): UserLocation = UserLocation(20.0, -99.3)
            override fun hasLocationPermission(): Boolean = true
        }
        assertEquals(20.0, locationRepo.getFreshLocation().latitude, 0.0)
        assertTrue(locationRepo.hasFineLocationPermission())
        assertTrue(locationRepo.isLocationEnabled())
        val locUseCase = GetUserLocationUseCase(locationRepo)
        assertEquals(-99.3, locUseCase.fresh().longitude, 0.0)
        assertTrue(locUseCase.hasPermission())

        val mapPrefsRepo = object : MapPreferencesRepository {
            override fun peekCachedLayer(): MapLayer? = null
            override suspend fun getPreferredLayer(): MapLayer = MapLayer.STANDARD
            override suspend fun savePreferredLayer(layer: MapLayer) = Unit
        }
        assertNull(mapPrefsRepo.getPreferredRadiusMeters())
        mapPrefsRepo.savePreferredRadiusMeters(1500)
    }

    @Test
    fun profileOffersSessionClaimsFeedAndSyncDefaultsWork() = runTest {
        val profileRepo = object : ProfileRepository {
            override suspend fun getProfile(): Result<UserProfile> = Result.Failure(AppError.Network())
            override suspend fun updateProfile(nombreCompleto: String, fotoPerfilUrl: String?): Result<UserProfile> = Result.Failure(AppError.Network())
        }
        assertTrue(profileRepo.uploadProfilePhoto(byteArrayOf(1)).isFailure)

        val offersRepo = object : OffersRepository {
            override suspend fun republishOffer(input: RepublishOfferInput): Result<ProductOffer> = Result.Success(baseOffer)
            override fun getOffers(): List<ProductOffer> = emptyList()
            override fun addOffer(offer: ProductOffer) = Unit
            override suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer> = Result.Success(baseOffer)
        }
        val updInput = UpdateOfferInput("o1", "T", "D", StoreCategory.BAKERY, 10.0, 5.0, 2, System.currentTimeMillis() + 60_000L)
        assertTrue(offersRepo.updateOffer(updInput).isFailure)
        assertTrue(offersRepo.changeOfferStatus("o1", OfferLifecycleStatus.PAUSED).isFailure)
        assertTrue(GetMerchantInventoryUseCase(offersRepo)(OfferLifecycleStatus.ACTIVE).isFailure)

        val sessionRepo = object : SessionRepository {
            override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState())
            override suspend fun saveGuestSession(token: String, maxRadiusMeters: Int) = Unit
            override suspend fun clearSession() = Unit
            override suspend fun setTermsAccepted(accepted: Boolean) = Unit
            override suspend fun setLocationPermission(granted: Boolean) = Unit
            override suspend fun setSplashCompleted(completed: Boolean) = Unit
        }
        sessionRepo.clearLocalSession()

        val checker = object : NotificationPermissionChecker {
            override fun canPostNotifications(): Boolean = false
        }
        assertTrue(checker.needsNotificationPermission())

        val syncMgr = object : SyncManager {
            override val isSyncing: StateFlow<Boolean> = MutableStateFlow(false)
            override suspend fun enqueueOperation(operationType: String, payload: String): Long = 1L
            override suspend fun syncPendingOperations() = SyncResult(0, 0)
            override suspend fun clearQueue() = Unit
        }
        assertEquals(0, syncMgr.pendingOperationsCount.value)
    }

    @Test
    fun claimsFeedDefaultsAndModelsEdgeCasesWork() = runTest {
        val claimRepo = object : ClaimsRepository {
            override suspend fun restoreClaim(offerId: String) = Result.Failure(AppError.NotFound())
            override suspend fun createClaim(offerId: String) = Result.Success(OfferClaim("c1", offerId, "Q1", "S1", 0L))
            override suspend fun redeemClaim(code: String) = Result.Failure(AppError.NotFound())
            override suspend fun getMyClaims() = Result.Success(emptyList<ClaimSummary>())
        }
        assertTrue(claimRepo.createCartClaim(emptyList()).isFailure)
        assertTrue(claimRepo.createCartClaim(listOf(CartItem(baseOffer, 2))).isSuccess)

        assertEquals("1.2 km", baseOffer.formattedDistance)
        assertEquals("800 m", baseOffer.copy(distanceMeters = 800).formattedDistance)
        assertEquals(20.0, baseOffer.savedAmount, 0.01)
        assertEquals(baseOffer.couponCode, ApartadoOrder.fromSingleOffer(baseOffer, 1).couponCode)
        assertEquals("En revisión", ModerationResult.Quarantine("En revisión", mapOf("sexual" to 0.1)).reason)
        assertEquals(OfferLifecycleStatus.EXPIRED, OfferLifecycleStatus.fromBackend("expired"))
        assertEquals(OfferLifecycleStatus.SOLD_OUT, OfferLifecycleStatus.fromBackend("sold_out"))
        assertEquals(ClaimStatus.CANCELLED, ClaimStatus.from("canceled"))
        assertTrue(ClaimsSessionIdentity.from(SessionState(activeEmail = "a@b.c")).hashCode() != 0)
    }

    @Test
    fun zeroPriceDiscountAndClusterBuilderBranchesWork() = runTest {
        val searchZero = OfferSearchItem(
            "o1", "Pan", "Pan fresco", "BAKERY", "Panadería", null, 0.0, 10.0, 1, 100, "", null, "Tienda", 1.0
        ).toProductOffer()
        val altZero = OfferAlternativeItem(
            "o2", "Leche", "DAIRY", "Lácteos", null, 0.0, 15.0, 2, 100, "", null, "Tienda"
        ).toProductOffer()
        val feedZero = OfferFeedItem(
            "o3", "Café", "BAKERY", "Pan", null, 0.0, 10.0, 1, 100, "", "m1", "Tienda", FeedSlot.RANKED
        ).toProductOffer()
        assertEquals(0, searchZero.discountPercent + altZero.discountPercent + feedZero.discountPercent)

        val emptyValid = baseProfile.copy(id = "m-empty", nombreComercial = "Tienda Vacía", latitud = 20.001, longitud = -99.301)
        val invalidCoords = MerchantProfile("m-bad", "u-2", "Sin GPS", "Dir", "42800", latitud = 0.0, longitud = 0.0)
        val aligned = baseOffer.copy(
            id = "o-cat",
            name = "Croissant",
            storeName = "Otra",
            merchantId = "m-1",
            latitude = 20.002,
            longitude = -99.302
        )
        val noMatch = baseOffer.copy(id = "o-bad", name = "Agua", category = "Bebidas", merchantId = null, storeName = "")

        val clusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(
                UserLocation(20.0, -99.3),
                listOf(aligned, noMatch),
                listOf(baseProfile, emptyValid, invalidCoords),
                "Panadería",
                StoreCategory.ALL,
                5.0f
            )
        )
        assertEquals(1, clusters.size)
        assertTrue(clusters.first().formattedDistance.endsWith("km") || clusters.first().formattedDistance.endsWith("m"))

        val emptyClusters = MapStoreClusterBuilder.buildClusters(
            StoreClusterQuery(
                UserLocation(20.0, -99.3),
                listOf(aligned.copy(merchantId = "m-bad")),
                listOf(invalidCoords, emptyValid),
                "Vacía",
                StoreCategory.ALL,
                5.0f
            )
        )
        assertTrue(emptyClusters.any { it.storeName == "Tienda Vacía" })
    }

    @Test
    fun useCaseValidationAndFailurePathsAreCovered() = runTest {
        val guestSession = mockk<SessionRepository> { every { sessionState } returns MutableStateFlow(SessionState(true, true)) }
        val authSession = mockk<SessionRepository> { every { sessionState } returns MutableStateFlow(SessionState(true, false)) }

        val offerDetailsRepo = mockk<OfferDetailsRepository>(relaxed = true) {
            coEvery { toggleLike("o1") } returns Result.Success(ToggleOfferLikeResult("o1", 4, true))
        }
        assertEquals(4, (ToggleOfferLikeUseCase(offerDetailsRepo)("o1") as Result.Success).data.likesCount)

        val updateMerchant = UpdateMerchantProfileUseCase(mockk<MerchantRepository>(relaxed = true))
        assertTrue(updateMerchant("pendejo", "Av. Reforma 123", "42800", emptyList()).isFailure)
        assertTrue(updateMerchant("Panadería", "pinche calle 12", "42800", emptyList()).isFailure)
        assertTrue(UpdateProfileUseCase(mockk(relaxed = true))("pendejo", null).isFailure)
        assertTrue(GetMyInterestsUseCase(mockk(relaxed = true), guestSession)().isFailure)

        val offerFeedRepo = mockk<OfferFeedRepository>(relaxed = true) {
            coEvery { getCachedFirstPage() } returns OfferFeedPage(emptyList(), 1, 20, 0, 0, null, "v1")
        }
        assertNotNull(GetOfferFeedUseCase(offerFeedRepo, authSession).loadCachedFirstPage())
        assertTrue(ChangeOfferStatusUseCase(mockk(relaxed = true))(baseOffer, OfferLifecycleStatus.SOLD_OUT).isFailure)
        assertNull((uploadMediaIfPresent(mockk(relaxed = true), mockk(relaxed = true), null) as Result.Success).data)

        val claimsRepo = mockk<ClaimsRepository>(relaxed = true) {
            coEvery { createClaim("offer-1") } returns Result.Success(OfferClaim("c1", "offer-1", "Q1", "S1", 0L))
        }
        assertTrue(CreateClaimUseCase(claimsRepo)(emptyList()).isFailure)
        assertTrue(CreateClaimUseCase(claimsRepo)(listOf(CartItem(baseOffer, 1))).isSuccess)
    }
}
