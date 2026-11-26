package com.example.sensus.presentation.merchant

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.common.OsmMapUtils
import com.example.sensus.theme.SensusBackground
import com.example.sensus.theme.SensusButtonShape
import com.example.sensus.theme.SensusCardBorder
import com.example.sensus.theme.SensusPurple
import com.example.sensus.theme.SensusPurpleDark
import com.example.sensus.theme.SensusPurpleLight
import com.example.sensus.theme.SensusSuccess
import com.example.sensus.theme.SensusSuccessBg
import com.example.sensus.theme.SensusSurface
import com.example.sensus.theme.SensusTextPrimary
import com.example.sensus.theme.SensusTextSecondary
import com.example.sensus.theme.SensusVioletBg
import com.example.sensus.theme.SensusVioletInk
import com.example.sensus.theme.SensusVioletLight
import com.example.sensus.theme.SensusWhite
import java.util.Locale
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private const val PICKER_MAP_ZOOM = 17.5
private const val RECENTER_ANIM_MS = 450L
private const val CAT_CANVAS_SIZE_DP = 48
private const val PIN_ICON_SIZE_DP = 30
private const val MAP_CENTER_PIN_SIZE_DP = 62
private const val CENTER_PIN_LIFT_DP = -30
private const val STROKE_OUTLINE_DP = 1.8f
private const val HALF_RATIO = 0.5f
private const val CHEEK_BLUSH_ALPHA = 0.55f
private const val SHADOW_DOT_ALPHA = 0.28f
private const val SMILE_STROKE_SCALE = 0.82f
private const val WHISKER_STROKE_SCALE = 0.65f
private const val PAW_WIDTH_RATIO = 0.16f
private const val PAW_HEIGHT_RATIO = 0.11f
private const val PAW_LEFT_X_RATIO = 0.24f
private const val PAW_RIGHT_X_RATIO = 0.60f
private const val PAW_TOP_Y_RATIO = 0.46f
private const val BLUSH_WIDTH_RATIO = 0.11f
private const val BLUSH_HEIGHT_RATIO = 0.06f
private const val BLUSH_LEFT_X_RATIO = 0.24f
private const val BLUSH_RIGHT_X_RATIO = 0.65f
private const val BLUSH_Y_RATIO = 0.36f
private const val NOSE_Y_RATIO = 0.37f
private const val NOSE_RADIUS_RATIO = 0.028f

@Composable
internal fun PeekingCatLocationButton(isResolving: Boolean, hasCoordinates: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val containerColor = if (hasCoordinates) SensusVioletBg else SensusVioletLight
    val borderColor = if (hasCoordinates) SensusPurple else SensusCardBorder
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !isResolving, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeekingCatPinBadge(badgeSize = CAT_CANVAS_SIZE_DP.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasCoordinates) "Ubicación en mapa lista" else "Usar mi ubicación actual",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SensusVioletInk
                )
                Text(
                    text = if (hasCoordinates) {
                        "Toca para mover el pin o ajustar en el mapa"
                    } else {
                        "Detecta tu GPS y ajusta el pin en el mapa"
                    },
                    fontSize = 11.sp,
                    color = SensusTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            PeekingCatStatusTrail(isResolving = isResolving, hasCoordinates = hasCoordinates)
        }
    }
}

@Composable
private fun PeekingCatStatusTrail(isResolving: Boolean, hasCoordinates: Boolean) {
    if (isResolving) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SensusPurple)
    } else if (hasCoordinates) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Ubicación seleccionada",
            tint = SensusSuccess,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
