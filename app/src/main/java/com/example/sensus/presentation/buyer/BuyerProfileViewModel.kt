package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.ProfileRepository
import com.example.sensus.domain.usecase.UpdateProfileUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyerProfileViewModel @Inject constructor(
    val profileRepository: ProfileRepository,
    val updateProfileUseCase: UpdateProfileUseCase
) : MviViewModel<BuyerProfileViewState, BuyerProfileViewIntent, BuyerProfileViewEffect>(BuyerProfileViewState()) {

    init {
        loadProfile()
    }

    override fun processIntent(intent: BuyerProfileViewIntent) {
        when (intent) {
            is BuyerProfileViewIntent.LoadProfile -> loadProfile()
            is BuyerProfileViewIntent.UpdateName -> updateName(intent.newName)
        }
    }

    private fun loadProfile() {
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = profileRepository.getProfile()) {
                is Result.Success -> updateState { copy(profile = res.data, isLoading = false) }
                is Result.Failure -> updateState { copy(errorMessage = res.error.message, isLoading = false) }
            }
        }
    }

    private fun updateName(newName: String) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch {
            when (val res = updateProfileUseCase(newName, viewState.value.profile?.fotoPerfilUrl)) {
                is Result.Success -> updateState { copy(profile = res.data, isLoading = false) }
                is Result.Failure -> updateState { copy(errorMessage = res.error.message, isLoading = false) }
            }
        }
    }
}
