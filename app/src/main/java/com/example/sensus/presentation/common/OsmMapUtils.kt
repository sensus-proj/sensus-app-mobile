package com.example.sensus.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.R
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay

private const val TILE_MIN_ZOOM_INT = 1
private const val TILE_MAX_ZOOM_INT = 18
private const val TILE_SIZE_PX = 256
private const val MIN_OFFER_METERS = 40
private const val CAT_MARKER_DP = 58
private const val CAT_HALO_ALPHA = 40
private const val CAT_HALO_RATIO = 0.98f
private const val CAT_CIRCLE_RATIO = 0.78f
private const val CAT_BORDER_WIDTH_DP = 2.5f
private const val CAT_BITMAP_RATIO = 0.58f
private const val PIN_HEIGHT_DP = 34
private const val PIN_TEXT_SIZE_SP = 11f
private const val PIN_PRICE_SIZE_SP = 11.5f
private const val PIN_NAME_LIMIT = 12
private const val PIN_NAME_TRUNCATE = 10
private const val PIN_PADDING_H_DP = 12f
private const val PIN_SPACE_BETWEEN_DP = 8f
private const val PIN_MIN_WIDTH_DP = 64
private const val PIN_CORNER_DP = 17f
private const val PIN_BORDER_WIDTH_DP = 1.5f
private const val PIN_RECT_INSET = 2f
private const val OFFSET_42M_LAT = 0.00032
private const val OFFSET_42M_LNG = 0.00022
private const val OFFSET_88M_LAT = -0.00065
private const val OFFSET_88M_LNG = 0.00048
private const val OFFSET_195M_LAT = 0.00140
private const val OFFSET_195M_LNG = -0.00110
private const val OFFSET_415M_LAT = -0.00300
private const val OFFSET_415M_LNG = -0.00240
private const val OFFSET_1030M_LAT = 0.00720
private const val OFFSET_1030M_LNG = -0.00620
private const val OFFSET_1800M_LAT = -0.01250
private const val OFFSET_1800M_LNG = 0.01100
private const val OFFSET_2510M_LAT = 0.01780
private const val OFFSET_2510M_LNG = 0.01480
private const val OFFSET_3950M_LAT = -0.02750
private const val OFFSET_3950M_LNG = -0.02400

enum class MapLayerStyle(
    val title: String,
    val subtitle: String,
    val id: String
) {
    STANDARD("Estándar", "Calles y comercios", "Mapnik"),
    SATELLITE("Satélite", "Vista aérea", "EsriWorldImagery"),
    TERRAIN("Relieve", "Topográfico y vías", "EsriWorldTopo"),
    DARK("Nocturno", "Modo oscuro", "SensusDarkOsm")
}

fun MapLayer.toStyle(): MapLayerStyle = when (this) {
    MapLayer.STANDARD -> MapLayerStyle.STANDARD
    MapLayer.SATELLITE -> MapLayerStyle.SATELLITE
    MapLayer.TERRAIN -> MapLayerStyle.TERRAIN
    MapLayer.DARK -> MapLayerStyle.DARK
}

fun MapLayerStyle.toDomain(): MapLayer = when (this) {
    MapLayerStyle.STANDARD -> MapLayer.STANDARD
    MapLayerStyle.SATELLITE -> MapLayer.SATELLITE
    MapLayerStyle.TERRAIN -> MapLayer.TERRAIN
    MapLayerStyle.DARK -> MapLayer.DARK
}

object OsmMapUtils {

    const val MIN_MAP_ZOOM = 13.0
    const val MAX_MAP_ZOOM = 18.0
    const val DEFAULT_MAP_ZOOM = 16.0

    private var isInitialized = false

    val MAP_TILE_SOURCE: ITileSource = TileSourceFactory.MAPNIK

    val CARTO_POSITRON: ITileSource get() = MAP_TILE_SOURCE

    val ESRI_SATELLITE_SOURCE: ITileSource by lazy {
        createArcGisTileSource(
            name = "EsriWorldImagery",
            url = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"
        )
    }

    val ESRI_TOPO_SOURCE: ITileSource by lazy {
        createArcGisTileSource(
            name = "EsriWorldTopo",
            url = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/"
        )
    }

