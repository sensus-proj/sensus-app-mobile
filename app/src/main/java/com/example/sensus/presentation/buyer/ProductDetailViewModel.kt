package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.OfferDetails
import com.example.sensus.domain.usecase.GetNearbyMerchantsUseCase
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

private const val DETAIL_LOOKUP_RADIUS_METERS = 1000

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getDetail: GetOfferDetailUseCase,
    private val registerView: RegisterOfferViewUseCase,
    private val nearbyMerchants: GetNearbyMerchantsUseCase? = null
) : MviViewModel<ProductDetailViewState, ProductDetailViewIntent, ProductDetailViewEffect>(ProductDetailViewState()) {
    private var loadJob: Job? = null
    private val viewedOffers = mutableSetOf<String>()

    override fun processIntent(intent: ProductDetailViewIntent) {
        when (intent) {
            is ProductDetailViewIntent.Load -> load(intent.offerId)
            is ProductDetailViewIntent.ChangeQuantity -> updateState {
                copy(quantity = intent.quantity.coerceIn(1, (details?.offer?.unitsLeft ?: 1).coerceAtLeast(1)))
            }
        }
    }

    private suspend fun loadMerchantProfile(loaded: OfferDetails) {
        val offer = loaded.offer
        if (offer.merchantId.isNullOrBlank() || !offer.latitude.isFinite() || !offer.longitude.isFinite()) return
        val profiles = nearbyMerchants?.invoke(offer.latitude, offer.longitude, DETAIL_LOOKUP_RADIUS_METERS, isGuest = true)?.getOrNull()
        val profile = profiles?.firstOrNull { it.id == offer.merchantId } ?: return
        kotlin.coroutines.coroutineContext.ensureActive()
        updateState { copy(details = loaded.copy(offer = offer.copy(merchantProfile = profile))) }
    }

    private fun load(offerId: String) {
        loadJob?.cancel()
        updateState { copy(isLoading = true, details = null, quantity = 1, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = getDetail(offerId)) {
                is Result.Success -> {
                    updateState { copy(isLoading = false, details = result.data) }
                    if (viewedOffers.add(offerId)) {
                        viewModelScope.launch { registerView(offerId) }
                    }
                    loadMerchantProfile(result.data)
                }
                is Result.Failure -> updateState { copy(isLoading = false, error = result.error) }
            }
        }
    }
}
