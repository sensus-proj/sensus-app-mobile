package com.example.sensus.presentation.merchant

import com.example.sensus.data.moderation.ImageModerationService
import com.example.sensus.domain.model.ModerationResult
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MerchantNewOfferViewModel @Inject constructor(
    private val offersRepository: OffersRepository,
    private val moderationService: ImageModerationService = ImageModerationService()
) : MviViewModel<MerchantNewOfferViewState, MerchantNewOfferViewIntent, MerchantNewOfferViewEffect>(
    MerchantNewOfferViewState()
) {

    suspend fun moderateImage(imageBytes: ByteArray, textContext: String? = null): ModerationResult {
        return moderationService.moderateImage(imageBytes, textContext)
    }

    override fun processIntent(intent: MerchantNewOfferViewIntent) {
        when (intent) {
            is MerchantNewOfferViewIntent.PublishOffer -> {
                offersRepository.addOffer(intent.offer)
                emitEffect(MerchantNewOfferViewEffect.OfferCreated(intent.offer))
            }
        }
    }
}
