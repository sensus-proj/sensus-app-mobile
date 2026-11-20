package com.example.sensus.presentation.support

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.presentation.auth.PRIVACY_POLICY_URL
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val SUPPORT_EMAIL_URI = "mailto:soporte@sensus.mx?subject=Soporte%20Sensus"
private const val FAQ_BOTTOM_PADDING_DP = 32
private const val BADGE_ALPHA = 0.1f

internal data class HelpFaqCallbacks(
    val onNavigateBack: () -> Unit,
    val onContactSupport: () -> Unit,
    val onOpenPrivacyWeb: () -> Unit,
    val onOpenTerms: (() -> Unit)? = null
)

@Composable
fun HelpFaqScreen(onNavigateBack: () -> Unit, onOpenTerms: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    HelpFaqContent(
        items = FaqCatalog.offlineItems,
        initialExpandedId = null,
        callbacks = HelpFaqCallbacks(
            onNavigateBack = onNavigateBack,
            onContactSupport = {
                try {
                    context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(SUPPORT_EMAIL_URI)))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Escríbenos a soporte@sensus.mx", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenPrivacyWeb = {
                try {
                    uriHandler.openUri(PRIVACY_POLICY_URL)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, PRIVACY_POLICY_URL, Toast.LENGTH_SHORT).show()
                }
            },
            onOpenTerms = onOpenTerms
        ),
        modifier = modifier
    )
}

@Composable
internal fun HelpFaqContent(items: List<FaqItem>, callbacks: HelpFaqCallbacks, modifier: Modifier = Modifier, initialExpandedId: String? = null) {
    var searchText by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf(initialExpandedId) }

    val visibleItems = remember(items, searchText) {
        val q = searchText.trim().lowercase()
        if (q.isEmpty()) items else items.filter { it.question.lowercase().contains(q) || it.answer.lowercase().contains(q) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = FAQ_BOTTOM_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HelpFaqTopBar(onNavigateBack = callbacks.onNavigateBack) }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(text = "Buscar pregunta frecuente...", fontSize = 14.sp, color = SensusTextMuted) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar FAQ", tint = SensusPurple) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(visibleItems, key = { it.id }) { item ->
            FaqAccordionCard(
                item = item,
                expanded = expandedId == item.id,
                onToggle = { expandedId = if (expandedId == item.id) null else item.id }
            )
        }
        item {
            SupportContactCard(
                onContactSupport = callbacks.onContactSupport,
                onOpenPrivacyWeb = callbacks.onOpenPrivacyWeb,
                onOpenTerms = callbacks.onOpenTerms
            )
        }
    }
}

@Composable
private fun HelpFaqTopBar(onNavigateBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White)) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = SensusPurple)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "Centro de ayuda y soporte", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = "Disponible 100 % sin conexión", fontSize = 12.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun FaqAccordionCard(item: FaqItem, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = if (expanded) "Expandido" else "Colapsado" }
            .clickable(onClick = onToggle)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = SensusPurple.copy(alpha = BADGE_ALPHA)) {
                        Text(
                            text = item.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SensusPurple,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = item.question, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer respuesta" else "Expandir respuesta",
                    tint = SensusPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = SensusCardBorder)
                    Text(text = item.answer, fontSize = 13.sp, color = SensusTextSecondary, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun SupportContactCard(onContactSupport: () -> Unit, onOpenPrivacyWeb: () -> Unit, onOpenTerms: (() -> Unit)?) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = "¿Necesitas más ayuda?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Button(
                onClick = onContactSupport,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Email, contentDescription = "Correo de soporte", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Contactar soporte", color = Color.White, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenPrivacyWeb,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Abrir privacidad",
                    tint = SensusPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Aviso de Privacidad en línea", color = SensusPurple, fontWeight = FontWeight.SemiBold)
            }
            if (onOpenTerms != null) {
                OutlinedButton(
                    onClick = onOpenTerms,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(text = "Leer términos y privacidad sin conexión", color = SensusPurple, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "FaqCollapsed")
@Composable
internal fun FaqCollapsedPreview() {
    SensusTheme {
        HelpFaqContent(
            items = FaqCatalog.offlineItems,
            callbacks = HelpFaqCallbacks(onNavigateBack = {}, onContactSupport = {}, onOpenPrivacyWeb = {}),
            initialExpandedId = null
        )
    }
}

@Preview(showBackground = true, name = "FaqExpanded")
@Composable
internal fun FaqExpandedPreview() {
    SensusTheme {
        HelpFaqContent(
            items = FaqCatalog.offlineItems,
            callbacks = HelpFaqCallbacks(onNavigateBack = {}, onContactSupport = {}, onOpenPrivacyWeb = {}),
            initialExpandedId = "feed_privacy"
        )
    }
}
