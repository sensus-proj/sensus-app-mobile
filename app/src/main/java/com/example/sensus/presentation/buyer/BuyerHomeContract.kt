package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class BuyerHomeViewState(
    val offers: List<ProductOffer> = emptyList(),
    val selectedCategory: StoreCategory = StoreCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : ViewState

sealed interface BuyerHomeViewIntent : ViewIntent {
    data class CategorySelected(val category: StoreCategory) : BuyerHomeViewIntent
    data class SearchQueryChanged(val query: String) : BuyerHomeViewIntent
    data object RefreshOffers : BuyerHomeViewIntent
}

sealed interface BuyerHomeViewEffect : ViewEffect
