package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.usecase.GetOfferDetailUseCase
import com.example.sensus.domain.usecase.RegisterOfferViewUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getDetail: GetOfferDetailUseCase,
    private val registerView: RegisterOfferViewUseCase
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
                }
                is Result.Failure -> updateState { copy(isLoading = false, error = result.error) }
            }
        }
    }
}
