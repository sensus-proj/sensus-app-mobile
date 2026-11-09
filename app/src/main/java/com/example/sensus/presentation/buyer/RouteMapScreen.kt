package com.example.sensus.presentation.buyer

import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.sensus.presentation.common.NavigationIntentUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.presentation.common.OsmMapUtils
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun RouteMapScreen(
    viewModel: RouteMapViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    offer: ProductOffer,
    onNavigateBack: () -> Unit,
    onShowQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // container removed, using viewModel

    var userLocation by remember { mutableStateOf(UserLocation()) }
    var selectedTravelMode by remember { mutableStateOf("walk") } // "walk", "bike", "car"
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        val loc = viewModel.viewState.value.userLocation
        userLocation = loc
    }

    OsmMapUtils.BindMapLifecycle(mapViewRef, lifecycleOwner)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        // SE-011: Renderizado nativo del mapa real interactivo con OSMDroid
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmMapUtils.createDefaultMapView(ctx).apply {
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val userGeo = GeoPoint(userLocation.latitude, userLocation.longitude)
                val storeGeo = GeoPoint(offer.latitude, offer.longitude)

                // 1. Trazado de ruta (Polyline) con estilo morado Sensus
                val routeLine = Polyline(mapView).apply {
                    outlinePaint.color = android.graphics.Color.parseColor("#6E4DDB")
                    outlinePaint.strokeWidth = 10f
                    addPoint(userGeo)
                    // Punto intermedio para simular trazo de calles real
                    val midLat = (userGeo.latitude + storeGeo.latitude) / 2
                    addPoint(GeoPoint(midLat, userGeo.longitude))
                    addPoint(GeoPoint(midLat, storeGeo.longitude))
                    addPoint(storeGeo)
                }
                mapView.overlays.add(routeLine)

                // 2. Marcador de destino: Tienda
                val storeMarker = Marker(mapView).apply {
                    position = storeGeo
                    val storePin = OsmMapUtils.createStorePinBitmap(
                        context = context,
                        storeName = offer.storeName,
                        price = "$${offer.discountPrice.toInt()}",
                        isSelected = true
                    )
                    icon = BitmapDrawable(context.resources, storePin)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = offer.storeName
                    snippet = offer.address
                }
                mapView.overlays.add(storeMarker)

                // 3. Marcador de origen: Gatito Sensus del usuario
                val catMarker = Marker(mapView).apply {
                    position = userGeo
                    val catBitmap = OsmMapUtils.createCatMarkerBitmap(context)
                    icon = BitmapDrawable(context.resources, catBitmap)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Tu ubicación actual"
                    snippet = "Gatito Sensus"
                }
                mapView.overlays.add(catMarker)

                // Encuadrar la vista entre el usuario y la tienda
                val north = maxOf(userGeo.latitude, storeGeo.latitude) + 0.002
                val south = minOf(userGeo.latitude, storeGeo.latitude) - 0.002
                val east = maxOf(userGeo.longitude, storeGeo.longitude) + 0.002
                val west = minOf(userGeo.longitude, storeGeo.longitude) - 0.002
                val box = BoundingBox(north, east, south, west)
                mapView.post {
                    mapView.zoomToBoundingBox(box, true, 180)
                }

                mapView.invalidate()
            }
        )

        // Encabezado superior con botón volver y modos de viaje
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = SensusPurple
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = SensusPurple,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Hacia ${offer.storeName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensusTextPrimary
                            )
                            Text(
                                text = offer.address,
                                fontSize = 11.sp,
                                color = SensusTextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector de transporte (A pie, bici, auto)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TravelModeChip(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    label = "A pie 5 min",
                    isSelected = selectedTravelMode == "walk",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTravelMode = "walk" }
                )
                TravelModeChip(
                    icon = Icons.AutoMirrored.Filled.DirectionsBike,
                    label = "Bici 2 min",
                    isSelected = selectedTravelMode == "bike",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTravelMode = "bike" }
                )
                TravelModeChip(
                    icon = Icons.Default.DirectionsCar,
                    label = "Auto 1 min",
                    isSelected = selectedTravelMode == "car",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTravelMode = "car" }
                )
            }
        }

        // Tarjeta de paso actual + Botón flotante "Ya llegué"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SensusPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TurnRight,
                            contentDescription = "Giro",
                            tint = SensusPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "En 120 metros",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusPurple
                        )
                        Text(
                            text = "Gira a la derecha en Av. Juárez",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusTextPrimary
                        )
                        Text(
                            text = "El comercio estará a tu izquierda (frente al parque).",
                            fontSize = 11.sp,
                            color = SensusTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SE-017: Botón para abrir Google Maps externo
            OutlinedButton(
                onClick = {
                    NavigationIntentUtils.launchGoogleMapsDirections(
                        context = context,
                        latitude = offer.latitude,
                        longitude = offer.longitude,
                        destinationName = offer.storeName
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = SensusPurple,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cómo llegar con Google Maps",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusPurple
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botón "Ya llegué - Mostrar QR"
            Button(
                onClick = onShowQrClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "QR",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Ya llegué • Mostrar mi código QR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TravelModeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SensusPurple else Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else SensusTextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else SensusTextPrimary
            )
        }
    }
}
