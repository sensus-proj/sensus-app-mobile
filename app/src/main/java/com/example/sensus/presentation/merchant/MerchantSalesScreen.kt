package com.example.sensus.presentation.merchant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.presentation.common.claimsErrorText
import java.time.Instant

private const val REDEEM_CONTENT_PADDING = 24
private const val CONTENT_SPACING = 16

@Composable
fun MerchantSalesScreen(modifier: Modifier = Modifier) {
    val viewModel: MerchantSalesViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    MerchantSalesContent(state, viewModel::processIntent, modifier)
}

@Composable
fun MerchantSalesContent(state: MerchantSalesViewState, onIntent: (MerchantSalesViewIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(REDEEM_CONTENT_PADDING.dp), verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp)) {
        Text(stringResource(R.string.claims_redeem_title))
        OutlinedTextField(
            value = state.input,
            onValueChange = { onIntent(MerchantSalesViewIntent.Edit(it)) },
            label = { Text(stringResource(R.string.claims_code)) },
            singleLine = true,
            enabled = !state.loading
        )
        Button(
            onClick = { onIntent(MerchantSalesViewIntent.Submit(state.input)) },
            enabled = !state.loading && state.input.isNotBlank() && state.result == null
        ) { Text(stringResource(R.string.claims_redeem)) }
        if (state.loading) CircularProgressIndicator()
        state.error?.let { Text(claimsErrorText(it)) }
        state.result?.let {
            Text(stringResource(R.string.claims_redeemed, Instant.ofEpochMilli(it.redeemedAtEpochMs).toString()))
        }
    }
}
