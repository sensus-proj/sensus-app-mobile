package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SavedOffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SavedInterestsViewModel @Inject constructor(
    private val savedOffersRepository: SavedOffersRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<SavedInterestsViewState, SavedInterestsViewIntent, SavedInterestsViewEffect>(SavedInterestsViewState()) {

    init {
        viewModelScope.launch {
            savedOffersRepository.savedOffers.collect { offers ->
                updateState { copy(savedOffers = offers) }
            }
        }
    }

    override fun processIntent(intent: SavedInterestsViewIntent) {
        when (intent) {
            is SavedInterestsViewIntent.ToggleOffer -> handleToggleOffer(intent.offer)
            is SavedInterestsViewIntent.RemoveOffer -> viewModelScope.launch {
                savedOffersRepository.removeSavedOffer(intent.offerId)
                emitEffect(SavedInterestsViewEffect.SavedStateChanged(isSaved = false))
            }
        }
    }

    private fun handleToggleOffer(offer: ProductOffer) {
        if (sessionRepository.sessionState.value.isGuest) {
            emitEffect(SavedInterestsViewEffect.RequireAuth)
            return
        }
        viewModelScope.launch {
            val nowSaved = savedOffersRepository.toggleSavedOffer(offer)
            emitEffect(SavedInterestsViewEffect.SavedStateChanged(isSaved = nowSaved))
        }
    }
}
