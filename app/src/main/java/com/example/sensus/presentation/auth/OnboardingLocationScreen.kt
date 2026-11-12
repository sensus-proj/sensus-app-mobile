package com.example.sensus.presentation.auth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusPurpleLight
import com.example.sensus.theme.SensusRed
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary

private const val RADAR_HALO_ALPHA = 0.12f
private const val FEATURE_BG_ALPHA = 0.15f

@Composable
fun OnboardingLocationScreen(modifier: Modifier = Modifier, onPermissionGranted: () -> Unit, onManualLocationSelected: () -> Unit) {
    val context = LocalContext.current
    var permissionDeniedPermanently by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            onPermissionGranted()
        } else {
            permissionDeniedPermanently = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SensusBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            OnboardingLocationHeader(permissionDeniedPermanently = permissionDeniedPermanently)
            Spacer(modifier = Modifier.height(32.dp))
            OnboardingLocationActions(
                permissionDeniedPermanently = permissionDeniedPermanently,
                onOpenSettings = { openAppLocationSettings(context) },
                onRequestLocation = {
                    if (hasLocationPermission(context)) {
                        onPermissionGranted()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onManualLocationSelected = onManualLocationSelected
            )
        }
    }
}

@Composable
private fun OnboardingLocationHeader(permissionDeniedPermanently: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        LocationRadarIcon(isDenied = permissionDeniedPermanently)
        Spacer(modifier = Modifier.height(32.dp))
        if (permissionDeniedPermanently) {
            DeniedPermissionContent()
        } else {
            LocationBenefitsContent()
        }
    }
}

@Composable
private fun LocationRadarIcon(isDenied: Boolean) {
    val tone = if (isDenied) SensusRed else SensusPurple
    Surface(
        shape = CircleShape,
        color = tone.copy(alpha = RADAR_HALO_ALPHA),
        modifier = Modifier.size(120.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                color = tone,
                shadowElevation = 6.dp,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isDenied) Icons.Default.LocationOff else Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeniedPermissionContent() {
    Text(
        text = "Permiso de ubicación necesario",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SensusTextPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Para mostrarte promociones y excedentes de panaderías y restaurantes cerca de ti " +
            "(radio de hasta 2 km), Sensus necesita acceso a tu ubicación GPS.",
        fontSize = 14.sp,
        color = SensusTextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
    Spacer(modifier = Modifier.height(20.dp))
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Cómo activar el permiso:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SensusTextPrimary
            )
            Text(
                text = "1. Toca en 'Abrir ajustes'.\n2. Selecciona 'Permisos'.\n3. Activa 'Ubicación' con precisión exacta.",
                fontSize = 13.sp,
                color = SensusTextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun LocationBenefitsContent() {
    Text(
        text = "Descubre ofertas cerca de ti",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = SensusTextPrimary,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Descubre las ofertas y excedentes disponibles a menos de 2 km a tu alrededor " +
            "en tiempo real y ahorra hasta un 50%.",
        fontSize = 14.sp,
        color = SensusTextSecondary,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )
    Spacer(modifier = Modifier.height(24.dp))
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FeatureRow(
                icon = Icons.Default.NearMe,
                title = "Geolocalización precisa",
                subtitle = "Filtro dinámico de 500 m a 2,000 m"
            )
            FeatureRow(
                icon = Icons.Default.Store,
                title = "Comercios de tu colonia",
                subtitle = "Apoya a los comercios locales de tu comunidad"
            )
            FeatureRow(
                icon = Icons.Default.Notifications,
                title = "Alertas en tiempo real",
                subtitle = "Recibe notificaciones cuando tu panadería favorita publique"
            )
        }
    }
}

@Composable
private fun OnboardingLocationActions(
    permissionDeniedPermanently: Boolean,
    onOpenSettings: () -> Unit,
    onRequestLocation: () -> Unit,
    onManualLocationSelected: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val primaryLabel = if (permissionDeniedPermanently) "Abrir ajustes del sistema" else "Permitir ubicación"
        val primaryIcon = if (permissionDeniedPermanently) Icons.Default.Settings else Icons.Default.LocationOn
        val secondaryLabel = if (permissionDeniedPermanently) {
            "Continuar sin ubicación"
        } else {
            "Ahora no / Ingresar colonia"
        }
        Button(
            onClick = if (permissionDeniedPermanently) onOpenSettings else onRequestLocation,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Icon(imageVector = primaryIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = primaryLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        OutlinedButton(
            onClick = onManualLocationSelected,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = secondaryLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SensusPurple)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SensusPurpleLight.copy(alpha = FEATURE_BG_ALPHA),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SensusTextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = SensusTextSecondary)
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

private fun openAppLocationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
