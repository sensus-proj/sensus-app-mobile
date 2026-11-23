package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.FriendShare
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantActivationInput
import com.example.sensus.domain.model.MerchantCoordinates
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.model.NotificationItem
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.SharedOfferMessage
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRepositoriesDefaultsTest {

    private val sampleProfile = MerchantProfile(
        id = "m-1",
        usuarioId = "u-1",
        nombreComercial = "Panadería Centro",
        direccion = "Av. Juárez 10",
        codigoPostal = "42800",
        averageRating = 4.6,
        reviewCount = 8
    )

    private val sampleOffer = ProductOffer(
        id = "o-1",
        name = "Bolillo artesanal",
        storeName = "Panadería Centro",
        category = "Panadería",
        originalPrice = 20.0,
        discountPrice = 10.0,
        discountPercent = 50,
        unitsLeft = 5,
        distanceMeters = 150
    )

    @Test
    fun merchantRepositoryDefaultOverloadsDelegateProperly() = runTest {
        val repo = object : MerchantRepository {
            override suspend fun getMyMerchant(): Result<MerchantProfile> = Result.Success(sampleProfile)
            override suspend fun activateMerchant(
                nombreComercial: String,
                direccion: String,
                codigoPostal: String,
                latitud: Double?,
                longitud: Double?
            ): Result<MerchantProfile> = Result.Success(
                sampleProfile.copy(
                    nombreComercial = nombreComercial,
                    latitud = latitud ?: sampleProfile.latitud,
                    longitud = longitud ?: sampleProfile.longitud
                )
            )
            override suspend fun updateMerchantProfile(
                nombreComercial: String?,
                direccion: String,
                codigoPostal: String,
                horarios: List<HorarioDia>
            ): Result<MerchantProfile> = Result.Success(sampleProfile.copy(direccion = direccion))
        }

        val activated = repo.activateMerchant(
            MerchantActivationInput("Panadería Sur", "Calle 2", "06000", MerchantCoordinates(20.1, -99.2))
        )
        assertEquals("Panadería Sur", (activated as Result.Success).data.nombreComercial)

        val updatedNoCoords = repo.updateMerchantProfile(
            MerchantProfileUpdateInput("Panadería Centro", "Av. Hidalgo 5", "42800", emptyList(), null)
        )
        assertEquals("Av. Hidalgo 5", (updatedNoCoords as Result.Success).data.direccion)

        val updatedWithCoords = repo.updateMerchantProfile(
            MerchantProfileUpdateInput("Panadería Centro", "Av. Reforma 9", "42800", emptyList(), MerchantCoordinates(20.0, -99.3))
        )
        assertEquals("Av. Reforma 9", (updatedWithCoords as Result.Success).data.direccion)

        val nearby = repo.getNearbyMerchants(20.0, -99.3, 2000)
        assertTrue((nearby as Result.Success).data.isEmpty())
    }

    @Test
    fun sessionAndOffersRepositoriesDefaultsAndModelsWork() = runTest {
        var cleared = false
        val sessionRepo = object : SessionRepository {
            override val sessionState: StateFlow<SessionState> = MutableStateFlow(SessionState())
            override suspend fun saveGuestSession(token: String, maxRadiusMeters: Int) = Unit
            override suspend fun clearSession() {
                cleared = true
            }
            override suspend fun setTermsAccepted(accepted: Boolean) = Unit
            override suspend fun setLocationPermission(granted: Boolean) = Unit
            override suspend fun setSplashCompleted(completed: Boolean) = Unit
        }
        sessionRepo.clearLocalSession()
        sessionRepo.syncSession()
        sessionRepo.setMerchantStatus(true, "Mi Tienda")
        sessionRepo.setStoreOpen(false)
        assertTrue(cleared)

        val offersRepo = object : OffersRepository {
            override suspend fun republishOffer(input: RepublishOfferInput): Result<ProductOffer> = Result.Success(sampleOffer)
            override fun getOffers(): List<ProductOffer> = listOf(sampleOffer)
            override fun addOffer(offer: ProductOffer) = Unit
            override suspend fun createOffer(input: CreateOfferInput, media: UploadedMedia?): Result<ProductOffer> {
                return Result.Success(sampleOffer)
            }
        }
        assertEquals(1, (offersRepo.fetchNearbyOffers(20.0, -99.0) as Result.Success).data.size)
        assertTrue(offersRepo.fetchMerchantOffers() is Result.Failure)
        assertEquals(3, (offersRepo.updateOfferStock(sampleOffer, 3) as Result.Success).data.unitsLeft)
        assertTrue(offersRepo.changeOfferStatus("o-1", OfferLifecycleStatus.PAUSED) is Result.Failure)

        val shared = SharedOfferMessage("s-1", "o-1", "u-1", "u-2", "Café", "Cafetería", "m-1", null, 50.0, 25.0, 50, null, 1L)
        assertEquals("Oferta", shared.toProductOffer().category)
        val notification = NotificationItem("n-1", "Aviso", "Nueva oferta", "Hace 1m", "Ofertas")
        assertEquals("Aviso", notification.title)
        val share = FriendShare("Luis", "300m", 2, "Hace 5m")
        assertEquals("Luis", share.name)
        assertEquals(StoreCategory.ALL.displayName, StoreCategory.ALL.displayName)
    }
}
