package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState
import com.example.sensus.presentation.common.MapLayerStyle

data class BuyerMapViewState(
    val offers: List<ProductOffer> = emptyList(),
    val selectedOffer: ProductOffer? = null,
    val userLocation: UserLocation = UserLocation(),
    val hasRealLocation: Boolean = false,
    val isLocating: Boolean = false,
    val isLocationEnabled: Boolean = true,
    val isLocationPermissionGranted: Boolean = false,
    val isGuest: Boolean = false,
    val selectedRadiusKm: Float = GuestRadiusPolicy.GUEST_MAX_RADIUS_KM,
    val selectedLayer: MapLayerStyle = MapLayerStyle.STANDARD,
    val recenterSignal: Int = 0
) : ViewState

sealed interface BuyerMapViewIntent : ViewIntent {
    data class OfferSelected(val offer: ProductOffer?) : BuyerMapViewIntent
    data class LocationPermissionGranted(val granted: Boolean) : BuyerMapViewIntent
    data class RefreshLocation(val forceFresh: Boolean = true) : BuyerMapViewIntent
    data class RadiusSelected(val radiusKm: Float) : BuyerMapViewIntent
    data class LayerSelected(val layer: MapLayerStyle) : BuyerMapViewIntent
}

sealed interface BuyerMapViewEffect : ViewEffect {
    data object RequireAuthForRadius : BuyerMapViewEffect
    data class ShowLocationMessage(val message: String, val isGpsDisabled: Boolean = false) : BuyerMapViewEffect
}
