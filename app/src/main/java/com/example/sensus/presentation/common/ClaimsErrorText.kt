package com.example.sensus.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.sensus.R
import com.example.sensus.common.AppError

@Composable
fun claimsErrorText(error: AppError): String = stringResource(
    when (error) {
        is AppError.Unauthorized -> R.string.claims_session_required
        is AppError.Forbidden -> R.string.claims_forbidden
        is AppError.NotFound -> R.string.claims_not_found
        is AppError.Unprocessable -> R.string.claims_unavailable
        is AppError.Network -> R.string.claims_network_uncertain
        else -> R.string.claims_failure
    }
)
