package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.InterestCatalog
import com.example.sensus.domain.usecase.GetMyInterestsUseCase
import com.example.sensus.domain.usecase.UpdateMyInterestsUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class InterestsPickerViewModel @Inject constructor(
    private val getMyInterestsUseCase: GetMyInterestsUseCase,
    private val updateMyInterestsUseCase: UpdateMyInterestsUseCase
) : MviViewModel<InterestsPickerViewState, InterestsPickerViewIntent, InterestsPickerViewEffect>(InterestsPickerViewState()) {

    override fun processIntent(intent: InterestsPickerViewIntent) {
        when (intent) {
            InterestsPickerViewIntent.LoadCurrent -> loadCurrent()
            is InterestsPickerViewIntent.ToggleCategory -> toggleCategory(intent.apiCode)
            InterestsPickerViewIntent.Save -> save()
            InterestsPickerViewIntent.Skip -> emitEffect(InterestsPickerViewEffect.Skipped)
            InterestsPickerViewIntent.DismissError -> updateState { copy(errorMessage = null) }
        }
    }

    private fun loadCurrent() {
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = getMyInterestsUseCase()) {
                is Result.Success -> updateState { copy(isLoading = false, selectedCodes = res.data) }
                is Result.Failure -> handleFailure(res.error, saving = false)
            }
        }
    }

    private fun toggleCategory(apiCode: String) {
        val current = viewState.value.selectedCodes
        if (apiCode in current) {
            updateState { copy(selectedCodes = current - apiCode, errorMessage = null) }
            return
        }

        if (current.size >= InterestCatalog.MAX_SELECTED) {
            updateState { copy(errorMessage = "Puedes elegir hasta ${InterestCatalog.MAX_SELECTED} categorías.") }
            return
        }

        updateState { copy(selectedCodes = current + apiCode, errorMessage = null) }
    }

    private fun save() {
        updateState { copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            val selected = viewState.value.selectedCodes
            when (val res = updateMyInterestsUseCase(selected)) {
                is Result.Success -> {
                    updateState { copy(isSaving = false, selectedCodes = res.data) }
                    emitEffect(InterestsPickerViewEffect.Saved(res.data))
                }
                is Result.Failure -> handleFailure(res.error, saving = true)
            }
        }
    }

    private fun handleFailure(error: AppError, saving: Boolean) {
        if (error is AppError.Unauthorized || error is AppError.Forbidden) {
            updateState { copy(isLoading = false, isSaving = false) }
            emitEffect(InterestsPickerViewEffect.RequireAuth)
        } else {
            updateState {
                copy(
                    isLoading = false,
                    isSaving = false,
                    errorMessage = if (saving) error.message else null
                )
            }
        }
    }
}
