package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.usecase.CreateOfferUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantNewOfferViewModel @Inject constructor(
    private val createOfferUseCase: CreateOfferUseCase
) : MviViewModel<MerchantNewOfferViewState, MerchantNewOfferViewIntent, MerchantNewOfferViewEffect>(
    MerchantNewOfferViewState()
) {

    override fun processIntent(intent: MerchantNewOfferViewIntent) {
        when (intent) {
            is MerchantNewOfferViewIntent.PublishOffer -> publish(intent)
            is MerchantNewOfferViewIntent.FieldEdited -> updateState { copy(fieldError = null, errorMessage = null) }
            is MerchantNewOfferViewIntent.DismissModeration -> updateState { copy(moderationMessage = null) }
        }
    }

    private fun publish(intent: MerchantNewOfferViewIntent.PublishOffer) {
        if (viewState.value.isPublishing) return
        val fieldError = OfferPublishingPolicy.validate(intent.input, System.currentTimeMillis())
        if (fieldError != null) {
            updateState { copy(fieldError = fieldError, errorMessage = null) }
            return
        }

        updateState { copy(isPublishing = true, fieldError = null, errorMessage = null, moderationMessage = null) }
        viewModelScope.launch {
            when (val result = createOfferUseCase(intent.input, intent.imageBytes)) {
                is Result.Success -> {
                    updateState { copy(isPublishing = false) }
                    emitEffect(MerchantNewOfferViewEffect.OfferCreated(result.data))
                }
                is Result.Failure -> updateState {
                    val error = result.error
                    if (error is AppError.ContentRejected) {
                        copy(isPublishing = false, moderationMessage = error.message)
                    } else {
                        copy(isPublishing = false, errorMessage = error.message)
                    }
                }
            }
        }
    }
}