package com.example.sensus.presentation.buyer

import android.content.Context
import android.graphics.Color as AndroidColor
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.NavigationIntentUtils
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val MAP_BG_HEX = 0xFFE5E7EB
private const val MIN_ROUTE_ZOOM = 6.0
private const val ROUTE_STROKE_WIDTH = 10f
private const val BOUNDING_BOX_MARGIN = 0.002
private const val BOUNDING_BOX_PADDING_PX = 180
private const val TURN_ICON_BG_ALPHA = 0.12f

@Composable
fun RouteMapScreen(
    viewModel: RouteMapViewModel = hiltViewModel(),
    offer: ProductOffer,
    onNavigateBack: () -> Unit,
    onShowQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val routeState by viewModel.viewState.collectAsState()
    var selectedTravelMode by remember { mutableStateOf("walk") }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    OsmMapUtils.BindMapLifecycle(mapViewRef, lifecycleOwner)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(MAP_BG_HEX))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmMapUtils.createDefaultMapView(ctx, minZoom = MIN_ROUTE_ZOOM).apply { mapViewRef = this }
            },
            update = { mapView ->
                renderRouteOverlays(mapView, context, routeState.userLocation, offer)
            }
        )
        RouteTopOverlay(
            headerProps = RouteHeaderProps(
                storeName = offer.storeName,
                address = offer.address,
                selectedTravelMode = selectedTravelMode
            ),
            onNavigateBack = onNavigateBack,
            onSelectMode = { selectedTravelMode = it },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        )
        RouteBottomOverlay(
            onOpenGoogleMaps = {
                NavigationIntentUtils.launchGoogleMapsDirections(
                    context = context,
                    latitude = offer.latitude,
                    longitude = offer.longitude,
                    destinationName = offer.storeName
                )
            },
            onShowQrClick = onShowQrClick,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        )
    }
}

@ComposeUiModel
private data class RouteHeaderProps(
    val storeName: String,
    val address: String,
    val selectedTravelMode: String
)

@Composable
private fun RouteTopOverlay(headerProps: RouteHeaderProps, onNavigateBack: () -> Unit, onSelectMode: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
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
            RouteDestinationPill(storeName = headerProps.storeName, address = headerProps.address, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TravelModeChip(
                icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                label = "A pie 5 min",
                isSelected = headerProps.selectedTravelMode == "walk",
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode("walk") }
            )
            TravelModeChip(
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "Bici 2 min",
                isSelected = headerProps.selectedTravelMode == "bike",
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode("bike") }
            )
            TravelModeChip(
                icon = Icons.Default.DirectionsCar,
                label = "Auto 1 min",
                isSelected = headerProps.selectedTravelMode == "car",
                modifier = Modifier.weight(1f),
                onClick = { onSelectMode("car") }
            )
        }
    }
}

@Composable
private fun RouteDestinationPill(storeName: String, address: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 2.dp, modifier = modifier) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Hacia $storeName", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(text = address, fontSize = 11.sp, color = SensusTextMuted)
            }
        }
    }
}

@Composable
private fun RouteBottomOverlay(onOpenGoogleMaps: () -> Unit, onShowQrClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        NextTurnInstructionCard()
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenGoogleMaps,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Icon(imageVector = Icons.Default.Navigation, contentDescription = null, tint = SensusPurple, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Cómo llegar con Google Maps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onShowQrClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = "QR", tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "Ya llegué • Mostrar mi código QR", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun NextTurnInstructionCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SensusPurple.copy(alpha = TURN_ICON_BG_ALPHA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.TurnRight, contentDescription = "Giro", tint = SensusPurple, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "En 120 metros", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SensusPurple)
                Text(text = "Gira a la derecha en Av. Juárez", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(
                    text = "El comercio estará a tu izquierda (frente al parque).",
                    fontSize = 11.sp,
                    color = SensusTextSecondary
                )
            }
        }
    }
}

@Composable
private fun TravelModeChip(icon: ImageVector, label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) SensusPurple else Color.White,
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick)
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

private fun renderRouteOverlays(mapView: MapView, context: Context, userLocation: UserLocation, offer: ProductOffer) {
    mapView.overlays.clear()
    val userGeo = GeoPoint(userLocation.latitude, userLocation.longitude)
    val storeGeo = GeoPoint(offer.latitude, offer.longitude)
    val midLat = (userGeo.latitude + storeGeo.latitude) / 2

    val routeLine = Polyline(mapView).apply {
        outlinePaint.color = AndroidColor.parseColor("#6E4DDB")
        outlinePaint.strokeWidth = ROUTE_STROKE_WIDTH
        addPoint(userGeo)
        addPoint(GeoPoint(midLat, userGeo.longitude))
        addPoint(GeoPoint(midLat, storeGeo.longitude))
        addPoint(storeGeo)
    }
    val storeMarker = Marker(mapView).apply {
        position = storeGeo
        val storePin = OsmMapUtils.createStorePinBitmap(context, offer.storeName, "$${offer.discountPrice.toInt()}", isSelected = true)
        icon = BitmapDrawable(context.resources, storePin)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = offer.storeName
        snippet = offer.address
    }
    val catMarker = Marker(mapView).apply {
        position = userGeo
        icon = BitmapDrawable(context.resources, OsmMapUtils.createCatMarkerBitmap(context))
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "Tu ubicación actual"
        snippet = "Gatito Sensus"
    }

    mapView.overlays.add(routeLine)
    mapView.overlays.add(storeMarker)
    mapView.overlays.add(catMarker)

    val box = BoundingBox(
        maxOf(userGeo.latitude, storeGeo.latitude) + BOUNDING_BOX_MARGIN,
        maxOf(userGeo.longitude, storeGeo.longitude) + BOUNDING_BOX_MARGIN,
        minOf(userGeo.latitude, storeGeo.latitude) - BOUNDING_BOX_MARGIN,
        minOf(userGeo.longitude, storeGeo.longitude) - BOUNDING_BOX_MARGIN
    )
    mapView.post { mapView.zoomToBoundingBox(box, true, BOUNDING_BOX_PADDING_PX) }
    mapView.invalidate()
}
