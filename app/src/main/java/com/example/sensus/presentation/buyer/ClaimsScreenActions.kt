package com.example.sensus.presentation.buyer

import com.example.sensus.presentation.common.ComposeUiModel
import java.time.Clock

@ComposeUiModel
data class ClaimsScreenActions(
    val onNavigateBack: () -> Unit = {},
    val onNavigateToRoute: () -> Unit = {},
    val onClaimConfirmed: () -> Unit = {},
    val expectExisting: Boolean = false,
    val clock: Clock = Clock.systemUTC()
)
