package com.example.sensus.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusTheme

private const val BANNER_BG_HEX = 0xFFF5F0FF
private const val BANNER_BORDER_HEX = 0xFFDDD6FE

/**
 * Banner no intrusivo para sesiones en modo invitado (FT-23, RF-23-03, C-49).
 */
@Composable
fun GuestModeBanner(onRegisterClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color(BANNER_BG_HEX),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(BANNER_BORDER_HEX)),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Modo invitado",
                    tint = SensusPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Modo invitado (radio 2 km)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SensusTextPrimary
                    )
                    Text(
                        text = "Regístrate en 30 s: radio 5 km, promociones y comunidad",
                        fontSize = 11.sp,
                        color = SensusTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRegisterClick,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = "Registrarme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, name = "GuestModeBanner")
@Composable
internal fun GuestModeBannerPreview() {
    SensusTheme {
        GuestModeBanner(onRegisterClick = {})
    }
}
