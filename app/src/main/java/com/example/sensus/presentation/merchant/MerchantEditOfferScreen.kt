package com.example.sensus.presentation.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.domain.model.OfferField
import com.example.sensus.domain.model.OfferLifecycleStatus
import com.example.sensus.domain.model.OfferPublishingPolicy
import com.example.sensus.domain.model.OfferValidationError
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.RepublishOfferInput
import com.example.sensus.domain.model.UpdateOfferInput
import com.example.sensus.domain.usecase.RepublishOfferUseCase
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.BrandOrangeDark
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.StatusRed
import com.example.sensus.theme.sensusTextButtonColors

private const val REPUBLISH_START_LEAD_MILLIS = 60_000L
private const val DEFAULT_LEAD_MILLIS = 3_600_000L
private const val DESCRIPTION_MAX_LINES = 3

@ComposeUiModel
private data class EditOfferFormHolder(
    val title: String,
    val description: String,
    val originalPriceText: String,
    val discountPriceText: String,
    val stockCount: Int,
    val expiresAtMillis: Long
)

@ComposeUiModel
private data class EditOfferActions(
    val onNavigateBack: () -> Unit,
    val onFormChange: (EditOfferFormHolder) -> Unit,
    val onToggleStatus: (OfferLifecycleStatus) -> Unit,
    val onRequestArchive: () -> Unit,
    val onRepublish: () -> Unit,
    val onSaveChanges: () -> Unit
)

@Composable
fun MerchantEditOfferScreen(
    offer: ProductOffer,
    viewModel: MerchantEditOfferViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onOfferChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vmState by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(offer.id) {
        viewModel.dispatchIntent(MerchantEditOfferViewIntent.Initialize(offer))
    }
    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is MerchantEditOfferViewEffect.OfferRepublished,
                is MerchantEditOfferViewEffect.OfferUpdated,
                is MerchantEditOfferViewEffect.OfferStatusChanged -> onOfferChanged()
            }
        }
    }

    MerchantEditOfferContent(offer, vmState, onNavigateBack, viewModel::dispatchIntent, modifier)
}

@Composable
internal fun MerchantEditOfferContent(
    offer: ProductOffer,
    vmState: MerchantEditOfferViewState,
    onNavigateBack: () -> Unit,
    onIntent: (MerchantEditOfferViewIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val effectiveOffer = vmState.currentOffer?.takeIf { it.id == offer.id } ?: offer
    var form by remember(offer.id) { mutableStateOf(initialEditForm(offer)) }
    var pendingStatus by remember(offer.id, effectiveOffer.status) { mutableStateOf<OfferLifecycleStatus?>(null) }

    EditOfferScrollContent(
        offer = effectiveOffer,
        form = form,
        vmState = vmState,
        actions = EditOfferActions(
            onNavigateBack = onNavigateBack,
            onFormChange = { updated ->
                form = updated
                onIntent(MerchantEditOfferViewIntent.FieldEdited)
            },
            onToggleStatus = { nextStatus ->
                if (nextStatus == OfferLifecycleStatus.PAUSED) {
                    pendingStatus = nextStatus
                } else {
                    onIntent(MerchantEditOfferViewIntent.ChangeStatus(effectiveOffer, nextStatus))
                }
            },
            onRequestArchive = { pendingStatus = OfferLifecycleStatus.ARCHIVED },
            onRepublish = {
                onIntent(
                    MerchantEditOfferViewIntent.Republish(
                        RepublishOfferInput(offer.id, System.currentTimeMillis() + REPUBLISH_START_LEAD_MILLIS, form.expiresAtMillis, form.stockCount)
                    )
                )
            },
            onSaveChanges = {
                onIntent(MerchantEditOfferViewIntent.SaveChanges(buildUpdateOfferInput(effectiveOffer, form)))
            }
        ),
        modifier = modifier
    )

    pendingStatus?.let { status ->
        OfferStatusConfirmationDialog(
            offerName = effectiveOffer.name,
            status = status,
            onDismiss = { pendingStatus = null },
            onConfirm = {
                pendingStatus = null
                onIntent(MerchantEditOfferViewIntent.ChangeStatus(effectiveOffer, status))
            }
        )
    }
}

@Composable
private fun EditOfferScrollContent(
    offer: ProductOffer,
    form: EditOfferFormHolder,
    vmState: MerchantEditOfferViewState,
    actions: EditOfferActions,
    modifier: Modifier = Modifier
) {
    val isBusy = vmState.isChangingStatus || vmState.isSaving
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusVioletBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(MerchantScreenPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditOfferHeader(offer = offer, onNavigateBack = actions.onNavigateBack)
        EditOfferImageBanner(offer)
        when {
            RepublishOfferUseCase.supports(offer.status) -> EditOfferRepublishSection(form, vmState, offer.status, actions)
            offer.status == OfferLifecycleStatus.ARCHIVED -> EditOfferArchivedCard()
            else -> {
                EditOfferFields(form = form, fieldError = vmState.fieldError, onFormChange = actions.onFormChange)
                EditOfferActiveToggle(offer = offer, isBusy = isBusy, onToggleStatus = actions.onToggleStatus)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = actions.onRequestArchive, enabled = !isBusy, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.merchant_edit_delete), color = SensusRed, fontWeight = FontWeight.SemiBold)
                    }
                }
                vmState.errorMessage?.let { MerchantErrorText(it) }
                MerchantPrimaryButton(
                    text = stringResource(R.string.merchant_edit_save),
                    onClick = actions.onSaveChanges,
                    enabled = !vmState.isChangingStatus,
                    loading = vmState.isSaving
                )
            }
        }
    }
}

