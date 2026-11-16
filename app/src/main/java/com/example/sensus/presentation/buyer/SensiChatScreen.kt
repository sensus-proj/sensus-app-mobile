package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
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
import com.example.sensus.presentation.common.MarkdownText
import com.example.sensus.presentation.modals.GuestPromptDialog
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusPurpleDark
import com.example.sensus.theme.SensusPurpleLight
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.sensusOutlinedTextFieldColors
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
        modifier = modifier,
        onSendMessage = { text -> viewModel.dispatchIntent(SensiChatViewIntent.SendMessage(text)) },
        onClearConversation = { viewModel.dispatchIntent(SensiChatViewIntent.ClearConversation) },
        onDismissError = { viewModel.dispatchIntent(SensiChatViewIntent.DismissError) },
        onDismissGuestPrompt = { viewModel.dispatchIntent(SensiChatViewIntent.DismissGuestPrompt) },
        actions = actions,
        onRequireAuth = onRequireAuth
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensiChatContent(
    state: SensiChatViewState,
    modifier: Modifier = Modifier,
    onSendMessage: (String) -> Unit,
    onClearConversation: () -> Unit,
    onDismissError: () -> Unit,
    onDismissGuestPrompt: () -> Unit,
    actions: SensiOfferActions,
    onRequireAuth: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0 || WindowInsets.isImeVisible
    val bottomPadding by animateDpAsState(
        targetValue = if (isKeyboardOpen) 0.dp else 76.dp,
        label = "InputBarBottomPadding"
    )

    LaunchedEffect(state.messages.size, state.isTyping) {
        val totalItems = state.messages.size + if (state.isTyping) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .imePadding()
    ) {
        SensiHeaderBar(
            isDegraded = state.isDegraded,
            onClear = onClearConversation
        )

        AnimatedVisibility(visible = state.isDegraded) {
            DegradedModeBanner()
        }

        AnimatedVisibility(visible = state.errorMessage != null) {
            state.errorMessage?.let { error ->
                ErrorMessageBanner(error = error, onDismiss = onDismissError)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.messages.isEmpty() && !state.isTyping) {
                item {
                    EmptyChatWelcomeCard()
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    actions = actions
                )
            }

            if (state.isTyping) {
                item {
                    TypingIndicatorBubble()
                }
            }
        }

        QuickSuggestionsBar(
            onSuggestionClick = { query -> onSendMessage(query) }
        )

        ChatInputBar(
            inputText = inputText,
            bottomPadding = bottomPadding,
            isSending = state.isSending,
            retryAfterSeconds = state.retryAfterSeconds,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val text = inputText
                    inputText = ""
                    onSendMessage(text)
                }
            }
        )
    }

    if (state.showGuestPrompt) {
        GuestPromptDialog(
            onDismiss = onDismissGuestPrompt,
            onNavigateToLogin = onRequireAuth,
            onNavigateToRegister = onRequireAuth
        )
    }
}

@Composable
private fun SensiHeaderBar(
    isDegraded: Boolean,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SensusPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Sensi IA",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sensi IA",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDegraded) SensusPurpleLight else SensusSuccess)
                    )
                }
                Text(
                    text = if (isDegraded) "Modo básico activo" else "Asistente de Ahorro y Combos Inteligentes",
                    fontSize = 12.sp,
                    color = SensusTextSecondary
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reiniciar conversación",
                    tint = SensusTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DegradedModeBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SensusVioletBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SensusPurple,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Modo básico activo: respuestas rápidas sin razonamiento extendido.",
                fontSize = 12.sp,
                color = SensusPurpleDark
            )
        }
    }
}

@Composable
private fun ErrorMessageBanner(error: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SensusRedBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = SensusRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                fontSize = 12.sp,
                color = SensusRed,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = SensusRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChatWelcomeCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SensusPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "¡Hola! Soy Sensi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dime tu presupuesto o lo que necesitas hoy y te armo el mejor paquete de ofertas locales.",
                fontSize = 13.sp,
                color = SensusTextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun QuickSuggestionsBar(onSuggestionClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickSuggestionChip("¿Qué hay por menos de $50?") {
            onSuggestionClick("¿Qué hay por menos de $50?")
        }
        QuickSuggestionChip("Despensa básica semanal") {
            onSuggestionClick("Necesito despensa básica por menos de $150")
        }
        QuickSuggestionChip("Panadería fresca de hoy") {
            onSuggestionClick("Quiero pan dulce de hoy en oferta")
        }
        QuickSuggestionChip("Comida para 2 por $120") {
            onSuggestionClick("Tengo $120 para comida de 2 personas")
        }
    }
}

