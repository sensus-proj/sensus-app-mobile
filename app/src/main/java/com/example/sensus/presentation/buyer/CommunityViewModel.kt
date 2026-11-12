package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.repository.StoreRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val storeRepository: StoreRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<CommunityViewState, CommunityViewIntent, CommunityViewEffect>(CommunityViewState()) {

    init {
        val friends = storeRepository.getSampleFriends()
        val initialSession = sessionRepository.sessionState.value
        val initialName = when {
            initialSession.isGuest -> "Invitado Sensus"
            else -> initialSession.activeName?.takeIf { it.isNotBlank() }
                ?: initialSession.activeEmail?.takeIf { it.isNotBlank() }
                ?: "Usuario Sensus"
        }
        updateState { copy(friends = friends, currentUserName = initialName) }

        viewModelScope.launch {
            sessionRepository.sessionState.collect { session ->
                val resolvedName = when {
                    session.isGuest -> "Invitado Sensus"
                    else -> session.activeName?.takeIf { it.isNotBlank() }
                        ?: session.activeEmail?.takeIf { it.isNotBlank() }
                        ?: "Usuario Sensus"
                }
                updateState { copy(currentUserName = resolvedName) }
            }
        }
    }

    override fun processIntent(intent: CommunityViewIntent) = Unit
}
