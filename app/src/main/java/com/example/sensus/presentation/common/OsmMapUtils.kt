package com.example.sensus.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.text.TextPaint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.sensus.R
import com.example.sensus.domain.model.MapLayer
import com.example.sensus.domain.model.ProductOffer
import com.example.sensus.domain.model.UserLocation
import com.example.sensus.theme.BrandViolet
import com.example.sensus.theme.BrandVioletDark
import com.example.sensus.theme.Ink
import com.example.sensus.theme.LightBorder
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
private const val PIN_TAIL_DP = 6
private const val PIN_TAIL_HALF_WIDTH_DP = 6f
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
        return offers.map { offer ->
            val distResults = FloatArray(1)
            Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                offer.latitude,
                offer.longitude,
                distResults
            )
            val measuredMeters = distResults[0].toInt()
            val realMeters = when {
                measuredMeters > 0 -> measuredMeters
                offer.distanceMeters > 0 -> offer.distanceMeters
                else -> MIN_OFFER_METERS
            }
            offer.copy(distanceMeters = realMeters)
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
            color = BrandViolet.toArgb()
            alpha = CAT_HALO_ALPHA
        }
        canvas.drawCircle(center, center, center * CAT_HALO_RATIO, haloPaint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(center, center, center * CAT_CIRCLE_RATIO, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BrandViolet.toArgb()
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
        val pillHeightPx = (PIN_HEIGHT_DP * density).toInt()
        val tailPx = (PIN_TAIL_DP * density).toInt()
        val totalHeightPx = pillHeightPx + tailPx
        val textPaint = createPinTextPaint(density, isSelected)
        val pricePaint = createPinPricePaint(density, isSelected)

        val displayName = if (storeName.length > PIN_NAME_LIMIT) storeName.take(PIN_NAME_TRUNCATE) + ".." else storeName
        val nameWidth = textPaint.measureText(displayName)
        val priceWidth = pricePaint.measureText(price)
        val paddingHorizontal = PIN_PADDING_H_DP * density
        val spaceBetween = PIN_SPACE_BETWEEN_DP * density
        val minWidthPx = (PIN_MIN_WIDTH_DP * density).toInt()
        val widthPx = (nameWidth + priceWidth + (paddingHorizontal * 2) + spaceBetween).toInt().coerceAtLeast(minWidthPx)

        val bitmap = Bitmap.createBitmap(widthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawPinBackgroundAndBorder(canvas, density, widthPx, pillHeightPx, isSelected)
        drawPinPointerTail(canvas, density, PinTailBounds(widthPx, pillHeightPx, totalHeightPx), isSelected)

        val baselineY = (pillHeightPx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
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
    color = if (isSelected) android.graphics.Color.WHITE else Ink.toArgb()
    textSize = PIN_TEXT_SIZE_SP * density
    typeface = Typeface.DEFAULT_BOLD
}

private fun createPinPricePaint(density: Float, isSelected: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    color = if (isSelected) android.graphics.Color.WHITE else BrandViolet.toArgb()
    textSize = PIN_PRICE_SIZE_SP * density
    typeface = Typeface.DEFAULT_BOLD
}

private fun drawPinBackgroundAndBorder(canvas: Canvas, density: Float, widthPx: Int, heightPx: Int, isSelected: Boolean) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) BrandViolet.toArgb() else android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val rect = RectF(PIN_RECT_INSET, PIN_RECT_INSET, widthPx - PIN_RECT_INSET, heightPx - PIN_RECT_INSET)
    val corner = PIN_CORNER_DP * density
    canvas.drawRoundRect(rect, corner, corner, bgPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) BrandVioletDark.toArgb() else LightBorder.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = PIN_BORDER_WIDTH_DP * density
    }
    canvas.drawRoundRect(rect, corner, corner, borderPaint)
}

private data class PinTailBounds(
    val widthPx: Int,
    val pillHeightPx: Int,
    val totalHeightPx: Int
)

private fun drawPinPointerTail(canvas: Canvas, density: Float, bounds: PinTailBounds, isSelected: Boolean) {
    val centerX = bounds.widthPx / 2f
    val halfBase = PIN_TAIL_HALF_WIDTH_DP * density
    val overlapY = bounds.pillHeightPx - PIN_RECT_INSET
    val tipY = bounds.totalHeightPx - PIN_RECT_INSET
    val tailPath = Path().apply {
        moveTo(centerX - halfBase, overlapY)
        lineTo(centerX + halfBase, overlapY)
        lineTo(centerX, tipY)
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) BrandViolet.toArgb() else android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) BrandVioletDark.toArgb() else LightBorder.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = PIN_BORDER_WIDTH_DP * density
    }
    canvas.drawPath(tailPath, fillPaint)
    canvas.drawLine(centerX - halfBase, overlapY, centerX, tipY, strokePaint)
    canvas.drawLine(centerX + halfBase, overlapY, centerX, tipY, strokePaint)
}