    private fun createArcGisTileSource(name: String, url: String): ITileSource = object : OnlineTileSourceBase(
        name,
        TILE_MIN_ZOOM_INT,
        TILE_MAX_ZOOM_INT,
        TILE_SIZE_PX,
        ".jpg",
        arrayOf(url)
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return "$baseUrl$z/$y/$x"
        }
    }

    fun clampZoom(zoom: Double, minZoom: Double = MIN_MAP_ZOOM, maxZoom: Double = MAX_MAP_ZOOM): Double = zoom.coerceIn(minZoom, maxZoom)

    fun initOsmDroid(context: Context) {
        if (isInitialized) return
        val ctx = context.applicationContext
        val cacheDir = File(ctx.cacheDir, "osmdroid")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        runCatching {
            val oldCartoCache = File(cacheDir, "tiles/CartoDB-Positron")
            if (oldCartoCache.exists()) oldCartoCache.deleteRecursively()
        }

        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "tiles")
        Configuration.getInstance().userAgentValue = "${ctx.packageName}/1.2 (Android; Sensus Mobile Map)"
        isInitialized = true
    }

    fun createDefaultMapView(context: Context, minZoom: Double = MIN_MAP_ZOOM, maxZoom: Double = MAX_MAP_ZOOM): MapView {
        initOsmDroid(context)
        return MapView(context).apply {
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            setTileSource(MAP_TILE_SOURCE)
            setMultiTouchControls(true)
            isTilesScaledToDpi = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = minZoom
            maxZoomLevel = maxZoom
            controller.setZoom(DEFAULT_MAP_ZOOM.coerceIn(minZoom, maxZoom))
        }
    }

    fun applyMapLayer(mapView: MapView, layer: MapLayerStyle, minZoom: Double = MIN_MAP_ZOOM, maxZoom: Double = MAX_MAP_ZOOM) {
        val targetSource = when (layer) {
            MapLayerStyle.STANDARD, MapLayerStyle.DARK -> MAP_TILE_SOURCE
            MapLayerStyle.SATELLITE -> ESRI_SATELLITE_SOURCE
            MapLayerStyle.TERRAIN -> ESRI_TOPO_SOURCE
        }
        val currentTag = mapView.tag as? String
        if (mapView.tileProvider.tileSource.name() != targetSource.name()) {
            mapView.setTileSource(targetSource)
        }
        if (currentTag != layer.id) {
            mapView.overlayManager.tilesOverlay.setColorFilter(
                if (layer == MapLayerStyle.DARK) TilesOverlay.INVERT_COLORS else null
            )
            mapView.tag = layer.id
            mapView.invalidate()
        }
        mapView.minZoomLevel = minZoom
        mapView.maxZoomLevel = maxZoom
        val currentZoom = mapView.zoomLevelDouble
        if (currentZoom < minZoom || currentZoom > maxZoom) {
            mapView.controller.setZoom(clampZoom(currentZoom, minZoom, maxZoom))
        }
    }

    fun localizeAndMeasureOffers(userLocation: UserLocation, offers: List<ProductOffer>): List<ProductOffer> {
        val offsets = listOf(
            OFFSET_42M_LAT to OFFSET_42M_LNG,
            OFFSET_88M_LAT to OFFSET_88M_LNG,
            OFFSET_195M_LAT to OFFSET_195M_LNG,
            OFFSET_415M_LAT to OFFSET_415M_LNG,
            OFFSET_1030M_LAT to OFFSET_1030M_LNG,
            OFFSET_1800M_LAT to OFFSET_1800M_LNG,
            OFFSET_2510M_LAT to OFFSET_2510M_LNG,
            OFFSET_3950M_LAT to OFFSET_3950M_LNG
        )
        return offers.mapIndexed { index, offer ->
            val (latOffset, lngOffset) = offsets[index % offsets.size]
            val targetLat = userLocation.latitude + latOffset
            val targetLng = userLocation.longitude + lngOffset
            val distResults = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                targetLat,
                targetLng,
                distResults
            )
            val realMeters = distResults[0].toInt().coerceAtLeast(MIN_OFFER_METERS)
            offer.copy(
                latitude = targetLat,
                longitude = targetLng,
                distanceMeters = realMeters
            )
        }.sortedBy { it.distanceMeters }
    }

    @Suppress("DEPRECATION")
    suspend fun resolveLocationLabel(context: Context, location: UserLocation): String = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) return@withContext "Ubicación actual"
            val geocoder = Geocoder(context, Locale("es", "MX"))
            val addr = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
            formatAddressLabel(
                area = addr?.subLocality ?: addr?.thoroughfare,
                city = addr?.locality ?: addr?.subAdminArea,
                admin = addr?.adminArea
            )
        } catch (_: IOException) {
            "Ubicación actual"
        } catch (_: IllegalArgumentException) {
            "Ubicación actual"
        }
    }

    @Composable
    fun BindMapLifecycle(mapView: MapView?, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current) {
        DisposableEffect(lifecycleOwner, mapView) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                mapView?.onDetach()
            }
        }
    }

    fun createCatMarkerBitmap(context: Context): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (CAT_MARKER_DP * density).toInt()
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = sizePx / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6E4DDB")
            alpha = CAT_HALO_ALPHA
        }
        canvas.drawCircle(center, center, center * CAT_HALO_RATIO, haloPaint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, center * CAT_CIRCLE_RATIO, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#6E4DDB")
            style = Paint.Style.STROKE
            strokeWidth = CAT_BORDER_WIDTH_DP * density
        }
        canvas.drawCircle(center, center, center * CAT_CIRCLE_RATIO, borderPaint)

        val rawCat = BitmapFactory.decodeResource(context.resources, R.drawable.sensus_cat)
        if (rawCat != null) {
            val catSize = (sizePx * CAT_BITMAP_RATIO).toInt()
            val scaledCat = Bitmap.createScaledBitmap(rawCat, catSize, catSize, true)
            val offset = (sizePx - catSize) / 2f
            canvas.drawBitmap(scaledCat, offset, offset, null)
        }
        return bitmap
    }

    fun createStorePinBitmap(context: Context, storeName: String, price: String, isSelected: Boolean): Bitmap {
        val density = context.resources.displayMetrics.density
        val heightPx = (PIN_HEIGHT_DP * density).toInt()
        val textPaint = createPinTextPaint(density, isSelected)
        val pricePaint = createPinPricePaint(density, isSelected)

        val displayName = if (storeName.length > PIN_NAME_LIMIT) storeName.take(PIN_NAME_TRUNCATE) + ".." else storeName
        val nameWidth = textPaint.measureText(displayName)
        val priceWidth = pricePaint.measureText(price)
        val paddingHorizontal = PIN_PADDING_H_DP * density
        val spaceBetween = PIN_SPACE_BETWEEN_DP * density
        val minWidthPx = (PIN_MIN_WIDTH_DP * density).toInt()
        val widthPx = (nameWidth + priceWidth + (paddingHorizontal * 2) + spaceBetween).toInt().coerceAtLeast(minWidthPx)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawPinBackgroundAndBorder(canvas, density, widthPx, heightPx, isSelected)

        val baselineY = (heightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(displayName, paddingHorizontal, baselineY, textPaint)
        canvas.drawText(price, paddingHorizontal + nameWidth + spaceBetween, baselineY, pricePaint)
        return bitmap
    }
}

