package com.example.sensus.presentation.buyer

import com.example.sensus.common.AppError
import com.example.sensus.domain.model.ClaimSummary
import com.example.sensus.presentation.base.ViewEffect
import com.example.sensus.presentation.base.ViewIntent
import com.example.sensus.presentation.base.ViewState

data class MyClaimsViewState(
    val loading: Boolean = false,
    val claims: List<ClaimSummary> = emptyList(),
    val loaded: Boolean = false,
    val error: AppError? = null,
    val cancellingClaimId: String? = null,
    val pendingCancelClaimId: String? = null,
    val cancelError: AppError? = null
) : ViewState {
    val isEmpty: Boolean get() = loaded && !loading && error == null && claims.isEmpty()
}

sealed interface MyClaimsViewIntent : ViewIntent {
    data object Load : MyClaimsViewIntent
    data class RequestCancel(val claimId: String) : MyClaimsViewIntent
    data object DismissCancel : MyClaimsViewIntent
    data object ConfirmCancel : MyClaimsViewIntent
}

sealed interface MyClaimsViewEffect : ViewEffect {
    data class Cancelled(val claimId: String) : MyClaimsViewEffect
}
