package com.example.sensus.presentation.merchant

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.AppError
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimsSessionIdentity
import com.example.sensus.domain.repository.SessionRepository
import com.example.sensus.domain.usecase.RedeemClaimUseCase
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
            is MerchantSalesViewIntent.Edit -> edit(intent.value, scanned = false)
            is MerchantSalesViewIntent.Scan -> edit(intent.value, scanned = true)
            is MerchantSalesViewIntent.Submit -> submit(intent.value)
        }
    }

    private fun edit(value: String, scanned: Boolean) {
        if (viewState.value.loading) return
        val invalidScan = scanned && value.isBlank()
        updateState {
            copy(
                input = if (invalidScan) "" else value,
                result = null,
                error = if (invalidScan) AppError.Validation() else null,
                scanned = scanned && !invalidScan
            )
        }
    }

    private fun submit(value: String) {
        if (viewState.value.loading || viewState.value.result != null) return
        updateState { copy(loading = true, error = null) }
        requestJob = viewModelScope.launch {
            val response = action(value)
            ensureActive()
            when (response) {
                is Result.Success -> updateState { copy(loading = false, input = "", result = response.data, scanned = false) }
                is Result.Failure -> updateState { copy(loading = false, error = response.error) }
            }
        }
    }
}
