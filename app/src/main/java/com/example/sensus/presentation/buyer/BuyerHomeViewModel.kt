package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.OffersRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyerHomeViewModel @Inject constructor(
    private val offersRepository: OffersRepository
) : MviViewModel<BuyerHomeViewState, BuyerHomeViewIntent, BuyerHomeViewEffect>(BuyerHomeViewState()) {

    init {
        loadOffers()
    }

    override fun processIntent(intent: BuyerHomeViewIntent) {
        when (intent) {
            is BuyerHomeViewIntent.CategorySelected -> updateState { copy(selectedCategory = intent.category) }
            is BuyerHomeViewIntent.SearchQueryChanged -> updateState { copy(searchQuery = intent.query) }
            is BuyerHomeViewIntent.RefreshOffers -> loadOffers()
        }
    }

    private fun loadOffers() {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            val offers = offersRepository.getOffers()
            updateState { copy(offers = offers, isLoading = false) }
        }
    }
}
