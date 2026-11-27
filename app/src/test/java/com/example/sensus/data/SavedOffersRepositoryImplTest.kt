package com.example.sensus.data

import com.example.sensus.data.local.db.dao.UserPreferenceDao
import com.example.sensus.data.local.db.entity.UserPreferenceEntity
import com.example.sensus.data.repository.SavedOffersRepositoryImpl
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.SessionState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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
class SavedOffersRepositoryImplTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessionFlow = MutableStateFlow(
        SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com")
    )
    private val sessionRepo = mockk<SessionRepository>(relaxed = true) {
        every { sessionState } returns sessionFlow
    }
    private val storedPrefs = mutableMapOf<String, UserPreferenceEntity>()
    private val fakeDao = object : UserPreferenceDao {
        override fun getByKey(key: String): UserPreferenceEntity? = storedPrefs[key]
        override fun upsert(preference: UserPreferenceEntity) {
            storedPrefs[preference.prefKey] = preference
        }
        override fun deleteByKey(key: String) {
            storedPrefs.remove(key)
        }
    }

    private val sampleOffer = ProductOffer(
        id = "offer-10",
        name = "Leche Deslactosada 1L",
        storeName = "Tienda La Palma",
        category = "Lácteos",
        originalPrice = 32.0,
        discountPrice = 22.0,
        discountPercent = 31,
        unitsLeft = 5,
        distanceMeters = 180
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        storedPrefs.clear()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun toggleOfferAndPostPersistInDaoAndRestoreAcrossInstances() = runTest(dispatcher) {
        val repo1 = SavedOffersRepositoryImpl(
            userPreferenceDao = fakeDao,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo1.toggleSavedOffer(sampleOffer))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo1.isOfferSaved(sampleOffer.id))

        val repo2 = SavedOffersRepositoryImpl(
            userPreferenceDao = fakeDao,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(sampleOffer), repo2.savedOffers.value)

        assertFalse(repo2.toggleSavedOffer(sampleOffer))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo2.savedOffers.value.isEmpty())
    }

    @Test
    fun switchingToGuestClearsActiveVisibleSavedItemsWithoutDeletingUserData() = runTest(dispatcher) {
        val repo = SavedOffersRepositoryImpl(
            userPreferenceDao = fakeDao,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        dispatcher.scheduler.advanceUntilIdle()

        repo.toggleSavedOffer(sampleOffer)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, repo.savedOffers.value.size)

        sessionFlow.value = SessionState(isSessionActive = true, isGuest = true)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(repo.savedOffers.value.isEmpty())

        sessionFlow.value = SessionState(isSessionActive = true, isGuest = false, activeEmail = "buyer@sensus.com")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(sampleOffer), repo.savedOffers.value)
    }

    @Test
    fun corruptJsonInDaoFallsBackToEmptyListsSafely() = runTest(dispatcher) {
        storedPrefs["saved_interested_offers_buyer@sensus.com"] =
            UserPreferenceEntity("saved_interested_offers_buyer@sensus.com", "{invalid-json", 1L)

        val repo = SavedOffersRepositoryImpl(
            userPreferenceDao = fakeDao,
            sessionRepository = sessionRepo,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(SupervisorJob() + dispatcher)
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(repo.savedOffers.value.isEmpty())
    }
}
