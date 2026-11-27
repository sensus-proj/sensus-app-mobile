package com.example.sensus.presentation.main

import app.cash.turbine.test
import com.example.sensus.domain.repository.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewState_initiallyLoading() = runTest {
        val viewModel = MainScreenViewModel(FakeMyModelRepository())
        assertEquals(MainScreenViewState.Loading, viewModel.viewState.value)
    }

    @Test
    fun viewState_onItemSaved_isDisplayed() = runTest {
        val viewModel = MainScreenViewModel(FakeMyModelRepository())
        viewModel.viewState.test {
            assertEquals(MainScreenViewState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(MainScreenViewState.Success(listOf("Sample")), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeMyModelRepository : DataRepository {
    override val data: Flow<List<String>> = flow { emit(listOf("Sample")) }
}
