package com.example.sensus.domain

import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.GeoCoordinatePolicy
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.ResolvedAddress
import com.example.sensus.domain.repository.MerchantRepository
import com.example.sensus.domain.repository.ReverseGeocodingRepository
import com.example.sensus.domain.usecase.ActivateMerchantUseCase
import com.example.sensus.domain.usecase.GetMyMerchantUseCase
import com.example.sensus.domain.usecase.ResolveAddressFromCoordinatesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantActivationUseCasesTest {

    @Test
    fun geoCoordinatePolicy_validatesPairsAndRanges() {
        assertNull(GeoCoordinatePolicy.validatePair(null, null))
        assertNull(GeoCoordinatePolicy.validatePair(19.4326, -99.1332))
        assertNotNull(GeoCoordinatePolicy.validatePair(19.4326, null))
        assertNotNull(GeoCoordinatePolicy.validatePair(null, -99.1332))
        assertNotNull(GeoCoordinatePolicy.validateRequired(-90.1, -99.1332))
        assertNotNull(GeoCoordinatePolicy.validateRequired(90.1, -99.1332))
        assertNotNull(GeoCoordinatePolicy.validateRequired(19.43, -180.1))
        assertNotNull(GeoCoordinatePolicy.validateRequired(19.43, 180.1))
        assertNotNull(GeoCoordinatePolicy.validateRequired(Double.NaN, -99.13))
        assertNotNull(GeoCoordinatePolicy.validateRequired(19.43, Double.POSITIVE_INFINITY))
    }

    @Test
    fun activateMerchantUseCase_succeedsWithTrimmedFieldsAndCoordinates() = runTest {
        val repo = mockk<MerchantRepository>()
        val expected = MerchantProfile(
            id = "m1",
            usuarioId = "u1",
            nombreComercial = "Café Sensus",
            direccion = "Av. Reforma 222",
            codigoPostal = "06600",
            latitud = 19.427,
            longitud = -99.163
        )
        coEvery {
            repo.activateMerchant("Café Sensus", "Av. Reforma 222", "06600", 19.427, -99.163)
        } returns Result.Success(expected)

        val useCase = ActivateMerchantUseCase(repo)
        val result = useCase("  Café Sensus  ", " Av. Reforma 222 ", " 06600 ", 19.427, -99.163)

        assertTrue(result is Result.Success)
        assertEquals("Café Sensus", (result as Result.Success).data.nombreComercial)
        coVerify(exactly = 1) {
            repo.activateMerchant("Café Sensus", "Av. Reforma 222", "06600", 19.427, -99.163)
        }
    }

    @Test
    fun activateMerchantUseCase_succeedsWithoutCoordinates() = runTest {
        val repo = mockk<MerchantRepository>()
        val expected = MerchantProfile(
            nombreComercial = "Panadería Sol",
            direccion = "Calle Hidalgo 10",
            codigoPostal = "42800"
        )
        coEvery {
            repo.activateMerchant("Panadería Sol", "Calle Hidalgo 10", "42800", null, null)
        } returns Result.Success(expected)

        val result = ActivateMerchantUseCase(repo)("Panadería Sol", "Calle Hidalgo 10", "42800")

        assertTrue(result is Result.Success)
    }

    @Test
    fun activateMerchantUseCase_rejectsInvalidInputsBeforeRepository() = runTest {
        val repo = mockk<MerchantRepository>(relaxed = true)
        val useCase = ActivateMerchantUseCase(repo)

        val shortName = useCase("A", "Av. Reforma 222", "06600")
        val longName = useCase("X".repeat(151), "Av. Reforma 222", "06600")
        val shortAddress = useCase("Tienda Sol", "Dir", "06600")
        val badPostal = useCase("Tienda Sol", "Av. Reforma 222", "12")
        val partialCoords = useCase("Tienda Sol", "Av. Reforma 222", "06600", 19.4, null)
        val outOfRangeCoords = useCase("Tienda Sol", "Av. Reforma 222", "06600", 95.0, -99.1)

        assertTrue(shortName is Result.Failure && shortName.error is AppError.Validation)
        assertTrue(longName is Result.Failure && longName.error is AppError.Validation)
        assertTrue(shortAddress is Result.Failure && shortAddress.error is AppError.Validation)
        assertTrue(badPostal is Result.Failure && badPostal.error is AppError.Validation)
        assertTrue(partialCoords is Result.Failure && partialCoords.error is AppError.Validation)
        assertTrue(outOfRangeCoords is Result.Failure && outOfRangeCoords.error is AppError.Validation)
        coVerify(exactly = 0) { repo.activateMerchant(any(), any(), any(), any(), any()) }
    }

    @Test
    fun resolveAddressUseCase_validatesCoordinatesAndDelegates() = runTest {
        val geoRepo = mockk<ReverseGeocodingRepository>()
        val resolved = ResolvedAddress("Av. Juárez 45", "06010", 19.435, -99.141)
        coEvery { geoRepo.resolveAddress(19.435, -99.141) } returns Result.Success(resolved)

        val useCase = ResolveAddressFromCoordinatesUseCase(geoRepo)
        val invalid = useCase(95.0, -99.141)
        val valid = useCase(19.435, -99.141)

        assertTrue(invalid is Result.Failure && invalid.error is AppError.Validation)
        assertTrue(valid is Result.Success)
        assertEquals("Av. Juárez 45", (valid as Result.Success).data.direccion)
        coVerify(exactly = 1) { geoRepo.resolveAddress(19.435, -99.141) }
    }

    @Test
    fun getMyMerchantUseCase_delegatesToMerchantRepository() = runTest {
        val repo = mockk<MerchantRepository>()
        val profile = MerchantProfile(nombreComercial = "Abarrotes Luna", direccion = "Centro 12", codigoPostal = "42800")
        coEvery { repo.getMyMerchant() } returns Result.Success(profile)

        val result = GetMyMerchantUseCase(repo)()

        assertTrue(result is Result.Success)
        assertEquals("Abarrotes Luna", (result as Result.Success).data.nombreComercial)
        coVerify(exactly = 1) { repo.getMyMerchant() }
    }
}
