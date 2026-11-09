package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.UserProfile
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.DeleteAccountUseCase
import com.example.sensus.domain.usecase.LoginUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import com.example.sensus.domain.usecase.StartGuestSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSessionFlowIntegrationTest {

    @Test
    fun `full auth and guest lifecycle integration test`() = runTest {
        val authRepo = mockk<AuthRepository>()
        val sessionRepo = mockk<SessionRepository>(relaxed = true)

        val sessionStateFlow = MutableStateFlow(SessionState())
        every { sessionRepo.sessionState } returns sessionStateFlow

        val loginUseCase = LoginUseCase(authRepo)
        val registerUseCase = RegisterUseCase(authRepo)
        val guestUseCase = StartGuestSessionUseCase(authRepo, sessionRepo)
        val deleteAccountUseCase = DeleteAccountUseCase(authRepo, sessionRepo)

        // 1. Validar registro
        val registeredUser = UserProfile("u-101", "Valeria Cruz", "valeria@sensus.mx", null, listOf("Consumidor"))
        coEvery { authRepo.register("Valeria Cruz", "valeria@sensus.mx", "Valeria123!") } returns Result.Success(registeredUser)

        val regResult = registerUseCase("Valeria Cruz", "valeria@sensus.mx", "Valeria123!")
        assertTrue(regResult is Result.Success)
        assertEquals("Valeria Cruz", (regResult as Result.Success).data.nombreCompleto)

        // 2. Validar login
        coEvery { authRepo.login("valeria@sensus.mx", "Valeria123!") } returns Result.Success(registeredUser)
        val loginResult = loginUseCase("valeria@sensus.mx", "Valeria123!")
        assertTrue(loginResult is Result.Success)

        // 3. Validar sesión de invitado (restricción a 2000m)
        coEvery { authRepo.createGuestSession() } returns Result.Success("guest-jwt-token-777")
        val guestResult = guestUseCase()
        assertTrue(guestResult is Result.Success)
        coVerify(exactly = 1) { sessionRepo.saveGuestSession("guest-jwt-token-777", 2000) }

        // Validar política de radio para invitado
        assertFalse(GuestRadiusPolicy.isRadiusAllowedForGuest(3.0f))
        assertTrue(GuestRadiusPolicy.isRadiusAllowedForGuest(2.0f))
        assertEquals(2.0f, GuestRadiusPolicy.clampRadius(4.5f, isGuest = true), 0.01f)
        assertEquals(4.5f, GuestRadiusPolicy.clampRadius(4.5f, isGuest = false), 0.01f)

        // 4. Validar eliminación de cuenta con purga
        coEvery { authRepo.deleteAccount("Valeria123!") } returns Result.Success(Unit)
        val deleteResult = deleteAccountUseCase("Valeria123!")
        assertTrue(deleteResult is Result.Success)
        coVerify(exactly = 1) { sessionRepo.clearSession() }
    }
}
