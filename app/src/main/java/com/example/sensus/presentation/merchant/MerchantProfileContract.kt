package com.example.sensus.presentation.merchant

import com.example.sensus.domain.model.HorarioDia
import com.example.sensus.domain.model.MerchantStore
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MerchantProfileViewState(
    val store: MerchantStore = MerchantStore(),
    val schedules: List<HorarioDia> = emptyList(),
    val isSaving: Boolean = false,
    val isUpdatingPhoto: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
) : ViewState

sealed interface MerchantProfileViewIntent : ViewIntent {
    data object LoadMerchant : MerchantProfileViewIntent
    data class UpdateStore(val store: MerchantStore) : MerchantProfileViewIntent
    class UpdateFacadePhoto(val imageBytes: ByteArray) : MerchantProfileViewIntent
    data class ToggleStoreOpen(val isOpen: Boolean) : MerchantProfileViewIntent
    data object ClearMessages : MerchantProfileViewIntent
}

sealed interface MerchantProfileViewEffect : ViewEffect
