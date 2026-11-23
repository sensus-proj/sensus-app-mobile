package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.repository.CartRepository
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.CreateClaimUseCase
import com.example.sensus.domain.usecase.RestoreClaimUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class ClaimOfferViewModel @Inject constructor(
    private val action: CreateClaimUseCase,
    sessions: SessionRepository,
    private val cartRepository: CartRepository,
    private val restore: RestoreClaimUseCase
) : MviViewModel<ClaimOfferViewState, ClaimOfferViewIntent, ClaimOfferViewEffect>(ClaimOfferViewState()) {
    private var requestJob: Job? = null
    private val confirmedClaims = mutableMapOf<String, OfferClaim>()
    private val expectedClaims = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            sessions.sessionState.map(ClaimsSessionIdentity::from).distinctUntilChanged().drop(1).collect {
                requestJob?.cancel()
                confirmedClaims.clear()
                expectedClaims.clear()
                updateState { ClaimOfferViewState() }
            }
        }
    }

    override fun processIntent(intent: ClaimOfferViewIntent) {
        when (intent) {
            is ClaimOfferViewIntent.Initialize -> initialize(intent.offerId, intent.expectExisting)
            is ClaimOfferViewIntent.Submit -> submit(intent.value)
        }
    }

    private fun initialize(offerId: String, expectExisting: Boolean) {
        if (expectExisting) expectedClaims.add(offerId)
        if (viewState.value.offerId == offerId) {
            if (offerId in expectedClaims) updateState { copy(expectExisting = true) }
            if (expectExisting && confirmedClaims[offerId] == null && requestJob?.isActive != true) restoreExisting(offerId)
            return
        }
        requestJob?.cancel()
        updateState {
            ClaimOfferViewState(offerId = offerId, expectExisting = offerId in expectedClaims, result = confirmedClaims[offerId])
        }
        if (offerId in expectedClaims && confirmedClaims[offerId] == null) restoreExisting(offerId)
    }

    private fun restoreExisting(offerId: String) {
        requestJob?.cancel()
        requestJob = viewModelScope.launch {
            updateState { copy(loading = true) }
            val restored = restore(offerId)
            ensureActive()
            when (restored) {
                is Result.Success -> {
                    confirmedClaims[offerId] = restored.data
                    updateState { copy(loading = false, result = restored.data) }
                }
                is Result.Failure -> updateState { copy(loading = false, error = restored.error) }
            }
        }
    }

    private fun submit(value: String) {
        if (!viewState.value.canSubmit || confirmedClaims.containsKey(value)) return
        if (viewState.value.offerId != null && viewState.value.offerId != value) return
        updateState { copy(offerId = value, loading = true, error = null) }
        requestJob = viewModelScope.launch {
            val response = action(value)
            ensureActive()
            when (response) {
                is Result.Success -> {
                    cartRepository.removeReservedUnit(response.data.offerId)
                    confirmedClaims[value] = response.data
                    updateState { copy(loading = false, result = response.data) }
                    emitEffect(ClaimOfferViewEffect.Confirmed(value))
                }
                is Result.Failure -> updateState { copy(loading = false, error = response.error) }
            }
        }
    }
}
