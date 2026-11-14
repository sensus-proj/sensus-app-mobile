package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.usecase.ChangeOfferStatusUseCase
import com.example.sensus.domain.usecase.UpdateOfferUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantEditOfferViewModel @Inject constructor(
    private val updateOfferUseCase: UpdateOfferUseCase,
    private val changeOfferStatusUseCase: ChangeOfferStatusUseCase
) : MviViewModel<MerchantEditOfferViewState, MerchantEditOfferViewIntent, MerchantEditOfferViewEffect>(
    MerchantEditOfferViewState()
) {

    override fun processIntent(intent: MerchantEditOfferViewIntent) {
        when (intent) {
            is MerchantEditOfferViewIntent.Initialize -> updateState {
                copy(currentOffer = intent.offer, fieldError = null, errorMessage = null)
            }
            is MerchantEditOfferViewIntent.FieldEdited -> updateState { copy(fieldError = null, errorMessage = null) }
            is MerchantEditOfferViewIntent.SaveChanges -> saveChanges(intent.input)
            is MerchantEditOfferViewIntent.ChangeStatus -> changeStatus(intent.offer, intent.newStatus)
        }
    }

    private fun saveChanges(input: UpdateOfferInput) {
        if (viewState.value.isSaving || viewState.value.isChangingStatus) return
        val validationError = OfferPublishingPolicy.validateUpdate(input, System.currentTimeMillis())
        if (validationError != null) {
            updateState { copy(fieldError = validationError, errorMessage = null) }
            return
        }
        updateState { copy(isSaving = true, fieldError = null, errorMessage = null) }
        viewModelScope.launch {
            when (val res = updateOfferUseCase(input)) {
                is Result.Success -> {
                    updateState { copy(currentOffer = res.data, isSaving = false) }
                    emitEffect(MerchantEditOfferViewEffect.OfferUpdated(res.data, "Promoción actualizada correctamente"))
                }
                is Result.Failure -> updateState { copy(isSaving = false, errorMessage = res.error.message) }
            }
        }
    }

    private fun changeStatus(offer: ProductOffer, newStatus: OfferLifecycleStatus) {
        if (viewState.value.isSaving || viewState.value.isChangingStatus) return
        updateState { copy(isChangingStatus = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = changeOfferStatusUseCase(offer, newStatus)) {
                is Result.Success -> {
                    val updated = res.data.copy(status = newStatus)
                    updateState { copy(currentOffer = updated, isChangingStatus = false) }
                    emitEffect(MerchantEditOfferViewEffect.OfferStatusChanged(updated, statusFeedback(newStatus)))
                }
                is Result.Failure -> updateState { copy(isChangingStatus = false, errorMessage = res.error.message) }
            }
        }
    }

    private fun statusFeedback(status: OfferLifecycleStatus): String = when (status) {
        OfferLifecycleStatus.PAUSED -> "Promoción pausada: ya no se muestra al público"
        OfferLifecycleStatus.ACTIVE -> "Promoción reactivada y visible al público"
        OfferLifecycleStatus.ARCHIVED -> "Promoción dada de baja"
        else -> "Estado actualizado"
    }
}
