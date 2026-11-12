package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantRegisterViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : MviViewModel<MerchantRegisterViewState, MerchantRegisterViewIntent, MerchantRegisterViewEffect>(
    MerchantRegisterViewState()
) {

    override fun processIntent(intent: MerchantRegisterViewIntent) {
        when (intent) {
            is MerchantRegisterViewIntent.ActivateMerchant -> {
                viewModelScope.launch { sessionRepository.syncSession() }
                emitEffect(MerchantRegisterViewEffect.ActivationSuccess)
            }
        }
    }
}
