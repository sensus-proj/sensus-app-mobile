package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.CreatePostInput
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.FeedQuery
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.PagedFeed
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.FeedRepository
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import com.example.sensus.domain.usecase.CreateFeedPostUseCase
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.domain.usecase.GetNearbyFeedUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishingUseCasesTest {
    private val offers = mockk<OffersRepository>()
    private val media = mockk<MediaRepository>()
    private val compressor = mockk<ImageCompressor>()
    private val feed = mockk<FeedRepository>()
    private val session = mockk<SessionRepository>()
    private val input = CreateOfferInput("Pan artesanal", "Sellado", StoreCategory.BAKERY, 100.0, 60.0, 7, 2000L)
    private val offer = ProductOffer("1", "Pan artesanal", "Panadería", "Panadería", 100.0, 60.0, 40, 7, 0)
    private val uploaded = UploadedMedia("media-1", "https://example.com/photo.jpg")
    private val post = FeedPost("post-1", "Vecino", "Oferta", "2026-11-12T12:00:00Z")

    @Test
    fun offerPolicy_rejectsEachInvalidFieldIncludingNonFinitePrices() {
        val invalid = listOf(
            input.copy(title = " ") to OfferField.TITLE,
            input.copy(description = "x".repeat(501)) to OfferField.DESCRIPTION,
            input.copy(originalPrice = Double.NaN) to OfferField.PRICE,
            input.copy(discountPrice = Double.POSITIVE_INFINITY) to OfferField.PRICE,
            input.copy(originalPrice = 0.0) to OfferField.PRICE,
            input.copy(discountPrice = 0.0) to OfferField.PRICE,
            input.copy(discountPrice = 100.0) to OfferField.PRICE,
            input.copy(stock = 0) to OfferField.STOCK,
            input.copy(stock = 1000) to OfferField.STOCK,
            input.copy(expiresAtMillis = 1000L) to OfferField.EXPIRATION
        )
        invalid.forEach { (value, field) -> assertEquals(field, OfferPublishingPolicy.validate(value, 1000L)?.field) }
        assertNull(OfferPublishingPolicy.validate(input, 1000L))
        assertEquals(40, OfferPublishingPolicy.discountPercent(100.0, 60.0))
        assertEquals(0, OfferPublishingPolicy.discountPercent(0.0, 60.0))
        assertEquals(0.5, OfferPublishingPolicy.savedAmount(100.0, 99.5), 0.001)
    }

    @Test
    fun invalidOffer_neverUploadsOrCreates() = runTest {
        val result = CreateOfferUseCase(offers, media, compressor) { 1000L }(input.copy(stock = 0), byteArrayOf(1))
        assertTrue(result is Result.Failure)
        coVerify(exactly = 0) { media.uploadAndModerate(any(), any()) }
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun validOffer_compressesUploadsAndCreatesWithApprovedMedia() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), "image/jpeg") } returns Result.Success(uploaded)
        coEvery { offers.createOffer(input, uploaded) } returns Result.Success(offer)
        assertEquals(Result.Success(offer), CreateOfferUseCase(offers, media, compressor) { 1000L }(input, byteArrayOf(1)))
        coVerifyOrder {
            compressor.compressForUpload(any())
            media.uploadAndModerate(any(), "image/jpeg")
            offers.createOffer(input, uploaded)
        }
    }

    @Test
    fun rejectedPhoto_neverCreatesOffer() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), any()) } returns Result.Failure(AppError.ContentRejected())
        assertTrue(CreateOfferUseCase(offers, media, compressor) { 1000L }(input, byteArrayOf(1)) is Result.Failure)
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun compressionFailureAndEmptyImage_neverUpload() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns null
        val useCase = CreateOfferUseCase(offers, media, compressor) { 1000L }
        assertTrue(useCase(input, byteArrayOf(1)) is Result.Failure)
        assertTrue(useCase(input, byteArrayOf()) is Result.Failure)
        coVerify(exactly = 0) { media.uploadAndModerate(any(), any()) }
    }

    @Test
    fun offerWithoutPhoto_createsWithoutUpload() = runTest {
        coEvery { offers.createOffer(input, null) } returns Result.Success(offer)
        assertEquals(Result.Success(offer), CreateOfferUseCase(offers, media, compressor) { 1000L }(input))
        coVerify(exactly = 0) { compressor.compressForUpload(any()) }
    }

    @Test
    fun guestAndInactiveUser_cannotCreatePost() = runTest {
        val state = MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))
        every { session.sessionState } returns state
        val useCase = CreateFeedPostUseCase(feed, session, media, compressor)
        assertTrue(useCase(CreatePostInput("Hola", 20.0, -99.0)) is Result.Failure)
        state.value = SessionState()
        assertTrue(useCase(CreatePostInput("Hola", 20.0, -99.0)) is Result.Failure)
        coVerify(exactly = 0) { feed.createPost(any(), any()) }
    }

    @Test
    fun invalidTextOrLocation_doesNotCreatePost() = runTest {
        every { session.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true))
        val useCase = CreateFeedPostUseCase(feed, session, media, compressor)
        listOf(
            CreatePostInput(" ", 20.0, -99.0),
            CreatePostInput("x".repeat(1001), 20.0, -99.0),
            CreatePostInput("Hola", Double.NaN, -99.0)
        ).forEach { assertTrue(useCase(it) is Result.Failure) }
        coVerify(exactly = 0) { feed.createPost(any(), any()) }
    }

    @Test
    fun post_trimsTextAndPreservesMerchantAndCoordinates() = runTest {
        every { session.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true))
        val input = CreatePostInput(" Oferta ", 20.0, -99.0, "store-1")
        coEvery { feed.createPost(input.copy(text = "Oferta"), null) } returns Result.Success(post)
        assertEquals(Result.Success(post), CreateFeedPostUseCase(feed, session, media, compressor)(input))
    }

    @Test
    fun postPhotoRejected_blocksPostCreation() = runTest {
        every { session.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true))
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), any()) } returns Result.Failure(AppError.ContentRejected())
        assertTrue(CreateFeedPostUseCase(feed, session, media, compressor)(CreatePostInput("Hola", 20.0, -99.0), byteArrayOf(1)) is Result.Failure)
        coVerify(exactly = 0) { feed.createPost(any(), any()) }
    }

    @Test
    fun nearbyFeed_clampsGuestRadiusAndPageSize() = runTest {
        every { session.sessionState } returns MutableStateFlow(SessionState(isSessionActive = true, isGuest = true))
        coEvery { feed.nearby(any()) } returns Result.Success(PagedFeed(emptyList(), 1, false))
        GetNearbyFeedUseCase(feed, session)(FeedQuery(20.0, -99.0, radiusMeters = 5000, pageSize = 200))
        coVerify { feed.nearby(FeedQuery(20.0, -99.0, radiusMeters = 2000, pageSize = 100)) }
        assertTrue(GetNearbyFeedUseCase(feed, session)(FeedQuery(91.0, -99.0)) is Result.Failure)
        assertTrue(GetNearbyFeedUseCase(feed, session)(FeedQuery(20.0, -99.0, page = 0)) is Result.Failure)
    }
}
