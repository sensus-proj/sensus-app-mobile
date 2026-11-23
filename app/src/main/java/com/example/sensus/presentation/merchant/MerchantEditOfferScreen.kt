package com.example.sensus.presentation.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusOrangeDark
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.StatusRed
import com.example.sensus.theme.sensusOutlinedButtonColors
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import com.example.sensus.theme.sensusTextButtonColors

private const val REPUBLISH_START_LEAD_MILLIS = 60_000L
private const val DEFAULT_LEAD_MILLIS = 3_600_000L
private const val DESCRIPTION_MIN_LINES = 2
private const val STATUS_ALPHA = 0.14f

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EditOfferHeader(offer = offer, onNavigateBack = actions.onNavigateBack)
        if (!RepublishOfferUseCase.supports(offer.status)) {
            EditOfferLifecycleCard(
                offer = offer,
                isBusy = vmState.isChangingStatus || vmState.isSaving,
                onTogglePause = actions.onToggleStatus,
                onRequestArchive = actions.onRequestArchive
            )
        }
        if (RepublishOfferUseCase.supports(offer.status)) {
            Text(stringResource(R.string.republish_schedule_hint), color = SensusTextSecondary)
            EditStockRow(form.stockCount, null) { actions.onFormChange(form.copy(stockCount = it)) }
            OfferExpirationPicker(value = form.expiresAtMillis, error = null, onChange = {
                actions.onFormChange(form.copy(expiresAtMillis = it))
            })
            if (vmState.republishFailed) Text(stringResource(R.string.republish_failed), color = SensusRed)
            RepublishOfferButton(offer.status, vmState.isSaving || vmState.isChangingStatus, actions.onRepublish)
        } else if (offer.status != OfferLifecycleStatus.ARCHIVED) {
            EditOfferFormCard(
                form = form,
                fieldError = vmState.fieldError,
                onFormChange = actions.onFormChange
            )
            vmState.errorMessage?.let { msg ->
                Text(text = msg, color = SensusRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            SaveOfferChangesButton(
                isSaving = vmState.isSaving,
                enabled = !vmState.isChangingStatus,
                onClick = actions.onSaveChanges
            )
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
    val (label, color) = editOfferStatusAppearance(offer)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigateBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusTextPrimary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Gestionar promoción", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = offer.name, fontSize = 13.sp, color = SensusTextSecondary)
        }
        Surface(color = color.copy(alpha = STATUS_ALPHA), shape = RoundedCornerShape(8.dp)) {
            Text(
                text = label,
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun editOfferStatusAppearance(offer: ProductOffer): Pair<String, Color> = when {
    offer.status == OfferLifecycleStatus.ARCHIVED -> "De baja" to SensusRed
    offer.status == OfferLifecycleStatus.EXPIRED -> "Expirada" to SensusRed
    offer.status == OfferLifecycleStatus.PAUSED || offer.unitsLeft <= 0 -> "Pausada" to SensusOrangeDark
    else -> "Activa" to SensusSuccess
}

@Composable
private fun EditOfferLifecycleCard(offer: ProductOffer, isBusy: Boolean, onTogglePause: (OfferLifecycleStatus) -> Unit, onRequestArchive: () -> Unit) {
    val isPausedOrZero = offer.status == OfferLifecycleStatus.PAUSED ||
        (offer.status == OfferLifecycleStatus.ACTIVE && offer.unitsLeft <= 0)
    val isArchived = offer.status == OfferLifecycleStatus.ARCHIVED
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "Visibilidad y estado", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            if (isArchived) {
                Text(
                    text = "Esta promoción ya fue dada de baja y no es visible en el mapa ni catálogo público.",
                    fontSize = 13.sp,
                    color = SensusTextSecondary
                )
            } else {
                val hint = if (isPausedOrZero) {
                    "Actualmente está en pausa y no aparece al público. Puedes reactivarla en cualquier momento."
                } else {
                    "Pausa temporalmente esta oferta si se agotó por mostrador o deseas ocultarla al público sin perder sus datos."
                }
                Text(text = hint, fontSize = 13.sp, color = SensusTextSecondary)
                LifecycleActionButtons(
                    isPaused = isPausedOrZero,
                    isBusy = isBusy,
                    canReactivate = offer.unitsLeft > 0,
                    onTogglePause = {
                        val target = if (isPausedOrZero) OfferLifecycleStatus.ACTIVE else OfferLifecycleStatus.PAUSED
                        onTogglePause(target)
                    },
                    onRequestArchive = onRequestArchive
                )
            }
        }
    }
}

@Composable
private fun LifecycleActionButtons(isPaused: Boolean, isBusy: Boolean, canReactivate: Boolean, onTogglePause: () -> Unit, onRequestArchive: () -> Unit) {
    val toggleLabel = if (isPaused) "Reactivar promoción" else "Pausar promoción"
    val toggleIcon = if (isPaused) Icons.Default.PlayCircleOutline else Icons.Default.PauseCircleOutline
    val toggleColor = if (isPaused) SensusSuccess else SensusOrangeDark
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            colors = sensusOutlinedButtonColors(),
            onClick = onTogglePause,
            enabled = !isBusy && (!isPaused || canReactivate),
            border = BorderStroke(1.dp, toggleColor),
            shape = SensusButtonShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = toggleIcon, contentDescription = null, tint = toggleColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = toggleLabel, color = toggleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        if (isPaused && !canReactivate) {
            Text(
                text = "Primero aumenta el stock a 1 o más piezas y guarda cambios para poder reactivarla.",
                fontSize = 11.sp,
                color = SensusOrangeDark
            )
        }
        OutlinedButton(
            colors = sensusOutlinedButtonColors(),
            onClick = onRequestArchive,
            enabled = !isBusy,
            border = BorderStroke(1.dp, SensusRed),
            shape = SensusButtonShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = SensusRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Dar de baja promoción", color = SensusRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EditOfferFormCard(form: EditOfferFormHolder, fieldError: OfferValidationError?, onFormChange: (EditOfferFormHolder) -> Unit) {
    val orig = form.originalPriceText.toDoubleOrNull() ?: 0.0
    val disc = form.discountPriceText.toDoubleOrNull() ?: 0.0
    val discountPercent = OfferPublishingPolicy.discountPercent(orig, disc)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Editar detalles", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            OutlinedTextField(
                value = form.title,
                onValueChange = { onFormChange(form.copy(title = it)) },
                label = { Text("Título del producto") },
                isError = fieldError?.field == OfferField.TITLE,
                supportingText = if (fieldError?.field == OfferField.TITLE) {
                    { Text(fieldError.message, color = SensusRed) }
                } else {
                    null
                },
                colors = sensusOutlinedTextFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = { onFormChange(form.copy(description = it)) },
                label = { Text("Descripción") },
                isError = fieldError?.field == OfferField.DESCRIPTION,
                supportingText = if (fieldError?.field == OfferField.DESCRIPTION) {
                    { Text(fieldError.message, color = SensusRed) }
                } else {
                    null
                },
                colors = sensusOutlinedTextFieldColors(),
                minLines = DESCRIPTION_MIN_LINES,
                modifier = Modifier.fillMaxWidth()
            )
            EditPricesRow(form = form, discountPercent = discountPercent, fieldError = fieldError, onFormChange = onFormChange)
            EditStockRow(stockCount = form.stockCount, fieldError = fieldError) { nextStock ->
                onFormChange(form.copy(stockCount = nextStock))
            }
            OfferExpirationPicker(
                value = form.expiresAtMillis,
                error = fieldError?.takeIf { it.field == OfferField.EXPIRATION }?.message,
                onChange = { nextMillis -> onFormChange(form.copy(expiresAtMillis = nextMillis)) }
            )
        }
    }
}

