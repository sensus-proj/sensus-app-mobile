package com.example.sensus.presentation.buyer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.sensus.R
import com.example.sensus.domain.model.MerchantOpenStatus
import com.example.sensus.domain.model.MerchantProfile
import com.example.sensus.domain.model.openStatusAt
import com.example.sensus.domain.model.scheduleFor
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

private const val MILLIS_PER_MINUTE = 60_000L

@Composable
internal fun merchantLocalMinute(): LocalDateTime {
    val now by produceState(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)) {
        while (true) {
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
            value = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        }
    }
    return now
}

/**
 * "Abierto ahora" / "Cerrado ahora" plus today's schedule, computed from the published merchant schedule.
 * Renders nothing when the merchant has not published a schedule for today.
 */
@Composable
internal fun MerchantOpenStatusText(profile: MerchantProfile?, now: LocalDateTime = merchantLocalMinute()) {
    val status = profile.openStatusAt(now)
    if (status == MerchantOpenStatus.UNKNOWN) return
    val schedule = profile.scheduleFor(now.dayOfWeek)
    val label = stringResource(if (status == MerchantOpenStatus.OPEN) R.string.merchant_open_now else R.string.merchant_closed_now)
    val text = if (schedule != null) stringResource(R.string.merchant_status_with_schedule, label, schedule) else label
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (status == MerchantOpenStatus.OPEN) SensusSuccess else SensusTextMuted,
        maxLines = 1
    )
}
