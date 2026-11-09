package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<CommunityViewState, CommunityViewIntent, CommunityViewEffect>(CommunityViewState()) {

    init {
        val friends = storeRepository.getSampleFriends()
        val email = sessionRepository.sessionState.value.activeEmail ?: "Usuario Sensus"
        updateState { copy(friends = friends, currentUserName = email) }
    }

    override fun processIntent(intent: CommunityViewIntent) {}
}
