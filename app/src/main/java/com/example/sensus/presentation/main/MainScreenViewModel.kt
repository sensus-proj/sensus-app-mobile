package com.example.sensus.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.DataRepository
import com.example.sensus.presentation.base.ViewState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MS = 5000L

sealed interface MainScreenViewState : ViewState {
    data object Loading : MainScreenViewState
    data class Error(val throwable: Throwable) : MainScreenViewState
    data class Success(val data: List<String>) : MainScreenViewState
}

class MainScreenViewModel(dataRepository: DataRepository) : ViewModel() {
    val viewState: StateFlow<MainScreenViewState> =
        dataRepository.data
            .map<List<String>, MainScreenViewState> { MainScreenViewState.Success(it) }
            .catch { emit(MainScreenViewState.Error(it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainScreenViewState.Loading)
}