internal fun PeekingCatPinBadge(badgeSize: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .size(badgeSize)
            .semantics { contentDescription = "Mascota Sensus sobre icono de ubicación" }
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = SensusVioletInk,
            modifier = Modifier.size(PIN_ICON_SIZE_DP.dp)
        )
        PeekingSensusCatHead(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PeekingSensusCatHead(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokePx = STROKE_OUTLINE_DP.dp.toPx()
        val headPath = buildCuteCatHeadSilhouette(w, h)
        drawPath(path = headPath, color = SensusWhite)
        drawInnerEars(w, h)
        drawPath(
            path = headPath,
            color = SensusPurpleDark,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCatFaceDetails(w, h, strokePx)
        drawPeekingPaws(w, h, strokePx)
    }
}

@Suppress("MagicNumber")
private fun buildCuteCatHeadSilhouette(w: Float, h: Float): Path = Path().apply {
    moveTo(w * 0.25f, h * 0.48f)
    cubicTo(w * 0.14f, h * 0.42f, w * 0.15f, h * 0.27f, w * 0.22f, h * 0.20f)
    lineTo(w * 0.20f, h * 0.06f)
    quadraticTo(w * 0.21f, h * 0.03f, w * 0.25f, h * 0.05f)
    lineTo(w * 0.37f, h * 0.15f)
    quadraticTo(w * 0.50f, h * 0.12f, w * 0.63f, h * 0.15f)
    lineTo(w * 0.75f, h * 0.05f)
    quadraticTo(w * 0.79f, h * 0.03f, w * 0.80f, h * 0.06f)
    lineTo(w * 0.78f, h * 0.20f)
    cubicTo(w * 0.85f, h * 0.27f, w * 0.86f, h * 0.42f, w * 0.75f, h * 0.48f)
    quadraticTo(w * 0.50f, h * 0.54f, w * 0.25f, h * 0.48f)
    close()
}

@Suppress("MagicNumber")
private fun DrawScope.drawInnerEars(w: Float, h: Float) {
    val innerPath = Path().apply {
        moveTo(w * 0.24f, h * 0.20f)
        lineTo(w * 0.23f, h * 0.09f)
        lineTo(w * 0.34f, h * 0.16f)
        close()

        moveTo(w * 0.76f, h * 0.20f)
        lineTo(w * 0.77f, h * 0.09f)
        lineTo(w * 0.66f, h * 0.16f)
        close()
    }
    drawPath(path = innerPath, color = SensusPurpleLight)
}

@Suppress("MagicNumber")
private fun DrawScope.drawCatFaceDetails(w: Float, h: Float, strokePx: Float) {
    val blushSize = Size(w * BLUSH_WIDTH_RATIO, h * BLUSH_HEIGHT_RATIO)
    drawOval(color = SensusPurpleLight.copy(alpha = CHEEK_BLUSH_ALPHA), topLeft = Offset(w * BLUSH_LEFT_X_RATIO, h * BLUSH_Y_RATIO), size = blushSize)
    drawOval(color = SensusPurpleLight.copy(alpha = CHEEK_BLUSH_ALPHA), topLeft = Offset(w * BLUSH_RIGHT_X_RATIO, h * BLUSH_Y_RATIO), size = blushSize)

    val happyEyesAndSmile = Path().apply {
        moveTo(w * 0.31f, h * 0.32f)
        quadraticTo(w * 0.37f, h * 0.25f, w * 0.43f, h * 0.32f)
        moveTo(w * 0.57f, h * 0.32f)
        quadraticTo(w * 0.63f, h * 0.25f, w * 0.69f, h * 0.32f)
        moveTo(w * 0.43f, h * 0.40f)
        quadraticTo(w * 0.465f, h * 0.44f, w * HALF_RATIO, h * 0.39f)
        quadraticTo(w * 0.535f, h * 0.44f, w * 0.57f, h * 0.40f)
    }
    drawPath(
        path = happyEyesAndSmile,
        color = SensusPurpleDark,
        style = Stroke(width = strokePx * SMILE_STROKE_SCALE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawCircle(color = SensusPurpleDark, radius = w * NOSE_RADIUS_RATIO, center = Offset(w * HALF_RATIO, h * NOSE_Y_RATIO))
    drawLine(
        color = SensusPurpleDark,
        start = Offset(w * 0.13f, h * 0.35f),
        end = Offset(w * 0.22f, h * 0.36f),
        strokeWidth = strokePx * WHISKER_STROKE_SCALE,
        cap = StrokeCap.Round
    )
    drawLine(
        color = SensusPurpleDark,
        start = Offset(w * 0.87f, h * 0.35f),
        end = Offset(w * 0.78f, h * 0.36f),
        strokeWidth = strokePx * WHISKER_STROKE_SCALE,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawPeekingPaws(w: Float, h: Float, strokePx: Float) {
    val pawSize = Size(w * PAW_WIDTH_RATIO, h * PAW_HEIGHT_RATIO)
    val leftTop = Offset(w * PAW_LEFT_X_RATIO, h * PAW_TOP_Y_RATIO)
    val rightTop = Offset(w * PAW_RIGHT_X_RATIO, h * PAW_TOP_Y_RATIO)
    drawOval(color = SensusWhite, topLeft = leftTop, size = pawSize)
    drawOval(color = SensusPurpleDark, topLeft = leftTop, size = pawSize, style = Stroke(width = strokePx * SMILE_STROKE_SCALE))
    drawOval(color = SensusWhite, topLeft = rightTop, size = pawSize)
    drawOval(color = SensusPurpleDark, topLeft = rightTop, size = pawSize, style = Stroke(width = strokePx * SMILE_STROKE_SCALE))
}

@Composable
internal fun MerchantLocationPickerModal(
    state: MerchantRegisterViewState,
    onDismiss: () -> Unit,
    onRecenterGps: () -> Unit,
    onPinMoved: (Double, Double) -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var pickerMapView by remember { mutableStateOf<MapView?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SensusBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            PickerHeaderBar(onDismiss = onDismiss)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                InteractiveOsmLocationMap(
                    state = state,
                    onPinMoved = onPinMoved,
                    onMapReady = { pickerMapView = it }
                )
                CenterMapTargetPin(modifier = Modifier.align(Alignment.Center))
                PickerGpsRecenterButton(onClick = onRecenterGps, modifier = Modifier.align(Alignment.BottomEnd))
            }
            PickerConfirmationPanel(
                state = state,
                onConfirm = {
                    val liveCenter = pickerMapView?.mapCenter
                    val lat = liveCenter?.latitude ?: state.pickerLatitude
                    val lng = liveCenter?.longitude ?: state.pickerLongitude
                    onConfirm(lat, lng)
                }
            )
        }
    }
}

@Composable
private fun PickerGpsRecenterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = SensusSurface,
        shadowElevation = 6.dp,
        modifier = modifier
            .padding(16.dp)
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Recentrar en mi ubicación actual",
                tint = SensusVioletInk
            )
        }
    }
}

