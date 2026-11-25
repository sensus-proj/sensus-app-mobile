package com.example.sensus.presentation.buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sensus.R
import com.example.sensus.domain.model.OfferDiscoveryFilters
import com.example.sensus.domain.model.OfferSortOrder

private const val MAX_DISCOUNT_PERCENT = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfferDiscoveryFilterSheet(current: OfferDiscoveryFilters, onApply: (OfferDiscoveryFilters) -> Unit, onDismiss: () -> Unit) {
    var price by rememberSaveable { mutableStateOf(current.maxPrice?.toString().orEmpty()) }
    var discount by rememberSaveable { mutableStateOf(current.minDiscount?.toString().orEmpty()) }
    var openNow by rememberSaveable { mutableStateOf(current.openNowOnly) }
    var order by rememberSaveable { mutableStateOf(current.order) }
    val parsedPrice = price.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val parsedDiscount = discount.trim().toIntOrNull()?.takeIf { it in 0..MAX_DISCOUNT_PERCENT }
    val invalidPrice = price.isNotBlank() && parsedPrice == null
    val invalidDiscount = discount.isNotBlank() && parsedDiscount == null
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.offer_filters_title))
            OfferFilterInput(price, { price = it }, R.string.offer_filters_max_price, KeyboardType.Decimal, invalidPrice)
            OfferFilterInput(discount, { discount = it }, R.string.offer_filters_min_discount, KeyboardType.Number, invalidDiscount)
            FilterChip(openNow, { openNow = !openNow }, { Text(stringResource(R.string.merchant_open_now)) })
            Text(stringResource(R.string.offer_filters_sort))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OfferSortOrder.entries.forEach { option ->
                    FilterChip(order == option, { order = option }, { Text(stringResource(option.labelResource())) })
                }
            }
            Button(
                onClick = { onApply(OfferDiscoveryFilters(parsedPrice, parsedDiscount, order, openNow)) },
                enabled = !invalidPrice && !invalidDiscount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.offer_filters_apply))
            }
            TextButton(onClick = { onApply(OfferDiscoveryFilters()) }) {
                Text(stringResource(R.string.offer_filters_clear))
            }
        }
    }
}

@Composable
private fun OfferFilterInput(value: String, onChange: (String) -> Unit, label: Int, keyboard: KeyboardType, invalid: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        isError = invalid,
        supportingText = if (invalid) ({ Text(stringResource(R.string.offer_filters_invalid)) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

private fun OfferSortOrder.labelResource(): Int = when (this) {
    OfferSortOrder.DISTANCE -> R.string.offer_filters_distance
    OfferSortOrder.DISCOUNT -> R.string.offer_filters_discount
    OfferSortOrder.PRICE -> R.string.offer_filters_price
}

@Composable
internal fun BuyerOfferFilterControl(filters: OfferDiscoveryFilters, onChange: (OfferDiscoveryFilters) -> Unit) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    FilterChip(
        selected = filters != OfferDiscoveryFilters(),
        onClick = { showFilters = true },
        label = { Text(stringResource(R.string.offer_filters_title)) },
        modifier = Modifier.padding(horizontal = 20.dp)
    )
    if (showFilters) {
        OfferDiscoveryFilterSheet(
            current = filters,
            onApply = {
                onChange(it)
                showFilters = false
            },
            onDismiss = { showFilters = false }
        )
    }
}
