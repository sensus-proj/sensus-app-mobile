package com.example.sensus.presentation.buyer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.sensus.R
import com.example.sensus.common.AppError
import com.example.sensus.domain.model.OfferClaim
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.presentation.common.ConfirmedClaimQr
import com.example.sensus.presentation.common.SensitiveQrGate
import com.example.sensus.presentation.common.claimsErrorText
import com.example.sensus.presentation.common.currentDeviceAuthenticator
import com.example.sensus.theme.BrandOrange
import com.example.sensus.theme.BrandOrangeDark
import com.example.sensus.theme.BrandOrangeSoft
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import java.time.Instant
import java.util.Locale

private const val MS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private val CardShape = RoundedCornerShape(16.dp)

@Composable
internal fun ClaimTopBar(onNavigateBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SensusSurface)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.claims_back), tint = SensusVioletInk, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.claims_screen_title), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
    }
}

@Composable
internal fun ClaimConfirmedHeader(storeName: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SensusSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.claims_confirmed_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(stringResource(R.string.claims_confirmed_subtitle, storeName), fontSize = 12.sp, color = SensusTextSecondary)
        }
    }
}

@Composable
internal fun ClaimProductCard(offer: ProductOffer) {
    ClaimCard {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ClaimProductThumbnail(offer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(offer.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
                Text(stringResource(R.string.claims_product_store_unit, offer.storeName), fontSize = 12.sp, color = SensusTextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatClaimPrice(offer.discountPrice), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BrandOrangeDark)
                Text(stringResource(R.string.claims_currency), fontSize = 10.sp, color = SensusTextSecondary)
            }
        }
    }
}

@Composable
private fun ClaimProductThumbnail(offer: ProductOffer) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SensusVioletBg),
        contentAlignment = Alignment.Center
    ) {
        if (!offer.imageUrl.isNullOrBlank()) {
            AsyncImage(model = offer.imageUrl, contentDescription = offer.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = SensusVioletInk, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
internal fun ClaimLoadingIndicator() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SensusPurple)
    }
}

@Composable
internal fun ClaimErrorCard(error: AppError) {
    ClaimStatusCard(text = claimsErrorText(error), textColor = SensusRed, background = SensusRedBg)
}

@Composable
internal fun ClaimExpiredCard() {
    ClaimStatusCard(text = stringResource(R.string.claims_expired), textColor = SensusRed, background = SensusRedBg)
}

@Composable
internal fun ClaimExistingCodeUnavailableCard() {
    ClaimStatusCard(text = stringResource(R.string.claims_existing_code_unavailable), textColor = SensusVioletInk, background = SensusSurface)
}

@Composable
private fun ClaimStatusCard(text: String, textColor: Color, background: Color) {
    Surface(color = background, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
        Text(text, fontSize = 13.sp, color = textColor, modifier = Modifier.padding(14.dp))
    }
}

@Composable
internal fun ClaimConfirmButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple, contentColor = Color.White),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Resumen de varias unidades apartadas en un solo QR. */
@Composable
internal fun ClaimItemsCard(title: String, lines: List<String>) {
    ClaimCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
            lines.forEach { Text(it, fontSize = 13.sp, color = SensusTextSecondary) }
        }
    }
}

@Composable
internal fun ClaimQrCard(claim: OfferClaim) {
    ClaimCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.claims_qr_instructions), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SensusTextPrimary)
            SensitiveQrGate(unlockKey = claim.claimId, deviceAuthenticator = currentDeviceAuthenticator()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        ConfirmedClaimQr(claim.qrPayload)
                    }
                    Text(claim.code, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SensusVioletInk, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
internal fun ClaimCountdownCard(remainingMs: Long) {
    Surface(color = BrandOrangeSoft, shape = CardShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = BrandOrangeDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.claims_countdown, formatRemaining(remainingMs)),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandOrangeDark
            )
        }
    }
}

@Composable
internal fun ClaimExpiryCaption(expiresAtEpochMs: Long) {
    Text(
        stringResource(R.string.claims_expiry, Instant.ofEpochMilli(expiresAtEpochMs).toString()),
        fontSize = 11.sp,
        color = SensusTextSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
internal fun ClaimPickupLocation(offer: ProductOffer) {
    Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                stringResource(R.string.claims_store_distance, offer.storeName, offer.distanceMeters),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = SensusTextPrimary
            )
            Text(offer.address, fontSize = 12.sp, color = SensusTextSecondary)
            Text(stringResource(R.string.claims_payment_info), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SensusSuccess)
        }
    }
}

@Composable
internal fun ClaimRouteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = SensusButtonShape,
        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange, contentColor = Color.White),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.claims_route), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ClaimCard(content: @Composable () -> Unit) {
    Surface(
        color = SensusSurface,
        shape = CardShape,
        border = BorderStroke(1.dp, SensusCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

private fun formatClaimPrice(amount: Double): String {
    val wholeAmount = amount % 1.0 == 0.0
    return if (wholeAmount) String.format(Locale.US, "$%.0f", amount) else String.format(Locale.US, "$%.2f", amount)
}

private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs.coerceAtLeast(0L) + MS_PER_SECOND - 1) / MS_PER_SECOND
    return String.format(Locale.US, "%02d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}