@Composable
private fun PickerHeaderBar(onDismiss: () -> Unit) {
    Surface(color = SensusSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar mapa", tint = SensusTextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Ubica tu comercio en el mapa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
                Text(text = "Arrastra o toca el mapa si necesitas mayor precisión", fontSize = 13.sp, color = SensusTextSecondary)
            }
        }
    }
}

@Composable
private fun InteractiveOsmLocationMap(state: MerchantRegisterViewState, onPinMoved: (Double, Double) -> Unit, onMapReady: (MapView) -> Unit) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasUserTouchedSinceRecenter by remember { mutableStateOf(false) }

    OsmMapUtils.BindMapLifecycle(mapView = mapViewRef)

    LaunchedEffect(state.recenterSignal, mapViewRef) {
        val map = mapViewRef ?: return@LaunchedEffect
        hasUserTouchedSinceRecenter = false
        val targetGeo = GeoPoint(state.pickerLatitude, state.pickerLongitude)
        map.controller.setZoom(PICKER_MAP_ZOOM)
        map.controller.setCenter(targetGeo)
        map.controller.animateTo(targetGeo, PICKER_MAP_ZOOM, RECENTER_ANIM_MS)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            createInteractivePickerMapView(
                context = ctx,
                initialCenter = GeoPoint(state.pickerLatitude, state.pickerLongitude),
                isUserGestureActive = { hasUserTouchedSinceRecenter },
                onUserTouch = { hasUserTouchedSinceRecenter = true },
                onCenterChanged = onPinMoved
            ).also {
                mapViewRef = it
                onMapReady(it)
            }
        },
        update = { mapView ->
            renderPickerUserMarker(mapView, context, state.userLocation)
        }
    )
}

