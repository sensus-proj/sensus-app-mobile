package com.example.sensus.presentation.buyer

import androidx.lifecycle.viewModelScope
import com.example.sensus.common.Result
import com.example.sensus.domain.model.ClaimStatus
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.domain.usecase.GetMyClaimsUseCase
import com.example.sensus.presentation.base.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class MyClaimsViewModel @Inject constructor(
    private val getMyClaims: GetMyClaimsUseCase
) : MviViewModel<MyClaimsViewState, MyClaimsViewIntent, MyClaimsViewEffect>(MyClaimsViewState()) {
    private var loadJob: Job? = null

    override fun processIntent(intent: MyClaimsViewIntent) {
        when (intent) {
            MyClaimsViewIntent.Load -> load()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        updateState { copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val result = getMyClaims()) {
                is Result.Success -> updateState { copy(loading = false, loaded = true, claims = result.data.sortedForDisplay()) }
                is Result.Failure -> updateState { copy(loading = false, loaded = true, error = result.error) }
            }
        }
    }
}

private val displayStatusOrder = listOf(
    ClaimStatus.ACTIVE,
    ClaimStatus.REDEEMED,
    ClaimStatus.EXPIRED,
    ClaimStatus.CANCELLED,
    ClaimStatus.UNKNOWN
)

private fun List<ClaimSummary>.sortedForDisplay(): List<ClaimSummary> = sortedBy { displayStatusOrder.indexOf(it.status) }
