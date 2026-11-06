package com.example.sensus.ui.buyer

import android.Manifest
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.data.MockDataProvider
import com.example.sensus.data.ProductOffer
import com.example.sensus.data.SensusAuthManager
import com.example.sensus.data.StoreCategory
import com.example.sensus.data.di.SensusContainer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import com.example.sensus.ui.common.OsmMapUtils
import com.example.sensus.ui.common.RadiusSelectorBottomSheet
import com.example.sensus.ui.common.formatRadiusText
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

@Composable
fun BuyerMapScreen(
    modifier: Modifier = Modifier,
    onProductClick: (ProductOffer) -> Unit,
    onRouteClick: (ProductOffer) -> Unit,
    onListToggle: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val container = remember { SensusContainer.getInstance(context) }

    var userLocation by remember { mutableStateOf(UserLocation()) }
    var hasGpsPermission by remember { mutableStateOf(container.locationRepository.hasLocationPermission()) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var selectedRadiusKm by remember { mutableFloatStateOf(2.0f) }
    var showRadiusSheet by remember { mutableStateOf(false) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // SE-010: Solicitud de permisos de GPS en runtime al entrar a la pantalla
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        hasGpsPermission = granted
        if (granted) {
            SensusAuthManager.setLocationPermissionGranted(true)
            scope.launch {
                val loc = container.getUserLocationUseCase()
                userLocation = loc
                mapViewRef?.let { map ->
                    map.controller.setZoom(16.0)
                    map.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasGpsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            scope.launch {
                val loc = container.getUserLocationUseCase()
                userLocation = loc
                mapViewRef?.let { map ->
                    map.controller.setZoom(16.0)
                    map.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                }
            }
        }
    }

    // Filtrar ofertas según búsqueda y categoría
    val rawFilteredOffers = remember(searchQuery, selectedCategory) {
        MockDataProvider.sampleOffers.filter { offer ->
            val matchesCategory = selectedCategory == StoreCategory.ALL ||
                    offer.category.contains(selectedCategory.displayName, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    offer.name.contains(searchQuery, ignoreCase = true) ||
                    offer.storeName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Posicionamiento dinámico de las ofertas alrededor de la ubicación real del usuario
    val localizedOffers = remember(userLocation, rawFilteredOffers) {
        rawFilteredOffers.mapIndexed { index, offer ->
            val latOffset = when (index % 6) {
                0 -> 0.0022
                1 -> -0.0018
                2 -> 0.0015
                3 -> -0.0025
                4 -> 0.0031
                else -> -0.0012
            }
            val lngOffset = when (index % 6) {
                0 -> 0.0019
                1 -> -0.0024
                2 -> -0.0029
                3 -> 0.0016
                4 -> -0.0015
                else -> 0.0026
            }
            offer.copy(
                latitude = userLocation.latitude + latOffset,
                longitude = userLocation.longitude + lngOffset
            )
        }
    }

    var selectedOffer by remember(localizedOffers) {
        mutableStateOf(localizedOffers.firstOrNull() ?: MockDataProvider.sampleOffers.first())
    }

    // Centrar mapa automáticamente cuando la ubicación GPS se resuelva
    LaunchedEffect(userLocation) {
        mapViewRef?.let { map ->
            map.controller.setZoom(16.0)
            map.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
        }
    }

    // Ciclo de vida para MapView de OSMDroid
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        // SE-011: Renderizado nativo del mapa real interactivo con OpenStreetMap
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmMapUtils.initOsmDroid(ctx)
                MapView(ctx).apply {
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    setTileSource(OsmMapUtils.MAP_TILE_SOURCE)
                    setMultiTouchControls(true)
                    isTilesScaledToDpi = false // Previene el efecto borroso/pixelado
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(userLocation.latitude, userLocation.longitude))
                    mapViewRef = this
                }
            },
            update = { mapView ->
                mapView.overlays.clear()

                val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)

                // 1. Círculo de Radio de búsqueda (0.5 a 5 km) centrado en el usuario
                val radiusCircle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(userGeoPoint, (selectedRadiusKm * 1000).toDouble())
                    fillPaint.color = android.graphics.Color.argb(20, 110, 77, 219)
                    outlinePaint.color = android.graphics.Color.argb(90, 110, 77, 219)
                    outlinePaint.strokeWidth = 2.5f * context.resources.displayMetrics.density
                }
                mapView.overlays.add(radiusCircle)

                // 2. Pines interactivos de los Comercios (en calles cercanas al usuario)
                localizedOffers.forEach { offer ->
                    val storeMarker = Marker(mapView).apply {
                        position = GeoPoint(offer.latitude, offer.longitude)
                        val isSelected = offer.id == selectedOffer.id
                        val pinBitmap = OsmMapUtils.createStorePinBitmap(
                            context = context,
                            storeName = offer.storeName,
                            price = "$${offer.discountPrice.toInt()}",
                            isSelected = isSelected
                        )
                        icon = BitmapDrawable(context.resources, pinBitmap)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = offer.storeName
                        snippet = offer.name

                        setOnMarkerClickListener { _, _ ->
                            selectedOffer = offer
                            mapView.controller.animateTo(position)
                            true
                        }
                    }
                    mapView.overlays.add(storeMarker)
                }

                // 3. Marcador distintivo del Gatito Sensus en la posición GPS del usuario (sin el punto azul clásico)
                val catMarker = Marker(mapView).apply {
                    position = userGeoPoint
                    val catBitmap = OsmMapUtils.createCatMarkerBitmap(context)
                    icon = BitmapDrawable(context.resources, catBitmap)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    title = "Tú estás aquí"
                    snippet = "Gatito Sensus (GPS)"
                }
                mapView.overlays.add(catMarker)

                mapView.invalidate()
            }
        )

        // Barra superior de búsqueda, selector funcional de radio y categorías
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Buscar en el mapa...",
                        fontSize = 14.sp,
                        color = SensusTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = SensusTextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onListToggle) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Ver lista",
                            tint = SensusPurple
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = sensusOutlinedTextFieldColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Fila de chip funcional de radio y categorías rápidas
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip interactivo para cambiar radio (0.5 a 5 km)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SensusPurple,
                    shadowElevation = 2.dp,
                    modifier = Modifier.clickable { showRadiusSheet = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Radio: ${formatRadiusText(selectedRadiusKm)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                StoreCategory.values().take(5).forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) SensusPurple else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else SensusTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Botones de control de Zoom (+ / -) flotantes en el lado derecho
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { mapViewRef?.controller?.zoomIn() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Acercar mapa",
                        tint = SensusPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { mapViewRef?.controller?.zoomOut() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Alejar mapa",
                        tint = SensusPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Botón flotante para recentrar GPS en el Gatito
        IconButton(
            onClick = {
                if (!hasGpsPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    scope.launch {
                        val loc = container.getUserLocationUseCase()
                        userLocation = loc
                        mapViewRef?.controller?.setZoom(16.5)
                        mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 260.dp, end = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Centrar en mi ubicación",
                tint = SensusPurple
            )
        }

        // Tarjeta flotante inferior de la tienda/oferta seleccionada
        Card(
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE2E8F0))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Tienda",
                                tint = SensusPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedOffer.storeName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SensusTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = selectedOffer.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusTextPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${selectedOffer.formattedDistance} • Recoge en ${selectedOffer.timeToCollectMinutes} min",
                            fontSize = 12.sp,
                            color = SensusTextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            color = SensusPurple,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "$${String.format("%.2f", selectedOffer.discountPrice)} MXN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "-${selectedOffer.discountPercent}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusSuccess
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRouteClick(selectedOffer) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Cómo llegar",
                            tint = SensusPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cómo llegar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SensusPurple
                        )
                    }

                    Button(
                        onClick = { onProductClick(selectedOffer) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Ver detalles",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Detalles",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Hoja inferior interactiva de selección de radio (0.5 a 5 km)
        if (showRadiusSheet) {
            RadiusSelectorBottomSheet(
                currentRadiusKm = selectedRadiusKm,
                onRadiusSelected = { selectedRadiusKm = it },
                onDismiss = { showRadiusSheet = false }
            )
        }
    }
}
