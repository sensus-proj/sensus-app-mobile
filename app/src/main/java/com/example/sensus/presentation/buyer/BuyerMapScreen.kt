package com.example.sensus.presentation.buyer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.domain.model.GuestRadiusPolicy
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.StoreCategory
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.presentation.common.RadiusSelectorBottomSheet
import kotlin.math.abs
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

private const val METERS_PER_KM = 1000
private const val MAP_BG_HEX = 0xFFE5E7EB
private const val FOCUS_ZOOM = 16.5
private const val SHORT_RADIUS_ZOOM = 17.6
private const val SHORT_RADIUS_THRESHOLD_KM = 0.25f
private const val RECENTER_ANIM_MS = 450L
private const val SHORT_ZOOM_ANIM_MS = 350L
private const val ZOOM_BUTTON_ANIM_MS = 220L
private const val ZOOM_STEP = 0.8
private const val ZOOM_EPSILON = 0.05
private const val ZOOM_CLAMP_EPSILON = 0.01

private val mapCategoryKeywords = mapOf(
    StoreCategory.GROCERY to "Abarrotes",
    StoreCategory.CLEANING to "Limpieza",
    StoreCategory.PERISHABLES to "Perecederos",
    StoreCategory.DAIRY to "Lácteos",
    StoreCategory.FOOD to "Comida",
    StoreCategory.BAKERY to "Panadería",
    StoreCategory.BEVERAGES to "Bebidas",
    StoreCategory.PETS to "Mascotas",
    StoreCategory.PHARMACY to "Farmacia"
)

@ComposeUiModel
data class BuyerMapCallbacks(
    val isGuest: Boolean = false,
    val onRequireAuth: () -> Unit = {},
    val onProductClick: (ProductOffer) -> Unit = {},
    val onRouteClick: (ProductOffer) -> Unit = {},
    val onListToggle: () -> Unit = {},
    val onOpenCart: (() -> Unit)? = null
)

@ComposeUiModel
@Stable
private class BuyerMapUiStateHolder {
    var searchQuery by mutableStateOf("")
    var selectedCategory by mutableStateOf(StoreCategory.ALL)
    var selectedOffer by mutableStateOf<ProductOffer?>(null)
    var showRadiusSheet by mutableStateOf(false)
    var showLayersMenu by mutableStateOf(false)
    var locationNotice by mutableStateOf<Pair<String, Boolean>?>(null)
    var mapViewRef by mutableStateOf<MapView?>(null)
    var currentZoom by mutableDoubleStateOf(OsmMapUtils.DEFAULT_MAP_ZOOM)
}

@ComposeUiModel
private data class BuyerMapHostInput(
    val mapState: BuyerMapViewState,
    val cartItemCount: Int,
    val localizedOffers: List<ProductOffer>,
    val effectiveIsGuest: Boolean,
    val effectiveRadiusKm: Float,
    val hasRuntimePermission: Boolean
)

@Composable
fun BuyerMapScreen(
    mapViewModel: BuyerMapViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    callbacks: BuyerMapCallbacks,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapState by mapViewModel.viewState.collectAsState()
    val cartState by cartViewModel.viewState.collectAsState()
    val uiHolder = remember { BuyerMapUiStateHolder() }

    val effectiveIsGuest = callbacks.isGuest || mapState.isGuest
    val effectiveRadiusKm = GuestRadiusPolicy.clampRadius(mapState.selectedRadiusKm, effectiveIsGuest)
    val hasRuntimePermission = remember(mapState.isLocationPermissionGranted) { hasMapLocationPermission(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        mapViewModel.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(granted))
        if (granted) uiHolder.locationNotice = null
    }

    BuyerMapLifecycleEffects(
        mapViewModel = mapViewModel,
        mapState = mapState,
        uiHolder = uiHolder,
        onRequireAuth = callbacks.onRequireAuth,
        onRequestPermission = { permissionLauncher.launch(locationPermissionsArray()) }
    )
    OsmMapUtils.BindMapLifecycle(uiHolder.mapViewRef, lifecycleOwner)

    val localizedOffers = remember(mapState.userLocation, uiHolder.searchQuery, uiHolder.selectedCategory, mapState.offers, effectiveRadiusKm) {
        computeLocalizedMapOffers(mapState.userLocation, mapState.offers, uiHolder.searchQuery, uiHolder.selectedCategory, effectiveRadiusKm)
    }
    LaunchedEffect(localizedOffers) {
        if (uiHolder.selectedOffer != null && localizedOffers.none { it.id == uiHolder.selectedOffer?.id }) {
            uiHolder.selectedOffer = null
        }
    }

    BuyerMapScreenScaffold(
        hostInput = BuyerMapHostInput(
            mapState = mapState,
            cartItemCount = cartState.storeCart.totalItemsCount,
            localizedOffers = localizedOffers,
            effectiveIsGuest = effectiveIsGuest,
            effectiveRadiusKm = effectiveRadiusKm,
            hasRuntimePermission = hasRuntimePermission
        ),
        deps = BuyerMapScaffoldDeps(
            mapViewModel = mapViewModel,
            uiHolder = uiHolder,
            callbacks = callbacks,
            onRequestPermission = { permissionLauncher.launch(locationPermissionsArray()) }
        ),
        modifier = modifier
    )
}

