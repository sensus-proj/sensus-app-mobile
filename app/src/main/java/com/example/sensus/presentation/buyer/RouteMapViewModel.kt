package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.domain.usecase.GetUserLocationUseCase
import com.example.sensus.presentation.base.MviViewModel
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteMapViewState(
    val userLocation: UserLocation = UserLocation(),
    val selectedTravelMode: String = "walk"
) : ViewState

sealed interface RouteMapViewIntent : ViewIntent {
    data class TravelModeChanged(val mode: String) : RouteMapViewIntent
    data object FetchLocation : RouteMapViewIntent
}

sealed interface RouteMapViewEffect : ViewEffect

@HiltViewModel
class RouteMapViewModel @Inject constructor(
    private val getUserLocationUseCase: GetUserLocationUseCase
) : MviViewModel<RouteMapViewState, RouteMapViewIntent, RouteMapViewEffect>(RouteMapViewState()) {

    init {
        fetchLocation()
    }

    override fun processIntent(intent: RouteMapViewIntent) {
        when (intent) {
            is RouteMapViewIntent.TravelModeChanged -> updateState { copy(selectedTravelMode = intent.mode) }
            is RouteMapViewIntent.FetchLocation -> fetchLocation()
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            val loc = getUserLocationUseCase()
            updateState { copy(userLocation = loc) }
        }
    }
}
