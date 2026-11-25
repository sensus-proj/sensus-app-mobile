package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.usecase.CancelClaimUseCase
import com.example.sensus.domain.usecase.GetMyClaimsUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class MyClaimsViewModel @Inject constructor(
    private val getMyClaims: GetMyClaimsUseCase,
    private val cancelClaim: CancelClaimUseCase
) : MviViewModel<MyClaimsViewState, MyClaimsViewIntent, MyClaimsViewEffect>(MyClaimsViewState()) {
    private var loadJob: Job? = null

    override fun processIntent(intent: MyClaimsViewIntent) {
        when (intent) {
            MyClaimsViewIntent.Load -> load()
            is MyClaimsViewIntent.RequestCancel -> requestCancel(intent.claimId)
            MyClaimsViewIntent.DismissCancel -> updateState { copy(pendingCancelClaimId = null) }
            MyClaimsViewIntent.ConfirmCancel -> confirmCancel()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        updateState { copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = getMyClaims()) {
                is Result.Success -> updateState { copy(loading = false, loaded = true, claims = result.data) }
                is Result.Failure -> updateState { copy(loading = false, loaded = true, error = result.error) }
            }
        }
    }

    private fun requestCancel(claimId: String) {
        if (viewState.value.cancellingClaimId != null) return
        if (viewState.value.claims.none { it.claimId == claimId }) return
        updateState { copy(pendingCancelClaimId = claimId, cancelError = null) }
    }

    private fun confirmCancel() {
        val claimId = viewState.value.pendingCancelClaimId ?: return
        if (viewState.value.cancellingClaimId != null) return
        updateState { copy(pendingCancelClaimId = null, cancellingClaimId = claimId, cancelError = null) }
        viewModelScope.launch {
            when (val result = cancelClaim(claimId)) {
                is Result.Success -> {
                    updateState {
                        copy(cancellingClaimId = null, claims = claims.map { if (it.claimId == claimId) result.data.withTitleFrom(it) else it })
                    }
                    emitEffect(MyClaimsViewEffect.Cancelled(claimId))
                }
                is Result.Failure -> updateState { copy(cancellingClaimId = null, cancelError = result.error) }
            }
        }
    }
}

private fun ClaimSummary.withTitleFrom(previous: ClaimSummary) = if (offerTitle == null) copy(offerTitle = previous.offerTitle) else this
