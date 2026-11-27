package com.example.sensus.presentation

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.cash.turbine.test
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.MerchantProfileUpdateInput
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.DataRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.ChangeOfferStatusUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.GetPreferredMapLayerUseCase
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.domain.usecase.UpdateOfferUseCase
import com.example.sensus.presentation.main.MainScreenViewModel
import com.example.sensus.presentation.main.MainScreenViewState
import com.example.sensus.presentation.merchant.MerchantDashboardViewIntent
import com.example.sensus.presentation.merchant.MerchantDashboardViewModel
import com.example.sensus.presentation.merchant.MerchantEditOfferViewIntent
import com.example.sensus.presentation.merchant.MerchantEditOfferViewModel
import com.example.sensus.presentation.merchant.MerchantOfferFilter
import com.example.sensus.presentation.merchant.MerchantProfileViewIntent
import com.example.sensus.presentation.merchant.MerchantProfileViewModel
import com.example.sensus.presentation.merchant.encodeMerchantCameraBitmap
import com.example.sensus.presentation.merchant.readMerchantImageBytes
import com.example.sensus.presentation.push.PushNotificationPresenter
import com.example.sensus.presentation.root.AppScreen
import com.example.sensus.presentation.root.AppViewIntent
import com.example.sensus.presentation.root.AppViewModel
import com.example.sensus.presentation.splash.computeFitCenterScale
import com.example.sensus.presentation.support.HelpFaqCallbacks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantPushAndRootGapTest {

    private val testDispatcher = StandardTestDispatcher()

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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun pushNotificationPresenterHandlesDisabledAndValidNotifications() {
        mockkStatic(NotificationManagerCompat::class)
        mockkStatic(PendingIntent::class)
        mockkConstructor(Intent::class)
        mockkConstructor(NotificationCompat.Builder::class)
        mockkConstructor(NotificationCompat.BigTextStyle::class)

        val compat = mockk<NotificationManagerCompat>(relaxed = true)
        every { NotificationManagerCompat.from(any()) } returns compat
        every { compat.areNotificationsEnabled() } returnsMany listOf(false, true)
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().setFlags(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Boolean>()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<NotificationCompat.BigTextStyle>().bigText(any()) } answers {
            self as NotificationCompat.BigTextStyle
        }
        listOf(
            every { anyConstructed<NotificationCompat.Builder>().setSmallIcon(any<Int>()) },
            every { anyConstructed<NotificationCompat.Builder>().setContentTitle(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setContentText(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setStyle(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setPriority(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setVisibility(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setAutoCancel(any()) },
            every { anyConstructed<NotificationCompat.Builder>().setContentIntent(any()) }
        ).forEach { it answers { self as NotificationCompat.Builder } }
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk(relaxed = true)

        val ctx = mockk<Context>(relaxed = true) { every { getString(any()) } returns "canal_sensus" }
        PushNotificationPresenter.show(ctx, "   ", "body", emptyMap())
        PushNotificationPresenter.show(ctx, "Título", "Cuerpo", mapOf("type" to "new_offer"))
        PushNotificationPresenter.show(ctx, "Título", "Cuerpo", mapOf("type" to "new_offer", "offerId" to "o1"))
        verify(exactly = 1) { compat.notify(any(), any()) }
    }

    @Test
    fun appViewModelNavigationMainScreenAndSplashHelpersWork() = runTest {
        val faq = HelpFaqCallbacks({}, {}, {}, {}).copy(onOpenTerms = null)
        assertNull(faq.onOpenTerms)
        assertEquals(1f to 1f, computeFitCenterScale(0, 100, 100, 100))
        assertTrue(computeFitCenterScale(100, 200, 100, 100).second < 1f)

        val failDataRepo = mockk<DataRepository> { every { data } returns flow { throw IllegalStateException("boom") } }
        MainScreenViewModel(failDataRepo).viewState.test {
            awaitItem()
            assertTrue(awaitItem() is MainScreenViewState.Error)
            cancelAndIgnoreRemainingEvents()
        }

        val sessionPending = MutableStateFlow(SessionState(hasAcceptedTerms = false))
        val mapLayerUc = mockk<GetPreferredMapLayerUseCase>(relaxed = true) {
            coEvery { this@mockk.invoke() } returns MapLayer.STANDARD
        }
        val vm1 = AppViewModel(mockk(relaxed = true) { every { sessionState } returns sessionPending }, mockk(relaxed = true), mapLayerUc)
        testDispatcher.scheduler.advanceUntilIdle()
        vm1.dispatchIntent(AppViewIntent.RegistrationCompleted)
        assertEquals(AppScreen.TERMS, vm1.viewState.value.currentScreen)
        vm1.dispatchIntent(AppViewIntent.TermsAccepted(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppScreen.INTERESTS_ONBOARDING, vm1.viewState.value.currentScreen)
        vm1.dispatchIntent(AppViewIntent.InterestsOnboardingCompleted)
        assertEquals(AppScreen.MAIN, vm1.viewState.value.currentScreen)
        vm1.dispatchIntent(AppViewIntent.NavigateTo(AppScreen.FORGOT_PASSWORD))
        assertEquals(AppScreen.FORGOT_PASSWORD, vm1.viewState.value.currentScreen)

        val sessionAccepted = MutableStateFlow(SessionState(hasAcceptedTerms = true))
        val vm2 = AppViewModel(mockk(relaxed = true) { every { sessionState } returns sessionAccepted }, mockk(relaxed = true), mapLayerUc)
        testDispatcher.scheduler.advanceUntilIdle()
        vm2.dispatchIntent(AppViewIntent.TermsAccepted(true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(AppScreen.MAIN, vm2.viewState.value.currentScreen)
        vm2.dispatchIntent(AppViewIntent.RegistrationCompleted)
        assertEquals(AppScreen.INTERESTS_ONBOARDING, vm2.viewState.value.currentScreen)
    }

    @Test
    fun merchantPhotoHelpersProfileEditAndDashboardCoverRemainingLines() = runTest {
        val bitmap = mockk<Bitmap> { every { compress(Bitmap.CompressFormat.JPEG, any(), any()) } returns true }
        assertNotNull(encodeMerchantCameraBitmap(bitmap))

        val uri = mockk<Uri>()
        val resolver = mockk<ContentResolver>()
        val ctx = mockk<Context> { every { contentResolver } returns resolver }
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
        assertEquals(3, readMerchantImageBytes(ctx, uri)?.size)
        every { resolver.openInputStream(uri) } throws IOException("fail")
        assertNull(readMerchantImageBytes(ctx, uri))
        every { resolver.openInputStream(uri) } throws SecurityException("denied")
        assertNull(readMerchantImageBytes(ctx, uri))

        val storeRepo = mockk<StoreRepository>(relaxed = true) {
            every { getSampleStore() } returns MerchantStore(name = "Tienda", address = "Centro 12", postalCode = "42800")
        }
        val merchantRepo = mockk<MerchantRepository>(relaxed = true) {
            coEvery { getMyMerchant() } returns Result.Success(baseProfile)
            coEvery { updateMerchantProfile(any<MerchantProfileUpdateInput>()) } returns Result.Failure(AppError.Network("fallo update"))
        }
        val compressor = mockk<ImageCompressor>(relaxed = true) { coEvery { compressForUpload(any()) } returns byteArrayOf(1, 2, 3) }
        val mediaRepo = mockk<MediaRepository>(relaxed = true) {
            coEvery { uploadAndModerate(any(), any(), any()) } returnsMany listOf(
                Result.Failure(AppError.ContentRejected("rechazada")),
                Result.Success(UploadedMedia("m1", "")),
                Result.Success(UploadedMedia("m1", "https://cdn.sensus.mx/f.jpg"))
            )
        }
        val mpVm = MerchantProfileViewModel(
            storeRepo,
            UpdateMerchantProfileUseCase(merchantRepo),
            GetMyMerchantUseCase(merchantRepo),
            mediaRepo,
            compressor,
            null
        )
        testDispatcher.scheduler.advanceUntilIdle()
        listOf("rechazada", "No se pudo subir la foto de fachada.", "fallo update").forEach { expected ->
            mpVm.dispatchIntent(MerchantProfileViewIntent.UpdateFacadePhoto(byteArrayOf(9)))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(expected, mpVm.viewState.value.errorMessage)
        }
    }

    @Test
    fun merchantEditAndDashboardCoverRemainingLines() = runTest {
        val merchantRepo = mockk<MerchantRepository>(relaxed = true) {
            coEvery { getMyMerchant() } returns Result.Success(baseProfile)
        }
        val editVm = MerchantEditOfferViewModel(
            mockk<UpdateOfferUseCase> { coEvery { this@mockk.invoke(any()) } returns Result.Failure(AppError.Generic("err")) },
            mockk<ChangeOfferStatusUseCase> {
                coEvery { this@mockk.invoke(any(), any()) } returnsMany listOf(
                    Result.Success(baseOffer.copy(status = OfferLifecycleStatus.ARCHIVED)),
                    Result.Success(baseOffer.copy(status = OfferLifecycleStatus.ACTIVE))
                )
            },
            mockk(relaxed = true)
        )
        editVm.dispatchIntent(
            MerchantEditOfferViewIntent.SaveChanges(
                UpdateOfferInput("o1", "Pan", "Desc valida", StoreCategory.BAKERY, 40.0, 20.0, 2, System.currentTimeMillis() + 60_000L)
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("err", editVm.viewState.value.errorMessage)
        editVm.dispatchIntent(MerchantEditOfferViewIntent.ChangeStatus(baseOffer, OfferLifecycleStatus.ARCHIVED))
        testDispatcher.scheduler.advanceUntilIdle()
        editVm.dispatchIntent(MerchantEditOfferViewIntent.ChangeStatus(baseOffer, OfferLifecycleStatus.ACTIVE))
        testDispatcher.scheduler.advanceUntilIdle()

        val offer2 = baseOffer.copy(id = "offer-2")
        val dashOffers = mockk<OffersRepository>(relaxed = true) {
            coEvery { fetchMerchantOffers(any()) } returns Result.Success(listOf(baseOffer, offer2))
            coEvery { updateOfferStock(baseOffer, 0) } returns Result.Success(baseOffer.copy(unitsLeft = 0))
            coEvery { updateOfferStock(baseOffer, 5) } returnsMany listOf(
                Result.Success(baseOffer.copy(unitsLeft = 5)),
                Result.Failure(AppError.Generic("fallo stock"))
            )
        }
        val dashVm = MerchantDashboardViewModel(dashOffers, GetMyMerchantUseCase(merchantRepo))
        testDispatcher.scheduler.advanceUntilIdle()
        dashVm.dispatchIntent(MerchantDashboardViewIntent.SelectOfferFilter(MerchantOfferFilter.FINISHED))
        dashVm.dispatchIntent(MerchantDashboardViewIntent.AdjustOfferStock(baseOffer, 0))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(OfferLifecycleStatus.PAUSED, dashVm.viewState.value.allStoreOffers.first().status)
        dashVm.dispatchIntent(MerchantDashboardViewIntent.AdjustOfferStock(baseOffer, 5))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, dashVm.viewState.value.allStoreOffers.first { it.id == baseOffer.id }.unitsLeft)
        dashVm.dispatchIntent(MerchantDashboardViewIntent.AdjustOfferStock(baseOffer, 5))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("fallo stock", dashVm.viewState.value.errorMessage)
    }
}