private fun initialEditForm(offer: ProductOffer): EditOfferFormHolder {
    val now = System.currentTimeMillis()
    val minimumEndMillis = if (RepublishOfferUseCase.supports(offer.status)) now + DEFAULT_LEAD_MILLIS else now
    val futureMillis = offer.expiresAtMillis?.takeIf { it > minimumEndMillis } ?: (now + DEFAULT_LEAD_MILLIS)
    return EditOfferFormHolder(
        title = offer.name,
        description = offer.description,
        originalPriceText = offer.originalPrice.toString(),
        discountPriceText = offer.discountPrice.toString(),
        stockCount = offer.unitsLeft.coerceAtLeast(1),
        expiresAtMillis = futureMillis
    )
}

private fun buildUpdateOfferInput(offer: ProductOffer, form: EditOfferFormHolder): UpdateOfferInput = UpdateOfferInput(
    offerId = offer.id,
    title = form.title,
    description = form.description,
    originalPrice = form.originalPriceText.toDoubleOrNull() ?: 0.0,
    discountPrice = form.discountPriceText.toDoubleOrNull() ?: 0.0,
    stock = form.stockCount,
    expiresAtMillis = form.expiresAtMillis,
    imageUrl = offer.imageUrl,
    expectedVersion = offer.version
)

@Composable
private fun EditOfferHeader(offer: ProductOffer, onNavigateBack: () -> Unit) {
    MerchantScreenHeader(
        title = stringResource(R.string.merchant_edit_title),
        subtitle = offer.name,
        onNavigateBack = onNavigateBack
    ) {
        EditOfferStatusChip(offer)
    }
}

@Composable
private fun EditOfferStatusChip(offer: ProductOffer) {
    when {
        offer.status == OfferLifecycleStatus.ARCHIVED -> MerchantStatusChip(stringResource(R.string.merchant_status_archived), SensusRed, SensusRedBg)
        offer.status == OfferLifecycleStatus.EXPIRED -> MerchantStatusChip(stringResource(R.string.merchant_status_expired), SensusRed, SensusRedBg)
        offer.status == OfferLifecycleStatus.PAUSED || offer.unitsLeft <= 0 ->
            MerchantStatusChip(stringResource(R.string.merchant_edit_status_paused), SensusTextSecondary, SensusSurface)
        else -> MerchantStatusChip(stringResource(R.string.merchant_status_active), SensusSuccess, SensusSuccessBg)
    }
}

