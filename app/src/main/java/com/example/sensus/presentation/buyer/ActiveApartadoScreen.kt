package com.example.sensus.presentation.buyer

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.domain.model.ApartadoOrder
import com.example.sensus.domain.model.CartItem
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.NavigationIntentUtils
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import java.util.Locale
import kotlinx.coroutines.delay

private const val INITIAL_MINUTES = 44
private const val SECONDS_PER_MINUTE = 60
private const val INITIAL_EXTRA_SECONDS = 58
private const val INITIAL_TOTAL_SECONDS = INITIAL_MINUTES * SECONDS_PER_MINUTE + INITIAL_EXTRA_SECONDS
private const val TIMER_TICK_MS = 1000L
private const val COUNTDOWN_BG_ALPHA = 0.08f
private const val QR_STATUS_ALPHA = 0.15f
private const val QR_CONTAINER_HEX = 0xFFF8FAFC
private const val COUPON_CHIP_HEX = 0xFFF1F5F9

private const val QR_GRID_DIVISIONS = 7f
private const val FINDER_OUTER_SCALE = 2.2f
private const val FINDER_MID_OFFSET = 0.4f
private const val FINDER_MID_SCALE = 1.4f
private const val FINDER_INNER_OFFSET = 0.7f
private const val FINDER_INNER_SCALE = 0.8f
private const val DATA_DOT_SCALE = 0.7f
private const val DOT_1_X = 3.0f
private const val DOT_1_Y = 1.2f
private const val DOT_2_X = 4.0f
private const val DOT_2_Y = 2.5f
private const val DOT_3_X = 2.2f
private const val DOT_3_Y = 3.5f
private const val DOT_4_X = 3.5f
private const val DOT_4_Y = 4.0f
private const val DOT_5_X = 5.0f
private const val DOT_5_Y = 4.8f
private const val DOT_6_X = 3.2f
private const val DOT_6_Y = 5.8f

private val qrDataDotPositions = listOf(
    DOT_1_X to DOT_1_Y,
    DOT_2_X to DOT_2_Y,
    DOT_3_X to DOT_3_Y,
    DOT_4_X to DOT_4_Y,
    DOT_5_X to DOT_5_Y,
    DOT_6_X to DOT_6_Y
)

@Composable
fun ActiveApartadoScreen(
    order: ApartadoOrder,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onCancelApartado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var secondsLeft by remember { mutableIntStateOf(INITIAL_TOTAL_SECONDS) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(TIMER_TICK_MS)
            secondsLeft -= 1
        }
    }

    val formattedTimer = String.format(
        Locale.US,
        "%02d:%02d",
        secondsLeft / SECONDS_PER_MINUTE,
        secondsLeft % SECONDS_PER_MINUTE
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ApartadoTopBar(storeName = order.storeName, onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))
        ApartadoCountdownCard(formattedTimer = formattedTimer)
        Spacer(modifier = Modifier.height(16.dp))
        ApartadoQrCard(
            order = order,
            onCopyCoupon = {
                clipboardManager.setText(AnnotatedString(order.couponCode))
                Toast.makeText(context, "Código copiado: ${order.couponCode}", Toast.LENGTH_SHORT).show()
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ApartadoSummaryCard(order = order)
        Spacer(modifier = Modifier.height(20.dp))
        ApartadoActionButtons(
            onOpenGoogleMaps = {
                NavigationIntentUtils.launchGoogleMapsDirections(
                    context = context,
                    latitude = order.latitude,
                    longitude = order.longitude,
                    destinationName = order.storeName
                )
            },
            onNavigateToRoute = onNavigateToRoute,
            onCancelApartado = onCancelApartado
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActiveApartadoScreen(
    offer: ProductOffer,
    onNavigateBack: () -> Unit,
    onNavigateToRoute: () -> Unit,
    onCancelApartado: () -> Unit,
    modifier: Modifier = Modifier
) {
    ActiveApartadoScreen(
        order = ApartadoOrder.fromSingleOffer(offer, 1),
        onNavigateBack = onNavigateBack,
        onNavigateToRoute = onNavigateToRoute,
        onCancelApartado = onCancelApartado,
        modifier = modifier
    )
}

@Composable
private fun ApartadoTopBar(storeName: String, onNavigateBack: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = SensusPurple)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "Apartado confirmado", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(text = "Tu ahorro te espera en $storeName", fontSize = 12.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
private fun ApartadoCountdownCard(formattedTimer: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SensusPurple.copy(alpha = COUNTDOWN_BG_ALPHA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SensusPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.AccessTime, contentDescription = "Tiempo", tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "$formattedTimer restantes para recoger",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple
                )
                Text(
                    text = "Tienes hasta las 20:00 para acudir y pagar en caja",
                    fontSize = 12.sp,
                    color = SensusTextSecondary
                )
            }
        }
    }
}

@Composable
private fun ApartadoQrCard(order: ApartadoOrder, onCopyCoupon: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrActiveStatusChip(totalUnits = order.items.sumOf { it.quantity })
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(QR_CONTAINER_HEX))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeGraphic(modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(16.dp))
            CouponCopyBadge(couponCode = order.couponCode, onClick = onCopyCoupon)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Presenta este código al encargado para validar y pagar en caja.",
                fontSize = 12.sp,
                color = SensusTextSecondary
            )
        }
    }
}

