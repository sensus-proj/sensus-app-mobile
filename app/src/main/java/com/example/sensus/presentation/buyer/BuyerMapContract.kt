package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class BuyerMapViewState(
    val offers: List<ProductOffer> = emptyList(),
    val selectedOffer: ProductOffer? = null,
    val userLatitude: Double = 20.0545,
    val userLongitude: Double = -99.3420,
    val isLocationPermissionGranted: Boolean = false
) : ViewState

sealed interface BuyerMapViewIntent : ViewIntent {
    data class OfferSelected(val offer: ProductOffer) : BuyerMapViewIntent
    data class LocationPermissionGranted(val granted: Boolean) : BuyerMapViewIntent
}

sealed interface BuyerMapViewEffect : ViewEffect