private fun formatAddressLabel(area: String?, city: String?, admin: String?): String = when {
    !area.isNullOrBlank() && !city.isNullOrBlank() -> "$area, $city"
    !city.isNullOrBlank() -> city
    !admin.isNullOrBlank() -> admin
    else -> "Ubicación actual"
}

private fun createPinTextPaint(density: Float, isSelected: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#1E293B")
    textSize = PIN_TEXT_SIZE_SP * density
    typeface = Typeface.DEFAULT_BOLD
}

private fun createPinPricePaint(density: Float, isSelected: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#6E4DDB")
    textSize = PIN_PRICE_SIZE_SP * density
    typeface = Typeface.DEFAULT_BOLD
}

private fun drawPinBackgroundAndBorder(canvas: Canvas, density: Float, widthPx: Int, heightPx: Int, isSelected: Boolean) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#6E4DDB") else android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val rect = RectF(PIN_RECT_INSET, PIN_RECT_INSET, widthPx - PIN_RECT_INSET, heightPx - PIN_RECT_INSET)
    val corner = PIN_CORNER_DP * density
    canvas.drawRoundRect(rect, corner, corner, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#5A3CB8") else android.graphics.Color.parseColor("#E2E8F0")
        style = Paint.Style.STROKE
        strokeWidth = PIN_BORDER_WIDTH_DP * density
    }
    canvas.drawRoundRect(rect, corner, corner, borderPaint)
}
