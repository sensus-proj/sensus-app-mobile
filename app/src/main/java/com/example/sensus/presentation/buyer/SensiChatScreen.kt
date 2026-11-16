package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.AiChatMessage
import com.example.sensus.domain.model.CitedOffer
import com.example.sensus.domain.model.RecommendedPackage
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.MarkdownText
import com.example.sensus.presentation.modals.GuestPromptDialog
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import java.util.Locale

private const val PACKAGE_BUBBLE_WIDTH_FRACTION = 0.95f
private const val TEXT_BUBBLE_WIDTH_FRACTION = 0.82f
private const val DIVIDER_HEX = 0xFFE2E8F0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensiChatScreen(
    viewModel: SensiChatViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    actions: SensiOfferActions = SensiOfferActions(),
    onRequireAuth: () -> Unit = {}
) {
    val chatState by viewModel.viewState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.viewEffect.collect { effect ->
            when (effect) {
                is SensiChatViewEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SensiChatContent(
        state = chatState,
        callbacks = SensiChatCallbacks(
            onSendMessage = { text -> viewModel.dispatchIntent(SensiChatViewIntent.SendMessage(text)) },
            onClearConversation = { viewModel.dispatchIntent(SensiChatViewIntent.ClearConversation) },
            onDismissError = { viewModel.dispatchIntent(SensiChatViewIntent.DismissError) },
            onDismissGuestPrompt = { viewModel.dispatchIntent(SensiChatViewIntent.DismissGuestPrompt) },
            onRequireAuth = onRequireAuth,
            actions = actions
        ),
        modifier = modifier
    )
}

/** Callbacks y acciones del chat de Sensi agrupados para mantener firmas cortas. */
@ComposeUiModel
data class SensiChatCallbacks(
    val onSendMessage: (String) -> Unit = {},
    val onClearConversation: () -> Unit = {},
    val onDismissError: () -> Unit = {},
    val onDismissGuestPrompt: () -> Unit = {},
    val onRequireAuth: () -> Unit = {},
    val actions: SensiOfferActions = SensiOfferActions()
)

@Composable
fun SensiChatContent(state: SensiChatViewState, callbacks: SensiChatCallbacks, modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    ScrollToLatestEffect(listState, state.messages.size, state.isTyping)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .imePadding()
    ) {
        SensiHeaderBar(isDegraded = state.isDegraded, onClear = callbacks.onClearConversation)
        AnimatedVisibility(visible = state.isDegraded) { DegradedModeBanner() }
        AnimatedVisibility(visible = state.errorMessage != null) {
            state.errorMessage?.let { error -> ErrorMessageBanner(error = error, onDismiss = callbacks.onDismissError) }
        }
        SensiMessageList(
            state = state,
            actions = callbacks.actions,
            listState = listState,
            modifier = Modifier.weight(1f)
        )
        QuickSuggestionsBar(onSuggestionClick = callbacks.onSendMessage)
        ChatInputBar(
            input = ChatInputUi(inputText, keyboardAwareBottomPadding(), state.isSending, state.retryAfterSeconds),
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val text = inputText
                    inputText = ""
                    callbacks.onSendMessage(text)
                }
            }
        )
    }

    if (state.showGuestPrompt) {
        GuestPromptDialog(
            onDismiss = callbacks.onDismissGuestPrompt,
            onNavigateToLogin = callbacks.onRequireAuth,
            onNavigateToRegister = callbacks.onRequireAuth
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun keyboardAwareBottomPadding(): Dp {
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0 || WindowInsets.isImeVisible
    val padding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 0.dp else 76.dp,
        label = "InputBarBottomPadding"
    )
    return padding
}

@Composable
private fun ScrollToLatestEffect(listState: LazyListState, messageCount: Int, isTyping: Boolean) {
    LaunchedEffect(messageCount, isTyping) {
        val totalItems = messageCount + if (isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }
}

@Composable
private fun SensiMessageList(state: SensiChatViewState, actions: SensiOfferActions, listState: LazyListState, modifier: Modifier = Modifier) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.messages.isEmpty() && !state.isTyping) {
            item { EmptyChatWelcomeCard() }
        }
        items(state.messages, key = { it.id }) { msg -> ChatBubble(message = msg, actions = actions) }
        if (state.isTyping) {
            item { TypingIndicatorBubble() }
        }
    }
}

@Composable
private fun ChatBubble(message: AiChatMessage, actions: SensiOfferActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape(message.isFromUser),
            color = if (message.isFromUser) SensusPurple else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(
                if (message.recommendedPackage != null) PACKAGE_BUBBLE_WIDTH_FRACTION else TEXT_BUBBLE_WIDTH_FRACTION
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                BubbleText(message)
                message.recommendedPackage?.let { pkg ->
                    Spacer(modifier = Modifier.height(12.dp))
                    RecommendedPackageCard(
                        pkg = pkg,
                        onApartarCombo = actions.onApartarPackage,
                        onProductClick = { actions.onDetail(it.toProductOffer()) }
                    )
                }
            }
        }
        if (!message.isFromUser) {
            visibleOfferCards(message.content, message.offersCited).forEach { offer ->
                Spacer(modifier = Modifier.height(8.dp))
                SensiOfferCard(
                    offer = offer,
                    actions = actions,
                    modifier = Modifier.fillMaxWidth(PACKAGE_BUBBLE_WIDTH_FRACTION)
                )
            }
        }
    }
}