@ComposeUiModel
private data class BuyerMapScaffoldDeps(
    val mapViewModel: BuyerMapViewModel,
    val uiHolder: BuyerMapUiStateHolder,
    val callbacks: BuyerMapCallbacks,
    val onRequestPermission: () -> Unit
)

@Composable
private fun BuyerMapScreenScaffold(hostInput: BuyerMapHostInput, deps: BuyerMapScaffoldDeps, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapState = hostInput.mapState
    val uiHolder = deps.uiHolder
    val controlsBottomPadding by animateDpAsState(
        targetValue = if (uiHolder.selectedOffer != null) 276.dp else 28.dp,
        label = "MapControlsBottomPadding"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(MAP_BG_HEX))) {
        BuyerOsmMapView(
            userLocation = mapState.userLocation,
            renderConfig = MapRenderConfig(
                context = context,
                userLocation = mapState.userLocation,
                effectiveRadiusKm = hostInput.effectiveRadiusKm,
                selectedLayer = mapState.selectedLayer,
                localizedOffers = hostInput.localizedOffers,
                selectedOfferId = uiHolder.selectedOffer?.id,
                onEmptyTap = {
                    if (uiHolder.showLayersMenu) uiHolder.showLayersMenu = false
                    if (uiHolder.selectedOffer != null) uiHolder.selectedOffer = null
                },
                onMarkerTap = { offer ->
                    uiHolder.showLayersMenu = false
                    uiHolder.selectedOffer = offer
                }
            ),
            onMapCreated = { uiHolder.mapViewRef = it },
            onZoomUpdated = { uiHolder.currentZoom = it }
        )

        BuyerMapTopOverlaySection(hostInput = hostInput, uiHolder = uiHolder, mapViewModel = deps.mapViewModel, callbacks = deps.callbacks)
        BuyerMapBottomOverlaySection(
            hostInput = hostInput,
            uiHolder = uiHolder,
            callbacks = deps.callbacks,
            controlsBottomPadding = controlsBottomPadding,
            actions = MapBottomOverlayActions(
                onRequestPermission = deps.onRequestPermission,
                onRefreshLocation = { deps.mapViewModel.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true)) },
                onRadiusSelected = { km ->
                    deps.mapViewModel.dispatchIntent(BuyerMapViewIntent.RadiusSelected(km))
                    applyShortRadiusFocusZoom(km, mapState.userLocation, uiHolder)
                }
            )
        )
    }
}

@Composable
private fun BuyerMapTopOverlaySection(
    hostInput: BuyerMapHostInput,
    uiHolder: BuyerMapUiStateHolder,
    mapViewModel: BuyerMapViewModel,
    callbacks: BuyerMapCallbacks
) {
    val context = LocalContext.current
    BuyerMapTopOverlay(
        state = MapTopOverlayState(
            searchQuery = uiHolder.searchQuery,
            effectiveIsGuest = hostInput.effectiveIsGuest,
            effectiveRadiusKm = hostInput.effectiveRadiusKm,
            selectedCategory = uiHolder.selectedCategory,
            locationNotice = uiHolder.locationNotice,
            selectedLayer = hostInput.mapState.selectedLayer,
            showLayersMenu = uiHolder.showLayersMenu
        ),
        callbacks = MapTopOverlayCallbacks(
            onSearchChange = { uiHolder.searchQuery = it },
            onListToggle = callbacks.onListToggle,
            onRadiusClick = {
                uiHolder.showLayersMenu = false
                uiHolder.showRadiusSheet = true
            },
            onCategoryToggle = { clicked ->
                uiHolder.selectedCategory = if (uiHolder.selectedCategory == clicked) StoreCategory.ALL else clicked
            },
            onOpenGpsSettings = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            onDismissNotice = { uiHolder.locationNotice = null },
            onToggleLayersMenu = { uiHolder.showLayersMenu = !uiHolder.showLayersMenu },
            onSelectLayer = { layer ->
                mapViewModel.dispatchIntent(BuyerMapViewIntent.LayerSelected(layer))
                uiHolder.showLayersMenu = false
            }
        )
    )
}

