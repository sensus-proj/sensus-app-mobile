package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.domain.model.FeedPost
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.repository.SavedOffersRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class SavedInterestsViewModel @Inject constructor(
    private val savedOffersRepository: SavedOffersRepository,
    private val sessionRepository: SessionRepository
) : MviViewModel<SavedInterestsViewState, SavedInterestsViewIntent, SavedInterestsViewEffect>(SavedInterestsViewState()) {

    init {
        viewModelScope.launch {
            combine(
                savedOffersRepository.savedOffers,
                savedOffersRepository.savedPosts
            ) { offers, posts -> offers to posts }
                .collect { (offers, posts) ->
                    updateState { copy(savedOffers = offers, savedPosts = posts) }
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
            is SavedInterestsViewIntent.TogglePost -> handleTogglePost(intent.post)
            is SavedInterestsViewIntent.RemovePost -> viewModelScope.launch {
                savedOffersRepository.removeSavedPost(intent.postId)
                emitEffect(SavedInterestsViewEffect.SavedStateChanged(isSaved = false))
            }
            is SavedInterestsViewIntent.SelectTab -> updateState { copy(selectedTab = intent.tab) }
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

    private fun handleTogglePost(post: FeedPost) {
        if (sessionRepository.sessionState.value.isGuest) {
            emitEffect(SavedInterestsViewEffect.RequireAuth)
            return
        }
        viewModelScope.launch {
            val nowSaved = savedOffersRepository.toggleSavedPost(post)
            emitEffect(SavedInterestsViewEffect.SavedStateChanged(isSaved = nowSaved))
        }
    }
}