@Composable
private fun EditOfferImageBanner(offer: ProductOffer) {
    val imageUrl = offer.imageUrl?.takeIf { it.isNotBlank() } ?: return
    AsyncImage(
        model = imageUrl,
        contentDescription = offer.name,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(MerchantCardShape)
    )
}

@Composable
private fun EditOfferFields(form: EditOfferFormHolder, fieldError: OfferValidationError?, onFormChange: (EditOfferFormHolder) -> Unit) {
    val discountPercent = OfferPublishingPolicy.discountPercent(
        form.originalPriceText.toDoubleOrNull() ?: 0.0,
        form.discountPriceText.toDoubleOrNull() ?: 0.0
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MerchantTextField(
            label = stringResource(R.string.merchant_edit_product),
            value = form.title,
            options = MerchantFieldOptions(
                error = fieldError?.takeIf { it.field == OfferField.TITLE }?.message,
                leadingIcon = Icons.Outlined.LocalOffer
            ),
            onValueChange = { onFormChange(form.copy(title = it)) }
        )
        MerchantTextField(
            label = stringResource(R.string.merchant_new_offer_description),
            value = form.description,
            options = MerchantFieldOptions(
                error = fieldError?.takeIf { it.field == OfferField.DESCRIPTION }?.message,
                maxLines = DESCRIPTION_MAX_LINES
            ),
            onValueChange = { onFormChange(form.copy(description = it)) }
        )
        val priceOptions = MerchantFieldOptions(keyboardType = KeyboardType.Decimal, leadingIcon = Icons.Outlined.LocalOffer)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MerchantTextField(
                label = stringResource(R.string.merchant_edit_offer_price),
                value = form.discountPriceText,
                options = priceOptions,
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(discountPriceText = it)) }
            )
            MerchantTextField(
                label = stringResource(R.string.merchant_edit_normal_price),
                value = form.originalPriceText,
                options = priceOptions,
                modifier = Modifier.weight(1f),
                onValueChange = { onFormChange(form.copy(originalPriceText = it)) }
            )
        }
        EditPriceFeedback(fieldError, discountPercent)
        EditStockRow(form.stockCount, fieldError) { onFormChange(form.copy(stockCount = it)) }
        OfferExpirationPicker(
            value = form.expiresAtMillis,
            error = fieldError?.takeIf { it.field == OfferField.EXPIRATION }?.message,
            onChange = { onFormChange(form.copy(expiresAtMillis = it)) }
        )
    }
}

@Composable
private fun EditPriceFeedback(fieldError: OfferValidationError?, discountPercent: Int) {
    if (fieldError?.field == OfferField.PRICE) {
        MerchantErrorText(fieldError.message)
    } else if (discountPercent > 0) {
        Text(stringResource(R.string.merchant_edit_discount, discountPercent), color = SensusSuccess, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditStockRow(stockCount: Int, fieldError: OfferValidationError?, onStockChange: (Int) -> Unit) {
    Column {
        MerchantFieldLabel(stringResource(R.string.merchant_edit_units))
        Surface(
            shape = MerchantFieldShape,
            color = SensusSurface,
            border = BorderStroke(1.dp, SensusCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Inventory2, contentDescription = null, tint = SensusTextSecondary)
                Text(
                    stringResource(R.string.merchant_edit_units_value, stockCount),
                    fontSize = 15.sp,
                    color = SensusTextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                IconButton(onClick = { if (stockCount > 1) onStockChange(stockCount - 1) }) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.merchant_edit_stock_less), tint = SensusVioletInk)
                }
                IconButton(onClick = { if (stockCount < OfferPublishingPolicy.MAX_STOCK) onStockChange(stockCount + 1) }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.merchant_edit_stock_more), tint = SensusVioletInk)
                }
            }
        }
        fieldError?.takeIf { it.field == OfferField.STOCK }?.let { MerchantErrorText(it.message) }
    }
}

