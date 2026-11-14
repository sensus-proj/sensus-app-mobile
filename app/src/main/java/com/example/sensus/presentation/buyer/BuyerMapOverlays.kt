package com.example.sensus.presentation.buyer

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import com.example.sensus.domain.model.MapStoreCluster
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.presentation.common.ComposeUiModel
import com.example.sensus.presentation.common.MapLayerStyle
import com.example.sensus.presentation.common.OsmMapUtils
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private const val METERS_PER_KM = 1000
private const val CIRCLE_STROKE_MULTIPLIER = 2.5f
private const val HC_FILL_A = 38
private const val HC_FILL_R = 168
private const val HC_FILL_G = 85
private const val HC_FILL_B = 247
private const val LC_FILL_A = 22
private const val LC_FILL_R = 110
private const val LC_FILL_G = 77
private const val LC_FILL_B = 219
private const val HC_OUT_A = 180
private const val HC_OUT_R = 192
private const val HC_OUT_G = 132
private const val HC_OUT_B = 252
private const val LC_OUT_A = 105

@ComposeUiModel
internal data class MapRenderConfig(
    val context: Context,
    val userLocation: UserLocation,
    val effectiveRadiusKm: Float,
    val selectedLayer: MapLayerStyle,
    val storeClusters: List<MapStoreCluster>,
    val selectedStoreKey: String?,
    val onEmptyTap: () -> Unit,
    val onStoreMarkerTap: (MapStoreCluster) -> Unit
)

internal fun renderBuyerMapOverlays(mapView: MapView, config: MapRenderConfig) {
    OsmMapUtils.applyMapLayer(
        mapView = mapView,
        layer = config.selectedLayer,
        minZoom = OsmMapUtils.MIN_MAP_ZOOM,
        maxZoom = OsmMapUtils.MAX_MAP_ZOOM
    )
    mapView.overlays.clear()
    mapView.overlays.add(createTapOverlay(config.onEmptyTap))

    val userGeoPoint = GeoPoint(config.userLocation.latitude, config.userLocation.longitude)
    val highContrast = config.selectedLayer == MapLayerStyle.SATELLITE || config.selectedLayer == MapLayerStyle.DARK
    mapView.overlays.add(createRadiusPolygon(mapView, config.context, userGeoPoint, config.effectiveRadiusKm, highContrast))

    config.storeClusters.forEach { cluster ->
        val selected = cluster.storeKey == config.selectedStoreKey
        mapView.overlays.add(createStoreClusterMarker(mapView, config.context, cluster, selected, config.onStoreMarkerTap))
    }

    mapView.overlays.add(createCatUserMarker(mapView, config.context, userGeoPoint))
    mapView.invalidate()
}

private fun createTapOverlay(onEmptyTap: () -> Unit): MapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        onEmptyTap()
        return false
    }

    override fun longPressHelper(p: GeoPoint?): Boolean = false
})

private fun createRadiusPolygon(mapView: MapView, context: Context, center: GeoPoint, radiusKm: Float, highContrast: Boolean): Polygon =
    Polygon(mapView).apply {
        points = Polygon.pointsAsCircle(center, (radiusKm * METERS_PER_KM).toDouble())
        fillPaint.color = if (highContrast) {
            AndroidColor.argb(HC_FILL_A, HC_FILL_R, HC_FILL_G, HC_FILL_B)
        } else {
            AndroidColor.argb(LC_FILL_A, LC_FILL_R, LC_FILL_G, LC_FILL_B)
        }
        outlinePaint.color = if (highContrast) {
            AndroidColor.argb(HC_OUT_A, HC_OUT_R, HC_OUT_G, HC_OUT_B)
        } else {
            AndroidColor.argb(LC_OUT_A, LC_FILL_R, LC_FILL_G, LC_FILL_B)
        }
        outlinePaint.strokeWidth = CIRCLE_STROKE_MULTIPLIER * context.resources.displayMetrics.density
    }

private fun createStoreClusterMarker(
    mapView: MapView,
    context: Context,
    cluster: MapStoreCluster,
    isSelected: Boolean,
    onStoreTap: (MapStoreCluster) -> Unit
): Marker = Marker(mapView).apply {
    position = GeoPoint(cluster.latitude, cluster.longitude)
    val rightBadge = when (cluster.matchingOffers.size) {
        0 -> "Tienda"
        1 -> "$${cluster.matchingOffers.first().discountPrice.toInt()}"
        else -> "${cluster.matchingOffers.size} ofertas"
    }
    val pinBitmap = OsmMapUtils.createStorePinBitmap(
        context = context,
        storeName = cluster.storeName,
        price = rightBadge,
        isSelected = isSelected
    )
    icon = BitmapDrawable(context.resources, pinBitmap)
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    title = cluster.storeName
    snippet = cluster.featuredOffer?.name ?: cluster.address
    setOnMarkerClickListener { _, _ ->
        onStoreTap(cluster)
        mapView.controller.animateTo(position)
        true
    }
}

private fun createCatUserMarker(mapView: MapView, context: Context, userGeoPoint: GeoPoint): Marker = Marker(mapView).apply {
    position = userGeoPoint
    icon = BitmapDrawable(context.resources, OsmMapUtils.createCatMarkerBitmap(context))
    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
    title = "Tú estás aquí"
    snippet = "Gatito Sensus (GPS)"
}
