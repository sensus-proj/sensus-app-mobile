package com.example.sensus.presentation.buyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.presentation.common.ComposeUiModel
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

private const val HEX_PURPLE_TINT = 0xFFEDE9FE
private const val HEX_DISABLED_GRAY = 0xFFD1D5DB

@Composable
internal fun SensiHeaderBar(isDegraded: Boolean, onClear: () -> Unit) {
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
            SensiAvatar()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Sensi IA", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
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
private fun SensiAvatar() {
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
}

@Composable
internal fun DegradedModeBanner() {
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
internal fun ErrorMessageBanner(error: String, onDismiss: () -> Unit) {
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
internal fun EmptyChatWelcomeCard() {
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
                    .background(Color(HEX_PURPLE_TINT)),
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
internal fun QuickSuggestionsBar(onSuggestionClick: (String) -> Unit) {
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

/** Estado visible de la barra de entrada del chat. */
@ComposeUiModel
internal data class ChatInputUi(
    val text: String,
    val bottomPadding: Dp,
    val isSending: Boolean,
    val retryAfterSeconds: Int?
) {
    val isBlocked: Boolean get() = retryAfterSeconds != null && retryAfterSeconds > 0
    val canSend: Boolean get() = text.isNotBlank() && !isSending && !isBlocked
}

@Composable
internal fun ChatInputBar(input: ChatInputUi, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = input.bottomPadding),
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
                value = input.text,
                onValueChange = onInputChange,
                placeholder = {
                    Text(
                        text = if (input.isBlocked) "Límite alcanzado..." else "Escribe tu presupuesto o necesidad...",
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
                enabled = !input.isBlocked
            )
            Spacer(modifier = Modifier.width(8.dp))
            SendButton(input = input, onSend = onSend)
        }
    }
}

@Composable
private fun SendButton(input: ChatInputUi, onSend: () -> Unit) {
    IconButton(
        onClick = onSend,
        enabled = input.canSend,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (input.canSend) SensusPurple else Color(HEX_DISABLED_GRAY))
    ) {
        when {
            input.isBlocked -> Text(
                text = "${input.retryAfterSeconds}s",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            input.isSending -> CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            else -> Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun TypingIndicatorBubble() {
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
