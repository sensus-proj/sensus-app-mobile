package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.RedeemClaimUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class MerchantSalesViewModel @Inject constructor(
    private val action: RedeemClaimUseCase,
    sessions: SessionRepository
) : MviViewModel<MerchantSalesViewState, MerchantSalesViewIntent, MerchantSalesViewEffect>(MerchantSalesViewState()) {
    private var requestJob: Job? = null

    init {
        viewModelScope.launch {
            sessions.sessionState.map(ClaimsSessionIdentity::from).distinctUntilChanged().drop(1).collect {
                requestJob?.cancel()
                updateState { MerchantSalesViewState() }
            }
        }
    }

    override fun processIntent(intent: MerchantSalesViewIntent) {
        when (intent) {
            is MerchantSalesViewIntent.Edit -> if (!viewState.value.loading) {
                updateState { copy(input = intent.value, result = null, error = null) }
            }
            is MerchantSalesViewIntent.Submit -> submit(intent.value)
        }
    }

    private fun submit(value: String) {
        if (viewState.value.loading || viewState.value.result != null) return
        updateState { copy(loading = true, error = null) }
        requestJob = viewModelScope.launch {
            when (val response = action(value)) {
                is Result.Success -> updateState { copy(loading = false, input = "", result = response.data) }
                is Result.Failure -> updateState { copy(loading = false, error = response.error) }
            }
        }
    }
}