@Composable
private fun QuickSuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = SensusPurple,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    bottomPadding: Dp,
    isSending: Boolean,
    retryAfterSeconds: Int?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val isBlockedByRateLimit = retryAfterSeconds != null && retryAfterSeconds > 0
    val canSend = inputText.isNotBlank() && !isSending && !isBlockedByRateLimit

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = if (isBlockedByRateLimit) "Límite alcanzado..." else "Escribe tu presupuesto o necesidad...",
                        fontSize = 13.sp,
                        color = SensusTextMuted
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = sensusOutlinedTextFieldColors(
                    containerColor = SensusBackground,
                    focusedBorderColor = SensusPurple,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true,
                enabled = !isBlockedByRateLimit
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) SensusPurple else Color(0xFFD1D5DB))
            ) {
                if (isBlockedByRateLimit) {
                    Text(
                        text = "${retryAfterSeconds}s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: AiChatMessage,
    actions: SensiOfferActions
) {
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isFromUser) 16.dp else 4.dp,
        bottomEnd = if (message.isFromUser) 4.dp else 16.dp
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = if (message.isFromUser) SensusPurple else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(
                if (message.recommendedPackage != null) PACKAGE_BUBBLE_WIDTH_FRACTION else TEXT_BUBBLE_WIDTH_FRACTION
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (message.isFromUser) {
                    Text(
                        text = message.content,
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    )
                } else {
                    MarkdownText(
                        text = message.content,
                        color = SensusTextPrimary
                    )
                }
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

@Composable
private fun RecommendedPackageCard(
    pkg: RecommendedPackage,
    onApartarCombo: () -> Unit,
    onProductClick: (CitedOffer) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SensusBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paquete Sugerido",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusPurple
                    )
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

@Composable
private fun TypingIndicatorBubble() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = SensusPurple,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sensi está pensando...",
                fontSize = 12.sp,
                color = SensusTextSecondary
            )
        }
    }
}

@Preview(name = "Normal con mensajes", showBackground = true)
@Composable
private fun SensiChatPreviewNormal() {
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
    SensiChatContent(
        state = state,
        onSendMessage = {},
        onClearConversation = {},
        onDismissError = {},
        onDismissGuestPrompt = {},
        actions = SensiOfferActions(),
        onRequireAuth = {}
    )
}

@Preview(name = "Escribiendo", showBackground = true)
@Composable
private fun SensiChatPreviewTyping() {
    val state = SensiChatViewState(
        messages = listOf(
            AiChatMessage(id = "1", role = "User", content = "Busco pan dulce")
        ),
        isTyping = true
    )
    SensiChatContent(
        state = state,
        onSendMessage = {},
        onClearConversation = {},
        onDismissError = {},
        onDismissGuestPrompt = {},
        actions = SensiOfferActions(),
        onRequireAuth = {}
    )
}

@Preview(name = "Modo degradado", showBackground = true)
@Composable
private fun SensiChatPreviewDegraded() {
    val state = SensiChatViewState(
        messages = listOf(
            AiChatMessage(id = "1", role = "User", content = "Hola"),
            AiChatMessage(id = "2", role = "Assistant", content = "¡Hola! Estoy en modo básico temporalmente.", degraded = true)
        ),
        isDegraded = true
    )
    SensiChatContent(
        state = state,
        onSendMessage = {},
        onClearConversation = {},
        onDismissError = {},
        onDismissGuestPrompt = {},
        actions = SensiOfferActions(),
        onRequireAuth = {}
    )
}

@Preview(name = "Vacío", showBackground = true)
@Composable
private fun SensiChatPreviewEmpty() {
    val state = SensiChatViewState()
    SensiChatContent(
        state = state,
        onSendMessage = {},
        onClearConversation = {},
        onDismissError = {},
        onDismissGuestPrompt = {},
        actions = SensiOfferActions(),
        onRequireAuth = {}
    )
}
