package com.example.sensus.presentation.buyer

import com.example.sensus.domain.model.InterestCatalog
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class InterestsPickerViewState(
    val availableCategories: List<StoreCategory> = InterestCatalog.selectableCategories,
    val selectedCodes: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val maxSelection: Int = InterestCatalog.MAX_SELECTED
) : ViewState

sealed interface InterestsPickerViewIntent : ViewIntent {
    data object LoadCurrent : InterestsPickerViewIntent
    data class ToggleCategory(val apiCode: String) : InterestsPickerViewIntent
    data object Save : InterestsPickerViewIntent
    data object Skip : InterestsPickerViewIntent
    data object DismissError : InterestsPickerViewIntent
}

sealed interface InterestsPickerViewEffect : ViewEffect {
    data class Saved(val selectedCodes: List<String>) : InterestsPickerViewEffect
    data object Skipped : InterestsPickerViewEffect
    data object RequireAuth : InterestsPickerViewEffect
}
