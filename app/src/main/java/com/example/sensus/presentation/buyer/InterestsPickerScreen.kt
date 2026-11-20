package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.domain.model.InterestCatalog
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.getCategoryIcon
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val HERO_BADGE_ALPHA = 0.12f

@com.example.sensus.presentation.common.ComposeUiModel
data class InterestsPickerCallbacks(
    val onCompleted: () -> Unit = {},
    val onSkip: (() -> Unit)? = null,
    val onNavigateBack: (() -> Unit)? = null,
    val onRequireAuth: () -> Unit = {}
)

@com.example.sensus.presentation.common.ComposeUiModel
internal data class InterestsPickerUiActions(
    val onToggle: (String) -> Unit,
    val onSave: () -> Unit,
    val onSkipOrBack: () -> Unit
)

@Composable
fun InterestsPickerScreen(
    isOnboarding: Boolean = false,
    callbacks: InterestsPickerCallbacks = InterestsPickerCallbacks(),
    modifier: Modifier = Modifier,
    viewModel: InterestsPickerViewModel = hiltViewModel()
) {
    val state by viewModel.viewState.collectAsStateWithLifecycle()

    LaunchedEffect(isOnboarding) {
        if (!isOnboarding) {
            viewModel.dispatchIntent(InterestsPickerViewIntent.LoadCurrent)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is InterestsPickerViewEffect.Saved -> callbacks.onCompleted()
                InterestsPickerViewEffect.Skipped -> (callbacks.onSkip ?: callbacks.onCompleted)()
                InterestsPickerViewEffect.RequireAuth -> callbacks.onRequireAuth()
            }
        }
    }

    InterestsPickerContent(
        state = state,
        isOnboarding = isOnboarding,
        actions = InterestsPickerUiActions(
            onToggle = { code -> viewModel.dispatchIntent(InterestsPickerViewIntent.ToggleCategory(code)) },
            onSave = { viewModel.dispatchIntent(InterestsPickerViewIntent.Save) },
            onSkipOrBack = {
                if (isOnboarding) {
                    viewModel.dispatchIntent(InterestsPickerViewIntent.Skip)
                } else {
                    (callbacks.onNavigateBack ?: callbacks.onCompleted)()
                }
            }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InterestsPickerContent(state: InterestsPickerViewState, isOnboarding: Boolean, actions: InterestsPickerUiActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        InterestsPickerHeader(
            isOnboarding = isOnboarding,
            selectedCount = state.selectedCodes.size,
            maxSelection = state.maxSelection,
            onBack = actions.onSkipOrBack
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SensusPurple)
            }
        } else {
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    state.availableCategories.forEach { category ->
                        val apiCode = category.apiCode ?: return@forEach
                        val selected = apiCode in state.selectedCodes
                        InterestCategoryChip(
                            category = category,
                            selected = selected,
                            onClick = { actions.onToggle(apiCode) }
                        )
                    }
                }
                if (!state.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        color = SensusRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        InterestsPickerActions(
            isOnboarding = isOnboarding,
            isSaving = state.isSaving,
            onSave = actions.onSave,
            onSkipOrBack = actions.onSkipOrBack
        )
    }
}

@Composable
private fun InterestsPickerHeader(isOnboarding: Boolean, selectedCount: Int, maxSelection: Int, onBack: () -> Unit) {
    if (!isOnboarding) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = SensusPurple
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Mis intereses",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = SensusPurple.copy(alpha = HERO_BADGE_ALPHA), modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = if (isOnboarding) "Elige tus categorías favoritas" else "Personaliza tus promociones",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = "Hasta $maxSelection categorías ($selectedCount/$maxSelection elegidas)",
                fontSize = 13.sp,
                color = SensusTextSecondary
            )
        }
    }
}

@Composable
private fun InterestCategoryChip(category: StoreCategory, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) SensusPurple else Color.White,
        border = BorderStroke(1.dp, if (selected) SensusPurple else SensusCardBorder),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = getCategoryIcon(category),
                contentDescription = category.displayName,
                tint = if (selected) Color.White else SensusPurple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else SensusTextPrimary
            )
            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionada",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun InterestsPickerActions(isOnboarding: Boolean, isSaving: Boolean, onSave: () -> Unit, onSkipOrBack: () -> Unit) {
    Button(
        onClick = onSave,
        enabled = !isSaving,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        if (isSaving) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text = "Guardar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }

    if (isOnboarding) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSkipOrBack,
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(text = "Omitir", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
        }
    }
}

@Preview(showBackground = true, name = "InterestsPicker")
@Composable
internal fun InterestsPickerPreview() {
    SensusTheme {
        InterestsPickerContent(
            state = InterestsPickerViewState(
                availableCategories = InterestCatalog.selectableCategories,
                selectedCodes = listOf("Bakery", "Dairy")
            ),
            isOnboarding = true,
            actions = InterestsPickerUiActions(onToggle = {}, onSave = {}, onSkipOrBack = {})
        )
    }
}