@Composable
private fun EditPricesRow(form: EditOfferFormHolder, discountPercent: Int, fieldError: OfferValidationError?, onFormChange: (EditOfferFormHolder) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = form.originalPriceText,
                onValueChange = { onFormChange(form.copy(originalPriceText = it)) },
                label = { Text("Precio normal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = fieldError?.field == OfferField.PRICE,
                colors = sensusOutlinedTextFieldColors(),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = form.discountPriceText,
                onValueChange = { onFormChange(form.copy(discountPriceText = it)) },
                label = { Text("Precio oferta") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = fieldError?.field == OfferField.PRICE,
                colors = sensusOutlinedTextFieldColors(),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (fieldError?.field == OfferField.PRICE) {
            Text(text = fieldError.message, color = SensusRed, fontSize = 13.sp)
        } else if (discountPercent > 0) {
            Text(text = "Descuento aplicado: -$discountPercent%", color = SensusSuccess, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditStockRow(stockCount: Int, fieldError: OfferValidationError?, onStockChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Stock disponible", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = { if (stockCount > 1) onStockChange(stockCount - 1) }) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Disminuir stock", tint = SensusVioletInk)
                }
                Text(text = "$stockCount u.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk)
                IconButton(onClick = { if (stockCount < OfferPublishingPolicy.MAX_STOCK) onStockChange(stockCount + 1) }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Aumentar stock", tint = SensusVioletInk)
                }
            }
        }
        fieldError?.takeIf { it.field == OfferField.STOCK }?.let {
            Text(text = it.message, color = SensusRed, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SaveOfferChangesButton(isSaving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSaving && enabled,
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        shape = SensusButtonShape,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
    ) {
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text = "Guardar cambios", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
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
        Button(shape = SensusButtonShape, onClick = onClick, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.republish_action))
        }
    }
}
