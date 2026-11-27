package com.example.sensus.presentation.merchant

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val storeRepository = mockk<StoreRepository>()
    private val merchantRepository = mockk<MerchantRepository>()

    private val profile = MerchantProfile(
        id = "m-1",
        usuarioId = "u-1",
        nombreComercial = "Café Real",
        direccion = "Av. Hidalgo 12",
        codigoPostal = "42800",
        latitud = 20.05,
        longitud = -99.34,
        horarios = listOf(HorarioDia("Lunes", "09:00", "20:00", false)),
        fotoFachadaUrl = "https://cdn.example.com/cafe.jpg",
        activo = true,
        averageRating = 4.4,
        reviewCount = 12
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { storeRepository.getSampleStore() } returns MerchantStore(name = "Borrador")
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initLoadsRealRatingAndReviewCountWithoutFakeValues() = runTest(dispatcher) {
        coEvery { merchantRepository.getMyMerchant() } returns Result.Success(profile)

        val vm = MerchantProfileViewModel(
            storeRepository = storeRepository,
            updateMerchantProfileUseCase = UpdateMerchantProfileUseCase(merchantRepository),
            getMyMerchantUseCase = GetMyMerchantUseCase(merchantRepository)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("Café Real", vm.viewState.value.store.name)
        assertEquals(4.4, vm.viewState.value.store.rating, 0.01)
        assertEquals(12, vm.viewState.value.store.reviewCount)
        assertTrue(vm.viewState.value.store.isOpen)
    }

    @Test
    fun toggleStoreOpenAndRollbackAndClearMessagesWork() = runTest(dispatcher) {
        coEvery { merchantRepository.getMyMerchant() } returns Result.Success(profile)
        coEvery { merchantRepository.updateMerchantProfile(any()) } returns Result.Success(profile.copy(activo = false))

        val vm = MerchantProfileViewModel(
            storeRepository = storeRepository,
            updateMerchantProfileUseCase = UpdateMerchantProfileUseCase(merchantRepository),
            getMyMerchantUseCase = GetMyMerchantUseCase(merchantRepository)
        )
        dispatcher.scheduler.advanceUntilIdle()

        vm.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(false))
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.viewState.value.store.isOpen)
        assertTrue(vm.viewState.value.statusMessage?.contains("cerrada", ignoreCase = true) == true)

        coEvery { merchantRepository.updateMerchantProfile(any()) } returns Result.Failure(AppError.Network("Sin red"))
        vm.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(true))
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.viewState.value.store.isOpen)
        assertEquals("Sin red", vm.viewState.value.errorMessage)

        vm.dispatchIntent(MerchantProfileViewIntent.ClearMessages)
        assertNull(vm.viewState.value.errorMessage)
        assertNull(vm.viewState.value.statusMessage)
    }

    @Test
    fun toggleStoreOpen_sharesInFlightProfileRequest_andReconcilesAuthoritativeServerState() = runTest(dispatcher) {
        val gate = CompletableDeferred<Result<MerchantProfile>>()
        coEvery { merchantRepository.getMyMerchant() } coAnswers { gate.await() }
        coEvery { merchantRepository.updateMerchantProfile(any()) } returns Result.Success(profile.copy(activo = true))

        val vm = MerchantProfileViewModel(
            storeRepository = storeRepository,
            updateMerchantProfileUseCase = UpdateMerchantProfileUseCase(merchantRepository),
            getMyMerchantUseCase = GetMyMerchantUseCase(merchantRepository)
        )
        dispatcher.scheduler.runCurrent()
        vm.dispatchIntent(MerchantProfileViewIntent.ToggleStoreOpen(false))
        dispatcher.scheduler.runCurrent()

        gate.complete(Result.Success(profile))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { merchantRepository.getMyMerchant() }
        assertTrue(vm.viewState.value.store.isOpen)
        assertEquals("El servidor aún no tiene desplegado el cambio de estado de tienda.", vm.viewState.value.errorMessage)
    }
}
