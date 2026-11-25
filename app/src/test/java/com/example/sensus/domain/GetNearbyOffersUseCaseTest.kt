package com.example.sensus.domain

import com.example.sensus.common.Result
import com.example.sensus.domain.model.NearbyOffersQuery
import com.example.sensus.domain.model.PagedOffers
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.repository.MapPreferencesRepository
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.domain.usecase.GetNearbyOffersUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetNearbyOffersUseCaseTest {

    private val repository = mockk<OffersRepository>()
    private val getNearbyOffers = GetNearbyOffersUseCase(repository)
    private val location = UserLocation(latitude = 20.0, longitude = -99.0)

    @Test
    fun usesFixedRadiusForGuestEvenWhenSmallerRadiusIsRequested() = runTest {
        coEvery { repository.fetchNearbyOffers(20.0, -99.0, 2.0, null) } returns Result.Success(emptyList())

        assertTrue(getNearbyOffers(location, 0.1f, isGuest = true) is Result.Success)
        coVerify(exactly = 1) { repository.fetchNearbyOffers(20.0, -99.0, 2.0, null) }
    }

    @Test
    fun usesBackendMinimumForSubKilometerSelection() = runTest {
        coEvery { repository.fetchNearbyOffers(20.0, -99.0, 1.0, null) } returns Result.Success(emptyList())

        assertTrue(getNearbyOffers(location, 0.5f, isGuest = false) is Result.Success)
        coVerify(exactly = 1) { repository.fetchNearbyOffers(20.0, -99.0, 1.0, null) }
    }

    @Test
    fun rejectsInvalidCoordinatesAndNonFiniteRadiusWithoutCallingRepository() = runTest {
        assertTrue(getNearbyOffers(location.copy(latitude = 91.0), 2f, isGuest = false) is Result.Failure)
        assertTrue(getNearbyOffers(location.copy(longitude = Double.NaN), 2f, isGuest = false) is Result.Failure)
        assertTrue(getNearbyOffers(location, Float.NaN, isGuest = false) is Result.Failure)
        coVerify(exactly = 0) { repository.fetchNearbyOffers(any(), any(), any(), any()) }
    }

    @Test
    fun paginatesWithTheRequestedPageAndRejectsOversizedPages() = runTest {
        val query = NearbyOffersQuery(location, 2.0, page = 2)
        coEvery { repository.fetchNearbyOffersPage(query) } returns Result.Success(PagedOffers(emptyList()))

        assertTrue(getNearbyOffers.page(location, 5f, isGuest = true, page = 2) is Result.Success)
        assertTrue(getNearbyOffers.page(location, 5f, isGuest = true, page = 2, pageSize = 101) is Result.Failure)
        coVerify(exactly = 1) { repository.fetchNearbyOffersPage(any()) }
    }

    @Test
    fun storesAndReadsThePreferredRadiusInMeters() = runTest {
        val preferences = mockk<MapPreferencesRepository>()
        coEvery { preferences.savePreferredRadiusMeters(300) } returns Unit
        coEvery { preferences.getPreferredRadiusMeters() } returns 300
        val useCase = GetNearbyOffersUseCase(repository, preferences)

        useCase.savePreferredRadiusKm(0.3f)
        assertEquals(0.3f, useCase.getPreferredRadiusKm())
        coVerify(exactly = 1) { preferences.savePreferredRadiusMeters(300) }
    }
}
