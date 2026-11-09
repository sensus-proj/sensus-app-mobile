package com.example.sensus.presentation.merchant

import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.domain.usecase.UpdateMerchantProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MerchantProfileViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    val updateMerchantProfileUseCase: UpdateMerchantProfileUseCase
) : MviViewModel<MerchantProfileViewState, MerchantProfileViewIntent, MerchantProfileViewEffect>(
    MerchantProfileViewState()
) {

    init {
        updateState { copy(store = storeRepository.getSampleStore()) }
    }

    override fun processIntent(intent: MerchantProfileViewIntent) {
        when (intent) {
            is MerchantProfileViewIntent.UpdateStore -> updateState { copy(store = intent.store) }
        }
    }
}
