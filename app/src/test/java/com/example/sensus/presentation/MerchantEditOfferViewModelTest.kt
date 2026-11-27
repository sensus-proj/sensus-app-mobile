package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.ChangeOfferStatusUseCase
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.UpdateOfferUseCase
import com.example.sensus.presentation.merchant.MerchantEditOfferViewEffect
import com.example.sensus.presentation.merchant.MerchantEditOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantEditOfferViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class MerchantEditOfferViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val updateUseCase = mockk<UpdateOfferUseCase>()
    private val changeStatusUseCase = mockk<ChangeOfferStatusUseCase>()
    private val baseOffer = ProductOffer(
        id = "o_10",
        name = "Pan dulce",
        storeName = "Panadería Centro",
        category = "Panadería",
        originalPrice = 100.0,
        discountPrice = 60.0,
        discountPercent = 40,
        unitsLeft = 5,
        distanceMeters = 0,
        status = OfferLifecycleStatus.ACTIVE,
        version = 2
    )
    private val validInput = UpdateOfferInput(
        offerId = "o_10",
        title = "Pan artesanal",
        description = "Horneado hoy",
        originalPrice = 100.0,
        discountPrice = 55.0,
        stock = 6,
        expiresAtMillis = Long.MAX_VALUE,
        expectedVersion = 2
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun invalidUpdate_stopsAtFieldError_andClearsWhenEdited() = runTest(dispatcher) {
        val vm = MerchantEditOfferViewModel(updateUseCase, changeStatusUseCase, mockk())
        vm.dispatchIntent(MerchantEditOfferViewIntent.Initialize(baseOffer))
        vm.dispatchIntent(MerchantEditOfferViewIntent.SaveChanges(validInput.copy(discountPrice = 120.0)))

        assertEquals(OfferField.PRICE, vm.viewState.value.fieldError?.field)
        coVerify(exactly = 0) { updateUseCase(any()) }

        vm.dispatchIntent(MerchantEditOfferViewIntent.FieldEdited)
        assertNull(vm.viewState.value.fieldError)
    }

    @Test
    fun saveChanges_callsUseCaseOnceAndEmitsEffect() = runTest(dispatcher) {
        val updated = baseOffer.copy(name = "Pan artesanal", discountPrice = 55.0, unitsLeft = 6, version = 3)
        coEvery { updateUseCase(validInput) } returns Result.Success(updated)
        val vm = MerchantEditOfferViewModel(updateUseCase, changeStatusUseCase, mockk())

        vm.viewEffect.test {
            vm.dispatchIntent(MerchantEditOfferViewIntent.SaveChanges(validInput))
            vm.dispatchIntent(MerchantEditOfferViewIntent.SaveChanges(validInput))
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is MerchantEditOfferViewEffect.OfferUpdated)
            assertEquals(updated, (effect as MerchantEditOfferViewEffect.OfferUpdated).offer)
            expectNoEvents()
        }
        assertFalse(vm.viewState.value.isSaving)
        coVerify(exactly = 1) { updateUseCase(validInput) }
    }

    @Test
    fun changeStatus_pausesAndArchivesOffer_orReportsFailure() = runTest(dispatcher) {
        val paused = baseOffer.copy(status = OfferLifecycleStatus.PAUSED)
        coEvery { changeStatusUseCase(baseOffer, OfferLifecycleStatus.PAUSED) } returns Result.Success(paused)
        coEvery {
            changeStatusUseCase(paused, OfferLifecycleStatus.ARCHIVED)
        } returns Result.Failure(AppError.Network("Error de red"))

        val vm = MerchantEditOfferViewModel(updateUseCase, changeStatusUseCase, mockk())
        vm.viewEffect.test {
            vm.dispatchIntent(MerchantEditOfferViewIntent.ChangeStatus(baseOffer, OfferLifecycleStatus.PAUSED))
            dispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem() as MerchantEditOfferViewEffect.OfferStatusChanged
            assertEquals(OfferLifecycleStatus.PAUSED, effect.offer.status)
        }

        vm.dispatchIntent(MerchantEditOfferViewIntent.ChangeStatus(paused, OfferLifecycleStatus.ARCHIVED))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Error de red", vm.viewState.value.errorMessage)
        assertFalse(vm.viewState.value.isChangingStatus)
    }

    @Test
    fun lifecycleAndNearbyUseCases_delegateCleanlyToRepositories() = runTest {
        val offersRepo = mockk<OffersRepository>()
        val merchantRepo = mockk<MerchantRepository>()
        val mediaRepo = mockk<MediaRepository>(relaxed = true)
        val compressor = mockk<ImageCompressor>(relaxed = true)
        val uploaded = UploadedMedia("media-2", "https://example.com/updated.jpg")
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(9)
        coEvery { mediaRepo.uploadAndModerate(any(), "image/jpeg", "Pan artesanal\nHorneado hoy") } returns Result.Success(uploaded)
        coEvery { offersRepo.updateOffer(validInput) } returns Result.Success(baseOffer)
        coEvery { offersRepo.updateOffer(validInput.copy(imageUrl = uploaded.url)) } returns Result.Success(baseOffer)
        coEvery { offersRepo.changeOfferStatus("o_10", OfferLifecycleStatus.ARCHIVED) } returns
            Result.Success(baseOffer.copy(status = OfferLifecycleStatus.ARCHIVED))
        val profile = MerchantProfile("m1", "u1", "Tienda", "Calle 1", "42800")
        coEvery { merchantRepo.getNearbyMerchants(20.0, -99.0, 2000) } returns Result.Success(listOf(profile))

        val useCase = UpdateOfferUseCase(offersRepo, mediaRepo, compressor) { 1000L }
        assertTrue(useCase.invoke(validInput) is Result.Success)
        assertTrue(useCase.invoke(validInput, byteArrayOf(7)) is Result.Success)
        coVerify { mediaRepo.uploadAndModerate(any(), "image/jpeg", "Pan artesanal\nHorneado hoy") }
        assertTrue(ChangeOfferStatusUseCase(offersRepo).invoke(baseOffer, OfferLifecycleStatus.ARCHIVED) is Result.Success)
        assertTrue(
            ChangeOfferStatusUseCase(offersRepo).invoke(
                baseOffer.copy(status = OfferLifecycleStatus.ARCHIVED),
                OfferLifecycleStatus.ACTIVE
            ) is Result.Failure
        )
        val clampedRes = GetNearbyMerchantsUseCase(merchantRepo).invoke(20.0, -99.0, 5000, isGuest = true)
        assertEquals(listOf(profile), (clampedRes as Result.Success).data)
        coVerify { merchantRepo.getNearbyMerchants(20.0, -99.0, 2000) }
    }
}
