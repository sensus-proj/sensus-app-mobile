package com.example.sensus.presentation.merchant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sensus.R
import com.example.sensus.presentation.common.QrScannerDialog
import com.example.sensus.presentation.common.SecureScreenEffect
import com.example.sensus.presentation.common.claimsErrorText
import com.example.sensus.theme.BrandOrange
import com.example.sensus.theme.BrandOrangeDark
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusRedBg
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import java.time.Instant

private const val SCAN_ICON_ALPHA = 0.2f
private const val SCAN_SUBTITLE_ALPHA = 0.85f

@Composable
fun MerchantSalesScreen(modifier: Modifier = Modifier) {
    SecureScreenEffect()
    val viewModel: MerchantSalesViewModel = hiltViewModel()
    val state by viewModel.viewState.collectAsStateWithLifecycle()
    MerchantSalesContent(state, viewModel::processIntent, modifier)
}

@Composable
fun MerchantSalesContent(state: MerchantSalesViewState, onIntent: (MerchantSalesViewIntent) -> Unit, modifier: Modifier = Modifier) {
    var showScanner by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .background(SensusVioletBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(MerchantScreenPadding)
            .padding(bottom = MerchantMenuClearance),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MerchantScreenHeader(title = stringResource(R.string.merchant_sales_title))
        SalesScanCard(enabled = !state.loading, onScanClick = { showScanner = true })
        SalesManualCodeCard(state, onIntent)
        MerchantSalesStatus(state)
    }
    if (showScanner && !state.loading) {
        QrScannerDialog(
            onQrDecoded = { code ->
                if (showScanner) {
                    showScanner = false
                    onIntent(MerchantSalesViewIntent.Scan(code))
                }
            },
            onDismiss = { showScanner = false },
            instructions = stringResource(R.string.claims_scan_instructions),
            securePolicy = SecureFlagPolicy.SecureOn
        )
    }
}

@Composable
private fun SalesScanCard(enabled: Boolean, onScanClick: () -> Unit) {
    Surface(color = BrandOrange, shape = MerchantCardShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = SCAN_ICON_ALPHA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.merchant_sales_scan_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(stringResource(R.string.merchant_sales_scan_subtitle), fontSize = 12.sp, color = Color.White.copy(alpha = SCAN_SUBTITLE_ALPHA))
                }
            }
            Button(
                onClick = onScanClick,
                enabled = enabled,
                shape = SensusButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandOrangeDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.claims_scan_qr), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SalesManualCodeCard(state: MerchantSalesViewState, onIntent: (MerchantSalesViewIntent) -> Unit) {
    MerchantCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.merchant_sales_manual_title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
            OutlinedTextField(
                value = state.input,
                onValueChange = { onIntent(MerchantSalesViewIntent.Edit(it)) },
                label = { Text(stringResource(R.string.claims_code)) },
                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                singleLine = true,
                enabled = !state.loading,
                shape = RoundedCornerShape(12.dp),
                colors = sensusOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            MerchantPrimaryButton(
                text = stringResource(R.string.claims_redeem),
                onClick = { onIntent(MerchantSalesViewIntent.Submit(state.input)) },
                enabled = state.input.isNotBlank() && state.result == null,
                loading = state.loading
            )
        }
    }
}

@Composable
private fun MerchantSalesStatus(state: MerchantSalesViewState) {
    if (state.loading) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SensusPurple) }
    }
    if (state.scanned && !state.loading && state.error == null) {
        Text(stringResource(R.string.claims_scan_ready), fontSize = 13.sp, color = SensusTextSecondary)
    }
    state.error?.let {
        Surface(color = SensusRedBg, shape = MerchantCardShape, modifier = Modifier.fillMaxWidth()) {
            Text(claimsErrorText(it), color = SensusRed, fontSize = 13.sp, modifier = Modifier.padding(14.dp))
        }
    }
    state.result?.let { SalesRedeemedCard(Instant.ofEpochMilli(it.redeemedAtEpochMs).toString()) }
}

@Composable
private fun SalesRedeemedCard(redeemedAt: String) {
    MerchantCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SensusSuccessBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SensusSuccess, modifier = Modifier.size(30.dp))
            }
            Text(stringResource(R.string.merchant_sales_redeemed_title), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
            Text(
                stringResource(R.string.claims_redeemed, redeemedAt),
                fontSize = 12.sp,
                color = SensusSuccess,
                textAlign = TextAlign.Center
            )
        }
    }
}
