package com.example.sensus.presentation.buyer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.presentation.common.RadiusSelectorBottomSheet
import com.example.sensus.presentation.common.formatRadiusText
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusTextMuted
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.sensusOutlinedTextFieldColors
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale

@Composable
fun BuyerMapScreen(
    mapViewModel: BuyerMapViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    cartViewModel: CartViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    isGuest: Boolean = false,
    onRequireAuth: () -> Unit = {},
    modifier: Modifier = Modifier,
    onProductClick: (ProductOffer) -> Unit,
    onRouteClick: (ProductOffer) -> Unit,
    onListToggle: () -> Unit,
    onOpenCart: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapState by mapViewModel.viewState.collectAsState()
    val cartState by cartViewModel.viewState.collectAsState()
    val cart = cartState.storeCart

    val effectiveIsGuest = isGuest || mapState.isGuest
    val effectiveRadiusKm = GuestRadiusPolicy.clampRadius(mapState.selectedRadiusKm, effectiveIsGuest)
    val userLocation = mapState.userLocation

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(StoreCategory.ALL) }
    var selectedOffer by remember { mutableStateOf<ProductOffer?>(null) }
    var showRadiusSheet by remember { mutableStateOf(false) }
    var showLayersMenu by remember { mutableStateOf(false) }
    var locationNotice by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var currentZoom by remember { mutableDoubleStateOf(OsmMapUtils.DEFAULT_MAP_ZOOM) }

    val hasRuntimePermission = remember(mapState.isLocationPermissionGranted) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        fine || coarse
    }

    // Recibir efectos del ViewModel (solicitud de cuenta al superar 2 km o avisos GPS)
    LaunchedEffect(Unit) {
        mapViewModel.viewEffect.collect { effect ->
            when (effect) {
                is BuyerMapViewEffect.RequireAuthForRadius -> onRequireAuth()
                is BuyerMapViewEffect.ShowLocationMessage -> {
                    locationNotice = effect.message to effect.isGpsDisabled
                }
            }
        }
    }

    // SE-010: Solicitud de permisos de GPS en runtime al entrar a la pantalla
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        mapViewModel.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(granted))
        if (granted) {
            locationNotice = null
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            mapViewModel.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(true))
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Detectar si el usuario activó el GPS o el permiso en ajustes al regresar a la app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if ((fine || coarse) && !mapState.hasRealLocation) {
                    locationNotice = null
                    mapViewModel.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Filtrar ofertas por categoría y búsqueda
    val rawFilteredOffers = remember(searchQuery, selectedCategory, mapState.offers) {
        mapState.offers.filter { offer ->
            val matchesCategory = when (selectedCategory) {
                StoreCategory.ALL -> true
                StoreCategory.GROCERY -> offer.category.contains("Abarrotes", ignoreCase = true)
                StoreCategory.CLEANING -> offer.category.contains("Limpieza", ignoreCase = true)
                StoreCategory.PERISHABLES -> offer.category.contains("Perecederos", ignoreCase = true)
                StoreCategory.DAIRY -> offer.category.contains("Lácteos", ignoreCase = true)
                StoreCategory.FOOD -> offer.category.contains("Comida", ignoreCase = true)
                StoreCategory.BAKERY -> offer.category.contains("Panadería", ignoreCase = true)
                StoreCategory.BEVERAGES -> offer.category.contains("Bebidas", ignoreCase = true)
                StoreCategory.PETS -> offer.category.contains("Mascotas", ignoreCase = true)
                StoreCategory.PHARMACY -> offer.category.contains("Farmacia", ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    offer.name.contains(searchQuery, ignoreCase = true) ||
                    offer.storeName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    // Distribuir ofertas alrededor de la ubicación GPS real del usuario y filtrar estrictamente por radio efectivo
    // En Modo Invitado effectiveRadiusKm nunca supera 2.0 km (2000 m)
    val localizedOffers = remember(userLocation, rawFilteredOffers, effectiveRadiusKm) {
        val maxRadiusMeters = (effectiveRadiusKm * 1000).toInt()
        OsmMapUtils.localizeAndMeasureOffers(userLocation, rawFilteredOffers)
            .filter { it.distanceMeters <= maxRadiusMeters }
    }

    // Limpiar oferta seleccionada si quedó fuera del nuevo radio
    LaunchedEffect(localizedOffers) {
        if (selectedOffer != null && localizedOffers.none { it.id == selectedOffer?.id }) {
            selectedOffer = null
        }
    }

    // Centrar cámara automáticamente cuando llega una ubicación GPS real o se pulsa el botón de ubicación
    LaunchedEffect(mapState.recenterSignal, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        if (mapState.recenterSignal > 0) {
            val targetGeo = GeoPoint(userLocation.latitude, userLocation.longitude)
            val targetZoom = OsmMapUtils.clampZoom(
                if (mapState.recenterSignal > 1) 16.5 else OsmMapUtils.DEFAULT_MAP_ZOOM
            )
            map.controller.animateTo(targetGeo, targetZoom, 450L)
            currentZoom = targetZoom
        }
    }

    // Vinculación del ciclo de vida del mapa
    OsmMapUtils.BindMapLifecycle(mapViewRef, lifecycleOwner)

    val controlsBottomPadding by animateDpAsState(
        targetValue = if (selectedOffer != null) 276.dp else 28.dp,
        label = "MapControlsBottomPadding"
    )
    val canZoomIn = currentZoom < OsmMapUtils.MAX_MAP_ZOOM - 0.05
    val canZoomOut = currentZoom > OsmMapUtils.MIN_MAP_ZOOM + 0.05

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        // SE-011: Renderizado nativo del mapa real interactivo con OpenStreetMap / capas seleccionables
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                OsmMapUtils.createDefaultMapView(
                    context = ctx,
                    minZoom = OsmMapUtils.MIN_MAP_ZOOM,
                    maxZoom = OsmMapUtils.MAX_MAP_ZOOM
                ).apply {
                    controller.setCenter(GeoPoint(userLocation.latitude, userLocation.longitude))
                    addMapListener(object : MapListener {
                        override fun onScroll(event: ScrollEvent?): Boolean = false

                        override fun onZoom(event: ZoomEvent?): Boolean {
                            val z = event?.zoomLevel ?: zoomLevelDouble
                            val clamped = OsmMapUtils.clampZoom(z)
                            if (kotlin.math.abs(z - clamped) > 0.01) {
                                controller.setZoom(clamped)
                            }
                            currentZoom = clamped
                            return false
                        }
                    })
                    mapViewRef = this
                }
            },
            update = { mapView ->
                OsmMapUtils.applyMapLayer(
                    mapView = mapView,
                    layer = mapState.selectedLayer,
                    minZoom = OsmMapUtils.MIN_MAP_ZOOM,
                    maxZoom = OsmMapUtils.MAX_MAP_ZOOM
                )

                mapView.overlays.clear()

                // 0. Detector de toque en área vacía para cerrar selector de capas o tarjeta activa
                val tapOverlay = MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (showLayersMenu) showLayersMenu = false
                        if (selectedOffer != null) selectedOffer = null
                        return false
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
                mapView.overlays.add(tapOverlay)

                val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                val highContrastCircle = mapState.selectedLayer == MapLayerStyle.SATELLITE ||
                        mapState.selectedLayer == MapLayerStyle.DARK

                // 1. Círculo de Radio de búsqueda centrado en el usuario
                val radiusCircle = Polygon(mapView).apply {
                    points = Polygon.pointsAsCircle(userGeoPoint, (effectiveRadiusKm * 1000).toDouble())
                    fillPaint.color = if (highContrastCircle) {
                        android.graphics.Color.argb(38, 168, 85, 247)
                    } else {
                        android.graphics.Color.argb(22, 110, 77, 219)
                    }
                    outlinePaint.color = if (highContrastCircle) {
                        android.graphics.Color.argb(180, 192, 132, 252)
                    } else {
                        android.graphics.Color.argb(105, 110, 77, 219)
                    }
                    outlinePaint.strokeWidth = 2.5f * context.resources.displayMetrics.density
                }
                mapView.overlays.add(radiusCircle)

                // 2. Pines interactivos de los Comercios dentro del radio permitido
                localizedOffers.forEach { offer ->
                    val storeMarker = Marker(mapView).apply {
                        position = GeoPoint(offer.latitude, offer.longitude)
                        val isSelected = offer.id == selectedOffer?.id
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
                            showLayersMenu = false
                            selectedOffer = offer
                            mapView.controller.animateTo(position)
                            true
                        }
                    }
                    mapView.overlays.add(storeMarker)
                }

                // 3. Marcador distintivo del Gatito Sensus en la posición GPS real del usuario
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

        // Barra superior de búsqueda, radio (validado para invitado), categorías y selector de capas
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

            // Fila de chip funcional de radio (con bloqueo 2 km si es invitado) y categorías rápidas
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SensusPurple,
                    shadowElevation = 2.dp,
                    modifier = Modifier.clickable {
                        showLayersMenu = false
                        showRadiusSheet = true
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (effectiveIsGuest) Icons.Default.Lock else Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (effectiveIsGuest) {
                                "Radio: ${formatRadiusText(effectiveRadiusKm)} (Máx. 2 km)"
                            } else {
                                "Radio: ${formatRadiusText(effectiveRadiusKm)}"
                            },
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

                StoreCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) SensusPurple else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.clickable {
                            selectedCategory = if (selectedCategory == cat) StoreCategory.ALL else cat
                        }
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

            Spacer(modifier = Modifier.height(10.dp))

            // Fila con aviso de GPS (si aplica) y selector de Capas del Mapa alineado a la derecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    locationNotice?.let { (message, isGpsOff) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, SensusPurple.copy(alpha = 0.25f)),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = SensusPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message,
                                        fontSize = 11.sp,
                                        color = SensusTextPrimary,
                                        lineHeight = 14.sp
                                    )
                                    if (isGpsOff) {
                                        Text(
                                            text = "Activar GPS ahora",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SensusPurple,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .clickable {
                                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                                }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { locationNotice = null },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar aviso",
                                        tint = SensusTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Selector flotante de Capas del Mapa
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (showLayersMenu) SensusPurple else Color.White,
                        shadowElevation = 4.dp,
                        border = BorderStroke(
                            1.dp,
                            if (showLayersMenu) SensusPurple else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.clickable { showLayersMenu = !showLayersMenu }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Capas del mapa",
                                tint = if (showLayersMenu) Color.White else SensusPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = mapState.selectedLayer.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (showLayersMenu) Color.White else SensusTextPrimary
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showLayersMenu,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { -it / 4 }
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .width(205.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Capa del mapa",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SensusTextMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                MapLayerStyle.values().forEach { layer ->
                                    val selected = mapState.selectedLayer == layer
                                    val layerIcon = when (layer) {
                                        MapLayerStyle.STANDARD -> Icons.Default.Map
                                        MapLayerStyle.SATELLITE -> Icons.Default.SatelliteAlt
                                        MapLayerStyle.TERRAIN -> Icons.Default.Terrain
                                        MapLayerStyle.DARK -> Icons.Default.DarkMode
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) SensusPurple.copy(alpha = 0.1f) else Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                mapViewModel.dispatchIntent(BuyerMapViewIntent.LayerSelected(layer))
                                                showLayersMenu = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = layerIcon,
                                                contentDescription = layer.title,
                                                tint = if (selected) SensusPurple else SensusTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = layer.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selected) SensusPurple else SensusTextPrimary
                                                )
                                                Text(
                                                    text = layer.subtitle,
                                                    fontSize = 10.sp,
                                                    color = SensusTextMuted
                                                )
                                            }
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = SensusPurple,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Controles de Zoom (+ / -) y Ubicación Precisa posicionados abajo a la derecha al alcance del pulgar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = controlsBottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cápsula compacta de Zoom (+ y -) con límites de alejamiento mínimo y acercamiento máximo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.width(44.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable(enabled = canZoomIn) {
                                mapViewRef?.let { map ->
                                    val next = OsmMapUtils.clampZoom(map.zoomLevelDouble + 0.8)
                                    map.controller.zoomTo(next, 220L)
                                    currentZoom = next
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Acercar mapa",
                            tint = SensusPurple,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(if (canZoomIn) 1f else 0.35f)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clickable(enabled = canZoomOut) {
                                mapViewRef?.let { map ->
                                    val next = OsmMapUtils.clampZoom(map.zoomLevelDouble - 0.8)
                                    map.controller.zoomTo(next, 220L)
                                    currentZoom = next
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Alejar mapa",
                            tint = SensusPurple,
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(if (canZoomOut) 1f else 0.35f)
                        )
                    }
                }
            }

            // Botón de ubicación GPS precisa más abajo en la esquina inferior derecha
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(
                    width = if (mapState.hasRealLocation) 1.5.dp else 1.dp,
                    color = if (mapState.hasRealLocation) SensusPurple.copy(alpha = 0.45f) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        if (!hasRuntimePermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        } else {
                            locationNotice = null
                            mapViewModel.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (mapState.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SensusPurple,
                            strokeWidth = 2.2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Centrar en mi ubicación precisa",
                            tint = if (hasRuntimePermission && mapState.isLocationEnabled) SensusPurple else SensusTextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Píldora flotante de acceso a la bolsa de compras en esquina inferior izquierda
        if (cart.totalItemsCount > 0) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SensusPurple,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = controlsBottomPadding)
                    .clickable { onOpenCart?.invoke() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bolsa (${cart.totalItemsCount})",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tarjeta flotante inferior de la tienda/oferta seleccionada (solo aparece al tocar un pin)
        selectedOffer?.let { offer ->
            Card(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFE2E8F0))
                        )

                        IconButton(
                            onClick = { selectedOffer = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = SensusTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                                    text = offer.storeName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SensusTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = offer.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = SensusTextPrimary
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${offer.formattedDistance} • Recoge en ${offer.timeToCollectMinutes} min",
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
                                    text = "$${String.format(Locale.US, "%.2f", offer.discountPrice)} MXN",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "-${offer.discountPercent}%",
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
                            onClick = { onRouteClick(offer) },
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
                            onClick = { onProductClick(offer) },
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
        }

        // Hoja inferior interactiva de selección de distancia personalizada (50 m a 5 km; > 2 km bloqueado en invitado)
        if (showRadiusSheet) {
            RadiusSelectorBottomSheet(
                currentRadiusKm = effectiveRadiusKm,
                isGuest = effectiveIsGuest,
                onRadiusSelected = { km ->
                    mapViewModel.dispatchIntent(BuyerMapViewIntent.RadiusSelected(km))
                    if (km < 0.25f) {
                        mapViewRef?.let { map ->
                            val z = OsmMapUtils.clampZoom(17.6)
                            map.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude), z, 350L)
                            currentZoom = z
                        }
                    }
                },
                onRequireAuth = onRequireAuth,
                onDismiss = { showRadiusSheet = false }
            )
        }
    }
}
