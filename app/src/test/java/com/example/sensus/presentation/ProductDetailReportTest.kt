package com.example.sensus.presentation

import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.ReportReason
import com.example.sensus.domain.model.SubmittedReport
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.domain.usecase.SubmitContentReportUseCase
import com.example.sensus.presentation.buyer.ProductDetailViewEffect
import com.example.sensus.presentation.buyer.ProductDetailViewIntent
import com.example.sensus.presentation.buyer.ProductDetailViewModel
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailReportTest {
    private val dispatcher = StandardTestDispatcher()
    private val getDetail = mockk<GetOfferDetailUseCase>()
    private val registerView = mockk<RegisterOfferViewUseCase>()
    private val submitReport = mockk<SubmitContentReportUseCase>()
    private val offer = ProductOffer("offer-1", "Pan", "Tienda", "CAFETERIAS", 100.0, 50.0, 50, 2, 100, expiresAtMillis = Long.MAX_VALUE)

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { getDetail(any()) } returns Result.Success(OfferDetails(offer, 3, false, 0))
        coEvery { registerView(any()) } returns Result.Success(Unit)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openAndSubmitOfferReport_handlesSuccessConflictAndUnauthorized() = runTest(dispatcher) {
        val vm = ProductDetailViewModel(getDetail, registerView, submitReportUseCase = submitReport)
        vm.dispatchIntent(ProductDetailViewIntent.OpenReport)
        assertTrue(vm.viewState.value.showReportDialog)

        coEvery { submitReport(any()) } returns Result.Success(
            SubmittedReport("r1", "offer", "offer-1", "fraude", false, "2026-11-21", "Gracias por reportar")
        )
        vm.viewEffect.test {
            vm.dispatchIntent(ProductDetailViewIntent.SubmitReport("offer-1", ReportReason.FRAUD, "Precio falso"))
            dispatcher.scheduler.advanceUntilIdle()
            assertFalse(vm.viewState.value.showReportDialog)
            assertEquals("Gracias por reportar", (awaitItem() as ProductDetailViewEffect.ShowMessage).message)

            coEvery { submitReport(any()) } returns Result.Failure(AppError.Unauthorized("Inicia sesión"))
            vm.dispatchIntent(ProductDetailViewIntent.OpenReport)
            vm.dispatchIntent(ProductDetailViewIntent.SubmitReport("offer-1", ReportReason.SPAM, ""))
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem() is ProductDetailViewEffect.RequireAuth)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
