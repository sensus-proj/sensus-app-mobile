package com.example.sensus.presentation.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.theme.BrandOrange
import com.example.sensus.theme.BrandOrangeDark
import com.example.sensus.theme.BrandOrangeSoft
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.sensusOutlinedButtonColors

private const val NOTICE_BORDER_ALPHA = 0.35f

internal val MerchantCardShape = RoundedCornerShape(16.dp)
internal val MerchantScreenPadding = 16.dp
internal val MerchantMenuClearance = 96.dp

/** Encabezado de la maqueta: flecha en círculo lila, título y subtítulo opcional. */
@Composable
internal fun MerchantScreenHeader(
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onNavigateBack != null) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SensusVioletBg)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.merchant_ui_back), tint = SensusVioletInk)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary,
                modifier = Modifier.semantics { heading() }
            )
            subtitle?.let { Text(it, fontSize = 12.sp, color = SensusTextSecondary) }
        }
        trailing()
    }
}

@Composable
internal fun MerchantCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = SensusSurface,
        shape = MerchantCardShape,
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
internal fun MerchantFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary, modifier = modifier.padding(bottom = 6.dp))
}

@Composable
internal fun MerchantSectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = SensusTextPrimary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        trailing?.let { Text(it, fontSize = 11.sp, color = SensusTextSecondary) }
    }
}

/** Color de fondo e icono opcional de un botón principal del vendedor. */
@ComposeUiModel
internal data class MerchantButtonStyle(val container: Color = SensusPurple, val icon: ImageVector? = null)

@Composable
internal fun MerchantPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    style: MerchantButtonStyle = MerchantButtonStyle()
) {
    val container = style.container
    val icon = style.icon
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MerchantOrangeButton(text: String, onClick: () -> Unit, enabled: Boolean = true, loading: Boolean = false) {
    MerchantPrimaryButton(text, onClick, enabled, loading, MerchantButtonStyle(container = BrandOrange))
}

@Composable
internal fun MerchantSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = SensusButtonShape,
        colors = sensusOutlinedButtonColors(),
        border = BorderStroke(1.dp, SensusPurple),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Aviso naranja de la maqueta (contador, consejos). */
@Composable
internal fun MerchantNoticeCard(text: String, icon: ImageVector, modifier: Modifier = Modifier, title: String? = null) {
    Surface(
        color = BrandOrangeSoft,
        shape = MerchantCardShape,
        border = BorderStroke(1.dp, BrandOrange.copy(alpha = NOTICE_BORDER_ALPHA)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = BrandOrangeDark, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                title?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandOrangeDark) }
                Text(text, fontSize = 12.sp, color = BrandOrangeDark)
            }
        }
    }
}

/** Chip de estado (Activo, Pausado, etc.). */
@Composable
internal fun MerchantStatusChip(text: String, textColor: Color, background: Color) {
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Icono dentro de un cuadro lila redondeado, usado en listas y tarjetas. */
@Composable
internal fun MerchantIconBadge(icon: ImageVector, modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SensusVioletBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
internal fun MerchantErrorText(text: String) {
    Text(text, color = SensusRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
}

internal val MerchantContentSpacing = Arrangement.spacedBy(14.dp)