private fun bubbleShape(isFromUser: Boolean) = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = if (isFromUser) 16.dp else 4.dp,
    bottomEnd = if (isFromUser) 4.dp else 16.dp
)

@Composable
private fun BubbleText(message: AiChatMessage) {
    if (message.isFromUser) {
        Text(text = message.content, fontSize = 14.sp, color = Color.White, lineHeight = 20.sp)
    } else {
        MarkdownText(text = message.content, color = SensusTextPrimary)
    }
}

@Composable
private fun RecommendedPackageCard(pkg: RecommendedPackage, onApartarCombo: () -> Unit, onProductClick: (CitedOffer) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SensusBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            PackageHeader(pkg)
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(DIVIDER_HEX))
            Spacer(modifier = Modifier.height(8.dp))

            pkg.items.forEach { item ->
                CitedOfferRow(item = item, onClick = { onProductClick(item) })
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApartarCombo,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Apartar paquete ($${String.format(Locale.US, "%.0f", pkg.totalPriceMxn)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PackageHeader(pkg: RecommendedPackage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Paquete Sugerido", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
            if (pkg.savingsMxn > 0) {
                Text(
                    text = "Ahorras $${String.format(Locale.US, "%.2f", pkg.savingsMxn)} MXN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SensusSuccess
                )
            }
        }
        Text(
            text = "$${String.format(Locale.US, "%.2f", pkg.totalPriceMxn)} MXN",
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SensusPurple
        )
    }
}

@Composable
private fun CitedOfferRow(item: CitedOffer, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SensusSuccess,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.merchantName,
                    fontSize = 11.sp,
                    color = SensusTextSecondary
                )
                item.distanceKm?.let { dist ->
                    Text(
                        text = " • ${String.format(Locale.US, "%.1f", dist)} km",
                        fontSize = 11.sp,
                        color = SensusTextMuted
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${String.format(Locale.US, "%.2f", item.offerPriceMxn)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SensusPurple
            )
            if (item.regularPriceMxn > item.offerPriceMxn) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.regularPriceMxn)}",
                    fontSize = 10.sp,
                    color = SensusTextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
    }
}

@Preview(name = "Normal con mensajes", showBackground = true)
@Composable
internal fun SensiChatPreviewNormal() {
    val samplePackage = RecommendedPackage(
        budgetMxn = 100.0,
        totalPriceMxn = 85.0,
        savingsMxn = 25.0,
        items = listOf(
            CitedOffer(
                offerId = "off_1",
                title = "Arroz 1 kg",
                merchantId = "m_1",
                merchantName = "Abarrotes Don Luis",
                offerPriceMxn = 25.0,
                regularPriceMxn = 35.0,
                availableStock = 5,
                isOpenNow = true,
                distanceKm = 0.8
            ),
            CitedOffer(
                offerId = "off_2",
                title = "Aceite vegetal 900ml",
                merchantId = "m_1",
                merchantName = "Abarrotes Don Luis",
                offerPriceMxn = 60.0,
                regularPriceMxn = 75.0,
                availableStock = 3,
                isOpenNow = true,
                distanceKm = 0.8
            )
        )
    )
    val state = SensiChatViewState(
        messages = listOf(
            AiChatMessage(id = "1", role = "User", content = "Tengo $100 para despensa"),
            AiChatMessage(
                id = "2",
                role = "Assistant",
                content = "Te armé este paquete con ofertas cercanas dentro de tu presupuesto:",
                recommendedPackage = samplePackage
            )
        )
    )
    SensiChatContent(state = state, callbacks = SensiChatCallbacks())
}

@Preview(name = "Escribiendo", showBackground = true)
@Composable
internal fun SensiChatPreviewTyping() {
    val state = SensiChatViewState(
        messages = listOf(
            AiChatMessage(id = "1", role = "User", content = "Busco pan dulce")
        ),
        isTyping = true
    )
    SensiChatContent(state = state, callbacks = SensiChatCallbacks())
}

@Preview(name = "Modo degradado", showBackground = true)
@Composable
internal fun SensiChatPreviewDegraded() {
    val state = SensiChatViewState(
        messages = listOf(
            AiChatMessage(id = "1", role = "User", content = "Hola"),
            AiChatMessage(id = "2", role = "Assistant", content = "¡Hola! Estoy en modo básico temporalmente.", degraded = true)
        ),
        isDegraded = true
    )
    SensiChatContent(state = state, callbacks = SensiChatCallbacks())
}

@Preview(name = "Vacío", showBackground = true)
@Composable
internal fun SensiChatPreviewEmpty() {
    val state = SensiChatViewState()
    SensiChatContent(state = state, callbacks = SensiChatCallbacks())
}
