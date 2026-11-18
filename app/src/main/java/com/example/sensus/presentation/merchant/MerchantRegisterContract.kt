package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantRegisterViewState(
    val isRegistering: Boolean = false,
    val errorMessage: String? = null,
    val userLocation: UserLocation = UserLocation(),
    val hasRealLocation: Boolean = false,
    val selectedLatitude: Double? = null,
    val selectedLongitude: Double? = null,
    val isResolvingLocation: Boolean = false,
    val showMapPicker: Boolean = false,
    val pickerLatitude: Double = userLocation.latitude,
    val pickerLongitude: Double = userLocation.longitude,
    val recenterSignal: Int = 0,
    val resolvedAddressPreview: String? = null,
    val resolvedPostalPreview: String? = null,
    val locationHelperHint: String? = null
) : ViewState

sealed interface MerchantRegisterViewIntent : ViewIntent {
    data object OpenMapPickerWithCurrentLocation : MerchantRegisterViewIntent
    data object OpenMapPickerManualFallback : MerchantRegisterViewIntent
    data object DismissMapPicker : MerchantRegisterViewIntent
    data object RecenterPickerOnCurrentLocation : MerchantRegisterViewIntent
    data class MoveMapPin(val latitude: Double, val longitude: Double) : MerchantRegisterViewIntent
    data class ConfirmMapLocation(val latitude: Double, val longitude: Double) : MerchantRegisterViewIntent
    data object ClearError : MerchantRegisterViewIntent

    /** Clase normal (no data class) para evitar incluir el ByteArray de la foto en equals/hashCode. */
    class ActivateMerchant(
        val storeName: String,
        val address: String = "",
        val postalCode: String = "",
        val facadePhotoBytes: ByteArray? = null
    ) : MerchantRegisterViewIntent
}

sealed interface MerchantRegisterViewEffect : ViewEffect {
    data class ActivationSuccess(val storeName: String) : MerchantRegisterViewEffect
    data class ApplyResolvedAddress(val address: String, val postalCode: String) : MerchantRegisterViewEffect
}
