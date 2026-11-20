package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.CreateOfferInput
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.moderation.ProfanityFilter
import com.example.sensus.domain.repository.AuthRepository
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.RegisterUseCase
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfanityFilterTest {

    @Test
    fun containsProfanity_detectsMexicanAndObfuscatedSwears() {
        val offensiveSamples = listOf(
            "pito",
            "puta",
            "PITO y PUTA en promo",
            "p.i.t.o",
            "puuuuuta",
            "p1t0",
            "pUt4",
            "Abarrotes El Pendejo"
        )
        offensiveSamples.forEach { sample ->
            assertTrue("Expected profanity detection for: $sample", ProfanityFilter.containsProfanity(sample))
        }
    }

    @Test
    fun containsProfanity_allowsCleanWords() {
        val cleanSamples = listOf(
            "Computadora portátil",
            "Disputa comercial justa",
            "Pistón hidráulico nuevo",
            "Peras dulces 1kg",
            "Cafetería La Esperanza"
        )
        cleanSamples.forEach { sample ->
            assertFalse("Unexpected profanity false positive for: $sample", ProfanityFilter.containsProfanity(sample))
        }
    }

    @Test
    fun offerPublishingPolicy_blocksProfanityInTitleAndDescription() {
        val now = 1_700_000_000_000L
        val badTitle = OfferPublishingPolicy.validate(
            CreateOfferInput("Oferta de pito", "Producto fresco y limpio", StoreCategory.GROCERY, 100.0, 80.0, 5, now + 86_400_000L),
            now
        )
        assertNotNull(badTitle)
        assertEquals(OfferField.TITLE, badTitle?.field)

        val badDesc = OfferPublishingPolicy.validate(
            CreateOfferInput("Pan artesanal", "Esta madre esta puta buena", StoreCategory.BAKERY, 100.0, 80.0, 5, now + 86_400_000L),
            now
        )
        assertNotNull(badDesc)
        assertEquals(OfferField.DESCRIPTION, badDesc?.field)

        val clean = OfferPublishingPolicy.validate(
            CreateOfferInput("Computadora HP", "Equipo en excelente estado", StoreCategory.ALL, 5000.0, 4200.0, 2, now + 86_400_000L),
            now
        )
        assertNull(clean)
    }

    @Test
    fun registerAndActivateMerchant_blockOffensiveNamesBeforeCallingNetwork() = runTest {
        val authRepo = mockk<AuthRepository>(relaxed = true)
        val merchantRepo = mockk<MerchantRepository>(relaxed = true)

        val registerResult = RegisterUseCase(authRepo)("Juan Puto", "juan@sensus.com", "Password123")
        assertTrue(registerResult is Result.Failure)
        assertTrue((registerResult as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { authRepo.register(any(), any(), any()) }

        val merchantResult = ActivateMerchantUseCase(merchantRepo)("Tienda Pita y Pito", "Av. Reforma 123", "06000")
        assertTrue(merchantResult is Result.Failure)
        assertTrue((merchantResult as Result.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { merchantRepo.activateMerchant(any(), any(), any(), any(), any()) }
    }

    @Test
    fun containsProfanity_handlesNullBlankPunctuationPhrasesAndAllLeetSymbols() {
        assertFalse(ProfanityFilter.containsProfanity(null))
        assertFalse(ProfanityFilter.containsProfanity("   "))
        assertFalse(ProfanityFilter.containsProfanity("...!!!---"))
        assertFalse(ProfanityFilter.containsProfanity("mi hijo de buenos modales"))
        assertTrue(ProfanityFilter.containsProfanity("h1j0 d3 pu74"))
        assertTrue(ProfanityFilter.containsProfanity("|diot4 y \$h!7 @sshole p3nd3j0"))
        assertTrue(ProfanityFilter.containsProfanity("eres un hijo de putamala luego hijo de puta"))
        assertTrue(ProfanityFilter.containsProfanity("f u c k"))
    }
}
