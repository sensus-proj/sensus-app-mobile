package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.media.ImageCompressor
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UploadedMedia
import com.example.sensus.domain.repository.MediaRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.CreateOfferUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishingUseCasesTest {
    private val offers = mockk<OffersRepository>()
    private val media = mockk<MediaRepository>()
    private val compressor = mockk<ImageCompressor>()
    private val input = CreateOfferInput("Pan artesanal", "Sellado", StoreCategory.BAKERY, 100.0, 60.0, 7, 2000L)
    private val offer = ProductOffer("1", "Pan artesanal", "Panadería", "Panadería", 100.0, 60.0, 40, 7, 0)
    private val uploaded = UploadedMedia("media-1", "https://example.com/photo.jpg")

    @Test
    fun offerPolicy_rejectsEachInvalidFieldIncludingNonFinitePrices() {
        val invalid = listOf(
            input.copy(title = " ") to OfferField.TITLE,
            input.copy(description = " ") to OfferField.DESCRIPTION,
            input.copy(description = "x".repeat(501)) to OfferField.DESCRIPTION,
            input.copy(category = StoreCategory.ALL) to OfferField.CATEGORY,
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
        coVerify(exactly = 0) { media.uploadAndModerate(any(), any(), any()) }
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun validOffer_compressesUploadsAndCreatesWithApprovedMedia() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), "image/jpeg", "Pan artesanal\nSellado") } returns Result.Success(uploaded)
        coEvery { offers.createOffer(input, uploaded) } returns Result.Success(offer)
        assertEquals(Result.Success(offer), CreateOfferUseCase(offers, media, compressor) { 1000L }(input, byteArrayOf(1)))
        coVerifyOrder {
            compressor.compressForUpload(any())
            media.uploadAndModerate(any(), "image/jpeg", "Pan artesanal\nSellado")
            offers.createOffer(input, uploaded)
        }
    }

    @Test
    fun rejectedPhoto_neverCreatesOffer() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), any(), any()) } returns Result.Failure(AppError.ContentRejected())
        assertTrue(CreateOfferUseCase(offers, media, compressor) { 1000L }(input, byteArrayOf(1)) is Result.Failure)
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun compressionFailureAndEmptyImage_neverUpload() = runTest {
        coEvery { compressor.compressForUpload(any()) } returns null
        val useCase = CreateOfferUseCase(offers, media, compressor) { 1000L }
        assertTrue(useCase(input, byteArrayOf(1)) is Result.Failure)
        assertTrue(useCase(input, byteArrayOf()) is Result.Failure)
        coVerify(exactly = 0) { media.uploadAndModerate(any(), any(), any()) }
    }

    @Test
    fun offerWithoutPhotoOrSpecificCategory_neverUploadsOrCreates() = runTest {
        val useCase = CreateOfferUseCase(offers, media, compressor) { 1000L }

        assertTrue((useCase(input) as Result.Failure).error is AppError.Validation)
        assertTrue((useCase(input.copy(category = StoreCategory.ALL), byteArrayOf(1)) as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { compressor.compressForUpload(any()) }
        coVerify(exactly = 0) { media.uploadAndModerate(any(), any(), any()) }
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun offerThatExpiresDuringImageUpload_neverCreates() = runTest {
        var now = 1000L
        coEvery { compressor.compressForUpload(any()) } returns byteArrayOf(2)
        coEvery { media.uploadAndModerate(any(), any(), any()) } coAnswers {
            now = input.expiresAtMillis
            Result.Success(uploaded)
        }

        val result = CreateOfferUseCase(offers, media, compressor) { now }(input, byteArrayOf(1))

        assertTrue((result as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { offers.createOffer(any(), any()) }
    }

    @Test
    fun storeCategory_mapsAllOfferCategoryApiCodesBidirectionally() {
        val expectedCodes = mapOf(
            StoreCategory.ALL to null,
            StoreCategory.GROCERY to "Grocery",
            StoreCategory.CLEANING to "CleaningHome",
            StoreCategory.PERISHABLES to "Perishables",
            StoreCategory.DAIRY to "Dairy",
            StoreCategory.FOOD to "Food",
            StoreCategory.BAKERY to "Bakery",
            StoreCategory.BEVERAGES to "Beverages",
            StoreCategory.PETS to "Pets",
            StoreCategory.PHARMACY to "Pharmacy"
        )
        expectedCodes.forEach { (category, code) ->
            assertEquals(code, category.apiCode)
            if (code != null) {
                assertEquals(category, StoreCategory.fromApiCode(code))
                assertEquals(category, StoreCategory.fromApiCode(" ${code.lowercase()} "))
                assertEquals(category.displayName, StoreCategory.displayNameFor(code))
            }
        }
        assertNull(StoreCategory.fromApiCode(null))
        assertNull(StoreCategory.fromApiCode(" "))
        assertNull(StoreCategory.fromApiCode("Moda"))
        assertEquals("Todo", StoreCategory.displayNameFor(" "))
        assertEquals("Abarrotes", StoreCategory.displayNameFor("Abarrotes"))
    }
}
