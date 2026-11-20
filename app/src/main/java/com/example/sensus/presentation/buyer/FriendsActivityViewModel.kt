package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.GetFriendsCommunityActivityUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class FriendsActivityViewModel @Inject constructor(
    private val getFriendsActivity: GetFriendsCommunityActivityUseCase,
    private val sessionRepository: SessionRepository
) : MviViewModel<FriendsActivityViewState, FriendsActivityViewIntent, FriendsActivityViewEffect>(
    FriendsActivityViewState()
) {

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                val guest = session.isGuest || !session.isSessionActive
                updateState { copy(isGuest = guest) }
                if (!guest) refresh() else updateState { copy(items = emptyList(), isLoading = false, errorMessage = null) }
            }
        }
    }

    override fun processIntent(intent: FriendsActivityViewIntent) {
        when (intent) {
            FriendsActivityViewIntent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        if (viewState.value.isGuest || viewState.value.isLoading) return
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val res = getFriendsActivity()) {
                is Result.Success -> updateState { copy(isLoading = false, items = res.data) }
                is Result.Failure -> updateState { copy(isLoading = false, errorMessage = res.error.message) }
            }
        }
    }
}