@SuppressLint("ClickableViewAccessibility")
private fun createInteractivePickerMapView(
    context: Context,
    initialCenter: GeoPoint,
    isUserGestureActive: () -> Boolean,
    onUserTouch: () -> Unit,
    onCenterChanged: (Double, Double) -> Unit
): MapView = OsmMapUtils.createDefaultMapView(
    context = context,
    minZoom = OsmMapUtils.MIN_MAP_ZOOM,
    maxZoom = OsmMapUtils.MAX_MAP_ZOOM
).apply {
    controller.setZoom(PICKER_MAP_ZOOM)
    controller.setCenter(initialCenter)
    setOnTouchListener { view, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
            onUserTouch()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            view.performClick()
            onCenterChanged(mapCenter.latitude, mapCenter.longitude)
        }
        false
    }
    overlays.add(
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                val point = p ?: return false
                onUserTouch()
                controller.setCenter(point)
                onCenterChanged(point.latitude, point.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
    )
    addMapListener(object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            if (isUserGestureActive()) {
                onCenterChanged(mapCenter.latitude, mapCenter.longitude)
            }
            return false
        }

        override fun onZoom(event: ZoomEvent?): Boolean = false
    })
}

private fun renderPickerUserMarker(mapView: MapView, context: Context, userLocation: UserLocation) {
    mapView.overlays.removeAll { it is Marker }
    val userGeoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
    val marker = Marker(mapView).apply {
        position = userGeoPoint
        icon = BitmapDrawable(context.resources, OsmMapUtils.createCatMarkerBitmap(context))
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        title = "Tú estás aquí"
        snippet = "Gatito Sensus (GPS)"
    }
    mapView.overlays.add(marker)
    mapView.invalidate()
}

@Composable
private fun CenterMapTargetPin(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.semantics { contentDescription = "Pin de ubicación de comercio" }
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(SensusPurpleDark.copy(alpha = SHADOW_DOT_ALPHA))
        )
        PeekingCatPinBadge(
            badgeSize = MAP_CENTER_PIN_SIZE_DP.dp,
            modifier = Modifier.offset(y = CENTER_PIN_LIFT_DP.dp)
        )
    }
}

@Composable
private fun PickerConfirmationPanel(state: MerchantRegisterViewState, onConfirm: () -> Unit) {
    Surface(color = SensusSurface, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), shadowElevation = 10.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PickerCoordinatesHeader(state.pickerLatitude, state.pickerLongitude)
            PickerAddressPreviewBody(state)
            state.locationHelperHint?.let { hint ->
                Text(text = hint, fontSize = 11.sp, color = SensusTextSecondary)
            }
            Button(
                onClick = onConfirm,
                shape = SensusButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = SensusPurple),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(text = "Confirmar ubicación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SensusWhite)
            }
        }
    }
}

@Composable
private fun PickerCoordinatesHeader(latitude: Double, longitude: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Dirección detectada", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SensusTextPrimary)
        Surface(shape = RoundedCornerShape(8.dp), color = SensusVioletBg) {
            Text(
                text = String.format(Locale.US, "%.5f, %.5f", latitude, longitude),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SensusVioletInk,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun PickerAddressPreviewBody(state: MerchantRegisterViewState) {
    if (state.isResolvingLocation) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SensusPurple)
            Text(text = "Consultando dirección exacta...", fontSize = 13.sp, color = SensusTextSecondary)
        }
    } else {
        Text(
            text = state.resolvedAddressPreview ?: "Punto listo; podrás editar tu dirección manualmente.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = SensusTextPrimary
        )
        state.resolvedPostalPreview?.let { cp ->
            Surface(shape = RoundedCornerShape(8.dp), color = SensusSuccessBg) {
                Text(
                    text = "Código postal: $cp",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SensusTextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
