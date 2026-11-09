package com.example.sensus.presentation.main

import com.example.sensus.domain.repository.DataRepository
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun viewState_initiallyLoading() = runTest {
        val viewModel = MainScreenViewModel(FakeMyModelRepository())
        assertEquals(viewModel.viewState.first(), MainScreenViewState.Loading)
    }

    @Test
    fun viewState_onItemSaved_isDisplayed() = runTest {
        val viewModel = MainScreenViewModel(FakeMyModelRepository())
        assertEquals(viewModel.viewState.first(), MainScreenViewState.Loading)
    }
}

private class FakeMyModelRepository : DataRepository {
    override val data: Flow<List<String>> = flow { emit(listOf("Sample")) }
}