@ComposeUiModel
private data class MapBottomOverlayActions(
    val onRequestPermission: () -> Unit,
    val onRefreshLocation: () -> Unit,
    val onRadiusSelected: (Float) -> Unit
)

@Composable
private fun BuyerMapBottomOverlaySection(
    hostInput: BuyerMapHostInput,
    uiHolder: BuyerMapUiStateHolder,
    callbacks: BuyerMapCallbacks,
    controlsBottomPadding: androidx.compose.ui.unit.Dp,
    actions: MapBottomOverlayActions
) {
    val mapState = hostInput.mapState
    Box(modifier = Modifier.fillMaxSize()) {
        BuyerMapRightControls(
            state = MapZoomLocationState(
                canZoomIn = uiHolder.currentZoom < OsmMapUtils.MAX_MAP_ZOOM - ZOOM_EPSILON,
                canZoomOut = uiHolder.currentZoom > OsmMapUtils.MIN_MAP_ZOOM + ZOOM_EPSILON,
                hasRealLocation = hostInput.hasRuntimePermission && mapState.hasRealLocation,
                isLocationEnabled = hostInput.hasRuntimePermission && mapState.isLocationEnabled,
                isLocating = mapState.isLocating
            ),
            onZoomIn = { uiHolder.mapViewRef?.let { uiHolder.currentZoom = stepMapZoom(it, +ZOOM_STEP) } },
            onZoomOut = { uiHolder.mapViewRef?.let { uiHolder.currentZoom = stepMapZoom(it, -ZOOM_STEP) } },
            onCenterLocation = {
                if (!hostInput.hasRuntimePermission) {
                    actions.onRequestPermission()
                } else {
                    uiHolder.locationNotice = null
                    actions.onRefreshLocation()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = controlsBottomPadding)
        )

        if (hostInput.cartItemCount > 0) {
            FloatingMapCartPill(
                itemCount = hostInput.cartItemCount,
                onClick = { callbacks.onOpenCart?.invoke() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, bottom = controlsBottomPadding)
            )
        }

        uiHolder.selectedOffer?.let { offer ->
            SelectedOfferBottomCard(
                offer = offer,
                onClose = { uiHolder.selectedOffer = null },
                onRouteClick = callbacks.onRouteClick,
                onProductClick = callbacks.onProductClick,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (uiHolder.showRadiusSheet) {
            RadiusSelectorBottomSheet(
                currentRadiusKm = hostInput.effectiveRadiusKm,
                isGuest = hostInput.effectiveIsGuest,
                onRadiusSelected = actions.onRadiusSelected,
                onRequireAuth = callbacks.onRequireAuth,
                onDismiss = { uiHolder.showRadiusSheet = false }
            )
        }
    }
}

@Composable
private fun BuyerOsmMapView(userLocation: UserLocation, renderConfig: MapRenderConfig, onMapCreated: (MapView) -> Unit, onZoomUpdated: (Double) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            OsmMapUtils.createDefaultMapView(
                context = ctx,
                minZoom = OsmMapUtils.MIN_MAP_ZOOM,
                maxZoom = OsmMapUtils.MAX_MAP_ZOOM
            ).apply {
                controller.setCenter(GeoPoint(userLocation.latitude, userLocation.longitude))
                OsmMapUtils.applyMapLayer(
                    mapView = this,
                    layer = renderConfig.selectedLayer,
                    minZoom = OsmMapUtils.MIN_MAP_ZOOM,
                    maxZoom = OsmMapUtils.MAX_MAP_ZOOM
                )
                addMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean = false

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        val zoomLevel = event?.zoomLevel ?: zoomLevelDouble
                        val clamped = OsmMapUtils.clampZoom(zoomLevel)
                        if (abs(zoomLevel - clamped) > ZOOM_CLAMP_EPSILON) controller.setZoom(clamped)
                        onZoomUpdated(clamped)
                        return false
                    }
                })
                onMapCreated(this)
            }
        },
        update = { mapView -> renderBuyerMapOverlays(mapView, renderConfig) }
    )
}