@Composable
private fun EditOfferActiveToggle(offer: ProductOffer, isBusy: Boolean, onToggleStatus: (OfferLifecycleStatus) -> Unit) {
    val isPausedOrZero = offer.status == OfferLifecycleStatus.PAUSED ||
        (offer.status == OfferLifecycleStatus.ACTIVE && offer.unitsLeft <= 0)
    val canReactivate = offer.unitsLeft > 0
    val enabled = !isBusy && (!isPausedOrZero || canReactivate)
    val toggleDescription = stringResource(if (isPausedOrZero) R.string.merchant_edit_reactivate else R.string.merchant_edit_pause)
    MerchantCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = !isPausedOrZero,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = { onToggleStatus(if (isPausedOrZero) OfferLifecycleStatus.ACTIVE else OfferLifecycleStatus.PAUSED) }
                )
                .semantics { contentDescription = toggleDescription }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.merchant_edit_active_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
                Text(stringResource(R.string.merchant_edit_active_hint), fontSize = 11.sp, color = SensusTextSecondary)
            }
            Switch(
                checked = !isPausedOrZero,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(checkedTrackColor = SensusPurple, checkedThumbColor = Color.White)
            )
        }
        if (isPausedOrZero && !canReactivate) {
            Text(
                stringResource(R.string.merchant_edit_reactivate_hint),
                fontSize = 11.sp,
                color = BrandOrangeDark,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun EditOfferArchivedCard() {
    Surface(color = SensusRedBg, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.merchant_edit_archived), fontSize = 13.sp, color = SensusRed, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun EditOfferRepublishSection(form: EditOfferFormHolder, vmState: MerchantEditOfferViewState, status: OfferLifecycleStatus, actions: EditOfferActions) {
    MerchantNoticeCard(text = stringResource(R.string.republish_schedule_hint), icon = Icons.Default.Inventory2)
    EditStockRow(form.stockCount, null) { actions.onFormChange(form.copy(stockCount = it)) }
    OfferExpirationPicker(value = form.expiresAtMillis, error = null, onChange = {
        actions.onFormChange(form.copy(expiresAtMillis = it))
    })
    if (vmState.republishFailed) MerchantErrorText(stringResource(R.string.republish_failed))
    RepublishOfferButton(status, vmState.isSaving || vmState.isChangingStatus, actions.onRepublish)
}

@Composable
private fun OfferStatusConfirmationDialog(offerName: String, status: OfferLifecycleStatus, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val isPause = status == OfferLifecycleStatus.PAUSED
    val title = if (isPause) R.string.merchant_pause_confirm_title else R.string.merchant_archive_confirm_title
    val message = if (isPause) R.string.merchant_pause_confirm_message else R.string.merchant_archive_confirm_message
    val action = if (isPause) R.string.merchant_pause_confirm_action else R.string.merchant_archive_confirm_action
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(title), fontWeight = FontWeight.Bold) },
        text = { Text(text = stringResource(message, offerName)) },
        confirmButton = {
            Button(
                shape = SensusButtonShape,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (isPause) SensusPurple else StatusRed)
            ) {
                Text(text = stringResource(action), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(colors = sensusTextButtonColors(), onClick = onDismiss) {
                Text(text = stringResource(R.string.publishing_cancel), color = SensusTextMuted)
            }
        }
    )
}

@Composable
internal fun RepublishOfferButton(status: OfferLifecycleStatus, isBusy: Boolean, onClick: () -> Unit) {
    if (RepublishOfferUseCase.supports(status)) {
        MerchantPrimaryButton(text = stringResource(R.string.republish_action), onClick = onClick, enabled = !isBusy)
    }
}