@Composable
private fun QrActiveStatusChip(totalUnits: Int) {
    Surface(color = SensusSuccess.copy(alpha = QR_STATUS_ALPHA), shape = RoundedCornerShape(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Código QR Activo ($totalUnits u.)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SensusSuccess
            )
        }
    }
}

@Composable
private fun CouponCopyBadge(couponCode: String, onClick: () -> Unit) {
    Surface(color = Color(COUPON_CHIP_HEX), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                text = couponCode,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = SensusPurple
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copiar", tint = SensusPurple, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ApartadoSummaryCard(order: ApartadoOrder) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Resumen del apartado", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            order.items.forEach { item ->
                ApartadoLineItemRow(item = item)
                Spacer(modifier = Modifier.height(6.dp))
            }
            HorizontalDivider(color = SensusBackground, modifier = Modifier.padding(vertical = 8.dp))
            SummaryKeyValueRow("Comercio", order.storeName, SensusTextPrimary, FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            SummaryKeyValueRow("Dirección", order.address, SensusTextSecondary, FontWeight.Normal)
            HorizontalDivider(color = SensusBackground, modifier = Modifier.padding(vertical = 8.dp))
            SummaryKeyValueRow(
                label = "Ahorro total estimado:",
                value = "$${String.format(Locale.US, "%.2f", order.totalSavedAmount)} MXN",
                valueColor = SensusSuccess,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Total a pagar en caja:", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SensusTextPrimary)
                Text(
                    text = "$${String.format(Locale.US, "%.2f", order.totalDiscountPrice)} MXN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SensusPurple
                )
            }
        }
    }
}

@Composable
private fun ApartadoLineItemRow(item: CartItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "${item.quantity}x ${item.offer.name}", fontSize = 13.sp, color = SensusTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Precio unitario: $${String.format(Locale.US, "%.2f", item.offer.discountPrice)}",
                fontSize = 11.sp,
                color = SensusTextMuted
            )
        }
        Text(
            text = "$${String.format(Locale.US, "%.2f", item.totalDiscountPrice)} MXN",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = SensusPurple
        )
    }
}

@Composable
private fun SummaryKeyValueRow(label: String, value: String, valueColor: Color, fontWeight: FontWeight) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 12.sp, color = SensusTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = fontWeight, color = valueColor)
    }
}

@Composable
private fun ApartadoActionButtons(onOpenGoogleMaps: () -> Unit, onNavigateToRoute: () -> Unit, onCancelApartado: () -> Unit) {
    Button(
        onClick = onOpenGoogleMaps,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Icon(imageVector = Icons.Default.Navigation, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Cómo llegar con Google Maps", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedButton(
        onClick = onNavigateToRoute,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Ver ruta en mapa Sensus", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
    }
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedButton(
        onClick = onCancelApartado,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(text = "Cancelar apartado", fontSize = 13.sp, color = SensusTextSecondary)
    }
}

@Composable
fun QrCodeGraphic(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = size.minDimension
        val step = side / QR_GRID_DIVISIONS
        val farOffset = side - step * FINDER_OUTER_SCALE
        drawFinderSquare(Offset(0f, 0f), step)
        drawFinderSquare(Offset(farOffset, 0f), step)
        drawFinderSquare(Offset(0f, farOffset), step)

        val dotSize = step * DATA_DOT_SCALE
        qrDataDotPositions.forEach { (col, row) ->
            drawRect(Color.Black, Offset(step * col, step * row), Size(dotSize, dotSize))
        }
    }
}

private fun DrawScope.drawFinderSquare(origin: Offset, step: Float) {
    drawRect(Color.Black, origin, Size(step * FINDER_OUTER_SCALE, step * FINDER_OUTER_SCALE))
    drawRect(
        color = Color.White,
        topLeft = Offset(origin.x + step * FINDER_MID_OFFSET, origin.y + step * FINDER_MID_OFFSET),
        size = Size(step * FINDER_MID_SCALE, step * FINDER_MID_SCALE)
    )
    drawRect(
        color = Color.Black,
        topLeft = Offset(origin.x + step * FINDER_INNER_OFFSET, origin.y + step * FINDER_INNER_OFFSET),
        size = Size(step * FINDER_INNER_SCALE, step * FINDER_INNER_SCALE)
    )
}