@Composable
private fun BuyerMapLifecycleEffects(
    mapViewModel: BuyerMapViewModel,
    mapState: BuyerMapViewState,
    uiHolder: BuyerMapUiStateHolder,
    onRequireAuth: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        mapViewModel.viewEffect.collect { effect ->
            when (effect) {
                is BuyerMapViewEffect.RequireAuthForRadius -> onRequireAuth()
                is BuyerMapViewEffect.ShowLocationMessage -> uiHolder.locationNotice = effect.message to effect.isGpsDisabled
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasMapLocationPermission(context)) {
            mapViewModel.dispatchIntent(BuyerMapViewIntent.LocationPermissionGranted(true))
        } else {
            onRequestPermission()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            syncLocationOnResume(event, context, mapState.hasRealLocation, uiHolder, mapViewModel)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mapState.recenterSignal, uiHolder.mapViewRef) {
        animateRecenterIfNeeded(uiHolder, mapState)
    }
}

private fun syncLocationOnResume(
    event: Lifecycle.Event,
    context: Context,
    hasRealLocation: Boolean,
    uiHolder: BuyerMapUiStateHolder,
    mapViewModel: BuyerMapViewModel
) {
    if (event == Lifecycle.Event.ON_RESUME && hasMapLocationPermission(context) && !hasRealLocation) {
        uiHolder.locationNotice = null
        mapViewModel.dispatchIntent(BuyerMapViewIntent.RefreshLocation(forceFresh = true))
    }
}

private fun animateRecenterIfNeeded(uiHolder: BuyerMapUiStateHolder, mapState: BuyerMapViewState) {
    val map = uiHolder.mapViewRef ?: return
    if (mapState.recenterSignal > 0) {
        val targetGeo = GeoPoint(mapState.userLocation.latitude, mapState.userLocation.longitude)
        val targetZoom = OsmMapUtils.clampZoom(if (mapState.recenterSignal > 1) FOCUS_ZOOM else OsmMapUtils.DEFAULT_MAP_ZOOM)
        map.controller.animateTo(targetGeo, targetZoom, RECENTER_ANIM_MS)
        uiHolder.currentZoom = targetZoom
    }
}

private fun applyShortRadiusFocusZoom(radiusKm: Float, userLocation: UserLocation, uiHolder: BuyerMapUiStateHolder) {
    if (radiusKm < SHORT_RADIUS_THRESHOLD_KM) {
        uiHolder.mapViewRef?.let { map ->
            val zoom = OsmMapUtils.clampZoom(SHORT_RADIUS_ZOOM)
            map.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude), zoom, SHORT_ZOOM_ANIM_MS)
            uiHolder.currentZoom = zoom
        }
    }
}

private fun stepMapZoom(map: MapView, delta: Double): Double {
    val next = OsmMapUtils.clampZoom(map.zoomLevelDouble + delta)
    map.controller.zoomTo(next, ZOOM_BUTTON_ANIM_MS)
    return next
}

private fun computeLocalizedMapOffers(
    userLocation: UserLocation,
    offers: List<ProductOffer>,
    searchQuery: String,
    selectedCategory: StoreCategory,
    effectiveRadiusKm: Float
): List<ProductOffer> {
    val keyword = mapCategoryKeywords[selectedCategory]
    val filtered = offers.filter { offer ->
        val matchesCategory = keyword == null || offer.category.contains(keyword, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
            offer.name.contains(searchQuery, ignoreCase = true) ||
            offer.storeName.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }
    val maxRadiusMeters = (effectiveRadiusKm * METERS_PER_KM).toInt()
    return OsmMapUtils.localizeAndMeasureOffers(userLocation, filtered).filter { it.distanceMeters <= maxRadiusMeters }
}

private fun hasMapLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private fun locationPermissionsArray(): Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)
