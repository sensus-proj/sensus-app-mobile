package com.example.sensus.data

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.data.local.SecurePreferences
import com.example.sensus.data.local.StoredMerchantStatus
import com.example.sensus.data.remote.NetworkClient
import com.example.sensus.data.repository.RetrofitAuthRepository
import com.example.sensus.data.repository.RetrofitMerchantRepository
import com.example.sensus.data.repository.RetrofitProfileRepository
import com.example.sensus.domain.model.HorarioDia
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RetrofitRepositoriesTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var securePreferences: SecurePreferences
    private lateinit var networkClient: NetworkClient

    private lateinit var authRepo: RetrofitAuthRepository
    private lateinit var profileRepo: RetrofitProfileRepository
    private lateinit var merchantRepo: RetrofitMerchantRepository

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        securePreferences = mockk(relaxed = true)
        val baseUrl = mockWebServer.url("/").toString()
        networkClient = NetworkClient(securePreferences, baseUrl)

        authRepo = RetrofitAuthRepository(networkClient.authApi, securePreferences)
        profileRepo = RetrofitProfileRepository(networkClient.profileApi)
        merchantRepo = RetrofitMerchantRepository(networkClient.merchantApi)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun authRepo_requestPasswordReset_shouldSucceedOn202() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(202)
                .setBody("{\"message\":\"Correo enviado\"}")
        )

        val result = authRepo.requestPasswordReset("usuario@sensus.app")
        assertTrue(result is Result.Success)

        val request = mockWebServer.takeRequest()
        assertEquals("/api/v1/auth/recover", request.path)
        assertTrue(request.body.readUtf8().contains("usuario@sensus.app"))
    }

    @Test
    fun authRepo_createGuestSession_shouldSaveTokenAndReturnIt() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"token\":\"guest_jwt_real_token\",\"tokenType\":\"Bearer\"," +
                        "\"maxRadiusMeters\":2000,\"role\":\"Invitado\"}"
                )
        )

        val result = authRepo.createGuestSession()
        assertTrue(result is Result.Success)
        val token = (result as Result.Success).data
        assertEquals("guest_jwt_real_token", token)

        verify(exactly = 1) { securePreferences.saveGuestSession("guest_jwt_real_token", 2000) }
    }

    @Test
    fun authRepo_logout_shouldPostWithBearerTokenAndClearPreferences() = runTest {
        every { securePreferences.getAuthToken() } returns "jwt-logout-123"
        mockWebServer.enqueue(MockResponse().setResponseCode(204))

        val result = authRepo.logout()
        assertTrue(result is Result.Success)

        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/logout", request.path)
        assertEquals("Bearer jwt-logout-123", request.getHeader("Authorization"))
        verify(exactly = 1) { securePreferences.clearAll() }
    }

    @Test
    fun authRepo_deleteAccount_shouldClearPreferencesOn204() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(204)
        )

        val result = authRepo.deleteAccount("MySecurePass123#")
        assertTrue(result is Result.Success)

        verify(exactly = 1) { securePreferences.clearAll() }
    }

    @Test
    fun profileRepo_getProfile_shouldReturnMappedUserProfile() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"id\":\"usr_999\",\"nombreCompleto\":\"Carlos Gómez\"," +
                        "\"correo\":\"carlos@test.com\",\"roles\":[\"Consumidor\"]," +
                        "\"estatus\":true,\"fechaRegistro\":\"2026-10-29T10:00:00Z\"}"
                )
        )

        val result = profileRepo.getProfile()
        assertTrue(result is Result.Success)
        val profile = (result as Result.Success).data
        assertEquals("usr_999", profile.id)
        assertEquals("Carlos Gómez", profile.nombreCompleto)
        assertEquals("carlos@test.com", profile.correo)
    }

    @Test
    fun profileRepo_updateProfile_shouldSendRequestAndReturnUpdatedUser() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"id\":\"usr_999\",\"nombreCompleto\":\"Carlos Modificado\"," +
                        "\"correo\":\"carlos@test.com\",\"roles\":[\"Consumidor\"]," +
                        "\"estatus\":true,\"fechaRegistro\":\"2026-10-29T10:00:00Z\"," +
                        "\"fotoPerfilUrl\":\"https://img.url/1\"}"
                )
        )

        val result = profileRepo.updateProfile("Carlos Modificado", "https://img.url/1")
        assertTrue(result is Result.Success)
        val profile = (result as Result.Success).data
        assertEquals("Carlos Modificado", profile.nombreCompleto)
        assertEquals("https://img.url/1", profile.fotoPerfilUrl)
    }

    @Test
    fun merchantRepo_getMyMerchant_shouldQueryMerchantsMeEndpoint() = runTest {
        every { securePreferences.getAuthToken() } returns "jwt-merchant-456"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"id\":\"merch_1\",\"usuarioId\":\"usr_1\"," +
                        "\"nombreComercial\":\"Panadería La Espiga\"," +
                        "\"direccion\":\"Av. Juárez 10\",\"codigoPostal\":\"42800\"," +
                        "\"latitud\":20.05,\"longitud\":-99.34,\"activo\":true," +
                        "\"fechaRegistro\":\"2026-10-29T10:00:00Z\"}"
                )
        )

        val result = merchantRepo.getMyMerchant()
        assertTrue(result is Result.Success)

        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/merchants/me", request.path)
        assertEquals("Bearer jwt-merchant-456", request.getHeader("Authorization"))

        val merchant = (result as Result.Success).data
        assertEquals("merch_1", merchant.id)
        assertEquals("Panadería La Espiga", merchant.nombreComercial)
        assertEquals("42800", merchant.codigoPostal)
    }

    @Test
    fun merchantRepo_updateMerchantProfile_shouldRecalculateCoordinates() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    "{\"id\":\"merch_1\",\"usuarioId\":\"usr_1\",\"nombreComercial\":\"Café Roma\"," +
                        "\"direccion\":\"Roma Norte 55\",\"codigoPostal\":\"06700\"," +
                        "\"latitud\":19.418000,\"longitud\":-99.162000,\"activo\":true," +
                        "\"fechaRegistro\":\"2026-10-29T10:00:00Z\"}"
                )
        )

        val result = merchantRepo.updateMerchantProfile(
            nombreComercial = "Café Roma",
            direccion = "Roma Norte 55",
            codigoPostal = "06700",
            horarios = listOf(HorarioDia("Lunes", "07:00", "22:00", false))
        )

        assertTrue(result is Result.Success)
        val merchant = (result as Result.Success).data
        assertEquals("Café Roma", merchant.nombreComercial)
        assertEquals(19.418000, merchant.latitud, 0.0001)
        assertEquals(-99.162000, merchant.longitud, 0.0001)
    }

    @Test
    fun merchantRepo_updateMerchantProfile_shouldMap422ToUnprocessable() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("{\"detail\":\"No fue posible geocodificar la dirección ingresada.\"}")
        )

        val result = merchantRepo.updateMerchantProfile(
            nombreComercial = "Café Invalido",
            direccion = "Calle Fantasma 99999",
            codigoPostal = "00000",
            horarios = emptyList()
        )

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Unprocessable)
    }

    @Test
    fun merchantRepo_activateMerchant_shouldSaveNewJwtAndMerchantStatusOn201() = runTest {
        var sessionUpdated = false
        val repoWithPrefs = RetrofitMerchantRepository(
            merchantApi = networkClient.merchantApi,
            securePreferences = securePreferences,
            onSessionUpdated = { sessionUpdated = true }
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    "{\"mensaje\":\"Comercio activado\",\"comercioId\":\"merch_1\",\"esComerciante\":true," +
                        "\"roles\":[\"Consumidor\",\"Comerciante\"]," +
                        "\"comercio\":{\"id\":\"merch_1\",\"usuarioId\":\"usr_1\",\"nombreComercial\":\"Café Luna\"," +
                        "\"direccion\":\"Av. Reforma 222\",\"codigoPostal\":\"06600\",\"latitud\":19.427,\"longitud\":-99.163}," +
                        "\"token\":\"new_jwt_merchant\",\"tokenType\":\"Bearer\"}"
                )
        )

        val result = repoWithPrefs.activateMerchant(
            nombreComercial = "Café Luna",
            direccion = "Av. Reforma 222",
            codigoPostal = "06600",
            latitud = 19.427,
            longitud = -99.163
        )

        assertTrue(result is Result.Success)
        val merchant = (result as Result.Success).data
        assertEquals("Café Luna", merchant.nombreComercial)
        assertEquals(19.427, merchant.latitud, 0.0001)
        verify { securePreferences.saveAuthToken("new_jwt_merchant") }
        verify {
            securePreferences.storedMerchantStatus = StoredMerchantStatus(
                isRegistered = true,
                storeName = "Café Luna",
                address = "Av. Reforma 222",
                postalCode = "06600"
            )
        }
        assertTrue(sessionUpdated)
    }

    @Test
    fun merchantRepo_activateMerchant_shouldMap409And400ToValidation() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setBody("{\"detail\":\"El usuario ya tiene un comercio activo.\"}")
        )
        val conflictResult = merchantRepo.activateMerchant("Café Sol", "Av. Reforma 10", "06600")
        assertTrue(conflictResult is Result.Failure)
        assertTrue((conflictResult as Result.Failure).error is AppError.Validation)
        assertEquals("El usuario ya tiene un comercio activo.", conflictResult.error.message)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("{\"detail\":\"Coordenadas fuera de rango.\"}")
        )
        val badRequestResult = merchantRepo.activateMerchant("Café Sol", "Av. Reforma 10", "06600", 95.0, -99.1)
        assertTrue(badRequestResult is Result.Failure)
        assertTrue((badRequestResult as Result.Failure).error is AppError.Validation)
    }

    @Test
    fun merchantRepo_activateMerchant_shouldFallbackToComerciosWhenActivateRouteReturns404() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(
                    "{\"mensaje\":\"Creado\",\"comercioId\":\"merch_legacy\",\"esComerciante\":true," +
                        "\"comercio\":{\"id\":\"merch_legacy\",\"usuarioId\":\"usr_1\",\"nombreComercial\":\"Tienda UTTT\"," +
                        "\"direccion\":\"Carretera Tula-Tepeji\",\"codigoPostal\":\"42800\",\"latitud\":20.0536,\"longitud\":-99.3436}," +
                        "\"token\":\"jwt_legacy\"}"
                )
        )

        val result = merchantRepo.activateMerchant("Tienda UTTT", "Carretera Tula-Tepeji", "42800", 20.0536, -99.3436)
        assertTrue(result is Result.Success)
        assertEquals("Tienda UTTT", (result as Result.Success).data.nombreComercial)

        val firstReq = mockWebServer.takeRequest()
        val secondReq = mockWebServer.takeRequest()
        assertEquals("/api/v1/merchants/activate", firstReq.path)
        assertEquals("/api/v1/comercios", secondReq.path)
    }
}
