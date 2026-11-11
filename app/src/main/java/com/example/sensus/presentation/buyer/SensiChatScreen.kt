package com.example.sensus.presentation.buyer

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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sensus.domain.model.ChatMessage
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.util.Locale

private const val COMBO_BUBBLE_WIDTH_FRACTION = 0.95f
private const val TEXT_BUBBLE_WIDTH_FRACTION = 0.82f
private const val DIVIDER_HEX = 0xFFE2E8F0
private const val SAMPLE_COMBO_PRICE = 98.0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SensiChatScreen(viewModel: SensiChatViewModel = hiltViewModel(), modifier: Modifier = Modifier, onApartarComboClick: () -> Unit) {
    val chatState by viewModel.viewState.collectAsState()
    val messages = remember(chatState.messages) { mutableStateListOf<ChatMessage>().apply { addAll(chatState.messages) } }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val isKeyboardOpen = imeBottom > 0 || WindowInsets.isImeVisible
    val bottomPadding by animateDpAsState(targetValue = if (isKeyboardOpen) 0.dp else 76.dp, label = "InputBarBottomPadding")

    LaunchedEffect(messages.size, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .imePadding()
    ) {
        SensiHeaderBar()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(message = msg, onApartarCombo = onApartarComboClick)
            }
        }
        QuickSuggestionsBar(onSuggestionClick = { query -> sendUserMessage(query, messages) })
        ChatInputBar(
            inputText = inputText,
            bottomPadding = bottomPadding,
            onInputChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    val text = inputText
                    inputText = ""
                    sendUserMessage(text, messages)
                }
            }
        )
    }
}

@Composable
private fun SensiHeaderBar() {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 2.dp) {
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
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Sensi IA", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Sensi IA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SensusSuccess))
                }
                Text(text = "Asistente de Ahorro y Combos Inteligentes", fontSize = 12.sp, color = SensusTextSecondary)
            }
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
    }
}

@Composable
private fun ChatInputBar(inputText: String, bottomPadding: Dp, onInputChange: (String) -> Unit, onSend: () -> Unit) {
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
                placeholder = { Text(text = "Escribe tu presupuesto o necesidad...", fontSize = 13.sp, color = SensusTextMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = sensusOutlinedTextFieldColors(
                    containerColor = SensusBackground,
                    focusedBorderColor = SensusPurple,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SensusPurple)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun sendUserMessage(text: String, messages: MutableList<ChatMessage>) {
    messages.add(ChatMessage(id = "usr_${System.currentTimeMillis()}", isFromUser = true, text = text))
    messages.add(
        ChatMessage(
            id = "bot_${System.currentTimeMillis()}",
            isFromUser = false,
            text = "¡Excelente! Analicé las ofertas disponibles y armé una opción perfecta para ti:",
            comboTitle = "Combo Express Inteligente",
            comboPrice = SAMPLE_COMBO_PRICE,
            comboItems = listOf(
                "Arroz 1 kg (Abarrotes Don Luis) $24",
                "Conchas 4 pzas (Panadería La Espiga) $30",
                "Croquetas 500 g (Mascotas Tula) $40"
            )
        )
    )
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
private fun ChatBubble(message: ChatMessage, onApartarCombo: () -> Unit) {
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
                if (message.comboTitle != null) COMBO_BUBBLE_WIDTH_FRACTION else TEXT_BUBBLE_WIDTH_FRACTION
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (message.isFromUser) Color.White else SensusTextPrimary,
                    lineHeight = 20.sp
                )
                message.comboTitle?.let { title ->
                    Spacer(modifier = Modifier.height(12.dp))
                    ComboRecommendationCard(
                        title = title,
                        price = message.comboPrice ?: 0.0,
                        items = message.comboItems,
                        onApartarCombo = onApartarCombo
                    )
                }
            }
        }
    }
}

@Composable
private fun ComboRecommendationCard(title: String, price: Double, items: List<String>, onApartarCombo: () -> Unit) {
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
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
                Text(
                    text = "$${String.format(Locale.US, "%.2f", price)} MXN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(DIVIDER_HEX))
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { item ->
                ComboChecklistRow(item = item)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApartarCombo,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Apartar paquete completo ($${String.format(Locale.US, "%.0f", price)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ComboChecklistRow(item: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = item, fontSize = 12.sp, color = SensusTextSecondary)
    }
}
